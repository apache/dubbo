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
package org.apache.dubbo.registry.client;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.client.metadata.MetadataUtils;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;
import org.apache.dubbo.registry.client.metadata.store.MetaCacheManager;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.List;
import java.util.Set;

import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_CLUSTER_KEY;
import static org.apache.dubbo.metadata.RevisionResolver.EMPTY_REVISION;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.EXPORTED_SERVICES_REVISION_PROPERTY_NAME;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.isValidInstance;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.setMetadataStorageType;

/**
 * Each service discovery is bond to one application.
 */
public abstract class AbstractServiceDiscovery implements ServiceDiscovery {
    private final Logger logger = LoggerFactory.getLogger(AbstractServiceDiscovery.class);
    private volatile boolean isDestroy;

    protected final String serviceName;
    protected volatile ServiceInstance serviceInstance;
    protected volatile MetadataInfo metadataInfo;
    protected MetadataReport metadataReport;
    protected String metadataType;
    protected MetaCacheManager metaCacheManager;
    protected URL registryURL;

    protected Set<ServiceInstancesChangedListener> instanceListeners = new ConcurrentHashSet<>();

    protected ApplicationModel applicationModel;

    public AbstractServiceDiscovery(ApplicationModel applicationModel, URL registryURL) {
        this(applicationModel.getApplicationName(), registryURL);
        this.applicationModel = applicationModel;
        MetadataReportInstance metadataReportInstance = applicationModel.getBeanFactory().getBean(MetadataReportInstance.class);
        metadataType = metadataReportInstance.getMetadataType();
        this.metadataReport = metadataReportInstance.getMetadataReport(registryURL.getParameter(REGISTRY_CLUSTER_KEY));
//        if (REMOTE_METADATA_STORAGE_TYPE.equals(metadataReportInstance.getMetadataType())) {
//            this.metadataReport = metadataReportInstance.getMetadataReport(registryURL.getParameter(REGISTRY_CLUSTER_KEY));
//        } else {
//            this.metadataReport = metadataReportInstance.getNopMetadataReport();
//        }
    }

    public AbstractServiceDiscovery(String serviceName, URL registryURL) {
        this.applicationModel = ApplicationModel.defaultModel();
        this.registryURL = registryURL;
        this.serviceName = serviceName;
        this.metadataInfo = new MetadataInfo(serviceName);
        this.metaCacheManager = new MetaCacheManager(getCacheNameSuffix());
    }

    public synchronized void register() throws RuntimeException {
        this.serviceInstance = createServiceInstance(this.metadataInfo);
        if (!isValidInstance(this.serviceInstance)) {
            logger.warn("No valid instance found, stop registering instance address to registry.");
            return;
        }

        boolean revisionUpdated = calOrUpdateInstanceRevision(this.serviceInstance);
        if (revisionUpdated) {
            reportMetadata(this.metadataInfo);
            doRegister(this.serviceInstance);
        }
    }

    /**
     * Update assumes that DefaultServiceInstance and its attributes will never get updated once created.
     * Checking hasExportedServices() before registration guarantees that at least one service is ready for creating the
     * instance.
     */
    @Override
    public synchronized void update() throws RuntimeException {
        if (isDestroy) {
            return;
        }

        if (this.serviceInstance == null) {
            this.serviceInstance = createServiceInstance(this.metadataInfo);
        } else if (!isValidInstance(this.serviceInstance)) {
            ServiceInstanceMetadataUtils.customizeInstance(this.serviceInstance, this.applicationModel);
        }

        if (!isValidInstance(this.serviceInstance)) {
            return;
        }

        boolean revisionUpdated = calOrUpdateInstanceRevision(this.serviceInstance);
        if (revisionUpdated) {
            logger.info(String.format("Metadata of instance changed, updating instance with revision %s.", this.serviceInstance.getServiceMetadata().getRevision()));
            doUpdate(this.serviceInstance);
        }
    }

    @Override
    public synchronized void unregister() throws RuntimeException {
        // fixme, this metadata info may still being shared by other instances
//        unReportMetadata(this.metadataInfo);
        doUnregister(this.serviceInstance);
    }

    @Override
    public final ServiceInstance getLocalInstance() {
        return this.serviceInstance;
    }

    @Override
    public MetadataInfo getLocalMetadata() {
        return this.metadataInfo;
    }

