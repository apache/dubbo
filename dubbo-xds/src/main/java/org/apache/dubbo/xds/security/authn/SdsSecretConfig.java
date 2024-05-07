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
package org.apache.dubbo.xds.security.authn;

import io.envoyproxy.envoy.config.core.v3.ApiConfigSource;

public class SdsSecretConfig implements SecretConfig {

    private String configName;

    private ConfigType configType;

    private ApiConfigSource apiConfigSource;

    public SdsSecretConfig(String configName, ConfigType configType, ApiConfigSource apiConfigSource) {
        this.configName = configName;
        this.configType = configType;
        this.apiConfigSource = apiConfigSource;
    }

    @Override
    public String name() {
        return configName;
    }

    @Override
    public ConfigType configType() {
        return configType;
    }

    @Override
    public Source source() {
        return Source.SDS;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public ConfigType getConfigType() {
        return configType;
    }

    public void setConfigType(ConfigType configType) {
        this.configType = configType;
    }

    public ApiConfigSource getApiConfigSource() {
        return apiConfigSource;
    }

    public void setApiConfigSource(ApiConfigSource apiConfigSource) {
        this.apiConfigSource = apiConfigSource;
    }
}
