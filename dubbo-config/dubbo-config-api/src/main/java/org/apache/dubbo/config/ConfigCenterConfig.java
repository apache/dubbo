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
import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.support.Parameter;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class ConfigCenterConfig extends AbstractConfig {
    private AtomicBoolean inited = new AtomicBoolean(false);

    private String protocol;
    private String address;
    private String cluster;
    private String namespace = "dubbo";
    private String group = "dubbo";
    private String username;
    private String password;
    private Long timeout = 3000L;
    private Boolean highestPriority = true;
    private Boolean check = true;

    private String appName;
    private String configFile = "dubbo.properties";
    private String appConfigFile;

    // customized parameters
    private Map<String, String> parameters;

    public ConfigCenterConfig() {
    }

    public URL toUrl() {
        Map<String, String> map = this.getMetaData();
        if (StringUtils.isEmpty(address)) {
            address = Constants.ANYHOST_VALUE;
        }
        map.put(Constants.PATH_KEY, ConfigCenterConfig.class.getSimpleName());
        // use 'zookeeper' as the default configcenter.
        if (StringUtils.isEmpty(map.get(Constants.PROTOCOL_KEY))) {
            map.put(Constants.PROTOCOL_KEY, Constants.ZOOKEEPER_PROTOCOL);
        }
        return UrlUtils.parseURL(address, map);
    }

    public boolean checkOrUpdateInited() {
        return inited.compareAndSet(false, true);
    }

    public void setExternalConfig(Map<String, String> externalConfiguration) {
        Environment.getInstance().setExternalConfigMap(externalConfiguration);
    }

    public void setAppExternalConfig(Map<String, String> appExternalConfiguration) {
        Environment.getInstance().setAppExternalConfigMap(appExternalConfiguration);
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

    @Parameter(key = Constants.CONFIG_CLUSTER_KEY, useKeyAsProperty = false)
    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    @Parameter(key = Constants.CONFIG_NAMESPACE_KEY, useKeyAsProperty = false)
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Parameter(key = Constants.CONFIG_GROUP_KEY, useKeyAsProperty = false)
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Parameter(key = Constants.CONFIG_CHECK_KEY, useKeyAsProperty = false)
    public Boolean isCheck() {
        return check;
    }

    public void setCheck(Boolean check) {
        this.check = check;
    }

    @Parameter(key = Constants.CONFIG_ENABLE_KEY, useKeyAsProperty = false)
    public Boolean isHighestPriority() {
        return highestPriority;
    }

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

    @Parameter(key = Constants.CONFIG_TIMEOUT_KEY, useKeyAsProperty = false)
    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    @Parameter(key = Constants.CONFIG_CONFIGFILE_KEY, useKeyAsProperty = false)
    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    @Parameter(excluded = true)
    public String getAppConfigFile() {
        return appConfigFile;
    }

    public void setAppConfigFile(String appConfigFile) {
        this.appConfigFile = appConfigFile;
    }

    @Parameter(key = Constants.CONFIG_APPNAME_KEY, useKeyAsProperty = false)
    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        checkParameterName(parameters);
        this.parameters = parameters;
    }

    @Override
    @Parameter(excluded = true)
    public boolean isValid() {
        if (StringUtils.isEmpty(address)) {
            return false;
        }

        return address.contains("://") || StringUtils.isNotEmpty(protocol);
    }
}
