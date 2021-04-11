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
package org.apache.dubbo.registry.eureka;

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Configurable {@link EurekaInstanceConfig} implementation
 */
class ConfigurableEurekaInstanceConfig implements EurekaInstanceConfig {

    private String appname;

    private String appGroupName;

    private boolean instanceEnabledOnit;

    private int nonSecurePort;

    private int securePort;

    private boolean nonSecurePortEnabled = true;

    private boolean securePortEnabled;

    private int leaseRenewalIntervalInSeconds = 30;

    private int leaseExpirationDurationInSeconds = 90;

    private String virtualHostName = "unknown";

    private String instanceId;

    private String secureVirtualHostName = "unknown";

    private String aSGName;

    private Map<String, String> metadataMap = new HashMap<>();

    private DataCenterInfo dataCenterInfo = new MyDataCenterInfo(DataCenterInfo.Name.MyOwn);

    private String ipAddress;

    private String statusPageUrlPath;

    private String statusPageUrl;

    private String homePageUrlPath = "/";

    private String homePageUrl;

    private String healthCheckUrlPath;

    private String healthCheckUrl;

    private String secureHealthCheckUrl;

    private String namespace = "eureka";

    private String hostname;

    private boolean preferIpAddress = false;

    private InstanceInfo.InstanceStatus initialStatus = InstanceInfo.InstanceStatus.UP;

    private String[] defaultAddressResolutionOrder = new String[0];

    @Override
    public String getAppname() {
        return appname;
    }

    public ConfigurableEurekaInstanceConfig setAppname(String appname) {
        this.appname = appname;
        return this;
    }

    @Override
    public String getAppGroupName() {
        return appGroupName;
    }

    public ConfigurableEurekaInstanceConfig setAppGroupName(String appGroupName) {
        this.appGroupName = appGroupName;
        return this;
    }

    @Override
    public boolean isInstanceEnabledOnit() {
        return instanceEnabledOnit;
    }

    public ConfigurableEurekaInstanceConfig setInstanceEnabledOnit(boolean instanceEnabledOnit) {
        this.instanceEnabledOnit = instanceEnabledOnit;
        return this;
    }

    @Override
    public int getNonSecurePort() {
        return nonSecurePort;
    }

    public ConfigurableEurekaInstanceConfig setNonSecurePort(int nonSecurePort) {
        this.nonSecurePort = nonSecurePort;
        return this;
    }

    @Override
    public int getSecurePort() {
        return securePort;
    }

    public ConfigurableEurekaInstanceConfig setSecurePort(int securePort) {
        this.securePort = securePort;
        return this;
    }

    @Override
    public boolean isNonSecurePortEnabled() {
        return nonSecurePortEnabled;
    }

    @Override
    public boolean getSecurePortEnabled() {
        return securePortEnabled;
    }

    public ConfigurableEurekaInstanceConfig setNonSecurePortEnabled(boolean nonSecurePortEnabled) {
        this.nonSecurePortEnabled = nonSecurePortEnabled;
        return this;
    }

    public ConfigurableEurekaInstanceConfig setSecurePortEnabled(boolean securePortEnabled) {
        this.securePortEnabled = securePortEnabled;
        return this;
    }

    @Override
    public int getLeaseRenewalIntervalInSeconds() {
        return leaseRenewalIntervalInSeconds;
    }

    public ConfigurableEurekaInstanceConfig setLeaseRenewalIntervalInSeconds(int leaseRenewalIntervalInSeconds) {
        this.leaseRenewalIntervalInSeconds = leaseRenewalIntervalInSeconds;
        return this;
    }

    @Override
    public int getLeaseExpirationDurationInSeconds() {
        return leaseExpirationDurationInSeconds;
    }

    public ConfigurableEurekaInstanceConfig setLeaseExpirationDurationInSeconds(int leaseExpirationDurationInSeconds) {
        this.leaseExpirationDurationInSeconds = leaseExpirationDurationInSeconds;
        return this;
    }

    @Override
    public String getVirtualHostName() {
        return virtualHostName;
    }

