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
package org.apache.dubbo.configcenter.support.apollo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ctrip.framework.apollo.model.ConfigChange;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.CHECK_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CLUSTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.CONFIG_NAMESPACE_KEY;

/**
 * Apollo implementation, https://github.com/ctripcorp/apollo
 * <p>
 * Apollo will be used for management of both governance rules and .properties files, by default, these two different
 * kinds of data share the same namespace 'dubbo'. To gain better performance, we recommend separate them by giving
 * namespace and group different values, for example:
 * <p>
 * <dubbo:config-center namespace="governance" group="dubbo" />, 'dubbo=governance' is for governance rules while
 * 'group=dubbo' is for properties files.
 * <p>
 * Please see http://dubbo.apache.org/zh-cn/docs/user/configuration/config-center.html for details.
 */
public class ApolloDynamicConfiguration implements DynamicConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(ApolloDynamicConfiguration.class);
    private static final String APOLLO_ENV_KEY = "env";
    private static final String APOLLO_ADDR_KEY = "apollo.meta";
    private static final String APOLLO_CLUSTER_KEY = "apollo.cluster";
    private static final String APOLLO_PROTOCOL_PREFIX = "http://";
    private static final String APOLLO_APPLICATION_KEY = "application";
    private static final String APOLLO_APPID_KEY = "app.id";

    private final URL url;
    private final Config dubboConfig;
    private final ConfigFile dubboConfigFile;
    private final ConcurrentMap<String, ApolloListener> listeners = new ConcurrentHashMap<>();

    ApolloDynamicConfiguration(URL url) {
        this.url = url;
        // Instead of using Dubbo's configuration, I would suggest use the original configuration method Apollo provides.
        String configEnv = url.getParameter(APOLLO_ENV_KEY);
        String configAddr = getAddressWithProtocolPrefix(url);
        String configCluster = url.getParameter(CLUSTER_KEY);
        String configAppId = url.getParameter(APOLLO_APPID_KEY);
        if (StringUtils.isEmpty(System.getProperty(APOLLO_ENV_KEY)) && configEnv != null) {
            System.setProperty(APOLLO_ENV_KEY, configEnv);
        }
        if (StringUtils.isEmpty(System.getProperty(APOLLO_ADDR_KEY)) && !ANYHOST_VALUE.equals(url.getHost())) {
            System.setProperty(APOLLO_ADDR_KEY, configAddr);
        }
        if (StringUtils.isEmpty(System.getProperty(APOLLO_CLUSTER_KEY)) && configCluster != null) {
            System.setProperty(APOLLO_CLUSTER_KEY, configCluster);
        }
        if (StringUtils.isEmpty(System.getProperty(APOLLO_APPID_KEY)) && configAppId != null) {
            System.setProperty(APOLLO_APPID_KEY, configAppId);
        }

        String namespace = url.getParameter(CONFIG_NAMESPACE_KEY, DEFAULT_GROUP);
        String apolloNamespace = StringUtils.isEmpty(namespace) ? url.getGroup(DEFAULT_GROUP) : namespace;
        dubboConfig = ConfigService.getConfig(apolloNamespace);
        dubboConfigFile = ConfigService.getConfigFile(apolloNamespace, ConfigFileFormat.Properties);

        // Decide to fail or to continue when failed to connect to remote server.
        boolean check = url.getParameter(CHECK_KEY, true);
        if (dubboConfig.getSourceType() != ConfigSourceType.REMOTE) {
            if (check) {
                throw new IllegalStateException("Failed to connect to config center, the config center is Apollo, " +
                    "the address is: " + (StringUtils.isNotEmpty(configAddr) ? configAddr : configEnv));
            } else {
                logger.warn("Failed to connect to config center, the config center is Apollo, " +
                    "the address is: " + (StringUtils.isNotEmpty(configAddr) ? configAddr : configEnv) +
                    ", will use the local cache value instead before eventually the connection is established.");
            }
        }
    }

    @Override
    public void close() {
        try {
            listeners.clear();
        } catch (UnsupportedOperationException e) {
            logger.warn("Failed to close connect from config center, the config center is Apollo");
        }
    }

    private String getAddressWithProtocolPrefix(URL url) {
        String address = url.getBackupAddress();
        if (StringUtils.isNotEmpty(address)) {
            address = Arrays.stream(COMMA_SPLIT_PATTERN.split(address))
                .map(addr -> {
                    if (addr.startsWith(APOLLO_PROTOCOL_PREFIX)) {
                        return addr;
                    }
                    return APOLLO_PROTOCOL_PREFIX + addr;
                })
                .collect(Collectors.joining(","));
        }
        return address;
    }

    /**
     * Since all governance rules will lay under dubbo group, this method now always uses the default dubboConfig and
     * ignores the group parameter.
     */
    @Override
    public void addListener(String key, String group, ConfigurationListener listener) {
        ApolloListener apolloListener = listeners.computeIfAbsent(group + key, k -> createTargetListener(key, group));
        apolloListener.addListener(listener);
        dubboConfig.addChangeListener(apolloListener, Collections.singleton(key));
    }

    @Override
    public void removeListener(String key, String group, ConfigurationListener listener) {
        ApolloListener apolloListener = listeners.get(group + key);
        if (apolloListener != null) {
            apolloListener.removeListener(listener);
            if (!apolloListener.hasInternalListener()) {
                dubboConfig.removeChangeListener(apolloListener);
            }
        }
    }

    @Override
    public String getConfig(String key, String group, long timeout) throws IllegalStateException {
        if (StringUtils.isNotEmpty(group)) {
            if (group.equals(url.getApplication())) {
                return ConfigService.getAppConfig().getProperty(key, null);
            } else {
                return ConfigService.getConfig(group).getProperty(key, null);
            }
        }
        return dubboConfig.getProperty(key, null);
    }

    /**
     * Recommend specify namespace and group when using Apollo.
     * <p>
     * <dubbo:config-center namespace="governance" group="dubbo" />, 'dubbo=governance' is for governance rules while
     * 'group=dubbo' is for properties files.
     *
     * @param key     default value is 'dubbo.properties', currently useless for Apollo.
     * @param group
     * @param timeout
     * @return
     * @throws IllegalStateException
     */
    @Override
    public String getProperties(String key, String group, long timeout) throws IllegalStateException {
        if (StringUtils.isEmpty(group)) {
            return dubboConfigFile.getContent();
        }
        if (group.equals(url.getApplication())) {
            return ConfigService.getConfigFile(APOLLO_APPLICATION_KEY, ConfigFileFormat.Properties).getContent();
        }

        ConfigFile configFile = ConfigService.getConfigFile(group, ConfigFileFormat.Properties);
        if (configFile == null) {
            throw new IllegalStateException("There is no namespace named " + group + " in Apollo.");
        }
        return configFile.getContent();
    }

    /**
     * This method will be used by Configuration to get valid value at runtime.
     * The group is expected to be 'app level', which can be fetched from the 'config.appnamespace' in url if necessary.
     * But I think Apollo's inheritance feature of namespace can solve the problem .
     */
    @Override
    public String getInternalProperty(String key) {
        return dubboConfig.getProperty(key, null);
    }

    /**
     * Ignores the group parameter.
     *
     * @param key   property key the native listener will listen on
     * @param group to distinguish different set of properties
     * @return
     */
    private ApolloListener createTargetListener(String key, String group) {
        return new ApolloListener();
    }

    public class ApolloListener implements ConfigChangeListener {

        private Set<ConfigurationListener> listeners = new CopyOnWriteArraySet<>();

        ApolloListener() {
        }

        @Override
        public void onChange(com.ctrip.framework.apollo.model.ConfigChangeEvent changeEvent) {
            for (String key : changeEvent.changedKeys()) {
                ConfigChange change = changeEvent.getChange(key);
                if ("".equals(change.getNewValue())) {
                    logger.warn("an empty rule is received for " + key + ", the current working rule is " +
                        change.getOldValue() + ", the empty rule will not take effect.");
                    return;
                }

                ConfigChangedEvent event = new ConfigChangedEvent(key, change.getNamespace(), change.getNewValue(), getChangeType(change));
                listeners.forEach(listener -> listener.process(event));
            }
        }

        private ConfigChangeType getChangeType(ConfigChange change) {
            if (change.getChangeType() == PropertyChangeType.DELETED) {
                return ConfigChangeType.DELETED;
            }
            return ConfigChangeType.MODIFIED;
        }

        void addListener(ConfigurationListener configurationListener) {
            this.listeners.add(configurationListener);
        }

        void removeListener(ConfigurationListener configurationListener) {
            this.listeners.remove(configurationListener);
        }

        boolean hasInternalListener() {
            return listeners != null && listeners.size() > 0;
        }
    }

}
