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
package org.apache.dubbo.registry.kubernetes;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.support.FailbackRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.ANY_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.registry.Constants.CONSUMER_PROTOCOL;
import static org.apache.dubbo.registry.Constants.PROVIDER_PROTOCOL;

/**
 * registry center implementation for kubernetes
 */
public class KubernetesRegistry extends FailbackRegistry {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesRegistry.class);

    private final String namespaces;

    private final String podWithLabel;

    private final KubernetesClient kubernetesClient;

    private final static String FULL_URL = "full_url";

    private static final String MARK = "mark";

    private static final String APP_LABEL = "app";

    private static final String SERVICE_KEY_PREFIX = "dubbo_service_";

    private static final ScheduledExecutorService KUBERNETS_EVENT_EXECUTOR = Executors.newScheduledThreadPool(8, new NamedThreadFactory("kubernetes-event-thread"));

    private final Map<URL, Watch> kubernetesWatcherMap = new ConcurrentHashMap<>(16);

    public KubernetesRegistry(KubernetesClient kubernetesClient, URL url, String namespaces, String podWithLabel) {
        super(url);
        this.kubernetesClient = kubernetesClient;
        this.namespaces = namespaces;
        this.podWithLabel = podWithLabel;
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
        List<Pod> pods = queryPodsByUnRegistryUrl(url);
        if (pods != null && pods.size() > 0) {
            pods.forEach(pod -> register(url, pod));
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
        List<Pod> pods = queryPodNameByRegistriedUrl(url);
        if (pods != null && pods.size() > 0) {
            pods.forEach(pod -> unregister(pod, url));
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
    public void doSubscribe(URL url, NotifyListener notifyListener) {
        final List<URL> urls = queryUrls(url);
        this.notify(url, notifyListener, urls);

        kubernetesWatcherMap.computeIfAbsent(url, k ->
                kubernetesClient.pods().inNamespace(namespaces).withLabel(APP_LABEL, podWithLabel)
                        .watch(new Watcher<Pod>() {
                            @Override
                            public void eventReceived(Action action, Pod pod) {
                                if (action == Action.ADDED || action == Action.DELETED) {
                                    KUBERNETS_EVENT_EXECUTOR.schedule(() -> {
                                        final List<URL> urlList = queryUrls(url);
                                        doNotify(url, notifyListener, urlList);
                                    }, 5, TimeUnit.SECONDS);
                                }
                            }

                            @Override
                            public void onClose(KubernetesClientException e) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("pod watch closed");
                                }
                                if (e != null) {
                                    logger.error("watcher onClose exception", e);
                                }
                            }
                        }));
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        if (isProviderSide(url)) {
            return;
        }

        super.unsubscribe(url, listener);
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener notifyListener) {
        Watch watch = kubernetesWatcherMap.remove(url);
        watch.close();
    }

    @Override
    public boolean isAvailable() {
        return CollectionUtils.isNotEmpty(getAllRunningService());
    }

    @Override
    public void destroy() {
        super.destroy();
        Collection<URL> urls = Collections.unmodifiableSet(kubernetesWatcherMap.keySet());
        urls.forEach(url -> {
            Watch watch = kubernetesWatcherMap.remove(url);
            watch.close();
        });
        KUBERNETS_EVENT_EXECUTOR.shutdown();
    }

    private boolean isConsumerSide(URL url) {
        return url.getProtocol().equals(CONSUMER_PROTOCOL);
    }

    private boolean isProviderSide(URL url) {
        return url.getProtocol().equals(PROVIDER_PROTOCOL);
    }

    private void register(URL url, Pod pod) {
        JSONObject meta = new JSONObject() {{
            put(INTERFACE_KEY, url.getServiceInterface());
            put(FULL_URL, url.toFullString());
            putAll(url.getParameters());
        }};
        kubernetesClient.pods().inNamespace(pod.getMetadata().getNamespace()).withName(pod.getMetadata().getName())
                .edit()
                .editMetadata()
                .addToLabels(MARK, DEFAULT_PROTOCOL)
                .addToAnnotations(serviceKey2UniqId(url.getServiceKey()), meta.toJSONString())
                .and()
                .done();
    }

    private String serviceKey2UniqId(String serviecKey) {
        return SERVICE_KEY_PREFIX + Integer.toHexString(serviecKey.hashCode());
    }

    private void unregister(Pod pod, URL url) {
        Pod registedPod = kubernetesClient.pods()
                .inNamespace(pod.getMetadata().getNamespace())
                .withName(pod.getMetadata().getName()).get();
        if (registedPod.getMetadata().getAnnotations() != null) {
            registedPod.getMetadata().getAnnotations().forEach((removeKey, value) -> {
                if (removeKey.equals(serviceKey2UniqId(url.getServiceKey()))) {
                    kubernetesClient.pods()
                            .inNamespace(pod.getMetadata().getNamespace())
                            .withName(pod.getMetadata().getName())
                            .edit()
                            .editMetadata()
                            .removeFromAnnotations(removeKey)
                            .and()
                            .done();

                }
            });
        }
    }

    private List<Pod> queryPodsByUnRegistryUrl(URL url) {
        return kubernetesClient.pods()
                .inNamespace(namespaces)
                .withLabel(APP_LABEL, podWithLabel)
                .list().getItems()
                .stream()
                .filter(pod -> pod.getStatus().getPodIP().equals(url.getHost()))
                .collect(Collectors.toList());
    }

    private List<Pod> queryPodNameByRegistriedUrl(URL url) {
        return kubernetesClient.pods()
                .inNamespace(namespaces)
                .withLabel(APP_LABEL, podWithLabel)
                .withLabel(MARK, DEFAULT_PROTOCOL)
                .list().getItems().stream()
                .filter(pod -> pod.getMetadata().getAnnotations().containsKey(serviceKey2UniqId(url.getServiceKey())))
                .collect(Collectors.toList());
    }

    private List<URL> getAllRunningService() {
        final List<URL> urls = new ArrayList<>();
        kubernetesClient.pods()
                .inNamespace(namespaces)
                .withLabel(MARK, DEFAULT_PROTOCOL)
                .list().getItems().stream()
                .filter(pod -> pod.getStatus().getPhase().equals(KubernetesStatus.Running.name()))
                .forEach(pod ->
                        pod.getMetadata().getAnnotations().forEach((key, value) -> {
                            if (key.startsWith(SERVICE_KEY_PREFIX)) {
                                JSONObject dubboMeta = JSON.parseObject(value);
                                urls.add(URL.valueOf(dubboMeta.getString(FULL_URL)));
                            }
                        })
                );
        return urls;
    }

    private List<URL> getServicesByKey(String serviceKey) {
        final List<URL> urls = new ArrayList<>();
        kubernetesClient.pods()
                .inNamespace(namespaces)
                .withLabel(MARK, DEFAULT_PROTOCOL)
                .list().getItems()
                .forEach(pod -> {
                    pod.getMetadata().getAnnotations().forEach((key, value) -> {
                        if (key.equals(serviceKey2UniqId(serviceKey))) {
                            JSONObject dubboMeta = JSON.parseObject(value);
                            urls.add(URL.valueOf(dubboMeta.getString(FULL_URL)));
                        }
                    });
                });
        return urls;
    }

    private List<URL> queryUrls(URL url) {
        final List<URL> urls = new ArrayList<>();
        if (ANY_VALUE.equals(url.getServiceInterface())) {
            urls.addAll(getAllRunningService());
        } else {
            urls.addAll(getServicesByKey(url.getServiceKey()));
        }
        return urls;
    }

    enum KubernetesStatus {
        Running,
        Pending,
        Terminating;
    }

}