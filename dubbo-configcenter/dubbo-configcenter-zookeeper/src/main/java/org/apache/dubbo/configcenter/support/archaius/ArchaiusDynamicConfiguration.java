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
package org.apache.dubbo.configcenter.support.archaius;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.config.DynamicWatchedConfiguration;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.configcenter.AbstractDynamicConfiguration;
import org.apache.dubbo.configcenter.ConfigChangeEvent;
import org.apache.dubbo.configcenter.ConfigChangeType;
import org.apache.dubbo.configcenter.ConfigType;
import org.apache.dubbo.configcenter.ConfigurationListener;
import org.apache.dubbo.configcenter.support.archaius.sources.ZooKeeperConfigurationSource;

import static org.apache.dubbo.common.Constants.CONFIG_NAMESPACE_KEY;
import static org.apache.dubbo.configcenter.support.archaius.sources.ZooKeeperConfigurationSource.ARCHAIUS_CONFIG_CHECK_KEY;
import static org.apache.dubbo.configcenter.support.archaius.sources.ZooKeeperConfigurationSource.ARCHAIUS_CONFIG_ROOT_PATH_KEY;
import static org.apache.dubbo.configcenter.support.archaius.sources.ZooKeeperConfigurationSource.ARCHAIUS_SOURCE_ADDRESS_KEY;
import static org.apache.dubbo.configcenter.support.archaius.sources.ZooKeeperConfigurationSource.DEFAULT_CONFIG_ROOT_PATH;

/**
 * Archaius supports various sources and it's extensiable: JDBC, ZK, Properties, ..., so should we make it extensiable?
 * FIXME: we should get rid of Archaius or move it to eco system since Archaius is out of maintenance, instead, we
 * should rely on curator-x-async which looks quite promising.
 */
public class ArchaiusDynamicConfiguration extends AbstractDynamicConfiguration<Runnable> {
    private static final Logger logger = LoggerFactory.getLogger(ArchaiusDynamicConfiguration.class);

    public ArchaiusDynamicConfiguration() {
    }

    @Override
    public void initWith(URL url) {
        super.initWith(url);

        //  String address = env.getCompositeConf().getString(ADDRESS_KEY);
        //  String app = env.getCompositeConf().getString(APP_KEY);

        String address = url.getBackupAddress();
        if (!address.equals(Constants.ANYHOST_VALUE)) {
            System.setProperty(ARCHAIUS_SOURCE_ADDRESS_KEY, address);
        }
        System.setProperty(ARCHAIUS_CONFIG_ROOT_PATH_KEY, url.getParameter(CONFIG_NAMESPACE_KEY, DEFAULT_CONFIG_ROOT_PATH));
        System.setProperty(ARCHAIUS_CONFIG_CHECK_KEY, url.getParameter(Constants.CONFIG_CHECK_KEY, "false"));

        try {
            ZooKeeperConfigurationSource zkConfigSource = new ZooKeeperConfigurationSource(url);
            zkConfigSource.start();
            /*if (!zkConfigSource.isConnected()) {
                // we can check the status of config center here, and decide to fail or continue if we cannot reach the config server.
            }*/
            DynamicWatchedConfiguration zkDynamicConfig = new DynamicWatchedConfiguration(zkConfigSource);
            ConfigurationManager.install(zkDynamicConfig);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * The hierarchy of configuration properties is:
     * 1. /{namespace}/config/dubbo/dubbo.properties
     * 2. /{namespace}/config/{applicationname}/dubbo.properties
     * <p>
     * To make the API compatible with other configuration systems, the key doesn't has group as prefix, so we should
     * add the group prefix before try to get value. If being used for dubbo router rules, the key must already
     * contains group prefix.
     */
    @Override
    protected String getTargetConfig(String key, String group, long timeout) {
        if (StringUtils.isNotEmpty(group)) {
            key = group + "." + key;
        }

        return DynamicPropertyFactory.getInstance()
                .getStringProperty(key, null)
                .get();
    }

    /**
     * First, get app level configuration. If there's no value in app level, try to get global dubbo level.
     */
    @Override
    protected Object getInternalProperty(String key) {
        return DynamicPropertyFactory.getInstance()
                .getStringProperty(key, null)
                .get();
    }

    @Override
    protected void addTargetListener(String key, Runnable runnable) {
        DynamicStringProperty prop = DynamicPropertyFactory.getInstance()
                .getStringProperty(key, null);
        prop.addCallback(runnable);
    }

    @Override
    protected Runnable createTargetListener(String key, ConfigurationListener listener) {
        return new ArchaiusListener(key, listener);
    }

    private class ArchaiusListener implements Runnable {
        private ConfigurationListener listener;
        private String key;
        private ConfigType type;

        public ArchaiusListener(String key, ConfigurationListener listener) {
            this.key = key;
            this.listener = listener;
            // Maybe we no longer need to identify the type of change. Because there's no scenario that a callback
            // will subscribe for both configurators and routers
            if (key.endsWith(Constants.CONFIGURATORS_SUFFIX)) {
                type = ConfigType.CONFIGURATORS;
            } else {
                /**
                 * used for all router rules:
                 * {@link Constants.ROUTERS_SUFFIX}
                 * {@link org.apache.dubbo.rpc.cluster.router.tag.TagRouter.TAGRULE_DATAID}
                 */
                type = ConfigType.ROUTERS;
            }
        }

        @Override
        public void run() {
            DynamicStringProperty prop = DynamicPropertyFactory.getInstance()
                    .getStringProperty(key, null);
            String newValue = prop.get();
            ConfigChangeEvent event = new ConfigChangeEvent(key, newValue, type);
            if (newValue == null) {
                event.setChangeType(ConfigChangeType.DELETED);
                listener.process(event);
            } else {
                if (newValue.equals("")) {
                    logger.warn("an empty rule is received for " + key + ", the current working rule is unknown, " +
                            "the empty rule will not take effect.");
                    return;
                }
                event.setChangeType(ConfigChangeType.MODIFIED);
                listener.process(event);
            }
        }
    }
}
