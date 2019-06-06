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

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.builders.ApplicationBuilder;

import java.util.Map;

/**
 * {@link ApplicationConfig Application} settings
 *
 * @since 2.7.3
 */
public class ApplicationSettings extends AbstractSettings {

    private final ApplicationBuilder builder;

    public ApplicationSettings(ApplicationBuilder builder, DubboBootstrap dubboBootstrap) {
        super(dubboBootstrap);
        this.builder = builder;
    }

    public ApplicationSettings version(String version) {
        builder.version(version);
        return this;
    }

    public ApplicationSettings owner(String owner) {
        builder.owner(owner);
        return this;
    }

    public ApplicationSettings organization(String organization) {
        builder.organization(organization);
        return this;
    }

    public ApplicationSettings architecture(String architecture) {
        builder.architecture(architecture);
        return this;
    }

    public ApplicationSettings environment(String environment) {
        builder.environment(environment);
        return this;
    }

    public ApplicationSettings compiler(String compiler) {
        builder.compiler(compiler);
        return this;
    }

    public ApplicationSettings logger(String logger) {
        builder.logger(logger);
        return this;
    }

    public ApplicationSettings monitor(MonitorConfig monitor) {
        builder.monitor(monitor);
        return this;
    }

    public ApplicationSettings monitor(String monitor) {
        builder.monitor(monitor);
        return this;
    }

    public ApplicationSettings isDefault(Boolean isDefault) {
        builder.isDefault(isDefault);
        return this;
    }

    public ApplicationSettings dumpDirectory(String dumpDirectory) {
        builder.dumpDirectory(dumpDirectory);
        return this;
    }

    public ApplicationSettings qosEnable(Boolean qosEnable) {
        builder.qosEnable(qosEnable);
        return this;
    }

    public ApplicationSettings qosPort(Integer qosPort) {
        builder.qosPort(qosPort);
        return this;
    }

    public ApplicationSettings qosAcceptForeignIp(Boolean qosAcceptForeignIp) {
        builder.qosAcceptForeignIp(qosAcceptForeignIp);
        return this;
    }

    public ApplicationSettings shutwait(String shutwait) {
        builder.shutwait(shutwait);
        return this;
    }

    public ApplicationSettings appendParameter(String key, String value) {
        builder.appendParameter(key, value);
        return this;
    }

    public ApplicationSettings appendParameters(Map<String, String> appendParameters) {
        builder.appendParameters(appendParameters);
        return this;
    }

    ApplicationConfig build() {
        return builder.build();
    }
}
