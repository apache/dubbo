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
package org.apache.dubbo.registry.client.event.listener;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.event.ConditionalEventListener;
import org.apache.dubbo.event.EventListener;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataInfo.ServiceInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.RegistryClusterIdentifier;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.metadata.MetadataUtils;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;
import org.apache.dubbo.registry.client.metadata.store.RemoteMetadataServiceImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_CLUSTER_KEY;
import static org.apache.dubbo.metadata.MetadataInfo.DEFAULT_REVISION;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getExportedServicesRevision;
import static org.apache.dubbo.rpc.Constants.ID_KEY;

/**
 * The Service Discovery Changed {@link EventListener Event Listener}
 *
 * @see ServiceInstancesChangedEvent
 * @since 2.7.5
 */
public class ServiceInstancesChangedListener implements ConditionalEventListener<ServiceInstancesChangedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ServiceInstancesChangedListener.class);

    private final Set<String> serviceNames;
    private final ServiceDiscovery serviceDiscovery;
    private final String registryId;
    private URL url;
    private Map<String, Set<NotifyListener>> listeners;

    private Map<String, List<ServiceInstance>> allInstances;

    private Map<String, List<URL>> serviceUrls;

    private Map<String, MetadataInfo> revisionToMetadata;

    public ServiceInstancesChangedListener(Set<String> serviceNames, ServiceDiscovery serviceDiscovery) {
        this.serviceNames = serviceNames;
        this.serviceDiscovery = serviceDiscovery;
        this.registryId = serviceDiscovery.getUrl().getParameter(ID_KEY);
        this.listeners = new HashMap<>();
        this.allInstances = new HashMap<>();
        this.serviceUrls = new HashMap<>();
        this.revisionToMetadata = new HashMap<>();
    }

    /**
     * On {@link ServiceInstancesChangedEvent the service instances change event}
     * 监听的服务实例发生变化
     *
     * @param event {@link ServiceInstancesChangedEvent}
     */
    public synchronized void onEvent(ServiceInstancesChangedEvent event) {
        logger.info("Received instance notification, serviceName: " + event.getServiceName() + ", instances: " + event.getServiceInstances().size());
        String appName = event.getServiceName();
        /**
         * 缓存注册中心中  服务以及对应的注册实例
         */
        allInstances.put(appName, event.getServiceInstances());
        if (logger.isDebugEnabled()) {
            logger.debug(event.getServiceInstances().toString());
        }

        Map<String, List<ServiceInstance>> revisionToInstances = new HashMap<>();
        Map<String, Set<String>> localServiceToRevisions = new HashMap<>();
        Map<Set<String>, List<URL>> revisionsToUrls = new HashMap();
        Map<String, List<URL>> tmpServiceUrls = new HashMap<>();
        /**
         *
         */
        for (Map.Entry<String, List<ServiceInstance>> entry : allInstances.entrySet()) {
            List<ServiceInstance> instances = entry.getValue();
            for (ServiceInstance instance : instances) {
                /**
                 * 获取实例中元数据对应的revision
                 */
                String revision = getExportedServicesRevision(instance);
                if (DEFAULT_REVISION.equals(revision)) {
                    logger.info("Find instance without valid service metadata: " + instance.getAddress());
                    continue;
                }
                /**
                 * 将revision与对应的实例缓存到revisionToInstances
                 */
                List<ServiceInstance> subInstances = revisionToInstances.computeIfAbsent(revision, r -> new LinkedList<>());
                subInstances.add(instance);

                /**
                 * 获取revisionToMetadata对应的缓存
                 */
                MetadataInfo metadata = revisionToMetadata.get(revision);
                if (metadata == null) {
                    /**
                     * 获取instance对应的metadata
                     * 服务自省   访问服务提供者元数据服务  获取导出的服务
                     * 服务自省   访问服务提供者元数据服务  获取导出的服务
                     * 服务自省   访问服务提供者元数据服务  获取导出的服务
                     */
                    metadata = getMetadataInfo(instance);
                    logger.info("MetadataInfo for instance " + instance.getAddress() + "?revision=" + revision + " is " + metadata);
                    if (metadata != null) {
                        /**
                         * 缓存revision与对应的metadata
                         */
                        revisionToMetadata.put(revision, metadata);
                    } else {

                    }
                }

                if (metadata != null) {
                    /**
                     * 向localServiceToRevisions缓存metadata内的服务与revision
                     */
                    parseMetadata(revision, metadata, localServiceToRevisions);
                    ((DefaultServiceInstance) instance).setServiceMetadata(metadata);
                }
//                else {
//                    logger.error("Failed to load service metadata for instance " + instance);
//                    Set<String> set = localServiceToRevisions.computeIfAbsent(url.getServiceKey(), k -> new TreeSet<>());
//                    set.add(revision);
//                }
            }

            /**
             * 获取导出的服务与对应的InstanceAddressURL
             * org.apache.dubbo.demo.DemoService:dubbo
             *     -> DefaultServiceInstance{id='172.203.144.103#20880#DEFAULT#DEFAULT_GROUP@@dubbo-demo-annotation-provider', serviceName='dubbo-demo-annotation-provider', host='172.203.144.103', port=20880, enabled=true, healthy=true, metadata={dubbo.metadata-service.url-params={"dubbo":{"version":"1.0.0","dubbo":"2.0.2","port":"20881"}}, dubbo.endpoints=[{"port":20880,"protocol":"dubbo"}], dubbo.metadata.revision=8BE83CC30467D06A691D5E9CAA26F913, dubbo.metadata.storage-type=local}}metadata{app='dubbo-demo-annotation-provider',revision='8BE83CC30467D06A691D5E9CAA26F913',services={org.apache.dubbo.demo.DemoService:dubbo=service{name='org.apache.dubbo.demo.DemoService',group='null',version='null',protocol='dubbo',params={deprecated=false, dubbo=2.0.2},consumerParams=null}, dubbo-demo-annotation-provider/org.apache.dubbo.metadata.MetadataService:1.0.0:dubbo=service{name='org.apache.dubbo.metadata.MetadataService',group='dubbo-demo-annotation-provider',version='1.0.0',protocol='dubbo',params={deprecated=false, dubbo=2.0.2, version=1.0.0, group=dubbo-demo-annotation-provider},consumerParams=null}}}
             * dubbo-demo-annotation-provider/org.apache.dubbo.metadata.MetadataService:1.0.0:dubbo
             *     -> DefaultServiceInstance{id='172.203.144.103#20880#DEFAULT#DEFAULT_GROUP@@dubbo-demo-annotation-provider', serviceName='dubbo-demo-annotation-provider', host='172.203.144.103', port=20880, enabled=true, healthy=true, metadata={dubbo.metadata-service.url-params={"dubbo":{"version":"1.0.0","dubbo":"2.0.2","port":"20881"}}, dubbo.endpoints=[{"port":20880,"protocol":"dubbo"}], dubbo.metadata.revision=8BE83CC30467D06A691D5E9CAA26F913, dubbo.metadata.storage-type=local}}metadata{app='dubbo-demo-annotation-provider',revision='8BE83CC30467D06A691D5E9CAA26F913',services={org.apache.dubbo.demo.DemoService:dubbo=service{name='org.apache.dubbo.demo.DemoService',group='null',version='null',protocol='dubbo',params={deprecated=false, dubbo=2.0.2},consumerParams=null}, dubbo-demo-annotation-provider/org.apache.dubbo.metadata.MetadataService:1.0.0:dubbo=service{name='org.apache.dubbo.metadata.MetadataService',group='dubbo-demo-annotation-provider',version='1.0.0',protocol='dubbo',params={deprecated=false, dubbo=2.0.2, version=1.0.0, group=dubbo-demo-annotation-provider},consumerParams=null}}}
             */
            localServiceToRevisions.forEach((serviceKey, revisions) -> {
                /**
                 *
                 */
                List<URL> urls = revisionsToUrls.get(revisions);
                if (urls != null) {
                    tmpServiceUrls.put(serviceKey, urls);
                } else {
                    /**
                     *
                     */
                    urls = new ArrayList<>();
                    for (String r : revisions) {
                        for (ServiceInstance i : revisionToInstances.get(r)) {
                            urls.add(i.toURL());
                        }
                    }
                    revisionsToUrls.put(revisions, urls);
                    /**
                     *
                     */
                    tmpServiceUrls.put(serviceKey, urls);
                }
            });
        }

        this.serviceUrls = tmpServiceUrls;
        /**
         * 通知地址发生变化
         */
        this.notifyAddressChanged();
    }

    /**
     * 向localServiceToRevisions缓存metadata内的服务与revision
     * @param revision
     * @param metadata
     * @param localServiceToRevisions
     * @return
     */
    private Map<String, Set<String>> parseMetadata(String revision, MetadataInfo metadata, Map<String, Set<String>> localServiceToRevisions) {
        /**
         * 导出的服务
         * metadata{app='dubbo-demo-annotation-provider',revision='8BE83CC30467D06A691D5E9CAA26F913',services={org.apache.dubbo.demo.DemoService:dubbo=service{name='org.apache.dubbo.demo.DemoService',group='null',version='null',protocol='dubbo',params={deprecated=false, dubbo=2.0.2},consumerParams=null}, dubbo-demo-annotation-provider/org.apache.dubbo.metadata.MetadataService:1.0.0:dubbo=service{name='org.apache.dubbo.metadata.MetadataService',group='dubbo-demo-annotation-provider',version='1.0.0',protocol='dubbo',params={deprecated=false, dubbo=2.0.2, version=1.0.0, group=dubbo-demo-annotation-provider},consumerParams=null}}}
         */
        Map<String, ServiceInfo> serviceInfos = metadata.getServices();
        for (Map.Entry<String, ServiceInfo> entry : serviceInfos.entrySet()) {
            Set<String> set = localServiceToRevisions.computeIfAbsent(entry.getKey(), k -> new TreeSet<>());
            set.add(revision);
        }

        /**
         *  导出的服务与对应的revision
         */
        return localServiceToRevisions;
    }

    /**
     * @param instance
     * @return
     */
    private MetadataInfo getMetadataInfo(ServiceInstance instance) {
        /**
         * 获取instance对应的dubbo.metadata.storage-type
         */
        String metadataType = ServiceInstanceMetadataUtils.getMetadataStorageType(instance);
        // FIXME, check "REGISTRY_CLUSTER_KEY" must be set by every registry implementation.
        /**
         * 向instance的extendParams中缓存REGISTRY_CLUSTER  值为url参数map中registry-cluster-type的值
         */
        instance.getExtendParams().putIfAbsent(REGISTRY_CLUSTER_KEY, RegistryClusterIdentifier.getExtension(url).consumerKey(url));
        MetadataInfo metadataInfo = null;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Instance " + instance.getAddress() + " is using metadata type " + metadataType);
            }
            if (REMOTE_METADATA_STORAGE_TYPE.equals(metadataType)) {
                /**
                 * remote    实例化RemoteMetadataServiceImpl
                 */
                RemoteMetadataServiceImpl remoteMetadataService = MetadataUtils.getRemoteMetadataService();
                /**
                 * 获取instance对应的metadataInfo  nacos没有实现  返回null
                 */
                metadataInfo = remoteMetadataService.getMetadata(instance);
            } else {
                /**
                 * local  生成服务提供者对应元数据服务的invoker
                 */
                MetadataService metadataServiceProxy = MetadataUtils.getMetadataServiceProxy(instance, serviceDiscovery);
                /**
                 * 通过代理类InvokerInvocationHandler的invoke  访问元数据服务
                 * public MetadataInfo getMetadataInfo(String revision);
                 * 访问服务提供者元数据服务  获取导出的服务
                 * 访问服务提供者元数据服务  获取导出的服务
                 * 访问服务提供者元数据服务  获取导出的服务
                 */
                metadataInfo = metadataServiceProxy.getMetadataInfo(ServiceInstanceMetadataUtils.getExportedServicesRevision(instance));
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Metadata " + metadataInfo.toString());
            }
        } catch (Exception e) {
            logger.error("Failed to load service metadata, metadata type is " + metadataType, e);
            // TODO, load metadata backup. Stop getting metadata after x times of failure for one revision?
        }
        return metadataInfo;
    }

    private void notifyAddressChanged() {
        listeners.forEach((key, notifyListeners) -> {
            notifyListeners.forEach(notifyListener -> {
                /**
                 * ServiceDiscoveryRegistryDirectory
                 */
                notifyListener.notify(toUrlsWithEmpty(serviceUrls.get(key)));
            });
        });
    }

    private List<URL> toUrlsWithEmpty(List<URL> urls) {
        if (urls == null) {
            urls = Collections.emptyList();
        }
        return urls;
    }

    public void addListener(String serviceKey, NotifyListener listener) {
        this.listeners.computeIfAbsent(serviceKey, k -> new HashSet<>()).add(listener);
    }

    public void removeListener(String serviceKey) {
        listeners.remove(serviceKey);
        if (listeners.isEmpty()) {
            serviceDiscovery.removeServiceInstancesChangedListener(this);
        }
    }

    public List<URL> getUrls(String serviceKey) {
        return toUrlsWithEmpty(serviceUrls.get(serviceKey));
    }

    /**
     * Get the correlative service name
     *
     * @return the correlative service name
     */
    public final Set<String> getServiceNames() {
        return serviceNames;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public URL getUrl() {
        return url;
    }

    /**
     * @param event {@link ServiceInstancesChangedEvent event}
     * @return If service name matches, return <code>true</code>, or <code>false</code>
     */
    public final boolean accept(ServiceInstancesChangedEvent event) {
        return serviceNames.contains(event.getServiceName());
    }

    public String getRegistryId() {
        return registryId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceInstancesChangedListener)) {
            return false;
        }
        ServiceInstancesChangedListener that = (ServiceInstancesChangedListener) o;
        return Objects.equals(getServiceNames(), that.getServiceNames()) && Objects.equals(getRegistryId(), that.getRegistryId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), getServiceNames(), getRegistryId());
    }
}
