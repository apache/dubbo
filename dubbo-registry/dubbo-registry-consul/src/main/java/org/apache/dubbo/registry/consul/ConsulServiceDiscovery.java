/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.registry.consul;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.event.EventListener;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;

import com.ecwid.consul.v1.ConsistencyMode;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.catalog.CatalogServicesRequest;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SEPARATOR_CHAR;
import static org.apache.dubbo.common.constants.CommonConstants.SEMICOLON_SPLIT_PATTERN;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.CHECK_PASS_INTERVAL;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.DEFAULT_CHECK_PASS_INTERVAL;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.DEFAULT_DEREGISTER_TIME;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.DEFAULT_PORT;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.DEFAULT_WATCH_TIMEOUT;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.DEREGISTER_AFTER;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.WATCH_TIMEOUT;
import static org.apache.dubbo.registry.consul.ConsulParameter.ACL_TOKEN;
import static org.apache.dubbo.registry.consul.ConsulParameter.CONSISTENCY_MODE;
import static org.apache.dubbo.registry.consul.ConsulParameter.DEFAULT_ZONE_METADATA_NAME;
import static org.apache.dubbo.registry.consul.ConsulParameter.INSTANCE_GROUP;
import static org.apache.dubbo.registry.consul.ConsulParameter.INSTANCE_ZONE;
import static org.apache.dubbo.registry.consul.ConsulParameter.TAGS;

/**
 * 2019-07-31
 */
