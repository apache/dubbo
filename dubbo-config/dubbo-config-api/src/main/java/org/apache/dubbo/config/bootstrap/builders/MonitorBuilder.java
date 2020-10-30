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
package org.apache.dubbo.config.bootstrap.builders;

import org.apache.dubbo.config.MonitorConfig;

import java.util.Map;

/**
 * This is a builder for build {@link MonitorConfig}.
 *
 * @since 2.7
 */
public class MonitorBuilder extends AbstractBuilder<MonitorConfig, MonitorBuilder> {
    /**
     * The protocol of the monitor, if the value is registry, it will search the monitor address from the registry center,
     * otherwise, it will directly connect to the monitor center
     */
    private String protocol;

    /**
     * The monitor address
     */
    private String address;

    /**
     * The monitor user name
     */
    private String username;

    /**
     * The password
     */
    private String password;

    private String group;

    private String version;

    private String interval;

    /**
     * customized parameters
     */
    private Map<String, String> parameters;

    /**
     * If it's default
     */
    private Boolean isDefault;

    public MonitorBuilder protocol(String protocol) {
        this.protocol = protocol;
        return getThis();
    }

    public MonitorBuilder address(String address) {
        this.address = address;
        return getThis();
    }

    public MonitorBuilder username(String username) {
        this.username = username;
        return getThis();
    }

    public MonitorBuilder password(String password) {
        this.password = password;
        return getThis();
    }

    public MonitorBuilder group(String group) {
        this.group = group;
        return getThis();
    }

    public MonitorBuilder version(String version) {
        this.version = version;
        return getThis();
    }

    public MonitorBuilder interval(String interval) {
        this.interval = interval;
        return getThis();
    }

    public MonitorBuilder isDefault(Boolean isDefault) {
        this.isDefault = isDefault;
        return getThis();
    }

    public MonitorBuilder appendParameter(String key, String value) {
        this.parameters = appendParameter(parameters, key, value);
        return getThis();
    }

    public MonitorBuilder appendParameters(Map<String, String> appendParameters) {
        this.parameters = appendParameters(parameters, appendParameters);
        return getThis();
    }

    public MonitorConfig build() {
        MonitorConfig monitorConfig = new MonitorConfig();
        super.build(monitorConfig);

        monitorConfig.setProtocol(protocol);
        monitorConfig.setAddress(address);
        monitorConfig.setUsername(username);
        monitorConfig.setPassword(password);
        monitorConfig.setGroup(group);
        monitorConfig.setVersion(version);
        monitorConfig.setInterval(interval);
        monitorConfig.setParameters(parameters);
        monitorConfig.setDefault(isDefault);

        return monitorConfig;
    }

    @Override
    protected MonitorBuilder getThis() {
        return this;
    }
}
