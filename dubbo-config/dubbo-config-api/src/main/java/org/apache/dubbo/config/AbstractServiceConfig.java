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
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.ExporterListener;

import java.util.Arrays;
import java.util.List;

/**
 * AbstractServiceConfig
 *
 * @export
 */
public abstract class AbstractServiceConfig extends AbstractInterfaceConfig {

    private static final long serialVersionUID = 1L;

    // version
    protected String version;

    // group
    protected String group;

    // whether the service is deprecated
    protected Boolean deprecated;

    // delay service exporting
    protected Integer delay;

    // whether to export the service
    protected Boolean export;

    // weight
    protected Integer weight;

    // document center
    protected String document;

    // whether to register as a dynamic service or not on register center
    protected Boolean dynamic;

    // whether to use token
    protected String token;

    // access log
    protected String accesslog;
    protected List<ProtocolConfig> protocols;
    protected String protocolIds;
    // provider tag
    protected String tag;
    // max allowed execute times
    private Integer executes;
    // whether to register
    private Boolean register;
    // warm up period
    private Integer warmup;
    // serialization
    private String serialization;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        checkKey("version", version);
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        checkKey("group", group);
        this.group = group;
    }

    public Integer getDelay() {
        return delay;
    }

    public void setDelay(Integer delay) {
        this.delay = delay;
    }

    public Boolean getExport() {
        return export;
    }

    public void setExport(Boolean export) {
        this.export = export;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    @Parameter(escaped = true)
    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public String getToken() {
        return token;
    }

    public void setToken(Boolean token) {
        if (token == null) {
            setToken((String) null);
        } else {
            setToken(String.valueOf(token));
        }
    }

    public void setToken(String token) {
        checkName("token", token);
        this.token = token;
    }

    public Boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    public Boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(Boolean dynamic) {
        this.dynamic = dynamic;
    }

    public List<ProtocolConfig> getProtocols() {
        return protocols;
    }

    @SuppressWarnings({"unchecked"})
    public void setProtocols(List<? extends ProtocolConfig> protocols) {
        this.protocols = (List<ProtocolConfig>) protocols;
    }

    public ProtocolConfig getProtocol() {
        return protocols == null || protocols.isEmpty() ? null : protocols.get(0);
    }

    public void setProtocol(ProtocolConfig protocol) {
        this.protocols = Arrays.asList(protocol);
    }

    @Parameter(excluded = true)
    public String getProtocolIds() {
        return protocolIds;
    }

    public void setProtocolIds(String protocolIds) {
        this.protocolIds = protocolIds;
    }

    public String getAccesslog() {
        return accesslog;
    }

    public void setAccesslog(Boolean accesslog) {
        if (accesslog == null) {
            setAccesslog((String) null);
        } else {
            setAccesslog(String.valueOf(accesslog));
        }
    }

    public void setAccesslog(String accesslog) {
        this.accesslog = accesslog;
    }

    public Integer getExecutes() {
        return executes;
    }

    public void setExecutes(Integer executes) {
        this.executes = executes;
    }

    @Override
    @Parameter(key = Constants.SERVICE_FILTER_KEY, append = true)
    public String getFilter() {
        return super.getFilter();
    }

    @Override
    @Parameter(key = Constants.EXPORTER_LISTENER_KEY, append = true)
    public String getListener() {
        return listener;
    }

    @Override
    public void setListener(String listener) {
        checkMultiExtension(ExporterListener.class, "listener", listener);
        this.listener = listener;
    }

    public Boolean isRegister() {
        return register;
    }

    public void setRegister(Boolean register) {
        this.register = register;
    }

    public Integer getWarmup() {
        return warmup;
    }

    public void setWarmup(Integer warmup) {
        this.warmup = warmup;
    }

    public String getSerialization() {
        return serialization;
    }

    public void setSerialization(String serialization) {
        this.serialization = serialization;
    }

    @Parameter(key = "dubbo.tag", useKeyAsProperty = false)
    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
