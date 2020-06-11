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
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.AbstractSharedListener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.http.HttpAgent;
import com.alibaba.nacos.client.config.impl.HttpSimpleClient;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
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
import static com.alibaba.nacos.api.PropertyKeyConst.NAMING_LOAD_CACHE_AT_START;
import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;
import static com.alibaba.nacos.client.naming.utils.UtilAndComs.NACOS_NAMING_LOG_NAME;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.dubbo.common.constants.RemotingConstants.BACKUP_KEY;
import static org.apache.dubbo.common.utils.StringConstantFieldValuePredicate.of;
import static org.apache.dubbo.common.utils.StringUtils.HYPHEN_CHAR;
import static org.apache.dubbo.common.utils.StringUtils.SLASH_CHAR;
import static org.apache.dubbo.common.utils.StringUtils.isBlank;

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
    private final ConfigService configService;

    private HttpAgent httpAgent;

    /**
     * The map store the key to {@link NacosConfigListener} mapping
     */
    private final ConcurrentMap<String, NacosConfigListener> watchListenerMap;

    NacosDynamicConfiguration(URL url) {
        this.nacosProperties = buildNacosProperties(url);
        this.configService = buildConfigService(url);
        this.httpAgent = getHttpAgent(configService);
        watchListenerMap = new ConcurrentHashMap<>();
    }

    private ConfigService buildConfigService(URL url) {
        ConfigService configService = null;
        try {
            configService = NacosFactory.createConfigService(nacosProperties);
        } catch (NacosException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getErrMsg(), e);
            }
            throw new IllegalStateException(e);
        }
        return configService;
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
        putPropertyIfAbsent(url, properties, NACOS_NAMING_LOG_NAME);

        // Get the parameters from constants
        Map<String, String> parameters = url.getParameters(of(PropertyKeyConst.class));
        // Put all parameters
        properties.putAll(parameters);

        putPropertyIfAbsent(url, properties, NAMING_LOAD_CACHE_AT_START, "true");
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
        NacosConfigListener nacosConfigListener = watchListenerMap.computeIfAbsent(listenerKey, k -> createTargetListener(key, resolvedGroup));
        nacosConfigListener.addListener(listener);
        try {
            configService.addListener(key, resolvedGroup, nacosConfigListener);
        } catch (NacosException e) {
            logger.error(e.getMessage());
        }
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
            List<String> paramsValues = asList(
                    "search", "accurate",
                    "dataId", "",
                    "group", resolveGroup(group),
                    "pageNo", "1",
                    "pageSize", String.valueOf(Integer.MAX_VALUE)
            );
            String encoding = getProperty(ENCODE, "UTF-8");
            HttpSimpleClient.HttpResult result = httpAgent.httpGet(GET_CONFIG_KEYS_PATH, emptyList(), paramsValues, encoding, 5 * 1000);
            Stream<String> keysStream = toKeysStream(result.content);
            keysStream.forEach(keys::add);
        } catch (IOException e) {
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
}
