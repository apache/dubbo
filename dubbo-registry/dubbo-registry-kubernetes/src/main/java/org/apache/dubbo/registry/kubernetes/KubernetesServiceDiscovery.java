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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.client.AbstractServiceDiscovery;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst;
import org.apache.dubbo.registry.kubernetes.util.KubernetesConfigUtils;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import com.alibaba.fastjson.JSONObject;
import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class KubernetesServiceDiscovery extends AbstractServiceDiscovery {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private KubernetesClient kubernetesClient;

    private String currentHostname;

    private URL registryURL;

    private String namespace;

    private boolean enableRegister;

    public final static String KUBERNETES_PROPERTIES_KEY = "io.dubbo/metadata";

    private final static ConcurrentHashMap<String, Watch> SERVICE_WATCHER = new ConcurrentHashMap<>(64);

    private final static ConcurrentHashMap<String, Watch> PODS_WATCHER = new ConcurrentHashMap<>(64);

    private final static ConcurrentHashMap<String, Watch> ENDPOINTS_WATCHER = new ConcurrentHashMap<>(64);

    private final static ConcurrentHashMap<String, AtomicLong> SERVICE_UPDATE_TIME = new ConcurrentHashMap<>(64);

    @Override
    public void doInitialize(URL registryURL) throws Exception {
        Config config = KubernetesConfigUtils.createKubernetesConfig(registryURL);
        this.kubernetesClient = new DefaultKubernetesClient(config);
        this.currentHostname = System.getenv("HOSTNAME");
        this.registryURL = registryURL;
        this.namespace = config.getNamespace();
        this.enableRegister = registryURL.getParameter(KubernetesClientConst.ENABLE_REGISTER, true);

        boolean availableAccess;
        try {
            availableAccess = kubernetesClient.pods().withName(currentHostname).get() != null;
        } catch (Throwable e) {
            availableAccess = false;
        }
        if (!availableAccess) {
            String message = "Unable to access api server. " +
                    "Please check your url config." +
                    " Master URL: " + config.getMasterUrl() +
                    " Hostname: " + currentHostname;
            logger.error(message);
        } else {
            KubernetesMeshEnvListener.injectKubernetesEnv(kubernetesClient, namespace);
        }
    }

    @Override
    public void doDestroy() throws Exception {
        SERVICE_WATCHER.forEach((k, v) -> v.close());
        SERVICE_WATCHER.clear();

        PODS_WATCHER.forEach((k, v) -> v.close());
        PODS_WATCHER.clear();

        ENDPOINTS_WATCHER.forEach((k, v) -> v.close());
        ENDPOINTS_WATCHER.clear();

        kubernetesClient.close();
    }

    @Override
    public void doRegister(ServiceInstance serviceInstance) throws RuntimeException {
        if (enableRegister) {
            kubernetesClient
                    .pods()
                    .inNamespace(namespace)
                    .withName(currentHostname)
                    .edit(pod ->
                            new PodBuilder(pod)
                                    .editOrNewMetadata()
                                    .addToAnnotations(KUBERNETES_PROPERTIES_KEY, JSONObject.toJSONString(serviceInstance.getMetadata()))
                                    .endMetadata()
                                    .build());
            if (logger.isInfoEnabled()) {
                logger.info("Write Current Service Instance Metadata to Kubernetes pod. " +
                        "Current pod name: " + currentHostname);
            }
        }
    }

    @Override
    public void doUpdate(ServiceInstance serviceInstance) throws RuntimeException {
        register(serviceInstance);
    }

    @Override
    public void doUnregister(ServiceInstance serviceInstance) throws RuntimeException {
        if (enableRegister) {
            kubernetesClient
                    .pods()
                    .inNamespace(namespace)
                    .withName(currentHostname)
                    .edit(pod ->
                            new PodBuilder(pod)
                                    .editOrNewMetadata()
                                    .removeFromAnnotations(KUBERNETES_PROPERTIES_KEY)
                                    .endMetadata()
                                    .build());
            if (logger.isInfoEnabled()) {
                logger.info("Remove Current Service Instance from Kubernetes pod. Current pod name: " + currentHostname);
            }
        }
    }

    @Override
    public Set<String> getServices() {
        return kubernetesClient
                .services()
                .inNamespace(namespace)
                .list()
                .getItems()
                .stream()
                .map(service -> service.getMetadata().getName())
                .collect(Collectors.toSet());
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws NullPointerException {
        Endpoints endpoints =
                kubernetesClient
                        .endpoints()
                        .inNamespace(namespace)
                        .withName(serviceName)
                        .get();

        return toServiceInstance(endpoints, serviceName);
    }

    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws NullPointerException, IllegalArgumentException {
        listener.getServiceNames().forEach(serviceName -> {
            SERVICE_UPDATE_TIME.put(serviceName, new AtomicLong(0L));

            // Watch Service Endpoint Modification
            watchEndpoints(listener, serviceName);

            // Watch Pods Modification, happens when ServiceInstance updated
            watchPods(listener, serviceName);

            // Watch Service Modification, happens when Service Selector updated, used to update pods watcher
            watchService(listener, serviceName);
        });
    }

    private void watchEndpoints(ServiceInstancesChangedListener listener, String serviceName) {
        Watch watch = kubernetesClient
                .endpoints()
                .inNamespace(namespace)
                .withName(serviceName)
                .watch(new Watcher<Endpoints>() {
                    @Override
                    public void eventReceived(Action action, Endpoints resource) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Received Endpoint Event. Event type: " + action.name() +
                                    ". Current pod name: " + currentHostname);
                        }

                        notifyServiceChanged(serviceName, listener);
                    }

                    @Override
                    public void onClose(WatcherException cause) {
                        // ignore
                    }
                });

        ENDPOINTS_WATCHER.put(serviceName, watch);
    }

    private void watchPods(ServiceInstancesChangedListener listener, String serviceName) {
        Map<String, String> serviceSelector = getServiceSelector(serviceName);
        if (serviceSelector == null) {
            return;
        }

        Watch watch = kubernetesClient
                .pods()
                .inNamespace(namespace)
                .withLabels(serviceSelector)
                .watch(new Watcher<Pod>() {
                    @Override
                    public void eventReceived(Action action, Pod resource) {
                        if (Action.MODIFIED.equals(action)) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Received Pods Update Event. Current pod name: " + currentHostname);
                            }

                            notifyServiceChanged(serviceName, listener);
                        }
                    }

                    @Override
                    public void onClose(WatcherException cause) {
                        // ignore
                    }
                });

        PODS_WATCHER.put(serviceName, watch);
    }

    private void watchService(ServiceInstancesChangedListener listener, String serviceName) {
        Watch watch = kubernetesClient
                .services()
                .inNamespace(namespace)
                .withName(serviceName)
                .watch(new Watcher<Service>() {
                    @Override
                    public void eventReceived(Action action, Service resource) {
                        if (Action.MODIFIED.equals(action)) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Received Service Update Event. Update Pods Watcher. " +
                                        "Current pod name: " + currentHostname);
                            }

                            if (PODS_WATCHER.containsKey(serviceName)) {
                                PODS_WATCHER.get(serviceName).close();
                                PODS_WATCHER.remove(serviceName);
                            }
                            watchPods(listener, serviceName);
                        }
                    }

                    @Override
                    public void onClose(WatcherException cause) {
                        // ignore
                    }
                });

        SERVICE_WATCHER.put(serviceName, watch);
    }

    private void notifyServiceChanged(String serviceName, ServiceInstancesChangedListener listener) {
        long receivedTime = System.nanoTime();

        ServiceInstancesChangedEvent event;

        event = new ServiceInstancesChangedEvent(serviceName, getInstances(serviceName));

        AtomicLong updateTime = SERVICE_UPDATE_TIME.get(serviceName);
        long lastUpdateTime = updateTime.get();

        if (lastUpdateTime <= receivedTime) {
            if (updateTime.compareAndSet(lastUpdateTime, receivedTime)) {
                listener.onEvent(event);
                return;
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info("Discard Service Instance Data. " +
                    "Possible Cause: Newer message has been processed or Failed to update time record by CAS. " +
                    "Current Data received time: " + receivedTime + ". " +
                    "Newer Data received time: " + lastUpdateTime + ".");
        }
    }

    @Override
    public URL getUrl() {
        return registryURL;
    }

    private Map<String, String> getServiceSelector(String serviceName) {
        Service service = kubernetesClient.services().inNamespace(namespace).withName(serviceName).get();
        if (service == null) {
            return null;
        }
        return service.getSpec().getSelector();
    }

    private List<ServiceInstance> toServiceInstance(Endpoints endpoints, String serviceName) {
        Map<String, String> serviceSelector = getServiceSelector(serviceName);
        if (serviceSelector == null) {
            return new LinkedList<>();
        }
        Map<String, Pod> pods = kubernetesClient
                .pods()
                .inNamespace(namespace)
                .withLabels(serviceSelector)
                .list()
                .getItems()
                .stream()
                .collect(
                        Collectors.toMap(
                                pod -> pod.getMetadata().getName(),
                                pod -> pod));

        List<ServiceInstance> instances = new LinkedList<>();
        Set<Integer> instancePorts = new HashSet<>();

        for (EndpointSubset endpointSubset : endpoints.getSubsets()) {
            instancePorts.addAll(
                    endpointSubset.getPorts()
                            .stream().map(EndpointPort::getPort)
                            .collect(Collectors.toSet()));
        }

        for (EndpointSubset endpointSubset : endpoints.getSubsets()) {
            for (EndpointAddress address : endpointSubset.getAddresses()) {
                Pod pod = pods.get(address.getTargetRef().getName());
                String ip = address.getIp();
                if (pod == null) {
                    logger.warn("Unable to match Kubernetes Endpoint address with Pod. " +
                            "EndpointAddress Hostname: " + address.getTargetRef().getName());
                    continue;
                }

                instancePorts.forEach(port -> {
                    ServiceInstance serviceInstance = new DefaultServiceInstance(serviceName, ip, port, ScopeModelUtil.getApplicationModel(getUrl().getScopeModel()));

                    String properties = pod.getMetadata().getAnnotations().get(KUBERNETES_PROPERTIES_KEY);
                    if (StringUtils.isNotEmpty(properties)) {
                        serviceInstance.getMetadata().putAll(JSONObject.parseObject(properties, Map.class));
                        instances.add(serviceInstance);
                    } else {
                        logger.warn("Unable to find Service Instance metadata in Pod Annotations. " +
                                "Possibly cause: provider has not been initialized successfully. " +
                                "EndpointAddress Hostname: " + address.getTargetRef().getName());
                    }
                });
            }
        }

        return instances;
    }

    /**
     * UT used only
     */
    @Deprecated
    public void setCurrentHostname(String currentHostname) {
        this.currentHostname = currentHostname;
    }

    /**
     * UT used only
     */
    @Deprecated
    public void setKubernetesClient(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }
}