    @Override
    public MetadataInfo getRemoteMetadata(String revision, ServiceInstance instance) {
        MetadataInfo metadata = metaCacheManager.get(revision);

        if (metadata != null && metadata != MetadataInfo.EMPTY) {
            // metadata loaded from cache
            if (logger.isDebugEnabled()) {
                logger.debug("MetadataInfo for instance " + instance.getAddress() + "?revision=" + revision
                    + "&cluster=" + instance.getRegistryCluster() + ", " + metadata);
            }
            return metadata;
        }

        // try to load metadata from remote.
        int triedTimes = 0;
        while (triedTimes < 3) {
            metadata = MetadataUtils.getRemoteMetadata(revision, instance, metadataReport);

            if (metadata != MetadataInfo.EMPTY) {// succeeded
                metadata.init();
                break;
            } else {// failed
                if (triedTimes > 0) {
                    logger.info("Retry the " + triedTimes + " times to get metadata for instance " + instance.getAddress() + "?revision=" + revision
                        + "&cluster=" + instance.getRegistryCluster());
                }
                triedTimes++;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        }

        if (metadata == MetadataInfo.EMPTY) {
            logger.error("Failed to get metadata for instance after 3 retries, " + instance.getAddress() + "?revision=" + revision
                + "&cluster=" + instance.getRegistryCluster());
        } else {
            metaCacheManager.put(revision, metadata);
        }
        return metadata;
    }

    @Override
    public MetadataInfo getRemoteMetadata(String revision) {
       return metaCacheManager.get(revision);
    }

    @Override
    public final void destroy() throws Exception {
        isDestroy = true;
        metaCacheManager.destroy();
        doDestroy();
    }

    @Override
    public final boolean isDestroy() {
        return isDestroy;
    }

    @Override
    public void register(URL url) {
        metadataInfo.addService(url);
    }

    @Override
    public void unregister(URL url) {
        metadataInfo.removeService(url);
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        metadataInfo.addSubscribedURL(url);
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        metadataInfo.removeSubscribedURL(url);
    }

    @Override
    public List<URL> lookup(URL url) {
       throw new UnsupportedOperationException("Service discovery implementation does not support lookup of url list.");
    }

    protected void doUpdate(ServiceInstance serviceInstance) throws RuntimeException {

        this.unregister();

        reportMetadata(serviceInstance.getServiceMetadata());
        this.doRegister(serviceInstance);
    }

    @Override
    public URL getUrl() {
        return registryURL;
    }

    protected abstract void doRegister(ServiceInstance serviceInstance) throws RuntimeException;

    protected abstract void doUnregister(ServiceInstance serviceInstance);

    protected abstract void doDestroy() throws Exception;

    protected ServiceInstance createServiceInstance(MetadataInfo metadataInfo) {
        DefaultServiceInstance instance = new DefaultServiceInstance(serviceName, applicationModel);
        instance.setServiceMetadata(metadataInfo);
        setMetadataStorageType(instance, metadataType);
        ServiceInstanceMetadataUtils.customizeInstance(instance, applicationModel);
        return instance;
    }

    protected boolean calOrUpdateInstanceRevision(ServiceInstance instance) {
        String existingInstanceRevision = instance.getMetadata().get(EXPORTED_SERVICES_REVISION_PROPERTY_NAME);
        MetadataInfo metadataInfo = instance.getServiceMetadata();
        String newRevision = metadataInfo.calAndGetRevision();
        if (!newRevision.equals(existingInstanceRevision)) {
            if (EMPTY_REVISION.equals(newRevision)) {
                return false;
            }
            instance.getMetadata().put(EXPORTED_SERVICES_REVISION_PROPERTY_NAME, metadataInfo.calAndGetRevision());
            return true;
        }
        return false;
    }

    protected void reportMetadata(MetadataInfo metadataInfo) {
        if (metadataReport != null) {
            SubscriberMetadataIdentifier identifier = new SubscriberMetadataIdentifier(serviceName, metadataInfo.getRevision());
            metadataReport.publishAppMetadata(identifier, metadataInfo);
        }
    }

    protected void unReportMetadata(MetadataInfo metadataInfo) {
        if (metadataReport != null) {
            SubscriberMetadataIdentifier identifier = new SubscriberMetadataIdentifier(serviceName, metadataInfo.getRevision());
            metadataReport.unPublishAppMetadata(identifier, metadataInfo);
        }
    }

    private String getCacheNameSuffix() {
        String name = this.getClass().getSimpleName();
        int i = name.indexOf("ServiceDiscovery");
        if (i != -1) {
            name = name.substring(0, i);
        }
        URL url = this.getUrl();
        if (url != null) {
           return name.toLowerCase() + url.getBackupAddress();
        }
        return name.toLowerCase();
    }
}