    public ConfigurableEurekaInstanceConfig setVirtualHostName(String virtualHostName) {
        this.virtualHostName = virtualHostName;
        return this;
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    public ConfigurableEurekaInstanceConfig setInstanceId(String instanceId) {
        this.instanceId = instanceId;
        return this;
    }

    @Override
    public String getSecureVirtualHostName() {
        return secureVirtualHostName;
    }

    @Override
    public String getASGName() {
        return aSGName;
    }

    @Override
    public String getHostName(boolean refresh) {
        return null;
    }

    public ConfigurableEurekaInstanceConfig setSecureVirtualHostName(String secureVirtualHostName) {
        this.secureVirtualHostName = secureVirtualHostName;
        return this;
    }

    public ConfigurableEurekaInstanceConfig setASGName(String aSGName) {
        this.aSGName = aSGName;
        return this;
    }

    @Override
    public Map<String, String> getMetadataMap() {
        return metadataMap;
    }

    public ConfigurableEurekaInstanceConfig setMetadataMap(Map<String, String> metadataMap) {
        this.metadataMap = metadataMap;
        return this;
    }

    @Override
    public DataCenterInfo getDataCenterInfo() {
        return dataCenterInfo;
    }

    public ConfigurableEurekaInstanceConfig setDataCenterInfo(DataCenterInfo dataCenterInfo) {
        this.dataCenterInfo = dataCenterInfo;
        return this;
    }

    @Override
    public String getIpAddress() {
        return ipAddress;
    }

    public ConfigurableEurekaInstanceConfig setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    @Override
    public String getStatusPageUrlPath() {
        return statusPageUrlPath;
    }

    public ConfigurableEurekaInstanceConfig setStatusPageUrlPath(String statusPageUrlPath) {
        this.statusPageUrlPath = statusPageUrlPath;
        return this;
    }

    @Override
    public String getStatusPageUrl() {
        return statusPageUrl;
    }

    public ConfigurableEurekaInstanceConfig setStatusPageUrl(String statusPageUrl) {
        this.statusPageUrl = statusPageUrl;
        return this;
    }

    @Override
    public String getHomePageUrlPath() {
        return homePageUrlPath;
    }

    public ConfigurableEurekaInstanceConfig setHomePageUrlPath(String homePageUrlPath) {
        this.homePageUrlPath = homePageUrlPath;
        return this;
    }

    @Override
    public String getHomePageUrl() {
        return homePageUrl;
    }

    public ConfigurableEurekaInstanceConfig setHomePageUrl(String homePageUrl) {
        this.homePageUrl = homePageUrl;
        return this;
    }

    @Override
    public String getHealthCheckUrlPath() {
        return healthCheckUrlPath;
    }

    public ConfigurableEurekaInstanceConfig setHealthCheckUrlPath(String healthCheckUrlPath) {
        this.healthCheckUrlPath = healthCheckUrlPath;
        return this;
    }

    @Override
    public String getHealthCheckUrl() {
        return healthCheckUrl;
    }

    public ConfigurableEurekaInstanceConfig setHealthCheckUrl(String healthCheckUrl) {
        this.healthCheckUrl = healthCheckUrl;
        return this;
    }

    @Override
    public String getSecureHealthCheckUrl() {
        return secureHealthCheckUrl;
    }

    public ConfigurableEurekaInstanceConfig setSecureHealthCheckUrl(String secureHealthCheckUrl) {
        this.secureHealthCheckUrl = secureHealthCheckUrl;
        return this;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    public ConfigurableEurekaInstanceConfig setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public String getHostname() {
        return hostname;
    }

    public ConfigurableEurekaInstanceConfig setHostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    public boolean isPreferIpAddress() {
        return preferIpAddress;
    }

    public ConfigurableEurekaInstanceConfig setPreferIpAddress(boolean preferIpAddress) {
        this.preferIpAddress = preferIpAddress;
        return this;
    }

    public InstanceInfo.InstanceStatus getInitialStatus() {
        return initialStatus;
    }

    public ConfigurableEurekaInstanceConfig setInitialStatus(InstanceInfo.InstanceStatus initialStatus) {
        this.initialStatus = initialStatus;
        return this;
    }

    @Override
    public String[] getDefaultAddressResolutionOrder() {
        return defaultAddressResolutionOrder;
    }

    public ConfigurableEurekaInstanceConfig setDefaultAddressResolutionOrder(String[] defaultAddressResolutionOrder) {
        this.defaultAddressResolutionOrder = defaultAddressResolutionOrder;
        return this;
    }
}
