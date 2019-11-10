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

import static com.alibaba.nacos.api.PropertyKeyConst.ACCESS_KEY;
import static com.alibaba.nacos.api.PropertyKeyConst.CLUSTER_NAME;
import static com.alibaba.nacos.api.PropertyKeyConst.ENCODE;
import static com.alibaba.nacos.api.PropertyKeyConst.ENDPOINT;
import static com.alibaba.nacos.api.PropertyKeyConst.NAMESPACE;
import static com.alibaba.nacos.api.PropertyKeyConst.SECRET_KEY;
import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;
import static com.alibaba.nacos.client.naming.utils.UtilAndComs.NACOS_NAMING_LOG_NAME;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_CHAR_SEPERATOR;
import static org.apache.dubbo.common.constants.RemotingConstants.BACKUP_KEY;

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

    public void publishNacosConfig(String key, String value) {
        String[] keyAndGroup = getKeyAndGroup(key);
        publishConfig(keyAndGroup[0], keyAndGroup[1], value);
    }

    @Override
    public boolean publishConfig(String key, String group, String content) {
        boolean published = false;
        try {
            published = configService.publishConfig(key, group, content);
        } catch (NacosException e) {
            logger.error(e.getErrMsg());
        }
        return published;
    }

    private String[] getKeyAndGroup(String key) {
        int i = key.lastIndexOf(GROUP_CHAR_SEPERATOR);
        if (i < 0) {
            return new String[]{key, null};
        } else {
            return new String[]{key.substring(0, i), key.substring(i + 1)};
        }
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

    private void setProperties(URL url, Properties properties) {
        putPropertyIfAbsent(url, properties, NAMESPACE);
        putPropertyIfAbsent(url, properties, NACOS_NAMING_LOG_NAME);
        putPropertyIfAbsent(url, properties, ENDPOINT);
        putPropertyIfAbsent(url, properties, ACCESS_KEY);
        putPropertyIfAbsent(url, properties, SECRET_KEY);
        putPropertyIfAbsent(url, properties, CLUSTER_NAME);
        putPropertyIfAbsent(url, properties, ENCODE);
    }

    private void putPropertyIfAbsent(URL url, Properties properties, String propertyName) {
        String propertyValue = url.getParameter(propertyName);
        if (StringUtils.isNotEmpty(propertyValue)) {
            properties.setProperty(propertyName, propertyValue);
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
        NacosConfigListener nacosConfigListener = watchListenerMap.computeIfAbsent(key, k -> createTargetListener(key, group));
        nacosConfigListener.addListener(listener);
        try {
            configService.addListener(key, group, nacosConfigListener);
        } catch (NacosException e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public void removeListener(String key, String group, ConfigurationListener listener) {
        NacosConfigListener eventListener = watchListenerMap.get(key);
        if (eventListener != null) {
            eventListener.removeListener(listener);
        }
    }

    @Override
    public String getConfig(String key, String group, long timeout) throws IllegalStateException {
        try {
            long nacosTimeout = timeout < 0 ? DEFAULT_TIMEOUT : timeout;
            if (StringUtils.isEmpty(group)) {
                group = DEFAULT_GROUP;
            }
            return configService.getConfig(key, group, nacosTimeout);
        } catch (NacosException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    @Override
    public Object getInternalProperty(String key) {
        try {
            return configService.getConfig(key, DEFAULT_GROUP, DEFAULT_TIMEOUT);
        } catch (NacosException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    @Override
    public SortedSet<String> getConfigKeys(String group) {
        // TODO use Nacos Client API to replace HTTP Open API
        SortedSet<String> keys = new TreeSet<>();
        try {
            List<String> paramsValues = asList("search", "accurate", "dataId", "", "group", group, "pageNo", "1", "pageSize", String.valueOf(Integer.MAX_VALUE));
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
}
