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

package org.apache.dubbo.configcenter.support.nacos;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.ConfigItem;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.MD5Utils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.mapping.MappingChangedEvent;
import org.apache.dubbo.mapping.MappingListener;
import org.apache.dubbo.mapping.ServiceNameMapping;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.AbstractSharedListener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.http.HttpAgent;
import com.alibaba.nacos.common.http.HttpRestResult;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import static com.alibaba.nacos.api.PropertyKeyConst.ENCODE;
import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;
import static java.util.Collections.emptyMap;
import static org.apache.dubbo.common.constants.RemotingConstants.BACKUP_KEY;
import static org.apache.dubbo.common.utils.StringConstantFieldValuePredicate.of;
import static org.apache.dubbo.common.utils.StringUtils.HYPHEN_CHAR;
import static org.apache.dubbo.common.utils.StringUtils.SLASH_CHAR;
import static org.apache.dubbo.common.utils.StringUtils.isBlank;
import static org.apache.dubbo.mapping.ServiceNameMapping.DEFAULT_MAPPING_GROUP;
import static org.apache.dubbo.mapping.ServiceNameMapping.getAppNames;

/**
 * The nacos implementation of {@link DynamicConfiguration}
 */
public class NacosDynamicConfiguration implements DynamicConfiguration {

    private static final String GET_CONFIG_KEYS_PATH = "/v1/cs/configs";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * the default timeout in millis to get config from nacos
     */
    private static final long DEFAULT_TIMEOUT = 5000L;

    private Properties nacosProperties;

    /**
     * The nacos configService
     */
    private final NacosConfigServiceWrapper configService;

    private HttpAgent httpAgent;

    private Map<String, MappingDataListener> casListenerMap = new ConcurrentHashMap<>();

    /**
     * The map store the key to {@link NacosConfigListener} mapping
     */
    private final ConcurrentMap<String, NacosConfigListener> watchListenerMap;

    NacosDynamicConfiguration(URL url) {
        this.nacosProperties = buildNacosProperties(url);
        this.configService = buildConfigService(url);
        this.httpAgent = getHttpAgent(configService.getConfigService());
        watchListenerMap = new ConcurrentHashMap<>();
    }

