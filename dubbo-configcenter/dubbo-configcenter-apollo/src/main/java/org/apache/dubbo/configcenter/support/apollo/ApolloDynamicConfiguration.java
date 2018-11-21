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

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.configcenter.AbstractDynamicConfiguration;
import org.apache.dubbo.configcenter.ConfigChangeType;
import org.apache.dubbo.configcenter.ConfigType;
import org.apache.dubbo.configcenter.ConfigurationListener;

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class ApolloDynamicConfiguration extends AbstractDynamicConfiguration<ConfigChangeListener> {
    private static final Logger logger = LoggerFactory.getLogger(ApolloDynamicConfiguration.class);
    private static final String APOLLO_ENV_KEY = "env";
    private static final String APOLLO_ADDR_KEY = "apollo.meta";
    private static final String APOLLO_CLUSTER_KEY = "apollo.cluster";

    private Config dubboConfig;

    public ApolloDynamicConfiguration() {

    }

    @Override
    public void init() {
        /**
         * Instead of using Dubbo's configuration, I would suggest use the original configuration method Apollo provides.
         */
        String configEnv = url.getParameter(Constants.CONFIG_ENV_KEY);
        String configAddr = url.getBackupAddress();
        String configCluster = url.getParameter(Constants.CONFIG_CLUSTER_KEY);
        if (configEnv != null) {
            System.setProperty(APOLLO_ENV_KEY, configEnv);
        }
        if (StringUtils.isEmpty(configEnv) && !Constants.ANYHOST_VALUE.equals(configAddr)) {
            System.setProperty(APOLLO_ADDR_KEY, configAddr);
        }
        if (configCluster != null) {
            System.setProperty(APOLLO_CLUSTER_KEY, configCluster);
        }

        dubboConfig = ConfigService.getConfig(url.getParameter(Constants.CONFIG_GROUP_KEY, DEFAULT_GROUP));
        // Decide to fail or to continue when failed to connect to remote server.
        boolean check = url.getParameter(Constants.CONFIG_CHECK_KEY, false);
        if (dubboConfig.getSourceType() != ConfigSourceType.REMOTE) {
            if (check) {
                throw new IllegalStateException("Failed to connect to ConfigCenter, the ConfigCenter is Apollo, the address is: " + (StringUtils.isNotEmpty(configAddr) ? configAddr : configEnv));
            } else {
                logger.warn("Failed to connect to ConfigCenter, the ConfigCenter is Apollo, " +
                        "the address is: " + (StringUtils.isNotEmpty(configAddr) ? configAddr : configEnv) +
                        ". will use the local cache value instead before finally connected.");
            }
        }
    }

    /**
     * This method will being used to,
     * 1. get configuration file at startup phase
     * 2. get all kinds of Dubbo rules
     *
     * @param key
     * @param group
     * @param timeout
     * @return
     */
    @Override
    protected String getInternalProperty(String key, String group, long timeout) {
        if (StringUtils.isNotEmpty(group) && !url.getParameter(Constants.CONFIG_GROUP_KEY, DEFAULT_GROUP).equals(group)) {
            Config config = ConfigService.getConfig(group);
            if (config != null) {
                return config.getProperty(key, null);
            }
            return null;
        }
        return dubboConfig.getProperty(key, null);
    }

    /**
     * This method will used by Configuration to get valid value at runtime.
     * The group is expected to be 'app level', which can be fetched from the 'config.appnamespace' in url if necessary.
     * But I think Apollo's inheritance feature of namespace can solve the problem .
     *
     * @param key
     * @return
     */
    @Override
    protected String getInternalProperty(String key) {
        return dubboConfig.getProperty(key, null);
    }

    @Override
    protected void addTargetListener(String key, ConfigChangeListener listener) {
        Set<String> keys = new HashSet<>(1);
        keys.add(key);
        this.dubboConfig.addChangeListener(listener, keys);
    }

    @Override
    protected ConfigChangeListener createTargetConfigListener(String key, ConfigurationListener listener) {
        return new ApolloListener(listener);
    }

    public ConfigChangeType getChangeType(ConfigChange change) {
        if (change.getChangeType() == PropertyChangeType.DELETED) {
            return ConfigChangeType.DELETED;
        }
        return ConfigChangeType.MODIFIED;
    }

    private class ApolloListener implements ConfigChangeListener {
        private ConfigurationListener listener;
        private URL url;

        public ApolloListener(ConfigurationListener listener) {
            this(listener.getUrl(), listener);
        }

        public ApolloListener(URL url, ConfigurationListener listener) {
            this.url = url;
            this.listener = listener;
        }

        @Override
        public void onChange(ConfigChangeEvent changeEvent) {
            for (String key : changeEvent.changedKeys()) {
                ConfigChange change = changeEvent.getChange(key);
                if (StringUtils.isEmpty(change.getNewValue())) {
                    logger.warn("We received an empty rule for " + key + ", the current working rule is " + change.getOldValue() + ", the empty rule will not take effect.");
                    return;
                }
                // TODO Maybe we no longer need to identify the type of change. Because there's no scenario that a callback will subscribe for both configurators and routers
                if (change.getPropertyName().endsWith(Constants.CONFIGURATORS_SUFFIX)) {
                    listener.process(new org.apache.dubbo.configcenter.ConfigChangeEvent(key, change.getNewValue(), ConfigType.CONFIGURATORS, getChangeType(change)));
                } else {
                    listener.process(new org.apache.dubbo.configcenter.ConfigChangeEvent(key, change.getNewValue(), ConfigType.ROUTERS, getChangeType(change)));
                }
            }
        }
    }

}
