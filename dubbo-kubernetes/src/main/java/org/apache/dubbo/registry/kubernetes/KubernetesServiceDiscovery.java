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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.client.AbstractServiceDiscovery;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.kubernetes.util.KubernetesClientConst;
import org.apache.dubbo.registry.kubernetes.util.KubernetesConfigUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_UNABLE_ACCESS_KUBERNETES;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_UNABLE_FIND_SERVICE_KUBERNETES;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_UNABLE_MATCH_KUBERNETES;

public class KubernetesServiceDiscovery extends AbstractServiceDiscovery {
    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    private KubernetesClient kubernetesClient;

    private String currentHostname;

    private final URL registryURL;

    private final String namespace;

    private final boolean enableRegister;

    public final static String KUBERNETES_PROPERTIES_KEY = "io.dubbo/metadata";

    private final static ConcurrentHashMap<String, AtomicLong> SERVICE_UPDATE_TIME = new ConcurrentHashMap<>(64);

    private final static ConcurrentHashMap<String, SharedIndexInformer<Service>> SERVICE_INFORMER = new ConcurrentHashMap<>(64);

    private final static ConcurrentHashMap<String, SharedIndexInformer<Pod>> PODS_INFORMER = new ConcurrentHashMap<>(64);

    private final static ConcurrentHashMap<String, SharedIndexInformer<Endpoints>> ENDPOINTS_INFORMER = new ConcurrentHashMap<>(64);

