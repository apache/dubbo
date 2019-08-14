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
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.event.EventListener;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.CHECK_PASS_INTERVAL;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.DEFAULT_CHECK_PASS_INTERVAL;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.DEFAULT_DEREGISTER_TIME;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.DEFAULT_PORT;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.DEFAULT_WATCH_TIMEOUT;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.DEREGISTER_AFTER;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.SERVICE_TAG;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.URL_META_KEY;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.WATCH_TIMEOUT;

/**
 * 2019-07-31
 */
public class ConsulServiceDiscovery implements ServiceDiscovery, EventListener<ServiceInstancesChangedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ConsulServiceDiscovery.class);

    private ConsulClient client;
    private ExecutorService notifierExecutor = newCachedThreadPool(
            new NamedThreadFactory("dubbo-consul-notifier", true));
    private TtlScheduler ttlScheduler;
    private long checkPassInterval;
    private URL url;

    public ConsulServiceDiscovery(URL url) {
        String host = url.getHost();
        int port = url.getPort() != 0 ? url.getPort() : DEFAULT_PORT;
        checkPassInterval = url.getParameter(CHECK_PASS_INTERVAL, DEFAULT_CHECK_PASS_INTERVAL);
        client = new ConsulClient(host, port);
        ttlScheduler = new TtlScheduler(checkPassInterval, client);
        this.url = url;
    }

    @Override
    public void onEvent(ServiceInstancesChangedEvent event) {

    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void register(ServiceInstance serviceInstance) throws RuntimeException {
        NewService consulService = buildService(serviceInstance);
        ttlScheduler.add(consulService.getId());
        client.agentServiceRegister(consulService);
    }

    @Override
    public void update(ServiceInstance serviceInstance) throws RuntimeException {

    }

    @Override
    public void unregister(ServiceInstance serviceInstance) throws RuntimeException {
        String id = buildId(serviceInstance);
        ttlScheduler.remove(id);
        client.agentServiceDeregister(id);
    }

    @Override
    public Set<String> getServices() {
        return null;
    }

    @Override
    public void addServiceInstancesChangedListener(String serviceName, ServiceInstancesChangedListener listener) throws NullPointerException, IllegalArgumentException {

    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws NullPointerException {
        Response<List<HealthService>> response = getHealthServices(serviceName, -1, buildWatchTimeout());
        return convert(response.getValue());
    }

    private List<ServiceInstance> convert(List<HealthService> services) {
        return services.stream()
                .map(HealthService::getService)
                .filter(service -> Objects.nonNull(service) && service.getMeta().containsKey(ServiceInstanceMetadataUtils.METADATA_SERVICE_URL_PARAMS_KEY))
                .map(service -> {
                    ServiceInstance instance = new DefaultServiceInstance(
                            service.getService(),
                            service.getAddress(),
                            service.getPort());
                    instance.getMetadata().putAll(service.getMeta());
                    return instance;
                })
                .collect(Collectors.toList());
    }

    private Response<List<HealthService>> getHealthServices(String service, long index, int watchTimeout) {
        HealthServicesRequest request = HealthServicesRequest.newBuilder()
                .setTag(SERVICE_TAG)
                .setQueryParams(new QueryParams(watchTimeout, index))
                .setPassing(true)
                .build();
        return client.getHealthServices(service, request);
    }

    private NewService buildService(ServiceInstance serviceInstance) {
        NewService service = new NewService();
        service.setAddress(serviceInstance.getHost());
        service.setPort(serviceInstance.getPort());
        service.setId(buildId(serviceInstance));
        service.setName(serviceInstance.getServiceName());
        service.setCheck(buildCheck(serviceInstance));
        service.setTags(buildTags(serviceInstance));
        service.setMeta(Collections.singletonMap(URL_META_KEY, serviceInstance.toString()));
        return service;
    }

    private String buildId(ServiceInstance serviceInstance) {
        return Integer.toHexString(serviceInstance.hashCode());
    }

    private List<String> buildTags(ServiceInstance serviceInstance) {
        Map<String, String> params = serviceInstance.getMetadata();
        List<String> tags = params.keySet().stream()
                .map(k -> k + "=" + params.get(k))
                .collect(Collectors.toList());
        tags.add(SERVICE_TAG);
        return tags;
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
        private ServiceInstance serviceInstance;
        private long consulIndex;
        private boolean running;

        ConsulNotifier(ServiceInstance serviceInstance, long consulIndex) {
            this.serviceInstance = serviceInstance;
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
//            String service = url.getServiceKey();
//            Response<List<HealthService>> response = getHealthServices(service, consulIndex, buildWatchTimeout(url));
//            Long currentIndex = response.getConsulIndex();
//            if (currentIndex != null && currentIndex > consulIndex) {
//                consulIndex = currentIndex;
//                List<HealthService> services = response.getValue();
//                List<URL> urls = convert(services, url);
//                for (NotifyListener listener : getSubscribed().get(url)) {
//                    doNotify(url, listener, urls);
//                }
//            }
        }

        void stop() {
            this.running = false;
        }
    }

    private static class TtlScheduler {

        private static final Logger logger = LoggerFactory.getLogger(TtlScheduler.class);

        private final Map<String, ScheduledFuture> serviceHeartbeats = new ConcurrentHashMap<>();

        private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        ;

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
                if (logger.isInfoEnabled()) {
                    logger.info("Sending consul heartbeat for: " + this.checkId);
                }
            }

        }

    }
}
