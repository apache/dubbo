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
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.support.FailbackRegistry;
import org.apache.dubbo.rpc.RpcException;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.catalog.CatalogServicesRequest;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.apache.dubbo.common.constants.CommonConstants.ANY_VALUE;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;
import static org.apache.dubbo.registry.Constants.CONSUMER_PROTOCOL;
import static org.apache.dubbo.registry.Constants.PROVIDER_PROTOCOL;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.CHECK_PASS_INTERVAL;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.DEFAULT_CHECK_PASS_INTERVAL;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.DEFAULT_DEREGISTER_TIME;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.DEFAULT_PORT;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.DEFAULT_WATCH_TIMEOUT;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.DEREGISTER_AFTER;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.SERVICE_TAG;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.URL_META_KEY;
import static org.apache.dubbo.registry.consul.AbstractConsulRegistry.WATCH_TIMEOUT;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;

/**
 * registry center implementation for consul
 */
public class ConsulRegistry extends FailbackRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ConsulRegistry.class);

    private ConsulClient client;
    private long checkPassInterval;
    private ExecutorService notifierExecutor = newCachedThreadPool(
            new NamedThreadFactory("dubbo-consul-notifier", true));
    private ConcurrentMap<URL, ConsulNotifier> notifiers = new ConcurrentHashMap<>();
    private ScheduledExecutorService ttlConsulCheckExecutor;
    /**
     * The ACL token
     */
    private String token;


    public ConsulRegistry(URL url) {
        super(url);
        token = url.getParameter(TOKEN_KEY, (String) null);
        String host = url.getHost();
        int port = url.getPort() != 0 ? url.getPort() : DEFAULT_PORT;
        client = new ConsulClient(host, port);
        checkPassInterval = url.getParameter(CHECK_PASS_INTERVAL, DEFAULT_CHECK_PASS_INTERVAL);
        ttlConsulCheckExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("Ttl-Consul-Check-Executor", true));
        ttlConsulCheckExecutor.scheduleAtFixedRate(this::checkPass, checkPassInterval / 8,
                checkPassInterval / 8, TimeUnit.MILLISECONDS);
    }

    @Override
    public void register(URL url) {
        if (isConsumerSide(url)) {
            return;
        }

        super.register(url);
    }

    @Override
    public void doRegister(URL url) {
        if (token == null) {
            client.agentServiceRegister(buildService(url));
        } else {
            client.agentServiceRegister(buildService(url), token);
        }
    }

    @Override
    public void unregister(URL url) {
        if (isConsumerSide(url)) {
            return;
        }

        super.unregister(url);
    }

    @Override
    public void doUnregister(URL url) {
        if (token == null) {
            client.agentServiceDeregister(buildId(url));
        } else {
            client.agentServiceDeregister(buildId(url), token);
        }
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        if (isProviderSide(url)) {
            return;
        }

        super.subscribe(url, listener);
    }

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {
        Long index;
        List<URL> urls;
        if (ANY_VALUE.equals(url.getServiceInterface())) {
            Response<Map<String, List<String>>> response = getAllServices(-1, buildWatchTimeout(url));
            index = response.getConsulIndex();
            List<HealthService> services = getHealthServices(response.getValue());
            urls = convert(services, url);
        } else {
            String service = url.getServiceInterface();
            Response<List<HealthService>> response = getHealthServices(service, -1, buildWatchTimeout(url));
            index = response.getConsulIndex();
            urls = convert(response.getValue(), url);
        }

        notify(url, listener, urls);
        ConsulNotifier notifier = notifiers.computeIfAbsent(url, k -> new ConsulNotifier(url, index));
        notifierExecutor.submit(notifier);
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        if (isProviderSide(url)) {
            return;
        }

        super.unsubscribe(url, listener);
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        ConsulNotifier notifier = notifiers.remove(url);
        notifier.stop();
    }

    @Override
    public List<URL> lookup(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("lookup url == null");
        }
        try {
            String service = url.getServiceKey();
            Response<List<HealthService>> result = getHealthServices(service, -1, buildWatchTimeout(url));
            if (result == null || result.getValue() == null || result.getValue().isEmpty()) {
                return new ArrayList<>();
            } else {
                return convert(result.getValue(), url);
            }
        } catch (Throwable e) {
            throw new RpcException("Failed to lookup " + url + " from consul " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return client.getAgentSelf() != null;
    }

    @Override
    public void destroy() {
        super.destroy();
        notifierExecutor.shutdown();
        ttlConsulCheckExecutor.shutdown();
    }

    private void checkPass() {
        for (URL url : getRegistered()) {
            String checkId = buildId(url);
            try {
                if (token == null) {
                    client.agentCheckPass("service:" + checkId);
                } else {
                    client.agentCheckPass("service:" + checkId, null, token);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("check pass for url: " + url + " with check id: " + checkId);
                }
            } catch (Throwable t) {
                logger.warn("fail to check pass for url: " + url + ", check id is: " + checkId, t);
            }
        }
    }

    private Response<List<HealthService>> getHealthServices(String service, long index, int watchTimeout) {
        HealthServicesRequest request = HealthServicesRequest.newBuilder()
                .setTag(SERVICE_TAG)
                .setQueryParams(new QueryParams(watchTimeout, index))
                .setPassing(true)
                .setToken(token)
                .build();
        return client.getHealthServices(service, request);
    }

    private Response<Map<String, List<String>>> getAllServices(long index, int watchTimeout) {
        CatalogServicesRequest request = CatalogServicesRequest.newBuilder()
                .setQueryParams(new QueryParams(watchTimeout, index))
                .setToken(token)
                .build();
        return client.getCatalogServices(request);
    }

    private List<HealthService> getHealthServices(Map<String, List<String>> services) {
        return services.entrySet().stream()
                .filter(s -> s.getValue().contains(SERVICE_TAG))
                .map(s -> getHealthServices(s.getKey(), -1, -1).getValue())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }


    private boolean isConsumerSide(URL url) {
        return url.getProtocol().equals(CONSUMER_PROTOCOL);
    }

    private boolean isProviderSide(URL url) {
        return url.getProtocol().equals(PROVIDER_PROTOCOL);
    }

    private List<URL> convert(List<HealthService> services, URL consumerURL) {
        if (CollectionUtils.isEmpty(services)) {
            return emptyURL(consumerURL);
        }
        return services.stream()
                .map(HealthService::getService)
                .filter(Objects::nonNull)
                .map(HealthService.Service::getMeta)
                .filter(m -> m != null && m.containsKey(URL_META_KEY))
                .map(m -> m.get(URL_META_KEY))
                .map(URL::valueOf)
                .filter(url -> UrlUtils.isMatch(consumerURL, url))
                .collect(Collectors.toList());
    }

    private List<URL> emptyURL(URL consumerURL) {
        // No Category Parameter
        URL empty = URLBuilder.from(consumerURL)
                .setProtocol(EMPTY_PROTOCOL)
                .removeParameter(CATEGORY_KEY)
                .build();
        List<URL> result = new ArrayList<URL>();
        result.add(empty);
        return result;
    }

    private NewService buildService(URL url) {
        NewService service = new NewService();
        service.setAddress(url.getHost());
        service.setPort(url.getPort());
        service.setId(buildId(url));
        service.setName(url.getServiceInterface());
        service.setCheck(buildCheck(url));
        service.setTags(buildTags(url));
        service.setMeta(Collections.singletonMap(URL_META_KEY, url.toFullString()));
        return service;
    }

    private List<String> buildTags(URL url) {
        Map<String, String> params = url.getParameters();
        List<String> tags = params.entrySet().stream()
                .map(k -> k.getKey() + "=" + k.getValue())
                .collect(Collectors.toList());
        tags.add(SERVICE_TAG);
        return tags;
    }

    private String buildId(URL url) {
        // let's simply use url's hashcode to generate unique service id for now
        return Integer.toHexString(url.hashCode());
    }

    private NewService.Check buildCheck(URL url) {
        NewService.Check check = new NewService.Check();
        check.setTtl((checkPassInterval / 1000) + "s");
        check.setDeregisterCriticalServiceAfter(url.getParameter(DEREGISTER_AFTER, DEFAULT_DEREGISTER_TIME));
        return check;
    }

    private int buildWatchTimeout(URL url) {
        return url.getParameter(WATCH_TIMEOUT, DEFAULT_WATCH_TIMEOUT) / 1000;
    }

    private class ConsulNotifier implements Runnable {
        private URL url;
        private long consulIndex;
        private boolean running;

        ConsulNotifier(URL url, long consulIndex) {
            this.url = url;
            this.consulIndex = consulIndex;
            this.running = true;
        }

        @Override
        public void run() {
            while (this.running) {
                if (ANY_VALUE.equals(url.getServiceInterface())) {
                    processServices();
                } else {
                    processService();
                }
            }
        }

        private void processService() {
            String service = url.getServiceKey();
            Response<List<HealthService>> response = getHealthServices(service, consulIndex, buildWatchTimeout(url));
            Long currentIndex = response.getConsulIndex();
            if (currentIndex != null && currentIndex > consulIndex) {
                consulIndex = currentIndex;
                List<HealthService> services = response.getValue();
                List<URL> urls = convert(services, url);
                for (NotifyListener listener : getSubscribed().get(url)) {
                    doNotify(url, listener, urls);
                }
            }
        }

        private void processServices() {
            Response<Map<String, List<String>>> response = getAllServices(consulIndex, buildWatchTimeout(url));
            Long currentIndex = response.getConsulIndex();
            if (currentIndex != null && currentIndex > consulIndex) {
                consulIndex = currentIndex;
                List<HealthService> services = getHealthServices(response.getValue());
                List<URL> urls = convert(services, url);
                for (NotifyListener listener : getSubscribed().get(url)) {
                    doNotify(url, listener, urls);
                }
            }
        }

        void stop() {
            this.running = false;
        }
    }
}
