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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.CONFIG_CONFIGFILE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CONFIG_ENABLE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.RemotingConstants.BACKUP_KEY;
import static org.apache.dubbo.common.utils.PojoUtils.updatePropertyIfAbsent;
import static org.apache.dubbo.config.Constants.CONFIG_APP_CONFIGFILE_KEY;
import static org.apache.dubbo.config.Constants.ZOOKEEPER_PROTOCOL;

/**
 * ConfigCenterConfig
 */
public class ConfigCenterConfig extends AbstractConfig {
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private String protocol;
    private String address;
    private Integer port;

    /**
     * The config center cluster, it's real meaning may very on different Config Center products.
     */
    private String cluster;

    /**
     * The namespace of the config center, generally it's used for multi-tenant,
     * but it's real meaning depends on the actual Config Center you use.
     * The default value is CommonConstants.DUBBO
     */
    private String namespace;

    /**
     * The group of the config center, generally it's used to identify an isolated space for a batch of config items,
     * but it's real meaning depends on the actual Config Center you use.
     * The default value is CommonConstants.DUBBO
     */
    private String group;
    private String username;
    private String password;

    /**
     * The default value is 30000L;
     */
    private Long timeout;

    /**
     * If the Config Center is given the highest priority, it will override all the other configurations
     * The default value is true
     * @deprecated no longer used
     */
    private Boolean highestPriority;

    /**
     * Decide the behaviour when initial connection try fails, 'true' means interrupt the whole process once fail.
     * The default value is true
     */
    private Boolean check;

    /**
     * Used to specify the key that your properties file mapping to, most of the time you do not need to change this parameter.
     * Notice that for Apollo, this parameter is meaningless, set the 'namespace' is enough.
     * The default value is CommonConstants.DEFAULT_DUBBO_PROPERTIES
     */
    private String configFile;

    /**
     * the properties file under 'configFile' is global shared while .properties under this one is limited only to this application
     */
    private String appConfigFile;

    /**
     * If the Config Center product you use have some special parameters that is not covered by this class, you can add it to here.
     * For example, with XML:
     *    <dubbo:config-center>
     *       <dubbo:parameter key="{your key}" value="{your value}" />
     *    </dubbo:config-center>
     */
    private Map<String, String> parameters;

    private Map<String, String> externalConfiguration;

    private Map<String, String> appExternalConfiguration;

    public ConfigCenterConfig() {
    }

    public ConfigCenterConfig(ApplicationModel applicationModel) {
        super(applicationModel);
    }

    @Override
    protected void checkDefault() {
        super.checkDefault();

        if (namespace == null) {
            namespace = CommonConstants.DUBBO;
        }
        if (group == null) {
            group = CommonConstants.DUBBO;
        }
        if (timeout == null) {
            timeout = 30000L;
        }
//        if (highestPriority == null) {
//            highestPriority = true;
//        }
        if (check == null) {
            check = true;
        }
        if (configFile == null) {
            configFile = CommonConstants.DEFAULT_DUBBO_PROPERTIES;
        }
    }

    public URL toUrl() {
        Map<String, String> map = new HashMap<>();
        appendParameters(map, this);
        if (StringUtils.isEmpty(address)) {
            address = ANYHOST_VALUE;
        }
        map.put(PATH_KEY, ConfigCenterConfig.class.getSimpleName());
        // use 'zookeeper' as the default config center.
        if (StringUtils.isEmpty(map.get(PROTOCOL_KEY))) {
            map.put(PROTOCOL_KEY, ZOOKEEPER_PROTOCOL);
        }
        return UrlUtils.parseURL(address, map).setScopeModel(getScopeModel());
    }

    public boolean checkOrUpdateInitialized(boolean update) {
        return initialized.compareAndSet(false, update);
    }

    public Map<String, String> getExternalConfiguration() {
        return externalConfiguration;
    }

    public Map<String, String> getAppExternalConfiguration() {
        return appExternalConfiguration;
    }

    public void setExternalConfig(Map<String, String> externalConfiguration) {
        this.externalConfiguration = externalConfiguration;
    }

    public void setAppExternalConfig(Map<String, String> appExternalConfiguration) {
        this.appExternalConfiguration = appExternalConfiguration;
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
        if (address != null) {
            try {
                URL url = URL.valueOf(address);
                updatePropertyIfAbsent(this::getUsername, this::setUsername, url.getUsername());
                updatePropertyIfAbsent(this::getPassword, this::setPassword, url.getPassword());
                updatePropertyIfAbsent(this::getProtocol, this::setProtocol, url.getProtocol());
                updatePropertyIfAbsent(this::getPort, this::setPort, url.getPort());

                Map<String, String> params = url.getParameters();
                if (CollectionUtils.isNotEmptyMap(params)) {
                    params.remove(BACKUP_KEY);
                }
                updateParameters(params);
            } catch (Exception ignored) {
            }
        }
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Boolean isCheck() {
        return check;
    }

    public void setCheck(Boolean check) {
        this.check = check;
    }

    @Deprecated
    @Parameter(key = CONFIG_ENABLE_KEY)
    public Boolean isHighestPriority() {
        return highestPriority;
    }

    @Deprecated
    public void setHighestPriority(Boolean highestPriority) {
        this.highestPriority = highestPriority;
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

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    @Parameter(key = CONFIG_CONFIGFILE_KEY)
    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    @Parameter(excluded = true, key = CONFIG_APP_CONFIGFILE_KEY)
    public String getAppConfigFile() {
        return appConfigFile;
    }

    public void setAppConfigFile(String appConfigFile) {
        this.appConfigFile = appConfigFile;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    @Parameter(excluded = true, attribute = false)
    public boolean isValid() {
        if (StringUtils.isEmpty(address)) {
            return false;
        }

        return address.contains("://") || StringUtils.isNotEmpty(protocol);
    }

    public void updateParameters(Map<String, String> parameters) {
        if (CollectionUtils.isEmptyMap(parameters)) {
            return;
        }
        if (this.parameters == null) {
            this.parameters = parameters;
        } else {
            this.parameters.putAll(parameters);
        }
    }

}