    public KubernetesServiceDiscovery(ApplicationModel applicationModel, URL registryURL) {
        super(applicationModel, registryURL);
        Config config = KubernetesConfigUtils.createKubernetesConfig(registryURL);
        this.kubernetesClient = new KubernetesClientBuilder().withConfig(config).build();
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
            logger.error(REGISTRY_UNABLE_ACCESS_KUBERNETES,"","",message);
        } else {
            KubernetesMeshEnvListener.injectKubernetesEnv(kubernetesClient, namespace);
        }
    }

    @Override
    public void doDestroy() {
        SERVICE_INFORMER.forEach((k, v) -> v.close());
        SERVICE_INFORMER.clear();

        PODS_INFORMER.forEach((k, v) -> v.close());
        PODS_INFORMER.clear();

        ENDPOINTS_INFORMER.forEach((k, v) -> v.close());
        ENDPOINTS_INFORMER.clear();

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
                                    .addToAnnotations(KUBERNETES_PROPERTIES_KEY, JsonUtils.toJson(serviceInstance.getMetadata()))
                                    .endMetadata()
                                    .build());
            if (logger.isInfoEnabled()) {
                logger.info("Write Current Service Instance Metadata to Kubernetes pod. " +
                        "Current pod name: " + currentHostname);
            }
        }
    }

    /**
     * Comparing to {@link AbstractServiceDiscovery#doUpdate(ServiceInstance, ServiceInstance)}, unregister() is unnecessary here.
     */
    @Override
    public void doUpdate(ServiceInstance oldServiceInstance, ServiceInstance newServiceInstance) throws RuntimeException {
        reportMetadata(newServiceInstance.getServiceMetadata());
        this.doRegister(newServiceInstance);
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
        Endpoints endpoints = null;
        SharedIndexInformer<Endpoints> endInformer = ENDPOINTS_INFORMER.get(serviceName);
        if (endInformer != null) {
            // get endpoints directly from informer local store
            List<Endpoints> endpointsList = endInformer.getStore().list();
            if (endpointsList.size() > 0) {
                endpoints = endpointsList.get(0);
            }
        }
        if (endpoints == null) {
            endpoints = kubernetesClient
                    .endpoints()
                    .inNamespace(namespace)
                    .withName(serviceName)
                    .get();
        }

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
        SharedIndexInformer<Endpoints> endInformer = kubernetesClient
                .endpoints()
                .inNamespace(namespace)
                .withName(serviceName)
                .inform(new ResourceEventHandler<Endpoints>() {
                    @Override
                    public void onAdd(Endpoints endpoints) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Received Endpoint Event. Event type: added. Current pod name: " + currentHostname +
                                    ". Endpoints is: " + endpoints);
                        }
                        notifyServiceChanged(serviceName, listener, toServiceInstance(endpoints, serviceName));
                    }

                    @Override
                    public void onUpdate(Endpoints oldEndpoints, Endpoints newEndpoints) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Received Endpoint Event. Event type: updated. Current pod name: " + currentHostname +
                                    ". The new Endpoints is: " + newEndpoints);
                        }
                        notifyServiceChanged(serviceName, listener, toServiceInstance(newEndpoints, serviceName));
                    }

                    @Override
                    public void onDelete(Endpoints endpoints, boolean deletedFinalStateUnknown) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Received Endpoint Event. Event type: deleted. Current pod name: " + currentHostname +
                                    ". Endpoints is: " + endpoints);
                        }
                        notifyServiceChanged(serviceName, listener, toServiceInstance(endpoints, serviceName));
                    }
                });

        ENDPOINTS_INFORMER.put(serviceName, endInformer);
    }

    private void watchPods(ServiceInstancesChangedListener listener, String serviceName) {
        Map<String, String> serviceSelector = getServiceSelector(serviceName);
        if (serviceSelector == null) {
            return;
        }

        SharedIndexInformer<Pod> podInformer = kubernetesClient
                .pods()
                .inNamespace(namespace)
                .withLabels(serviceSelector)
                .inform(new ResourceEventHandler<Pod>() {
                    @Override
                    public void onAdd(Pod pod) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Received Pods Event. Event type: added. Current pod name: " + currentHostname +
                                    ". Pod is: " + pod);
                        }
                    }

                    @Override
                    public void onUpdate(Pod oldPod, Pod newPod) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Received Pods Event. Event type: updated. Current pod name: " + currentHostname +
                                    ". new Pod is: " + newPod);
                        }

                        notifyServiceChanged(serviceName, listener, getInstances(serviceName));
                    }

                    @Override
                    public void onDelete(Pod pod, boolean deletedFinalStateUnknown) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Received Pods Event. Event type: deleted. Current pod name: " + currentHostname +
                                    ". Pod is: " + pod);
                        }
                    }
                });

        PODS_INFORMER.put(serviceName, podInformer);
    }

    private void watchService(ServiceInstancesChangedListener listener, String serviceName) {
        SharedIndexInformer<Service> serviceInformer = kubernetesClient
                .services()
                .inNamespace(namespace)
                .withName(serviceName)
                .inform(
                        new ResourceEventHandler<Service>() {
                            @Override
                            public void onAdd(Service service) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Received Service Added Event. " +
                                            "Current pod name: " + currentHostname);
                                }
                            }

                            @Override
                            public void onUpdate(Service oldService, Service newService) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Received Service Update Event. Update Pods Watcher. Current pod name: " + currentHostname +
                                            ". The new Service is: " + newService);
                                }
                                if (PODS_INFORMER.containsKey(serviceName)) {
                                    PODS_INFORMER.get(serviceName).close();
                                    PODS_INFORMER.remove(serviceName);
                                }
                                watchPods(listener, serviceName);
                            }

                            @Override
                            public void onDelete(Service service, boolean deletedFinalStateUnknown) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Received Service Delete Event. " +
                                            "Current pod name: " + currentHostname);
                                }
                            }
                        }
                );

        SERVICE_INFORMER.put(serviceName, serviceInformer);
    }

    private void notifyServiceChanged(String serviceName, ServiceInstancesChangedListener listener, List<ServiceInstance> serviceInstanceList) {
        long receivedTime = System.nanoTime();

        ServiceInstancesChangedEvent event;

        event = new ServiceInstancesChangedEvent(serviceName, serviceInstanceList);

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
                    logger.warn(REGISTRY_UNABLE_MATCH_KUBERNETES, "", "", "Unable to match Kubernetes Endpoint address with Pod. " +
                        "EndpointAddress Hostname: " + address.getTargetRef().getName());
                    continue;
                }
                instancePorts.forEach(port -> {
                    ServiceInstance serviceInstance = new DefaultServiceInstance(serviceName, ip, port, ScopeModelUtil.getApplicationModel(getUrl().getScopeModel()));

                    String properties = pod.getMetadata().getAnnotations().get(KUBERNETES_PROPERTIES_KEY);
                    if (StringUtils.isNotEmpty(properties)) {
                        serviceInstance.getMetadata().putAll(JsonUtils.toJavaObject(properties, Map.class));
                        instances.add(serviceInstance);
                    } else {
                        logger.warn(REGISTRY_UNABLE_FIND_SERVICE_KUBERNETES, "", "", "Unable to find Service Instance metadata in Pod Annotations. " +
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
