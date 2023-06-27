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
package org.apache.dubbo.metadata.store.zookeeper;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigItem;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MappingChangedEvent;
import org.apache.dubbo.metadata.MappingListener;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.report.identifier.BaseMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.KeyTypeEnum;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.ServiceMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.metadata.report.support.AbstractMetadataReport;
import org.apache.dubbo.remoting.zookeeper.DataListener;
import org.apache.dubbo.remoting.zookeeper.EventType;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter;

import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ZOOKEEPER_EXCEPTION;
import static org.apache.dubbo.metadata.ServiceNameMapping.DEFAULT_MAPPING_GROUP;
import static org.apache.dubbo.metadata.ServiceNameMapping.getAppNames;

/**
 * ZookeeperMetadataReport
 */
public class ZookeeperMetadataReport extends AbstractMetadataReport {

    private final static ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ZookeeperMetadataReport.class);

    private final String root;

    ZookeeperClient zkClient;

    private ConcurrentMap<String, MappingDataListener> casListenerMap = new ConcurrentHashMap<>();


    public ZookeeperMetadataReport(URL url, ZookeeperTransporter zookeeperTransporter) {
        super(url);
        if (url.isAnyHost()) {
            throw new IllegalStateException("registry address == null");
        }
        String group = url.getGroup(DEFAULT_ROOT);
        if (!group.startsWith(PATH_SEPARATOR)) {
            group = PATH_SEPARATOR + group;
        }
        this.root = group;
        zkClient = zookeeperTransporter.connect(url);
    }

    protected String toRootDir() {
        if (root.equals(PATH_SEPARATOR)) {
            return root;
        }
        return root + PATH_SEPARATOR;
    }

    @Override
    protected void doStoreProviderMetadata(MetadataIdentifier providerMetadataIdentifier, String serviceDefinitions) {
        storeMetadata(providerMetadataIdentifier, serviceDefinitions);
    }

    @Override
    protected void doStoreConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, String value) {
        storeMetadata(consumerMetadataIdentifier, value);
    }

    @Override
    protected void doSaveMetadata(ServiceMetadataIdentifier metadataIdentifier, URL url) {
        zkClient.createOrUpdate(getNodePath(metadataIdentifier), URL.encode(url.toFullString()), false);
    }

    @Override
    protected void doRemoveMetadata(ServiceMetadataIdentifier metadataIdentifier) {
        zkClient.delete(getNodePath(metadataIdentifier));
    }

    @Override
    protected List<String> doGetExportedURLs(ServiceMetadataIdentifier metadataIdentifier) {
        String content = zkClient.getContent(getNodePath(metadataIdentifier));
        if (StringUtils.isEmpty(content)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(Collections.singletonList(URL.decode(content)));
    }

    @Override
    protected void doSaveSubscriberData(SubscriberMetadataIdentifier subscriberMetadataIdentifier, String urls) {
        zkClient.createOrUpdate(getNodePath(subscriberMetadataIdentifier), urls, false);
    }

    @Override
    protected String doGetSubscribedURLs(SubscriberMetadataIdentifier subscriberMetadataIdentifier) {
        return zkClient.getContent(getNodePath(subscriberMetadataIdentifier));
    }

    @Override
    public String getServiceDefinition(MetadataIdentifier metadataIdentifier) {
        return zkClient.getContent(getNodePath(metadataIdentifier));
    }

    private void storeMetadata(MetadataIdentifier metadataIdentifier, String v) {
        zkClient.createOrUpdate(getNodePath(metadataIdentifier), v, false);
    }

    String getNodePath(BaseMetadataIdentifier metadataIdentifier) {
        return toRootDir() + metadataIdentifier.getUniqueKey(KeyTypeEnum.PATH);
    }

    @Override
    public void publishAppMetadata(SubscriberMetadataIdentifier identifier, MetadataInfo metadataInfo) {
        String path = getNodePath(identifier);
        if (StringUtils.isBlank(zkClient.getContent(path)) && StringUtils.isNotEmpty(metadataInfo.getContent())) {
            zkClient.createOrUpdate(path, metadataInfo.getContent(), false);
        }
    }

    @Override
    public void unPublishAppMetadata(SubscriberMetadataIdentifier identifier, MetadataInfo metadataInfo) {
        String path = getNodePath(identifier);
        if (StringUtils.isNotEmpty(zkClient.getContent(path))) {
            zkClient.delete(path);
        }
    }

    @Override
    public MetadataInfo getAppMetadata(SubscriberMetadataIdentifier identifier, Map<String, String> instanceMetadata) {
        String content = zkClient.getContent(getNodePath(identifier));
        return JsonUtils.toJavaObject(content, MetadataInfo.class);
    }

    @Override
    public Set<String> getServiceAppMapping(String serviceKey, MappingListener listener, URL url) {
        String path = buildPathKey(DEFAULT_MAPPING_GROUP, serviceKey);
        MappingDataListener mappingDataListener = ConcurrentHashMapUtils.computeIfAbsent(casListenerMap, path, _k -> {
            MappingDataListener newMappingListener = new MappingDataListener(serviceKey, path);
            zkClient.addDataListener(path, newMappingListener);
            return newMappingListener;
        });
        mappingDataListener.addListener(listener);
        return getAppNames(zkClient.getContent(path));
    }

    @Override
    public void removeServiceAppMappingListener(String serviceKey, MappingListener listener) {
        String path = buildPathKey(DEFAULT_MAPPING_GROUP, serviceKey);
        if (null != casListenerMap.get(path)) {
            removeCasServiceMappingListener(path, listener);
        }
    }

    @Override
    public Set<String> getServiceAppMapping(String serviceKey, URL url) {
        String path = buildPathKey(DEFAULT_MAPPING_GROUP, serviceKey);
        return getAppNames(zkClient.getContent(path));
    }

    @Override
    public ConfigItem getConfigItem(String serviceKey, String group) {
        String path = buildPathKey(group, serviceKey);
        return zkClient.getConfigItem(path);
    }

    @Override
    public boolean registerServiceAppMapping(String key, String group, String content, Object ticket) {
        try {
            if (ticket != null && !(ticket instanceof Stat)) {
                throw new IllegalArgumentException("zookeeper publishConfigCas requires stat type ticket");
            }
            String pathKey = buildPathKey(group, key);
            zkClient.createOrUpdate(pathKey, content, false, ticket == null ? null : ((Stat) ticket).getVersion());
            return true;
        } catch (Exception e) {
            logger.warn(REGISTRY_ZOOKEEPER_EXCEPTION, "", "", "zookeeper publishConfigCas failed.", e);
            return false;
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        // release zk client reference, but should not close it
        zkClient = null;
    }

    private String buildPathKey(String group, String serviceKey) {
        return toRootDir() + group + PATH_SEPARATOR + serviceKey;
    }

    private void removeCasServiceMappingListener(String path, MappingListener listener) {
        MappingDataListener mappingDataListener = casListenerMap.get(path);
        mappingDataListener.removeListener(listener);
        if (mappingDataListener.isEmpty()) {
            zkClient.removeDataListener(path, mappingDataListener);
            casListenerMap.remove(path, mappingDataListener);
        }
    }

    private static class MappingDataListener implements DataListener {

        private String serviceKey;
        private String path;
        private Set<MappingListener> listeners;

        public MappingDataListener(String serviceKey, String path) {
            this.serviceKey = serviceKey;
            this.path = path;
            this.listeners = new HashSet<>();
        }

        public void addListener(MappingListener listener) {
            this.listeners.add(listener);
        }

        public void removeListener(MappingListener listener) {
            this.listeners.remove(listener);
        }

        public boolean isEmpty() {
            return listeners.isEmpty();
        }

        @Override
        public void dataChanged(String path, Object value, EventType eventType) {
            if (!this.path.equals(path)) {
                return;
            }
            if (EventType.NodeCreated != eventType && EventType.NodeDataChanged != eventType) {
                return;
            }

            Set<String> apps = getAppNames((String) value);

            MappingChangedEvent event = new MappingChangedEvent(serviceKey, apps);

            listeners.forEach(mappingListener -> mappingListener.onEvent(event));
        }
    }
}
