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
import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.Environment;

import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;

/**
 * Utilities for manipulating configurations from different sources
 */
public class ConfigurationUtils {
    /**
     * If user opens DynamicConfig, the extension instance must has been created during the initialization of
     * ConfigCenterConfig with the right extension type user specifies. If no DynamicConfig presents,
     * NopDynamicConfiguration will be used.
     */
    public static DynamicConfiguration getDynamicConfiguration() {
        DynamicConfiguration dynamicConfiguration = (DynamicConfiguration) Environment.getInstance().getDynamicConfiguration();
        if (dynamicConfiguration == null) {
            dynamicConfiguration = getExtensionLoader(DynamicConfiguration.class).getDefaultExtension();
        }
        return dynamicConfiguration;
    }

    @SuppressWarnings("deprecation")
    public static int getServerShutdownTimeout() {
        int timeout = Constants.DEFAULT_SERVER_SHUTDOWN_TIMEOUT;
        Configuration configuration = Environment.getInstance().getConfiguration();
        String value = configuration.getString(Constants.SHUTDOWN_WAIT_KEY);

        if (value != null && value.length() > 0) {
            try {
                timeout = Integer.parseInt(value);
            } catch (Exception e) {
                // ignore
            }
        } else {
            value = configuration.getString(Constants.SHUTDOWN_WAIT_SECONDS_KEY);
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
}
