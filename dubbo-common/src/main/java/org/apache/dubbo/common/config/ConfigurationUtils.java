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
package org.apache.dubbo.common.config;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_SERVER_SHUTDOWN_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_SECONDS_KEY;

/**
 * Utilities for manipulating configurations from different sources
 */
public class ConfigurationUtils {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationUtils.class);

    /**
     * Used to get properties from the jvm
     *
     * @return
     */
    public static Configuration getSystemConfiguration() {
        return ApplicationModel.getEnvironment().getSystemConfiguration();
    }

    /**
     * Used to get properties from the os environment
     *
     * @return
     */
    public static Configuration getEnvConfiguration() {
        return ApplicationModel.getEnvironment().getEnvironmentConfiguration();
    }

    /**
     * Used to get an composite property value.
     * <p>
     * Also see {@link Environment#getConfiguration()}
     *
     * @return
     */
    public static Configuration getGlobalConfiguration() {
        return ApplicationModel.getEnvironment().getConfiguration();
    }

    public static Configuration getDynamicGlobalConfiguration() {
        return ApplicationModel.getEnvironment().getDynamicGlobalConfiguration();
    }

    // FIXME
    @SuppressWarnings("deprecation")
    public static int getServerShutdownTimeout() {
        int timeout = DEFAULT_SERVER_SHUTDOWN_TIMEOUT;
        Configuration configuration = getGlobalConfiguration();
        String value = StringUtils.trim(configuration.getString(SHUTDOWN_WAIT_KEY));

        if (value != null && value.length() > 0) {
            try {
                timeout = Integer.parseInt(value);
            } catch (Exception e) {
                // ignore
            }
        } else {
            value = StringUtils.trim(configuration.getString(SHUTDOWN_WAIT_SECONDS_KEY));
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

    public static String getDynamicProperty(String property) {
        return getDynamicProperty(property, null);
    }

    public static String getDynamicProperty(String property, String defaultValue) {
        return StringUtils.trim(getDynamicGlobalConfiguration().getString(property, defaultValue));
    }

    public static String getProperty(String property) {
        return getProperty(property, null);
    }

    public static String getProperty(String property, String defaultValue) {
        return StringUtils.trim(getGlobalConfiguration().getString(property, defaultValue));
    }

    public static int get(String property, int defaultValue) {
        return getGlobalConfiguration().getInt(property, defaultValue);
    }

    public static Map<String, String> parseProperties(String content) throws IOException {
        Map<String, String> map = new HashMap<>();
        if (StringUtils.isNotEmpty(content)) {
            Properties properties = new Properties();
            properties.load(new StringReader(content));
            properties.stringPropertyNames().forEach(
                    k -> map.put(k, properties.getProperty(k))
            );
        }
        return map;
    }

}
