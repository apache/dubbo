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
package org.apache.dubbo.config;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.context.Environment;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.governance.DynamicConfiguration;
import org.apache.dubbo.governance.DynamicConfigurationFactory;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 */
public class ConfigCenterConfig extends AbstractConfig {
    private String type;
    private String address;
    private String env;
    private String cluster;
    private String namespace;
    private String appnamespace;
    private String username;
    private String password;
    private long timeout;
    private boolean priority;
    private boolean check;

    private String dataid;

    // customized parameters
    private Map<String, String> parameters;

//    private RegistryConfig registry;

    public ConfigCenterConfig() {
    }

    private URL toConfigUrl() {
        String host = address;
        int port = 0;
        try {
            if (StringUtils.isNotEmpty(address)) {
                String[] addrs = address.split(":");
                if (addrs.length == 2) {
                    host = addrs[0];
                    port = Integer.parseInt(addrs[1]);
                }
            }
        } catch (Exception e) {
            throw e;
        }

        Map<String, String> map = this.getMetaData();
        return new URL("config", username, password, host, port, ConfigCenterConfig.class.getSimpleName(), map);
    }

    @PostConstruct
    public void init() throws Exception {
        // give jvm properties the chance of overriding local configs.
        refresh();

        URL url = toConfigUrl();
        DynamicConfiguration dynamicConfiguration = ExtensionLoader.getExtensionLoader(DynamicConfigurationFactory.class).getAdaptiveExtension().getDynamicConfiguration(url);
        String configContent = dynamicConfiguration.getConfig(dataid, namespace);
        try {
            if (configContent == null) {
                logger.warn("You specified the config centre, but there's not even one single config item in it.");
            } else {
                Properties properties = new Properties();
                properties.load(new StringReader(configContent));
                Map<String, String> map = new HashMap<>();
                properties.stringPropertyNames().forEach(
                        k -> map.put(k, properties.getProperty(k))
                );
                Environment.getInstance().setConfigCenterFirst(priority);
                Environment.getInstance().updateExternalConfiguration(map);
            }
        } catch (IOException e) {
            throw e;
        }
    }

    @Parameter(key = Constants.CONFIG_TYPE_KEY)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Parameter(key = Constants.CONFIG_ADDRESS_KEY)
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Parameter(key = Constants.CONFIG_ENV_KEY)
    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    @Parameter(key = Constants.CONFIG_CLUSTER_KEY)
    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    @Parameter(key = Constants.CONFIG_NAMESPACE_KEY)
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Parameter(key = "config.appnamespace")
    public String getAppnamespace() {
        return appnamespace;
    }

    public void setAppnamespace(String appnamespace) {
        this.appnamespace = appnamespace;
    }

    @Parameter(key = Constants.CONFIG_CHECK_KEY)
    public boolean isCheck() {
        return check;
    }

    public void setCheck(boolean check) {
        this.check = check;
    }

    @Parameter(key = "config.priority")
    public boolean isPriority() {
        return priority;
    }

    public void setPriority(boolean priority) {
        this.priority = priority;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Parameter(key = "config.timeout")
    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getDataid() {
        return dataid;
    }

    public void setDataid(String dataid) {
        this.dataid = dataid;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        checkParameterName(parameters);
        this.parameters = parameters;
    }
}