public class ConsulServiceDiscovery implements ServiceDiscovery, EventListener<ServiceInstancesChangedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ConsulServiceDiscovery.class);

    private static final String QUERY_TAG = "consul_query_tag";
    private static final String REGISTER_TAG = "consul_register_tag";

    private List<String> registeringTags = new ArrayList<>();
    private String tag;
    private ConsulClient client;
    private ExecutorService notifierExecutor = newCachedThreadPool(
            new NamedThreadFactory("dubbo-service-discovery-consul-notifier", true));
    private ConsulNotifier notifier;
    private TtlScheduler ttlScheduler;
    private long checkPassInterval;
    private URL url;

    private String aclToken;

    private List<String> tags;

    private ConsistencyMode consistencyMode;

    private String defaultZoneMetadataName;

    /**
     * Service instance zone.
     */
    private String instanceZone;

    /**
     * Service instance group.
     */
    private String instanceGroup;


    @Override
    public void onEvent(ServiceInstancesChangedEvent event) {

    }

    @Override
    public void initialize(URL registryURL) throws Exception {
        this.url = registryURL;
        String host = url.getHost();
        int port = url.getPort() != 0 ? url.getPort() : DEFAULT_PORT;
        checkPassInterval = url.getParameter(CHECK_PASS_INTERVAL, DEFAULT_CHECK_PASS_INTERVAL);
        client = new ConsulClient(host, port);
        ttlScheduler = new TtlScheduler(checkPassInterval, client);
        this.tag = registryURL.getParameter(QUERY_TAG);
        this.registeringTags.addAll(getRegisteringTags(url));
        this.aclToken = ACL_TOKEN.getValue(registryURL);
        this.tags = getTags(registryURL);
        this.consistencyMode = getConsistencyMode(registryURL);
        this.defaultZoneMetadataName = DEFAULT_ZONE_METADATA_NAME.getValue(registryURL);
        this.instanceZone = INSTANCE_ZONE.getValue(registryURL);
        this.instanceGroup = INSTANCE_GROUP.getValue(registryURL);
    }

    /**
     * Get the {@link ConsistencyMode}
     *
     * @param registryURL the {@link URL} of registry
     * @return non-null, {@link ConsistencyMode#DEFAULT} as default
     * @sine 2.7.8
     */
    private ConsistencyMode getConsistencyMode(URL registryURL) {
        String value = CONSISTENCY_MODE.getValue(registryURL);
        if (StringUtils.isNotEmpty(value)) {
            return ConsistencyMode.valueOf(value);
        }
        return ConsistencyMode.DEFAULT;
    }

    /**
     * Get the "tags" from the {@link URL} of registry
     *
     * @param registryURL the {@link URL} of registry
     * @return non-null
     * @sine 2.7.8
     */
    private List<String> getTags(URL registryURL) {
        String value = TAGS.getValue(registryURL);
        return StringUtils.splitToList(value, COMMA_SEPARATOR_CHAR);
    }

    private List<String> getRegisteringTags(URL url) {
        List<String> tags = new ArrayList<>();
        String rawTag = url.getParameter(REGISTER_TAG);
        if (StringUtils.isNotEmpty(rawTag)) {
            tags.addAll(Arrays.asList(SEMICOLON_SPLIT_PATTERN.split(rawTag)));
        }
        return tags;
    }

    @Override
    public void destroy() {
        if (notifier != null) {
            notifier.stop();
        }
        notifier = null;
        notifierExecutor.shutdownNow();
        ttlScheduler.stop();
    }

    @Override
    public void register(ServiceInstance serviceInstance) throws RuntimeException {
        NewService consulService = buildService(serviceInstance);
        ttlScheduler.add(consulService.getId());
        client.agentServiceRegister(consulService, aclToken);
    }

    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws NullPointerException, IllegalArgumentException {
        if (notifier == null) {
            String serviceName = listener.getServiceName();
            Response<List<HealthService>> response = getHealthServices(serviceName, -1, buildWatchTimeout());
            Long consulIndex = response.getConsulIndex();
            notifier = new ConsulNotifier(serviceName, consulIndex);
        }
        notifierExecutor.execute(notifier);
    }

    @Override
    public void update(ServiceInstance serviceInstance) throws RuntimeException {
        // TODO
        // client.catalogRegister(buildCatalogService(serviceInstance));
    }

    @Override
    public void unregister(ServiceInstance serviceInstance) throws RuntimeException {
        String id = buildId(serviceInstance);
        ttlScheduler.remove(id);
        client.agentServiceDeregister(id, aclToken);
    }

    @Override
    public Set<String> getServices() {
        CatalogServicesRequest request = CatalogServicesRequest.newBuilder()
                .setQueryParams(QueryParams.DEFAULT)
                .setToken(aclToken)
                .build();
        return this.client.getCatalogServices(request).getValue().keySet();
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws NullPointerException {
        Response<List<HealthService>> response = getHealthServices(serviceName, -1, buildWatchTimeout());
        Long consulIndex = response.getConsulIndex();
        if (notifier == null) {
            notifier = new ConsulNotifier(serviceName, consulIndex);
        }
        return convert(response.getValue());
    }

    private List<ServiceInstance> convert(List<HealthService> services) {
        return services.stream()
                .map(HealthService::getService)
                .map(service -> {
                    ServiceInstance instance = new DefaultServiceInstance(
                            service.getId(),
                            service.getService(),
                            service.getAddress(),
                            service.getPort());
                    instance.getMetadata().putAll(getMetadata(service));
                    return instance;
                })
                .collect(Collectors.toList());
    }

    private Response<List<HealthService>> getHealthServices(String service, long index, int watchTimeout) {
        HealthServicesRequest request = HealthServicesRequest.newBuilder()
                .setTag(tag)
                .setQueryParams(new QueryParams(watchTimeout, index))
                .setPassing(true)
                .build();
        return client.getHealthServices(service, request);
    }

    private Map<String, String> getMetadata(HealthService.Service service) {
        Map<String, String> metadata = service.getMeta();
        metadata = decodeMetadata(metadata);
        if (CollectionUtils.isEmptyMap(metadata)) {
            metadata = getScCompatibleMetadata(service.getTags());
        }
        return metadata;
    }

    private Map<String, String> getScCompatibleMetadata(List<String> tags) {
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        if (tags != null) {
            for (String tag : tags) {
                String[] parts = StringUtils.delimitedListToStringArray(tag, "=");
                switch (parts.length) {
                    case 0:
                        break;
                    case 1:
                        metadata.put(parts[0], parts[0]);
                        break;
                    case 2:
                        metadata.put(parts[0], parts[1]);
                        break;
                    default:
                        String[] end = Arrays.copyOfRange(parts, 1, parts.length);
                        metadata.put(parts[0], StringUtils.arrayToDelimitedString(end, "="));
                        break;
                }

            }
        }

        return metadata;
    }

    private NewService buildService(ServiceInstance serviceInstance) {
        NewService service = new NewService();
        service.setAddress(serviceInstance.getHost());
        service.setPort(serviceInstance.getPort());
        service.setId(buildId(serviceInstance));
        service.setName(serviceInstance.getServiceName());
        service.setCheck(buildCheck(serviceInstance));
        service.setTags(buildTags(serviceInstance));
        return service;
    }

    private String buildId(ServiceInstance serviceInstance) {
        return Integer.toHexString(serviceInstance.hashCode());
    }

    private List<String> buildTags(ServiceInstance serviceInstance) {
        List<String> tags = new LinkedList<>(this.tags);

        if (StringUtils.isNotEmpty(instanceZone)) {
            tags.add(defaultZoneMetadataName + "=" + instanceZone);
        }

        if (StringUtils.isNotEmpty(instanceGroup)) {
            tags.add("group=" + instanceGroup);
        }

        Map<String, String> params = serviceInstance.getMetadata();
        params.keySet().stream()
                .map(k -> k + "=" + params.get(k))
                .forEach(tags::add);

        tags.addAll(registeringTags);
        return tags;
    }

    private Map<String, String> buildMetadata(ServiceInstance serviceInstance) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.putAll(getScCompatibleMetadata(registeringTags));
        if (CollectionUtils.isNotEmptyMap(serviceInstance.getMetadata())) {
            metadata.putAll(serviceInstance.getMetadata());
        }
        metadata = encodeMetadata(metadata);
        return metadata;
    }

    private Map<String, String> encodeMetadata(Map<String, String> metadata) {
        if (metadata == null) {
            return metadata;
        }
        Map<String, String> encoded = new HashMap<>(metadata.size());
        metadata.forEach((k, v) -> encoded.put(Base64.getEncoder().encodeToString(k.getBytes()), v));
        return encoded;
    }

    private Map<String, String> decodeMetadata(Map<String, String> metadata) {
        if (metadata == null) {
            return metadata;
        }
        Map<String, String> decoded = new HashMap<>(metadata.size());
        metadata.forEach((k, v) -> decoded.put(new String(Base64.getDecoder().decode(k)), v));
        return decoded;
    }

    private NewService.Check buildCheck(ServiceInstance serviceInstance) {
        NewService.Check check = new NewService.Check();
        check.setTtl((checkPassInterval / 1000) + "s");
        String deregister = serviceInstance.getMetadata().get(DEREGISTER_AFTER);
        check.setDeregisterCriticalServiceAfter(deregister == null ? DEFAULT_DEREGISTER_TIME : deregister);
        return check;
    }

    private int buildWatchTimeout() {
        return url.getParameter(WATCH_TIMEOUT, DEFAULT_WATCH_TIMEOUT) / 1000;
    }

    private class ConsulNotifier implements Runnable {
        private String serviceName;
        private long consulIndex;
        private boolean running;

        ConsulNotifier(String serviceName, long consulIndex) {
            this.serviceName = serviceName;
            this.consulIndex = consulIndex;
            this.running = true;
        }

        @Override
        public void run() {
            while (this.running) {
                processService();
            }
        }

        private void processService() {
            Response<List<HealthService>> response = getHealthServices(serviceName, consulIndex, Integer.MAX_VALUE);
            Long currentIndex = response.getConsulIndex();
            if (currentIndex != null && currentIndex > consulIndex) {
                consulIndex = currentIndex;
                List<HealthService> services = response.getValue();
                List<ServiceInstance> serviceInstances = convert(services);
                dispatchServiceInstancesChangedEvent(serviceName, serviceInstances);
            }
        }

        void stop() {
            this.running = false;
        }
    }

    private static class TtlScheduler {

        private static final Logger logger = LoggerFactory.getLogger(TtlScheduler.class);

        private final Map<String, ScheduledFuture> serviceHeartbeats = new ConcurrentHashMap<>();

        private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        private long checkInterval;

        private ConsulClient client;

        public TtlScheduler(long checkInterval, ConsulClient client) {
            this.checkInterval = checkInterval;
            this.client = client;
        }

        /**
         * Add a service to the checks loop.
         *
         * @param instanceId instance id
         */
        public void add(String instanceId) {
            ScheduledFuture task = this.scheduler.scheduleAtFixedRate(
                    new ConsulHeartbeatTask(instanceId),
                    checkInterval / 8,
                    checkInterval / 8,
                    TimeUnit.MILLISECONDS);
            ScheduledFuture previousTask = this.serviceHeartbeats.put(instanceId, task);
            if (previousTask != null) {
                previousTask.cancel(true);
            }
        }

        public void remove(String instanceId) {
            ScheduledFuture task = this.serviceHeartbeats.get(instanceId);
            if (task != null) {
                task.cancel(true);
            }
            this.serviceHeartbeats.remove(instanceId);
        }

        private class ConsulHeartbeatTask implements Runnable {

            private String checkId;

            ConsulHeartbeatTask(String serviceId) {
                this.checkId = serviceId;
                if (!this.checkId.startsWith("service:")) {
                    this.checkId = "service:" + this.checkId;
                }
            }

            @Override
            public void run() {
                TtlScheduler.this.client.agentCheckPass(this.checkId);
                if (logger.isDebugEnabled()) {
                    logger.debug("Sending consul heartbeat for: " + this.checkId);
                }
            }

        }

        public void stop() {
            scheduler.shutdownNow();
        }

    }
}
