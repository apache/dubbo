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
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.metadata.MetadataUtils;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;
import org.apache.dubbo.registry.client.metadata.store.MetaCacheManager;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_CLUSTER_KEY;
import static org.apache.dubbo.metadata.RevisionResolver.EMPTY_REVISION;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.EXPORTED_SERVICES_REVISION_PROPERTY_NAME;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.setMetadataStorageType;

/**
 * Each service discovery is bond to one application.
 */
public abstract class AbstractServiceDiscovery implements ServiceDiscovery {
    private Logger logger = LoggerFactory.getLogger(AbstractServiceDiscovery.class);
    private volatile boolean isDestroy;

    private final String serviceName;
    protected volatile ServiceInstance serviceInstance;
    protected volatile MetadataInfo metadataInfo;
    protected MetadataReport metadataReport;
    protected String metadataType;
    protected MetaCacheManager metaCacheManager;
    protected URL registryURL;

    private ApplicationModel applicationModel;

    public AbstractServiceDiscovery(ApplicationModel applicationModel, URL registryURL) {
        this(applicationModel.getApplicationName(), registryURL);
        this.applicationModel = applicationModel;
        MetadataReportInstance metadataReportInstance = applicationModel.getBeanFactory().getBean(MetadataReportInstance.class);
        metadataType = metadataReportInstance.getMetadataType();
        if (REMOTE_METADATA_STORAGE_TYPE.equals(metadataReportInstance.getMetadataType())) {
            this.metadataReport = metadataReportInstance.getMetadataReport(registryURL.getParameter(REGISTRY_CLUSTER_KEY));
        } else {
            this.metadataReport = metadataReportInstance.getNopMetadataReport();
        }
    }

    public AbstractServiceDiscovery(String serviceName, URL registryURL) {
        this.applicationModel = ApplicationModel.defaultModel();
        this.registryURL = registryURL;
        this.serviceName = serviceName;
        this.metadataInfo = new MetadataInfo(serviceName);
        this.metaCacheManager = new MetaCacheManager(getCacheNameSuffix());
    }

    public synchronized final void register() throws RuntimeException {
        this.serviceInstance = createServiceInstance();
        boolean revisionUpdated = calOrUpdateInstanceRevision();
        if (revisionUpdated) {
            reportMetadata();
            doRegister(serviceInstance);
        }
    }

    @Override
    public synchronized final void update() throws RuntimeException {
        if (this.serviceInstance == null) {
            this.serviceInstance = createServiceInstance();
        }
        boolean revisionUpdated = calOrUpdateInstanceRevision();
        if (revisionUpdated) {
            doUpdate();
        }
    }

    @Override
    public synchronized final void unregister() throws RuntimeException {
        unReportMetadata();
        doUnregister();
    }

    @Override
    public final ServiceInstance getLocalInstance() {
        return serviceInstance;
    }

    @Override
    public MetadataInfo getMetadata() {
        return metadataInfo;
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
    public final void destroy() throws Exception {
        isDestroy = true;
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

    public void doUpdate() throws RuntimeException {
        this.unregister();

        reportMetadata();
        this.doRegister(serviceInstance);
    }

    @Override
    public URL getUrl() {
        return registryURL;
    }

    public abstract void doRegister(ServiceInstance serviceInstance) throws RuntimeException;

    public abstract void doUnregister();

    public abstract void doDestroy() throws Exception;

    private ServiceInstance createServiceInstance() {
        DefaultServiceInstance instance = new DefaultServiceInstance(serviceName, applicationModel);
        instance.setServiceMetadata(metadataInfo);
        setMetadataStorageType(instance, metadataType);
        ServiceInstanceMetadataUtils.customizeInstance(instance, applicationModel);
        return instance;
    }

    protected boolean calOrUpdateInstanceRevision() {
        String existingInstanceRevision = serviceInstance.getMetadata().get(EXPORTED_SERVICES_REVISION_PROPERTY_NAME);
        String newRevision = metadataInfo.calAndGetRevision();
        if (!newRevision.equals(existingInstanceRevision)) {
            if (EMPTY_REVISION.equals(newRevision)) {
                return false;
            }
            serviceInstance.getMetadata().put(EXPORTED_SERVICES_REVISION_PROPERTY_NAME, metadataInfo.calAndGetRevision());
            return true;
        }
        return false;
    }

    protected void reportMetadata() {
        if (metadataReport != null) {
            SubscriberMetadataIdentifier identifier = new SubscriberMetadataIdentifier(serviceName, metadataInfo.calAndGetRevision());
            metadataReport.publishAppMetadata(identifier, metadataInfo);
        }
    }

    protected void unReportMetadata() {
        if (metadataReport != null) {
            SubscriberMetadataIdentifier identifier = new SubscriberMetadataIdentifier(serviceName, metadataInfo.calAndGetRevision());
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
