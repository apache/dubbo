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
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.context.Environment;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.configcenter.DynamicConfiguration;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 *
 */
public class ConfigCenterConfig extends AbstractConfig {
    private String protocol;
    private String address;
    private String env;
    private String cluster;
    private String namespace = "dubbo";
    private String group = "dubbo";
    private String username;
    private String password;
    private Long timeout = 3000L;
    private Boolean priority = true;
    private Boolean check = true;

    private String appname;
    private String configfile = "dubbo.properties";
    private String localconfigfile;

    private ApplicationConfig application;
    private RegistryConfig registry;

    // customized parameters
    private Map<String, String> parameters;

//    private RegistryConfig registry;

    public ConfigCenterConfig() {
    }

    public void init() {
        DynamicConfiguration dynamicConfiguration = startDynamicConfiguration();
        String configContent = dynamicConfiguration.getConfig(configfile, group);

        String appGroup = getApplicationName();
        String appConfigContent = null;
        if (StringUtils.isNotEmpty(appGroup)) {
            appConfigContent = dynamicConfiguration.getConfig
                    (
                            StringUtils.isNotEmpty(localconfigfile) ? localconfigfile : configfile,
                            appGroup
                    );
        }
        try {
            Environment.getInstance().setConfigCenterFirst(priority);
            Environment.getInstance().updateExternalConfigurationMap(parseProperties(configContent));
            Environment.getInstance().updateAppExternalConfigurationMap(parseProperties(appConfigContent));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse configurations from Config Center.", e);
        }
    }

    /*public void initWithoutRemoteConfig() {
        startDynamicConfiguration();
    }*/

    private DynamicConfiguration startDynamicConfiguration() {
        // give jvm properties the chance to override local configs, e.g., -Ddubbo.configcenter.config.priority

        refresh();
        // try to use registryConfig as the default configcenter, only applies to zookeeper.
        if (!isValid() && registry != null && registry.isZookeeperProtocol()) {
            setAddress(registry.getAddress());
            setProtocol(registry.getProtocol());
        }
//        checkConfigCenter();

        URL url = toConfigUrl();
        Set<Object> loadedConfigurations = ExtensionLoader.getExtensionLoader(DynamicConfiguration.class).getLoadedExtensionInstances();
        if (CollectionUtils.isEmpty(loadedConfigurations)) {
            DynamicConfiguration dynamicConfiguration = ExtensionLoader.getExtensionLoader(DynamicConfiguration.class).getExtension(url.getProtocol());
            // TODO, maybe we need a factory to do this?
            dynamicConfiguration.setUrl(url);
            dynamicConfiguration.init();
            return dynamicConfiguration;
        }

        return (DynamicConfiguration) loadedConfigurations.iterator().next();
    }

    private URL toConfigUrl() {
        Map<String, String> map = this.getMetaData();
        if (StringUtils.isNotEmpty(env) && StringUtils.isEmpty(address)) {
            address = Constants.ANYHOST_VALUE;
        }
        map.put(Constants.PATH_KEY, ConfigCenterConfig.class.getSimpleName());
        // use 'zookeeper' as the default configcenter.
        if (StringUtils.isEmpty(map.get(Constants.PROTOCOL_KEY))) {
            map.put(Constants.PROTOCOL_KEY, "zookeeper");
        }
        return UrlUtils.parseURL(address, map);
    }

    private String getApplicationName() {
        if (application != null) {
            if (!application.isValid()) {
                throw new IllegalStateException(
                        "No application config found or it's not a valid config! Please add <dubbo:application name=\"...\" /> to your spring config.");
            }
            return application.getName();
        }
        return appname;
    }

    protected Map<String, String> parseProperties(String content) throws IOException {
        Map<String, String> map = new HashMap<>();
        if (StringUtils.isEmpty(content)) {
            logger.warn("You specified the config centre, but there's not even one single config item in it.");
        } else {
            Properties properties = new Properties();
            properties.load(new StringReader(content));
            properties.stringPropertyNames().forEach(
                    k -> map.put(k, properties.getProperty(k))
            );
        }
        return map;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Parameter(excluded = true)
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

    @Parameter(key = Constants.CONFIG_GROUP_KEY)
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Parameter(key = Constants.CONFIG_CHECK_KEY)
    public Boolean isCheck() {
        return check;
    }

    public void setCheck(Boolean check) {
        this.check = check;
    }

    @Parameter(key = Constants.CONFIG_PRIORITY_KEY)
    public Boolean isPriority() {
        return priority;
    }

    public void setPriority(Boolean priority) {
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

    @Parameter(key = Constants.CONFIG_TIMEOUT_KEY)
    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    @Parameter(key = Constants.CONFIG_CONFIGFILE_KEY)
    public String getConfigfile() {
        return configfile;
    }

    public void setConfigfile(String configfile) {
        this.configfile = configfile;
    }

    @Parameter(excluded = true)
    public String getLocalconfigfile() {
        return localconfigfile;
    }

    public void setLocalconfigfile(String localconfigfile) {
        this.localconfigfile = localconfigfile;
    }

    @Parameter(key = Constants.CONFIG_APPNAME_KEY)
    public String getAppname() {
        return appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        checkParameterName(parameters);
        this.parameters = parameters;
    }

    public ApplicationConfig getApplication() {
        return application;
    }

    public void setApplication(ApplicationConfig application) {
        this.application = application;
    }

    public RegistryConfig getRegistry() {
        return registry;
    }

    public void setRegistry(RegistryConfig registry) {
        this.registry = registry;
    }

    private void checkConfigCenter() {
        if ((StringUtils.isEmpty(env) && StringUtils.isEmpty(address))
                || (StringUtils.isEmpty(protocol) && (StringUtils.isEmpty(address) || !address.contains("://")))) {
            throw new IllegalStateException("You must specify the right parameter for configcenter.");
        }
    }

    @Override
    public boolean isValid() {
        if (StringUtils.isEmpty(address) && StringUtils.isEmpty(env)) {
            return false;
        }
        if (StringUtils.isNotEmpty(address)) {
            if (!address.contains("://") && StringUtils.isEmpty(protocol)) {
                return false;
            }
        } else if (StringUtils.isNotEmpty(env) && StringUtils.isEmpty(protocol)) {
            return false;
        }
        return true;
    }
}