    private NacosConfigServiceWrapper buildConfigService(URL url) {
        ConfigService configService = null;
        try {
            configService = NacosFactory.createConfigService(nacosProperties);
        } catch (NacosException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getErrMsg(), e);
            }
            throw new IllegalStateException(e);
        }
        return new NacosConfigServiceWrapper(configService);
    }

    private HttpAgent getHttpAgent(ConfigService configService) {
        HttpAgent agent = null;
        try {
            Field field = configService.getClass().getDeclaredField("agent");
            field.setAccessible(true);
            agent = (HttpAgent) field.get(configService);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return agent;
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
                        .append(":")
                        .append(url.getPort()); // Port

        // Append backup parameter as other servers
        String backup = url.getParameter(BACKUP_KEY);
        if (backup != null) {
            serverAddrBuilder.append(",").append(backup);
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
    public Set<String> getServiceAppMapping(String serviceKey, MappingListener listener, URL url) {
        String serviceInterface = url.getServiceInterface();
        return getConfigKeys(ServiceNameMapping.buildGroup(serviceInterface));
    }

    /**
     * Ignores the group parameter.
     *
     * @param key   property key the native listener will listen on
     * @param group to distinguish different set of properties
     * @return
     */
    private NacosConfigListener createTargetListener(String key, String group) {
        NacosConfigListener configListener = new NacosConfigListener();
        configListener.fillContext(key, group);
        return configListener;
    }

    @Override
    public void addListener(String key, String group, ConfigurationListener listener) {
        String resolvedGroup = resolveGroup(group);
        String listenerKey = buildListenerKey(key, group);
        NacosConfigListener nacosConfigListener =
                watchListenerMap.computeIfAbsent(listenerKey, k -> createTargetListener(key, resolvedGroup));
        nacosConfigListener.addListener(listener);
        try {
            configService.addListener(key, resolvedGroup, nacosConfigListener);
        } catch (NacosException e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public Set<String> getCasServiceAppMapping(String serviceKey, MappingListener listener, URL url) {
        String group = DEFAULT_MAPPING_GROUP;

        if (null == casListenerMap.get(buildListenerKey(serviceKey, group))) {
            addCasServiceMappingListener(serviceKey, group, listener);
        }

        String content = getConfig(serviceKey, group);

        return ServiceNameMapping.getAppNames(content);
    }

    private void addCasServiceMappingListener(String serviceKey, String group, MappingListener listener) {
        MappingDataListener mappingDataListener = casListenerMap.computeIfAbsent(buildListenerKey(serviceKey, group), k -> new MappingDataListener(serviceKey, group));
        mappingDataListener.addListeners(listener);
        addListener(serviceKey, DEFAULT_MAPPING_GROUP, mappingDataListener);
    }

    @Override
    public void removeListener(String key, String group, ConfigurationListener listener) {
        String listenerKey = buildListenerKey(key, group);
        NacosConfigListener eventListener = watchListenerMap.get(listenerKey);
        if (eventListener != null) {
            eventListener.removeListener(listener);
        }
    }

    @Override
    public String getConfig(String key, String group, long timeout) throws IllegalStateException {
        String resolvedGroup = resolveGroup(group);
        try {
            long nacosTimeout = timeout < 0 ? getDefaultTimeout() : timeout;
            if (StringUtils.isEmpty(resolvedGroup)) {
                resolvedGroup = DEFAULT_GROUP;
            }
            return configService.getConfig(key, resolvedGroup, nacosTimeout);
        } catch (NacosException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    @Override
    public ConfigItem getConfigItem(String key, String group) {
        String content = getConfig(key, group);
        String casMd5 = "";
        if (StringUtils.isNotEmpty(content)) {
            casMd5 = MD5Utils.getMd5(content);
        }
        return new ConfigItem(content, casMd5);
    }

    @Override
    public Object getInternalProperty(String key) {
        try {
            return configService.getConfig(key, DEFAULT_GROUP, getDefaultTimeout());
        } catch (NacosException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    @Override
    public boolean publishConfig(String key, String group, String content) {
        boolean published = false;
        String resolvedGroup = resolveGroup(group);
        try {
            published = configService.publishConfig(key, resolvedGroup, content);
        } catch (NacosException e) {
            logger.error(e.getErrMsg(), e);
        }
        return published;
    }

    @Override
    public boolean publishConfigCas(String key, String group, String content, Object ticket) {
        String resolvedGroup = resolveGroup(group);
        try {
            if (!(ticket instanceof String)) {
                throw new IllegalArgumentException("nacos publishConfigCas requires string type ticket");
            }
            return configService.publishConfigCas(key, resolvedGroup, content, (String) ticket);
        } catch (NacosException e) {
            logger.warn("nacos publishConfigCas failed.", e);
            return false;
        }
    }

    @Override
    public long getDefaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    /**
     * TODO Nacos does not support atomic update of the value mapped to a key.
     *
     * @param group the specified group
     * @return
     */
    @Override
    public SortedSet<String> getConfigKeys(String group) {
        // TODO use Nacos Client API to replace HTTP Open API
        SortedSet<String> keys = new TreeSet<>();
        try {

            Map<String, String> paramsValues = new HashMap<>();
            paramsValues.put("search", "accurate");
            paramsValues.put("dataId", "");
            paramsValues.put("group", resolveGroup(group));
            paramsValues.put("pageNo", "1");
            paramsValues.put("pageSize", String.valueOf(Integer.MAX_VALUE));

            String encoding = getProperty(ENCODE, "UTF-8");

            HttpRestResult<String> result = httpAgent.httpGet(GET_CONFIG_KEYS_PATH, emptyMap(), paramsValues, encoding, 5 * 1000);
            Stream<String> keysStream = toKeysStream(result.getData());
            keysStream.forEach(keys::add);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
        return keys;
    }

    @Override
    public boolean removeConfig(String key, String group) {
        boolean removed = false;
        try {
            removed = configService.removeConfig(key, group);
        } catch (NacosException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
        return removed;
    }

    private Stream<String> toKeysStream(String content) {
        JSONObject jsonObject = JSON.parseObject(content);
        JSONArray pageItems = jsonObject.getJSONArray("pageItems");
        return pageItems.stream()
                .map(object -> (JSONObject) object)
                .map(json -> json.getString("dataId"));
    }

    private String getProperty(String name, String defaultValue) {
        return nacosProperties.getProperty(name, defaultValue);
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

        @Override
        public void process(ConfigChangedEvent event) {
            if (ConfigChangeType.DELETED == event.getChangeType()) {
                return;
            }
            if (!dataId.equals(event.getKey()) || !groupId.equals(event.getGroup())) {
                return;
            }

            Set<String> apps = getAppNames(event.getContent());

            MappingChangedEvent mappingChangedEvent = MappingChangedEvent.buildCasModelEvent(serviceKey, apps);

            listeners.forEach(listener -> listener.onEvent(mappingChangedEvent));
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

    protected String buildListenerKey(String key, String group) {
        return key + HYPHEN_CHAR + resolveGroup(group);
    }

    protected String resolveGroup(String group) {
        return isBlank(group) ? group : group.replace(SLASH_CHAR, HYPHEN_CHAR);
    }

    @Override
    public boolean isSupportCas() {
        return true;
    }
}
