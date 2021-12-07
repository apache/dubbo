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

package org.apache.dubbo.metadata.store.nacos;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.ConfigItem;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.utils.MD5Utils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MappingChangedEvent;
import org.apache.dubbo.metadata.MappingListener;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.ServiceNameMapping;
import org.apache.dubbo.metadata.report.identifier.BaseMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.KeyTypeEnum;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.ServiceMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.metadata.report.support.AbstractMetadataReport;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.listener.AbstractSharedListener;
import com.alibaba.nacos.api.exception.NacosException;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;

import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.RemotingConstants.BACKUP_KEY;
import static org.apache.dubbo.common.utils.StringConstantFieldValuePredicate.of;
import static org.apache.dubbo.common.utils.StringUtils.HYPHEN_CHAR;
import static org.apache.dubbo.metadata.ServiceNameMapping.DEFAULT_MAPPING_GROUP;
import static org.apache.dubbo.metadata.ServiceNameMapping.getAppNames;

/**
 * metadata report impl for nacos
 */
public class NacosMetadataReport extends AbstractMetadataReport {

    private NacosConfigServiceWrapper configService;

    private Gson gson = new Gson();

    /**
     * The group used to store metadata in Nacos
     */
    private String group;

    private Map<String, NacosConfigListener> watchListenerMap = new ConcurrentHashMap<>();

    private Map<String, MappingDataListener> casListenerMap = new ConcurrentHashMap<>();

    private MD5Utils md5Utils = new MD5Utils();

    public NacosMetadataReport(URL url) {
        super(url);
        this.configService = buildConfigService(url);
        group = url.getParameter(GROUP_KEY, DEFAULT_ROOT);
    }

