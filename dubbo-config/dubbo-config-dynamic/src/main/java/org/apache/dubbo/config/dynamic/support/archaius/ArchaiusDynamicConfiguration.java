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
package org.apache.dubbo.config.dynamic.support.archaius;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.config.DynamicWatchedConfiguration;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.dynamic.AbstractDynamicConfiguration;
import org.apache.dubbo.config.dynamic.ConfigChangeEvent;
import org.apache.dubbo.config.dynamic.ConfigChangeType;
import org.apache.dubbo.config.dynamic.ConfigType;
import org.apache.dubbo.config.dynamic.ConfigurationListener;
import org.apache.dubbo.config.dynamic.support.archaius.sources.ZooKeeperConfigurationSource;

/**
 * Archaius supports various sources and it's extensiable: JDBC, ZK, Properties, ..., so should we make it extensiable?
 */
public class ArchaiusDynamicConfiguration extends AbstractDynamicConfiguration<Runnable> {

    public ArchaiusDynamicConfiguration() {
    }

    @Override
    public void init() {
        //  String address = env.getCompositeConf().getString(ADDRESS_KEY);
        //  String app = env.getCompositeConf().getString(APP_KEY);

        String address = url.getParameter(Constants.CONFIG_ADDRESS_KEY, url.getAddress());
        if (!address.equals(Constants.ANYHOST_VALUE)) {
            System.setProperty(ZooKeeperConfigurationSource.ARCHAIUS_SOURCE_ADDRESS_KEY, address);
        }
        System.setProperty(ZooKeeperConfigurationSource.ARCHAIUS_CONFIG_ROOT_PATH_KEY, url.getParameter(Constants.CONFIG_NAMESPACE_KEY, ZooKeeperConfigurationSource.DEFAULT_CONFIG_ROOT_PATH));

        try {
            ZooKeeperConfigurationSource zkConfigSource = new ZooKeeperConfigurationSource();
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

    @Override
    protected String getInternalProperty(String key, String group, long timeout, ConfigurationListener listener) {
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
    protected Runnable createTargetConfigListener(String key, ConfigurationListener listener) {
        return new ArchaiusListener(key, listener);
    }


    private class ArchaiusListener implements Runnable {
        private ConfigurationListener listener;
        private URL url;
        private String key;
        private ConfigType type;

        public ArchaiusListener(String key, ConfigurationListener listener) {
            this.key = key;
            this.listener = listener;
            this.url = listener.getUrl();
            // Maybe we no longer need to identify the type of change. Because there's no scenario that a callback will subscribe for both configurators and routers
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
            if (StringUtils.isEmpty(newValue)) {
                event.setChangeType(ConfigChangeType.DELETED);
                listener.process(event);
            } else {
                event.setChangeType(ConfigChangeType.MODIFIED);
                listener.process(event);
            }
        }
    }
}
