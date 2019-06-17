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
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.builders.ReferenceBuilder;

import java.util.List;
import java.util.Map;

/**
 * The settings of {@link ReferenceConfig}
 *
 * @since 2.7.3
 */
public class ReferenceSettings<S> extends AbstractSettings {

    private final ReferenceBuilder<S> builder;

    public ReferenceSettings(ReferenceBuilder<S> builder, DubboBootstrap dubboBootstrap) {
        super(dubboBootstrap);
        this.builder = builder;
    }

    public ReferenceSettings<S> interfaceName(String interfaceName) {
        builder.interfaceName(interfaceName);
        return this;
    }

    public ReferenceSettings<S> interfaceClass(Class<?> interfaceClass) {
        builder.interfaceClass(interfaceClass);
        return this;
    }

    public ReferenceSettings<S> client(String client) {
        builder.client(client);
        return this;
    }

    public ReferenceSettings<S> url(String url) {
        builder.url(url);
        return this;
    }

    public ReferenceSettings<S> addMethods(List<MethodConfig> methods) {
        builder.addMethods(methods);
        return this;
    }

    public ReferenceSettings<S> addMethod(MethodConfig method) {
        builder.addMethod(method);
        return this;
    }

    public ReferenceSettings<S> consumer(ConsumerConfig consumer) {
        builder.consumer(consumer);
        return this;
    }

    public ReferenceSettings<S> protocol(String protocol) {
        builder.protocol(protocol);
        return this;
    }

    public ReferenceSettings<S> check(Boolean check) {
        builder.check(check);
        return this;
    }

    public ReferenceSettings<S> init(Boolean init) {
        builder.init(init);
        return this;
    }

    public ReferenceSettings<S> generic(String generic) {
        builder.generic(generic);
        return this;
    }

    public ReferenceSettings<S> generic(Boolean generic) {
        builder.generic(generic);
        return this;
    }

    @Deprecated
    public ReferenceSettings<S> injvm(Boolean injvm) {
        builder.injvm(injvm);
        return this;
    }

    public ReferenceSettings<S> lazy(Boolean lazy) {
        builder.lazy(lazy);
        return this;
    }

    public ReferenceSettings<S> reconnect(String reconnect) {
        builder.reconnect(reconnect);
        return this;
    }

    public ReferenceSettings<S> sticky(Boolean sticky) {
        builder.sticky(sticky);
        return this;
    }

    public ReferenceSettings<S> version(String version) {
        builder.version(version);
        return this;
    }

    public ReferenceSettings<S> group(String group) {
        builder.group(group);
        return this;
    }

    @Deprecated
    public ReferenceSettings<S> local(String local) {
        builder.local(local);
        return this;
    }

    @Deprecated
    public ReferenceSettings<S> local(Boolean local) {
        builder.local(local);
        return this;
    }

    public ReferenceSettings<S> stub(String stub) {
        builder.stub(stub);
        return this;
    }

    public ReferenceSettings<S> stub(Boolean stub) {
        builder.stub(stub);
        return this;
    }

    public ReferenceSettings<S> monitor(MonitorConfig monitor) {
        builder.monitor(monitor);
        return this;
    }

    public ReferenceSettings<S> monitor(String monitor) {
        builder.monitor(monitor);
        return this;
    }

    public ReferenceSettings<S> proxy(String proxy) {
        builder.proxy(proxy);
        return this;
    }

    public ReferenceSettings<S> cluster(String cluster) {
        builder.cluster(cluster);
        return this;
    }

    public ReferenceSettings<S> filter(String filter) {
        builder.filter(filter);
        return this;
    }

    public ReferenceSettings<S> listener(String listener) {
        builder.listener(listener);
        return this;
    }

    public ReferenceSettings<S> owner(String owner) {
        builder.owner(owner);
        return this;
    }

    public ReferenceSettings<S> connections(Integer connections) {
        builder.connections(connections);
        return this;
    }

    public ReferenceSettings<S> layer(String layer) {
        builder.layer(layer);
        return this;
    }

    public ReferenceSettings<S> application(ApplicationConfig application) {
        builder.application(application);
        return this;
    }

    public ReferenceSettings<S> module(ModuleConfig module) {
        builder.module(module);
        return this;
    }

    public ReferenceSettings<S> addRegistries(List<RegistryConfig> registries) {
        builder.addRegistries(registries);
        return this;
    }

    public ReferenceSettings<S> addRegistry(RegistryConfig registry) {
        builder.addRegistry(registry);
        return this;
    }

    public ReferenceSettings<S> registryIds(String registryIds) {
        builder.registryIds(registryIds);
        return this;
    }

    public ReferenceSettings<S> onconnect(String onconnect) {
        builder.onconnect(onconnect);
        return this;
    }

    public ReferenceSettings<S> ondisconnect(String ondisconnect) {
        builder.ondisconnect(ondisconnect);
        return this;
    }

    public ReferenceSettings<S> metadataReportConfig(MetadataReportConfig metadataReportConfig) {
        builder.metadataReportConfig(metadataReportConfig);
        return this;
    }

    public ReferenceSettings<S> configCenter(ConfigCenterConfig configCenter) {
        builder.configCenter(configCenter);
        return this;
    }

    public ReferenceSettings<S> callbacks(Integer callbacks) {
        builder.callbacks(callbacks);
        return this;
    }

    public ReferenceSettings<S> scope(String scope) {
        builder.scope(scope);
        return this;
    }

    public ReferenceSettings<S> tag(String tag) {
        builder.tag(tag);
        return this;
    }

    public ReferenceSettings<S> timeout(Integer timeout) {
        builder.timeout(timeout);
        return this;
    }

    public ReferenceSettings<S> retries(Integer retries) {
        builder.retries(retries);
        return this;
    }

    public ReferenceSettings<S> actives(Integer actives) {
        builder.actives(actives);
        return this;
    }

    public ReferenceSettings<S> loadbalance(String loadbalance) {
        builder.loadbalance(loadbalance);
        return this;
    }

    public ReferenceSettings<S> async(Boolean async) {
        builder.async(async);
        return this;
    }

    public ReferenceSettings<S> sent(Boolean sent) {
        builder.sent(sent);
        return this;
    }

    public ReferenceSettings<S> mock(String mock) {
        builder.mock(mock);
        return this;
    }

    public ReferenceSettings<S> mock(Boolean mock) {
        builder.mock(mock);
        return this;
    }

    public ReferenceSettings<S> merger(String merger) {
        builder.merger(merger);
        return this;
    }

    public ReferenceSettings<S> cache(String cache) {
        builder.cache(cache);
        return this;
    }

    public ReferenceSettings<S> validation(String validation) {
        builder.validation(validation);
        return this;
    }

    public ReferenceSettings<S> appendParameters(Map<String, String> appendParameters) {
        builder.appendParameters(appendParameters);
        return this;
    }

    public ReferenceSettings<S> appendParameter(String key, String value) {
        builder.appendParameter(key, value);
        return this;
    }

    public ReferenceSettings<S> forks(Integer forks) {
        builder.forks(forks);
        return this;
    }
}
