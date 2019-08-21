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
 * Mutable {@link EurekaInstanceConfig} implementation
 */
class MutableEurekaInstanceConfig implements EurekaInstanceConfig {

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

    public void setAppname(String appname) {
        this.appname = appname;
    }

    @Override
    public String getAppGroupName() {
        return appGroupName;
    }

    public void setAppGroupName(String appGroupName) {
        this.appGroupName = appGroupName;
    }

    @Override
    public boolean isInstanceEnabledOnit() {
        return instanceEnabledOnit;
    }

    public void setInstanceEnabledOnit(boolean instanceEnabledOnit) {
        this.instanceEnabledOnit = instanceEnabledOnit;
    }

    @Override
    public int getNonSecurePort() {
        return nonSecurePort;
    }

    public void setNonSecurePort(int nonSecurePort) {
        this.nonSecurePort = nonSecurePort;
    }

    @Override
    public int getSecurePort() {
        return securePort;
    }

    public void setSecurePort(int securePort) {
        this.securePort = securePort;
    }

    @Override
    public boolean isNonSecurePortEnabled() {
        return nonSecurePortEnabled;
    }

    @Override
    public boolean getSecurePortEnabled() {
        return securePortEnabled;
    }

    public void setNonSecurePortEnabled(boolean nonSecurePortEnabled) {
        this.nonSecurePortEnabled = nonSecurePortEnabled;
    }

    public void setSecurePortEnabled(boolean securePortEnabled) {
        this.securePortEnabled = securePortEnabled;
    }

    @Override
    public int getLeaseRenewalIntervalInSeconds() {
        return leaseRenewalIntervalInSeconds;
    }

    public void setLeaseRenewalIntervalInSeconds(int leaseRenewalIntervalInSeconds) {
        this.leaseRenewalIntervalInSeconds = leaseRenewalIntervalInSeconds;
    }

    @Override
    public int getLeaseExpirationDurationInSeconds() {
        return leaseExpirationDurationInSeconds;
    }

    public void setLeaseExpirationDurationInSeconds(int leaseExpirationDurationInSeconds) {
        this.leaseExpirationDurationInSeconds = leaseExpirationDurationInSeconds;
    }

    @Override
    public String getVirtualHostName() {
        return virtualHostName;
    }

    public void setVirtualHostName(String virtualHostName) {
        this.virtualHostName = virtualHostName;
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
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

    public void setSecureVirtualHostName(String secureVirtualHostName) {
        this.secureVirtualHostName = secureVirtualHostName;
    }

    public void setASGName(String aSGName) {
        this.aSGName = aSGName;
    }

    @Override
    public Map<String, String> getMetadataMap() {
        return metadataMap;
    }

    public void setMetadataMap(Map<String, String> metadataMap) {
        this.metadataMap = metadataMap;
    }

    @Override
    public DataCenterInfo getDataCenterInfo() {
        return dataCenterInfo;
    }

    public void setDataCenterInfo(DataCenterInfo dataCenterInfo) {
        this.dataCenterInfo = dataCenterInfo;
    }

    @Override
    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public String getStatusPageUrlPath() {
        return statusPageUrlPath;
    }

    public void setStatusPageUrlPath(String statusPageUrlPath) {
        this.statusPageUrlPath = statusPageUrlPath;
    }

    @Override
    public String getStatusPageUrl() {
        return statusPageUrl;
    }

    public void setStatusPageUrl(String statusPageUrl) {
        this.statusPageUrl = statusPageUrl;
    }

    @Override
    public String getHomePageUrlPath() {
        return homePageUrlPath;
    }

    public void setHomePageUrlPath(String homePageUrlPath) {
        this.homePageUrlPath = homePageUrlPath;
    }

    @Override
    public String getHomePageUrl() {
        return homePageUrl;
    }

    public void setHomePageUrl(String homePageUrl) {
        this.homePageUrl = homePageUrl;
    }

    @Override
    public String getHealthCheckUrlPath() {
        return healthCheckUrlPath;
    }

    public void setHealthCheckUrlPath(String healthCheckUrlPath) {
        this.healthCheckUrlPath = healthCheckUrlPath;
    }

    @Override
    public String getHealthCheckUrl() {
        return healthCheckUrl;
    }

    public void setHealthCheckUrl(String healthCheckUrl) {
        this.healthCheckUrl = healthCheckUrl;
    }

    @Override
    public String getSecureHealthCheckUrl() {
        return secureHealthCheckUrl;
    }

    public void setSecureHealthCheckUrl(String secureHealthCheckUrl) {
        this.secureHealthCheckUrl = secureHealthCheckUrl;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public boolean isPreferIpAddress() {
        return preferIpAddress;
    }

    public void setPreferIpAddress(boolean preferIpAddress) {
        this.preferIpAddress = preferIpAddress;
    }

    public InstanceInfo.InstanceStatus getInitialStatus() {
        return initialStatus;
    }

    public void setInitialStatus(InstanceInfo.InstanceStatus initialStatus) {
        this.initialStatus = initialStatus;
    }

    @Override
    public String[] getDefaultAddressResolutionOrder() {
        return defaultAddressResolutionOrder;
    }

    public void setDefaultAddressResolutionOrder(String[] defaultAddressResolutionOrder) {
        this.defaultAddressResolutionOrder = defaultAddressResolutionOrder;
    }
}
