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
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.builders.ServiceBuilder;

import java.util.List;
import java.util.Map;

/**
 * The settings of {@link ServiceConfig Dubbo service}
 *
 * @since 2.7.4
 */
public class ServiceSettings<S> extends AbstractSettings {

    private final ServiceBuilder<S> builder;

    public ServiceSettings(ServiceBuilder<S> builder, DubboBootstrap dubboBootstrap) {
        super(dubboBootstrap);
        this.builder = builder;
    }

    public ServiceSettings<S> interfaceName(String interfaceName) {
        builder.interfaceName(interfaceName);
        return this;
    }

    public ServiceSettings<S> interfaceClass(Class<?> interfaceClass) {
        builder.interfaceClass(interfaceClass);
        return this;
    }

    public ServiceSettings<S> ref(S ref) {
        builder.ref(ref);
        return this;
    }

    public ServiceSettings<S> path(String path) {
        builder.path(path);
        return this;
    }

    public ServiceSettings<S> addMethod(MethodConfig method) {
        builder.addMethod(method);
        return this;
    }

    public ServiceSettings<S> addMethods(List<? extends MethodConfig> methods) {
        builder.addMethods(methods);
        return this;
    }

    public ServiceSettings<S> provider(ProviderConfig provider) {
        builder.provider(provider);
        return this;
    }

    public ServiceSettings<S> providerIds(String providerIds) {
        builder.providerIds(providerIds);
        return this;
    }

    public ServiceSettings<S> generic(String generic) {
        builder.generic(generic);
        return this;
    }

    public ServiceSettings<S> mock(String mock) {
        builder.mock(mock);
        return this;
    }

    public ServiceSettings<S> mock(Boolean mock) {
        builder.mock(mock);
        return this;
    }

    public ServiceSettings<S> version(String version) {
        builder.version(version);
        return this;
    }

    public ServiceSettings<S> group(String group) {
        builder.group(group);
        return this;
    }

    public ServiceSettings<S> deprecated(Boolean deprecated) {
        builder.deprecated(deprecated);
        return this;
    }

    public ServiceSettings<S> delay(Integer delay) {
        builder.delay(delay);
        return this;
    }

    public ServiceSettings<S> export(Boolean export) {
        builder.export(export);
        return this;
    }

    public ServiceSettings<S> weight(Integer weight) {
        builder.weight(weight);
        return this;
    }

    public ServiceSettings<S> document(String document) {
        builder.document(document);
        return this;
    }

    public ServiceSettings<S> dynamic(Boolean dynamic) {
        builder.dynamic(dynamic);
        return this;
    }

    public ServiceSettings<S> token(String token) {
        builder.token(token);
        return this;
    }

    public ServiceSettings<S> token(Boolean token) {
        builder.token(token);
        return this;
    }

    public ServiceSettings<S> accesslog(String accesslog) {
        builder.accesslog(accesslog);
        return this;
    }

    public ServiceSettings<S> accesslog(Boolean accesslog) {
        builder.accesslog(accesslog);
        return this;
    }

    public ServiceSettings<S> addProtocols(List<ProtocolConfig> protocols) {
        builder.addProtocols(protocols);
        return this;
    }

    public ServiceSettings<S> addProtocol(ProtocolConfig protocol) {
        builder.addProtocol(protocol);
        return this;
    }

    public ServiceSettings<S> protocolIds(String protocolIds) {
        builder.protocolIds(protocolIds);
        return this;
    }

    public ServiceSettings<S> executes(Integer executes) {
        builder.executes(executes);
        return this;
    }

    public ServiceSettings<S> register(Boolean register) {
        builder.register(register);
        return this;
    }

    public ServiceSettings<S> warmup(Integer warmup) {
        builder.warmup(warmup);
        return this;
    }

    public ServiceSettings<S> serialization(String serialization) {
        builder.serialization(serialization);
        return this;
    }

    @Deprecated
    public ServiceSettings<S> local(String local) {
        builder.local(local);
        return this;
    }

    @Deprecated
    public ServiceSettings<S> local(Boolean local) {
        builder.local(local);
        return this;
    }

