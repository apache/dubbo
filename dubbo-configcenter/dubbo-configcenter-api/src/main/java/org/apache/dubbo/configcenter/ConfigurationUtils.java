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
package org.apache.dubbo.configcenter;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.CompositeConfiguration;
import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.CollectionUtils;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utilities for manipulating configurations from different sources
 */
public class ConfigurationUtils {
    private static final CompositeConfiguration compositeConfiguration;

    static {
        compositeConfiguration = new CompositeConfiguration();
        compositeConfiguration.addConfiguration(getDynamicConfiguration());
        compositeConfiguration.addConfiguration(Environment.getInstance().getAppExternalConfiguration(null, null));
        compositeConfiguration.addConfiguration(Environment.getInstance().getExternalConfiguration(null, null));
        compositeConfiguration.addConfiguration(Environment.getInstance().getSystemConf(null, null));
        compositeConfiguration.addConfiguration(Environment.getInstance().getPropertiesConf(null, null));
    }

    private volatile Map<String, CompositeConfiguration> runtimeCompositeConfsHolder = new ConcurrentHashMap<>();

    /**
     * FIXME This method will recreate Configuration for each RPC, how much latency affect will this action has on performance?
     *
     * @param url,    the url metadata.
     * @param method, the method name the RPC is trying to invoke.
     * @return
     */
    public static CompositeConfiguration getRuntimeCompositeConf(URL url, String method) {
        CompositeConfiguration compositeConfiguration = new CompositeConfiguration();

        String app = url.getParameter(Constants.APPLICATION_KEY);
        String service = url.getServiceKey();
        compositeConfiguration.addConfiguration(new ConfigurationWrapper(app, service, method, getDynamicConfiguration()));

        compositeConfiguration.addConfiguration(url.toConfiguration());

        return compositeConfiguration;
    }

    /**
     * If user opens DynamicConfig, the extension instance must has been created during the initialization of ConfigCenterConfig with the right extension type user specified.
     * If no DynamicConfig presents, NopDynamicConfiguration will be used.
     *
     * @return
     */
    public static DynamicConfiguration getDynamicConfiguration() {
        Set<DynamicConfiguration> configurations = ExtensionLoader.getExtensionLoader(DynamicConfiguration.class).getExtensions();
        if (CollectionUtils.isEmpty(configurations)) {
            return ExtensionLoader.getExtensionLoader(DynamicConfiguration.class).getDefaultExtension();
        } else {
            return configurations.iterator().next();
        }
    }

    @SuppressWarnings("deprecation")
    public static int getServerShutdownTimeout() {
        int timeout = Constants.DEFAULT_SERVER_SHUTDOWN_TIMEOUT;
        String value = getProperty(Constants.SHUTDOWN_WAIT_KEY);
        if (value != null && value.length() > 0) {
            try {
                timeout = Integer.parseInt(value);
            } catch (Exception e) {
                // ignore
            }
        } else {
            value = getProperty(Constants.SHUTDOWN_WAIT_SECONDS_KEY);
            if (value != null && value.length() > 0) {
                try {
                    timeout = Integer.parseInt(value) * 1000;
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        return timeout;
    }

    public static String getProperty(String key) {
        return compositeConfiguration.getString(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return compositeConfiguration.getString(key, defaultValue);
    }

}