    public NacosConfigServiceWrapper buildConfigService(URL url) {
        Properties nacosProperties = buildNacosProperties(url);
        try {
            configService = new NacosConfigServiceWrapper(NacosFactory.createConfigService(nacosProperties));
        } catch (NacosException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getErrMsg(), e);
            }
            throw new IllegalStateException(e);
        }
        return configService;
    }

    private Properties buildNacosProperties(URL url) {
        Properties properties = new Properties();
        setServerAddr(url, properties);
        setProperties(url, properties);
        return properties;
    }

    private void setServerAddr(URL url, Properties properties) {
        StringBuilder serverAddrBuilder =
                new StringBuilder(url.getHost()) // Host
                        .append(':')
                        .append(url.getPort()); // Port
        // Append backup parameter as other servers
        String backup = url.getParameter(BACKUP_KEY);
        if (backup != null) {
            serverAddrBuilder.append(',').append(backup);
        }
        String serverAddr = serverAddrBuilder.toString();
        properties.put(SERVER_ADDR, serverAddr);
    }

    private static void setProperties(URL url, Properties properties) {
        // Get the parameters from constants
        Map<String, String> parameters = url.getParameters(of(PropertyKeyConst.class));
        // Put all parameters
        properties.putAll(parameters);
    }

    private static void putPropertyIfAbsent(URL url, Properties properties, String propertyName) {
        String propertyValue = url.getParameter(propertyName);
        if (StringUtils.isNotEmpty(propertyValue)) {
            properties.setProperty(propertyName, propertyValue);
        }
    }

    private static void putPropertyIfAbsent(URL url, Properties properties, String propertyName, String defaultValue) {
        String propertyValue = url.getParameter(propertyName);
        if (StringUtils.isNotEmpty(propertyValue)) {
            properties.setProperty(propertyName, propertyValue);
        } else {
            properties.setProperty(propertyName, defaultValue);
        }
    }

    @Override
    public void publishAppMetadata(SubscriberMetadataIdentifier identifier, MetadataInfo metadataInfo) {
        String content = gson.toJson(metadataInfo);
        try {
            configService.publishConfig(identifier.getApplication(), identifier.getRevision(), content);
        } catch (NacosException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public void unPublishAppMetadata(SubscriberMetadataIdentifier identifier, MetadataInfo metadataInfo) {
        try {
            configService.removeConfig(identifier.getApplication(), identifier.getRevision());
        } catch (NacosException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public MetadataInfo getAppMetadata(SubscriberMetadataIdentifier identifier, Map<String, String> instanceMetadata) {
        try {
            String content = configService.getConfig(identifier.getApplication(), identifier.getRevision(), 3000L);
            return gson.fromJson(content, MetadataInfo.class);
        } catch (NacosException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    protected void doStoreProviderMetadata(MetadataIdentifier providerMetadataIdentifier, String serviceDefinitions) {
        this.storeMetadata(providerMetadataIdentifier, serviceDefinitions);
    }

    @Override
    protected void doStoreConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, String value) {
        this.storeMetadata(consumerMetadataIdentifier, value);
    }

    @Override
    protected void doSaveMetadata(ServiceMetadataIdentifier serviceMetadataIdentifier, URL url) {
        storeMetadata(serviceMetadataIdentifier, URL.encode(url.toFullString()));
    }

    @Override
    protected void doRemoveMetadata(ServiceMetadataIdentifier serviceMetadataIdentifier) {
        deleteMetadata(serviceMetadataIdentifier);
    }

    @Override
    protected List<String> doGetExportedURLs(ServiceMetadataIdentifier metadataIdentifier) {
        String content = getConfig(metadataIdentifier);
        if (StringUtils.isEmpty(content)) {
            return Collections.emptyList();
        }
        return new ArrayList<String>(Arrays.asList(URL.decode(content)));
    }

    @Override
    protected void doSaveSubscriberData(SubscriberMetadataIdentifier subscriberMetadataIdentifier, String urlListStr) {
        storeMetadata(subscriberMetadataIdentifier, urlListStr);
    }

    @Override
    protected String doGetSubscribedURLs(SubscriberMetadataIdentifier subscriberMetadataIdentifier) {
        return getConfig(subscriberMetadataIdentifier);
    }

    @Override
    public String getServiceDefinition(MetadataIdentifier metadataIdentifier) {
        return getConfig(metadataIdentifier);
    }

    @Override
    public boolean registerServiceAppMapping(String key, String group, String content, Object ticket) {
        try {
            if (!(ticket instanceof String)) {
                throw new IllegalArgumentException("nacos publishConfigCas requires string type ticket");
            }
            return configService.publishConfigCas(key, group, content, (String) ticket);
        } catch (NacosException e) {
            logger.warn("nacos publishConfigCas failed.", e);
            return false;
        }
    }

    @Override
    public ConfigItem getConfigItem(String key, String group) {
        String content = getConfig(key, group);
        String casMd5 = "";
        if (StringUtils.isNotEmpty(content)) {
            casMd5 = md5Utils.getMd5(content);
        }
        return new ConfigItem(content, casMd5);
    }

    @Override
    public Set<String> getServiceAppMapping(String serviceKey, MappingListener listener, URL url) {
        String group = DEFAULT_MAPPING_GROUP;

        if (null == casListenerMap.get(buildListenerKey(serviceKey, group))) {
            addCasServiceMappingListener(serviceKey, group, listener);
        }
        String content = getConfig(serviceKey, group);
        return ServiceNameMapping.getAppNames(content);
    }

    @Override
    public void removeServiceAppMappingListener(String serviceKey, MappingListener listener) {
        MappingDataListener mappingDataListener = casListenerMap.get(buildListenerKey(serviceKey, group));
        if (null != mappingDataListener) {
            removeCasServiceMappingListener(serviceKey, group, listener);
        }
    }

    @Override
    public Set<String> getServiceAppMapping(String serviceKey, URL url) {
        String content = getConfig(serviceKey, DEFAULT_MAPPING_GROUP);
        return ServiceNameMapping.getAppNames(content);
    }

    private String getConfig(String dataId, String group) {
        try {
            return configService.getConfig(dataId, group);
        } catch (NacosException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    private void addCasServiceMappingListener(String serviceKey, String group, MappingListener listener) {
        MappingDataListener mappingDataListener = casListenerMap.computeIfAbsent(buildListenerKey(serviceKey, group), k -> new MappingDataListener(serviceKey, group));
        mappingDataListener.addListeners(listener);
        addListener(serviceKey, DEFAULT_MAPPING_GROUP, mappingDataListener);
    }

    private void removeCasServiceMappingListener(String serviceKey, String group, MappingListener listener) {
        MappingDataListener mappingDataListener = casListenerMap.get(buildListenerKey(serviceKey, group));
        if (mappingDataListener != null) {
            mappingDataListener.removeListeners(listener);
            if (mappingDataListener.isEmpty()) {
                removeListener(serviceKey, DEFAULT_MAPPING_GROUP, mappingDataListener);
                casListenerMap.remove(buildListenerKey(serviceKey, group), mappingDataListener);
            }
        }
    }

    public void addListener(String key, String group, ConfigurationListener listener) {
        String listenerKey = buildListenerKey(key, group);
        NacosConfigListener nacosConfigListener =
                watchListenerMap.computeIfAbsent(listenerKey, k -> createTargetListener(key, group));
        nacosConfigListener.addListener(listener);
        try {
            configService.addListener(key, group, nacosConfigListener);
        } catch (NacosException e) {
            logger.error(e.getMessage());
        }
    }

    public void removeListener(String key, String group, ConfigurationListener listener) {
        String listenerKey = buildListenerKey(key, group);
        NacosConfigListener nacosConfigListener = watchListenerMap.get(listenerKey);
        try {
            if (nacosConfigListener != null) {
                nacosConfigListener.removeListener(listener);
                if (nacosConfigListener.isEmpty()) {
                    configService.removeListener(key, group, nacosConfigListener);
                    watchListenerMap.remove(listenerKey);
                }
            }
        } catch (NacosException e) {
            logger.error(e.getMessage());
        }
    }

    private NacosConfigListener createTargetListener(String key, String group) {
        NacosConfigListener configListener = new NacosConfigListener();
        configListener.fillContext(key, group);
        return configListener;
    }

    private String buildListenerKey(String key, String group) {
        return key + HYPHEN_CHAR + group;
    }


    private void storeMetadata(BaseMetadataIdentifier identifier, String value) {
        try {
            boolean publishResult = configService.publishConfig(identifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), group, value);
            if (!publishResult) {
                throw new RuntimeException("publish nacos metadata failed");
            }
        } catch (Throwable t) {
            logger.error("Failed to put " + identifier + " to nacos " + value + ", cause: " + t.getMessage(), t);
            throw new RuntimeException("Failed to put " + identifier + " to nacos " + value + ", cause: " + t.getMessage(), t);
        }
    }

    private void deleteMetadata(BaseMetadataIdentifier identifier) {
        try {
            boolean publishResult = configService.removeConfig(identifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), group);
            if (!publishResult) {
                throw new RuntimeException("remove nacos metadata failed");
            }
        } catch (Throwable t) {
            logger.error("Failed to remove " + identifier + " from nacos , cause: " + t.getMessage(), t);
            throw new RuntimeException("Failed to remove " + identifier + " from nacos , cause: " + t.getMessage(), t);
        }
    }

    private String getConfig(BaseMetadataIdentifier identifier) {
        try {
            return configService.getConfig(identifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), group, 3000L);
        } catch (Throwable t) {
            logger.error("Failed to get " + identifier + " from nacos , cause: " + t.getMessage(), t);
            throw new RuntimeException("Failed to get " + identifier + " from nacos , cause: " + t.getMessage(), t);
        }
    }

    public class NacosConfigListener extends AbstractSharedListener {

        private Set<ConfigurationListener> listeners = new CopyOnWriteArraySet<>();
        /**
         * cache data to store old value
         */
        private Map<String, String> cacheData = new ConcurrentHashMap<>();

        @Override
        public Executor getExecutor() {
            return null;
        }

        /**
         * receive
         *
         * @param dataId     data ID
         * @param group      group
         * @param configInfo content
         */
        @Override
        public void innerReceive(String dataId, String group, String configInfo) {
            String oldValue = cacheData.get(dataId);
            ConfigChangedEvent event = new ConfigChangedEvent(dataId, group, configInfo, getChangeType(configInfo, oldValue));
            if (configInfo == null) {
                cacheData.remove(dataId);
            } else {
                cacheData.put(dataId, configInfo);
            }
            listeners.forEach(listener -> listener.process(event));
        }

        void addListener(ConfigurationListener configurationListener) {

            this.listeners.add(configurationListener);
        }

        void removeListener(ConfigurationListener configurationListener) {
            this.listeners.remove(configurationListener);
        }

        boolean isEmpty() {
            return this.listeners.isEmpty();
        }

        private ConfigChangeType getChangeType(String configInfo, String oldValue) {
            if (StringUtils.isBlank(configInfo)) {
                return ConfigChangeType.DELETED;
            }
            if (StringUtils.isBlank(oldValue)) {
                return ConfigChangeType.ADDED;
            }
            return ConfigChangeType.MODIFIED;
        }
    }

    static class MappingDataListener implements ConfigurationListener {

        private String dataId;

        private String groupId;

        private String serviceKey;

        private Set<MappingListener> listeners;

        public MappingDataListener(String dataId, String groupId) {
            this.serviceKey = dataId;
            this.dataId = dataId;
            this.groupId = groupId;
            this.listeners = new HashSet<>();
        }

        public void addListeners(MappingListener mappingListener) {
            listeners.add(mappingListener);
        }

        public void removeListeners(MappingListener mappingListener) {
            listeners.remove(mappingListener);
        }

        public boolean isEmpty() {
            return listeners.isEmpty();
        }

        @Override
        public void process(ConfigChangedEvent event) {
            if (ConfigChangeType.DELETED == event.getChangeType()) {
                return;
            }
            if (!dataId.equals(event.getKey()) || !groupId.equals(event.getGroup())) {
                return;
            }

            Set<String> apps = getAppNames(event.getContent());

            MappingChangedEvent mappingChangedEvent = new MappingChangedEvent(serviceKey, apps);

            listeners.forEach(listener -> listener.onEvent(mappingChangedEvent));
        }
    }
}
