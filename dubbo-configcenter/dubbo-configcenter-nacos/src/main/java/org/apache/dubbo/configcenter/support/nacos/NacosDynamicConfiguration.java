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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.configcenter.ConfigChangeEvent;
import org.apache.dubbo.configcenter.ConfigChangeType;
import org.apache.dubbo.configcenter.ConfigurationListener;
import org.apache.dubbo.configcenter.DynamicConfiguration;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.AbstractSharedListener;
import com.alibaba.nacos.api.exception.NacosException;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;

import static com.alibaba.nacos.api.PropertyKeyConst.ACCESS_KEY;
import static com.alibaba.nacos.api.PropertyKeyConst.CLUSTER_NAME;
import static com.alibaba.nacos.api.PropertyKeyConst.ENDPOINT;
import static com.alibaba.nacos.api.PropertyKeyConst.NAMESPACE;
import static com.alibaba.nacos.api.PropertyKeyConst.SECRET_KEY;
import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;
import static com.alibaba.nacos.client.naming.utils.UtilAndComs.NACOS_NAMING_LOG_NAME;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_CHAR_SEPERATOR;
import static org.apache.dubbo.common.constants.CommonConstants.PROPERTIES_CHAR_SEPERATOR;
import static org.apache.dubbo.configcenter.Constants.CONFIG_NAMESPACE_KEY;
import static org.apache.dubbo.common.constants.RemotingConstants.BACKUP_KEY;

/**
 * The nacos implementation of {@link DynamicConfiguration}
 */
public class NacosDynamicConfiguration implements DynamicConfiguration {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The final root path would be: /$NAME_SPACE/config
     */
    private String rootPath;

    /**
     * The nacos configService
     */

    private ConfigService configService;

    /**
     * The map store the key to {@link NacosConfigListener} mapping
     */
    private final ConcurrentMap<String, NacosConfigListener> watchListenerMap;

    NacosDynamicConfiguration(URL url) {
        rootPath = url.getParameter(CONFIG_NAMESPACE_KEY, DEFAULT_GROUP) + "-config";
        buildConfigService(url);
        watchListenerMap = new ConcurrentHashMap<>();
    }

    private ConfigService buildConfigService(URL url) {
        Properties nacosProperties = buildNacosProperties(url);
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

    public void publishNacosConfig(String key, String value) {
        try {
            String[] keyAndGroup = getKeyAndGroup(key);
            configService.publishConfig(keyAndGroup[0], keyAndGroup[1], value);
        } catch (NacosException e) {
            logger.error(e.getErrMsg());
        }
    }

    private String[] getKeyAndGroup(String key) {
        int i = key.lastIndexOf(GROUP_CHAR_SEPERATOR);
        if (i < 0) {
            return new String[]{key, null};
        } else {
            return new String[]{key.substring(0, i), key.substring(i+1)};
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
        String[] keyAndGroup = getKeyAndGroup(key);
        if (keyAndGroup[1] != null) {
            group = keyAndGroup[1];
        }
        String finalGroup = group;
        NacosConfigListener nacosConfigListener = watchListenerMap.computeIfAbsent(generateKey(key, group), k -> createTargetListener(key, finalGroup));
        String keyInNacos = rootPath + PROPERTIES_CHAR_SEPERATOR + key;
        nacosConfigListener.addListener(listener);
        try {
            configService.addListener(keyInNacos, group, nacosConfigListener);
            System.out.println("1");
        } catch (NacosException e) {
            logger.error(e.getMessage());
        }
    }

    private String generateKey(String key, String group) {
        if (StringUtils.isNotEmpty(group)) {
            key = key + GROUP_CHAR_SEPERATOR + group;
        }
        return key;
    }

    @Override
    public void removeListener(String key, String group, ConfigurationListener listener) {
        NacosConfigListener eventListener = watchListenerMap.get(generateKey(key, group));
        if (eventListener != null) {
            eventListener.removeListener(listener);
        }
    }

    @Override
    public String getConfig(String key, String group, long timeout) throws IllegalStateException {
        key = generateKey(key, group);
        return (String) getInternalProperty(rootPath + PROPERTIES_CHAR_SEPERATOR + key);
    }

    @Override
    public String getConfigs(String key, String group, long timeout) throws IllegalStateException {
        return getConfig(key, group, timeout);
    }

    @Override
    public Object getInternalProperty(String key) {
        try {
            String[] keyAndGroup = getKeyAndGroup(key);
            return configService.getConfig(keyAndGroup[0], keyAndGroup[1], 5000L);
        } catch (NacosException e) {
            logger.error(e.getMessage());
        }
        return null;
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
            ConfigChangeEvent event = new ConfigChangeEvent(dataId, configInfo, getChangeType(configInfo, oldValue));
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
