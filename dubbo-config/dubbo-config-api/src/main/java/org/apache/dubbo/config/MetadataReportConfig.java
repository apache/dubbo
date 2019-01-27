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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.support.Parameter;

import java.util.Map;

import static org.apache.dubbo.common.Constants.PROPERTIES_CHAR_SEPERATOR;

/**
 * RegistryConfig
 *
 * @export
 */
public class MetadataReportConfig extends AbstractConfig {

    private static final long serialVersionUID = 55233L;
    /**
     * the value is : metadata-report
     */
    private static final String PREFIX_TAG = StringUtils.camelToSplitName(
            MetadataReportConfig.class.getSimpleName().substring(0, MetadataReportConfig.class.getSimpleName().length() - 6), PROPERTIES_CHAR_SEPERATOR);

    // Register center address
    private String address;

    // Username to login register center
    private String username;

    // Password to login register center
    private String password;

    // Request timeout in milliseconds for register center
    private Integer timeout;

    /**
     * The group the metadata in . It is the same as registry
     */
    private String group;

    // Customized parameters
    private Map<String, String> parameters;

    private Integer retryTimes;

    private Integer retryPeriod;
    /**
     * By default the metadatastore will store full metadata repeatly every day .
     */
    private Boolean cycleReport;

    /**
     * Sync report, default async
     */
    private Boolean syncReport;

    public MetadataReportConfig() {
    }

    public MetadataReportConfig(String address) {
        setAddress(address);
    }

    @Parameter(excluded = true)
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Parameter(key = "retry-times")
    public Integer getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(Integer retryTimes) {
        this.retryTimes = retryTimes;
    }

    @Parameter(key = "retry-period")
    public Integer getRetryPeriod() {
        return retryPeriod;
    }

    public void setRetryPeriod(Integer retryPeriod) {
        this.retryPeriod = retryPeriod;
    }

    @Parameter(key = "cycle-report")
    public Boolean getCycleReport() {
        return cycleReport;
    }

    public void setCycleReport(Boolean cycleReport) {
        this.cycleReport = cycleReport;
    }

    @Parameter(key = "sync-report")
    public Boolean getSyncReport() {
        return syncReport;
    }

    public void setSyncReport(Boolean syncReport) {
        this.syncReport = syncReport;
    }

    @Override
    @Parameter(excluded = true)
    public String getPrefix() {
        return StringUtils.isNotEmpty(prefix) ? prefix : (Constants.DUBBO + "." + PREFIX_TAG);
    }

    @Override
    @Parameter(excluded = true)
    public boolean isValid() {
        return StringUtils.isNotEmpty(address);
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}