    public ServiceSettings<S> stub(String stub) {
        builder.stub(stub);
        return this;
    }

    public ServiceSettings<S> stub(Boolean stub) {
        builder.stub(stub);
        return this;
    }

    public ServiceSettings<S> monitor(MonitorConfig monitor) {
        builder.monitor(monitor);
        return this;
    }

    public ServiceSettings<S> monitor(String monitor) {
        builder.monitor(monitor);
        return this;
    }

    public ServiceSettings<S> proxy(String proxy) {
        builder.proxy(proxy);
        return this;
    }

    public ServiceSettings<S> cluster(String cluster) {
        builder.cluster(cluster);
        return this;
    }

    public ServiceSettings<S> filter(String filter) {
        builder.filter(filter);
        return this;
    }

    public ServiceSettings<S> listener(String listener) {
        builder.listener(listener);
        return this;
    }

    public ServiceSettings<S> owner(String owner) {
        builder.owner(owner);
        return this;
    }

    public ServiceSettings<S> connections(Integer connections) {
        builder.connections(connections);
        return this;
    }

    public ServiceSettings<S> layer(String layer) {
        builder.layer(layer);
        return this;
    }

    public ServiceSettings<S> application(ApplicationConfig application) {
        builder.application(application);
        return this;
    }

    public ServiceSettings<S> module(ModuleConfig module) {
        builder.module(module);
        return this;
    }

    public ServiceSettings<S> addRegistries(List<RegistryConfig> registries) {
        builder.addRegistries(registries);
        return this;
    }

    public ServiceSettings<S> addRegistry(RegistryConfig registry) {
        builder.addRegistry(registry);
        return this;
    }

    public ServiceSettings<S> registryIds(String registryIds) {
        builder.registryIds(registryIds);
        return this;
    }

    public ServiceSettings<S> onconnect(String onconnect) {
        builder.onconnect(onconnect);
        return this;
    }

    public ServiceSettings<S> ondisconnect(String ondisconnect) {
        builder.ondisconnect(ondisconnect);
        return this;
    }

    public ServiceSettings<S> metadataReportConfig(MetadataReportConfig metadataReportConfig) {
        builder.metadataReportConfig(metadataReportConfig);
        return this;
    }

    public ServiceSettings<S> configCenter(ConfigCenterConfig configCenter) {
        builder.configCenter(configCenter);
        return this;
    }

    public ServiceSettings<S> callbacks(Integer callbacks) {
        builder.callbacks(callbacks);
        return this;
    }

    public ServiceSettings<S> scope(String scope) {
        builder.scope(scope);
        return this;
    }

    public ServiceSettings<S> tag(String tag) {
        builder.tag(tag);
        return this;
    }

    public ServiceSettings<S> timeout(Integer timeout) {
        builder.timeout(timeout);
        return this;
    }

    public ServiceSettings<S> retries(Integer retries) {
        builder.retries(retries);
        return this;
    }

    public ServiceSettings<S> actives(Integer actives) {
        builder.actives(actives);
        return this;
    }

    public ServiceSettings<S> loadbalance(String loadbalance) {
        builder.loadbalance(loadbalance);
        return this;
    }

    public ServiceSettings<S> async(Boolean async) {
        builder.async(async);
        return this;
    }

    public ServiceSettings<S> sent(Boolean sent) {
        builder.sent(sent);
        return this;
    }

    public ServiceSettings<S> merger(String merger) {
        builder.merger(merger);
        return this;
    }

    public ServiceSettings<S> cache(String cache) {
        builder.cache(cache);
        return this;
    }

    public ServiceSettings<S> validation(String validation) {
        builder.validation(validation);
        return this;
    }

    public ServiceSettings<S> appendParameters(Map<String, String> appendParameters) {
        builder.appendParameters(appendParameters);
        return this;
    }

    public ServiceSettings<S> appendParameter(String key, String value) {
        builder.appendParameter(key, value);
        return this;
    }

    public ServiceSettings<S> forks(Integer forks) {
        builder.forks(forks);
        return this;
    }
}
