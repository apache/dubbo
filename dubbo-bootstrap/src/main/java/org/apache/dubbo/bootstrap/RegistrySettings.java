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
package org.apache.dubbo.bootstrap;

import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.builders.RegistryBuilder;

import java.util.Map;

/**
 * The settings of {@link RegistryConfig}
 *
 * @since 2.7.3
 */
public class RegistrySettings extends AbstractSettings {

    private final RegistryBuilder builder;

    public RegistrySettings(RegistryBuilder builder, DubboBootstrap dubboBootstrap) {
        super(dubboBootstrap);
        this.builder = builder;
    }

    public RegistrySettings address(String address) {
        builder.address(address);
        return this;
    }

    public RegistrySettings username(String username) {
        builder.username(username);
        return this;
    }

    public RegistrySettings password(String password) {
        builder.password(password);
        return this;
    }

    public RegistrySettings port(Integer port) {
        builder.port(port);
        return this;
    }

    public RegistrySettings protocol(String protocol) {
        builder.protocol(protocol);
        return this;
    }

    public RegistrySettings transporter(String transporter) {
        builder.transporter(transporter);
        return this;
    }

    @Deprecated
    public RegistrySettings transport(String transport) {
        builder.transport(transport);
        return this;
    }

    public RegistrySettings server(String server) {
        builder.server(server);
        return this;
    }

    public RegistrySettings client(String client) {
        builder.client(client);
        return this;
    }

    public RegistrySettings cluster(String cluster) {
        builder.cluster(cluster);
        return this;
    }

    public RegistrySettings group(String group) {
        builder.group(group);
        return this;
    }

    public RegistrySettings version(String version) {
        builder.version(version);
        return this;
    }

    public RegistrySettings timeout(Integer timeout) {
        builder.timeout(timeout);
        return this;
    }

    public RegistrySettings session(Integer session) {
        builder.session(session);
        return this;
    }

    public RegistrySettings file(String file) {
        builder.file(file);
        return this;
    }

    @Deprecated
    public RegistrySettings wait(Integer wait) {
        builder.wait(wait);
        return this;
    }

    public RegistrySettings isCheck(Boolean check) {
        builder.isCheck(check);
        return this;
    }

    public RegistrySettings isDynamic(Boolean dynamic) {
        builder.isDynamic(dynamic);
        return this;
    }

    public RegistrySettings register(Boolean register) {
        builder.register(register);
        return this;
    }

    public RegistrySettings subscribe(Boolean subscribe) {
        builder.subscribe(subscribe);
        return this;
    }

    public RegistrySettings appendParameter(String key, String value) {
        builder.appendParameter(key, value);
        return this;
    }

    public RegistrySettings appendParameters(Map<String, String> appendParameters) {
        builder.appendParameters(appendParameters);
        return this;
    }

    public RegistrySettings isDefault(Boolean isDefault) {
        builder.isDefault(isDefault);
        return this;
    }

    public RegistrySettings simplified(Boolean simplified) {
        builder.simplified(simplified);
        return this;
    }

    public RegistrySettings extraKeys(String extraKeys) {
        builder.extraKeys(extraKeys);
        return this;
    }
}
