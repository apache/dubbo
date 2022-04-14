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
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.CYCLE_REPORT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REPORT_DEFINITION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REPORT_METADATA_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.RETRY_PERIOD_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.RETRY_TIMES_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SYNC_REPORT_KEY;
import static org.apache.dubbo.common.constants.RemotingConstants.BACKUP_KEY;
import static org.apache.dubbo.common.utils.PojoUtils.updatePropertyIfAbsent;
import static org.apache.dubbo.common.utils.StringUtils.isEmpty;

/**
 * MetadataReportConfig
 *
 * @export
 */
public class MetadataReportConfig extends AbstractConfig {

    private static final long serialVersionUID = 55233L;

    private String protocol;

    /**
     * metadata center address
     */
    private String address;

    /**
     * Default port for metadata center
     */
    private Integer port;

    /**
     * Username to login metadata center
     */
    private String username;

    /**
     * Password to login metadata center
     */
    private String password;

    /**
     * Request timeout in milliseconds for metadata center
     */
    private Integer timeout;

    /**
     * The group the metadata in . It is the same as registry
     */
    private String group;

    /**
     * Customized parameters
     */
    private Map<String, String> parameters;

    private Integer retryTimes;

    private Integer retryPeriod;
    /**
     * By default, the metadata store will store full metadata repeatedly every day .
     */
    private Boolean cycleReport;

    /**
     * Sync report, default async
     */
    private Boolean syncReport;

    /**
     * cluster
     */
    private Boolean cluster;

    /**
     * registry id
     */
    private String registry;

    /**
     * File for saving metadata center dynamic list
     */
    private String file;

    /**
     * Decide the behaviour when initial connection try fails,
     * 'true' means interrupt the whole process once fail.
     * The default value is true
     */
    private Boolean check;

    private Boolean reportMetadata;

    private Boolean reportDefinition;

    public MetadataReportConfig() {
    }

    public MetadataReportConfig(ApplicationModel applicationModel) {
        super(applicationModel);
    }

    public MetadataReportConfig(String address) {
        setAddress(address);
    }

    public MetadataReportConfig(ApplicationModel applicationModel, String address) {
        super(applicationModel);
        setAddress(address);
    }

    public URL toUrl() throws IllegalArgumentException {
        String address = this.getAddress();
        if (isEmpty(address)) {
            throw new IllegalArgumentException("The address of metadata report is invalid.");
        }
        Map<String, String> map = new HashMap<>();
        URL url = URL.valueOf(address, getScopeModel());
        // Issue : https://github.com/apache/dubbo/issues/6491
        // Append the parameters from address
        map.putAll(url.getParameters());
        // Append or overrides the properties as parameters
        appendParameters(map, this);
        // Normalize the parameters
        map.putAll(convert(map, null));
        // put the protocol of URL as the "metadata"
        map.put(METADATA, isEmpty(url.getProtocol()) ? map.get(PROTOCOL_KEY) : url.getProtocol());
        return new ServiceConfigURL(METADATA, StringUtils.isBlank(url.getUsername()) ? this.getUsername() : url.getUsername(),
            StringUtils.isBlank(url.getPassword()) ? this.getPassword() : url.getPassword(), url.getHost(),
            url.getPort(), url.getPath(), map).setScopeModel(getScopeModel());
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

                // Refactor since 2.7.8
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

    @Parameter(key = RETRY_TIMES_KEY)
    public Integer getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(Integer retryTimes) {
        this.retryTimes = retryTimes;
    }

    @Parameter(key = RETRY_PERIOD_KEY)
    public Integer getRetryPeriod() {
        return retryPeriod;
    }

    public void setRetryPeriod(Integer retryPeriod) {
        this.retryPeriod = retryPeriod;
    }

    @Parameter(key = CYCLE_REPORT_KEY)
    public Boolean getCycleReport() {
        return cycleReport;
    }

    public void setCycleReport(Boolean cycleReport) {
        this.cycleReport = cycleReport;
    }

    @Parameter(key = SYNC_REPORT_KEY)
    public Boolean getSyncReport() {
        return syncReport;
    }

    public void setSyncReport(Boolean syncReport) {
        this.syncReport = syncReport;
    }

    @Override
    @Parameter(excluded = true, attribute = false)
    public boolean isValid() {
        return StringUtils.isNotEmpty(address);
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Boolean getCluster() {
        return cluster;
    }

    public void setCluster(Boolean cluster) {
        this.cluster = cluster;
    }

    public String getRegistry() {
        return registry;
    }

    public void setRegistry(String registry) {
        this.registry = registry;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
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

    public Boolean isCheck() {
        return check;
    }

    public void setCheck(Boolean check) {
        this.check = check;
    }

    @Parameter(key = REPORT_METADATA_KEY)
    public Boolean getReportMetadata() {
        return reportMetadata;
    }

    public void setReportMetadata(Boolean reportMetadata) {
        this.reportMetadata = reportMetadata;
    }

    @Parameter(key = REPORT_DEFINITION_KEY)
    public Boolean getReportDefinition() {
        return reportDefinition;
    }

    public void setReportDefinition(Boolean reportDefinition) {
        this.reportDefinition = reportDefinition;
    }
}
