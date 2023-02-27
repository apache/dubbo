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
package org.apache.dubbo.config.spring.reference;

import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.spring.ReferenceBean;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Builder for ReferenceBean, used to return ReferenceBean instance in Java-config @Bean method,
 * equivalent to {@link DubboReference} annotation.
 * </p>
 *
 * <p>
 * <b>It is recommended to use {@link DubboReference} on the @Bean method in the Java-config class.</b>
 * </p>
 *
 * Step 1: Register ReferenceBean in Java-config class:
 * <pre class="code">
 * &#64;Configuration
 * public class ReferenceConfiguration {
 *
 *     &#64;Bean
 *     public ReferenceBean&lt;HelloService&gt; helloService() {
 *         return new ReferenceBeanBuilder()
 *                 .setGroup("demo")
 *                 .build();
 *     }
 *
 *     &#64;Bean
 *     public ReferenceBean&lt;HelloService&gt; helloService2() {
 *         return new ReferenceBean();
 *     }
 *
 *     &#64;Bean
 *     public ReferenceBean&lt;GenericService&gt; genericHelloService() {
 *         return new ReferenceBeanBuilder()
 *                 .setGroup("demo")
 *                 .setInterface(HelloService.class)
 *                 .build();
 *     }
 *
 * }
 * </pre>
 *
 * Step 2: Inject ReferenceBean by @Autowired
 * <pre class="code">
 * public class FooController {
 *     &#64;Autowired
 *     private HelloService helloService;
 *
 *     &#64;Autowired
 *     private GenericService genericHelloService;
 * }
 * </pre>
 *
 * @see org.apache.dubbo.config.annotation.DubboReference
 * @see org.apache.dubbo.config.spring.ReferenceBean
 */
public class ReferenceBeanBuilder {
    private Map<String, Object> attributes = new HashMap<>();

    public <T> ReferenceBean<T> build() {
        return new ReferenceBean(attributes);
    }

    public ReferenceBeanBuilder setServices(String services) {
        attributes.put(ReferenceAttributes.SERVICES, services);
        return this;
    }

    public ReferenceBeanBuilder setInterface(String interfaceName) {
        attributes.put(ReferenceAttributes.INTERFACE_NAME, interfaceName);
        return this;
    }

    public ReferenceBeanBuilder setInterface(Class interfaceClass) {
        attributes.put(ReferenceAttributes.INTERFACE_CLASS, interfaceClass);
        return this;
    }

    public ReferenceBeanBuilder setClient(String client) {
        attributes.put(ReferenceAttributes.CLIENT, client);
        return this;
    }

    public ReferenceBeanBuilder setUrl(String url) {
        attributes.put(ReferenceAttributes.URL, url);
        return this;
    }

    public ReferenceBeanBuilder setConsumer(ConsumerConfig consumer) {
        attributes.put(ReferenceAttributes.CONSUMER, consumer);
        return this;
    }

    public ReferenceBeanBuilder setConsumer(String consumer) {
        attributes.put(ReferenceAttributes.CONSUMER, consumer);
        return this;
    }

    public ReferenceBeanBuilder setProtocol(String protocol) {
        attributes.put(ReferenceAttributes.PROTOCOL, protocol);
        return this;
    }

    public ReferenceBeanBuilder setCheck(Boolean check) {
        attributes.put(ReferenceAttributes.CHECK, check);
        return this;
    }

    public ReferenceBeanBuilder setInit(Boolean init) {
        attributes.put(ReferenceAttributes.INIT, init);
        return this;
    }

    //@Deprecated
    public ReferenceBeanBuilder setGeneric(Boolean generic) {
        attributes.put(ReferenceAttributes.GENERIC, generic);
        return this;
    }

    /**
     * @param injvm
     * @deprecated instead, use the parameter <b>scope</b> to judge if it's in jvm, scope=local
     */
    @Deprecated
    public ReferenceBeanBuilder setInjvm(Boolean injvm) {
        attributes.put(ReferenceAttributes.INJVM, injvm);
        return this;
    }

    public ReferenceBeanBuilder setListener(String listener) {
        attributes.put(ReferenceAttributes.LISTENER, listener);
        return this;
    }

    public ReferenceBeanBuilder setLazy(Boolean lazy) {
        attributes.put(ReferenceAttributes.LAZY, lazy);
        return this;
    }

    public ReferenceBeanBuilder setOnconnect(String onconnect) {
        attributes.put(ReferenceAttributes.ONCONNECT, onconnect);
        return this;
    }

    public ReferenceBeanBuilder setOndisconnect(String ondisconnect) {
        attributes.put(ReferenceAttributes.ONDISCONNECT, ondisconnect);
        return this;
    }

    public ReferenceBeanBuilder setReconnect(String reconnect) {
        attributes.put(ReferenceAttributes.RECONNECT, reconnect);
        return this;
    }

    public ReferenceBeanBuilder setSticky(Boolean sticky) {
        attributes.put(ReferenceAttributes.STICKY, sticky);
        return this;
    }

    public ReferenceBeanBuilder setVersion(String version) {
        attributes.put(ReferenceAttributes.VERSION, version);
        return this;
    }

    public ReferenceBeanBuilder setGroup(String group) {
        attributes.put(ReferenceAttributes.GROUP, group);
        return this;
    }

    public ReferenceBeanBuilder setProvidedBy(String providedBy) {
        attributes.put(ReferenceAttributes.PROVIDED_BY, providedBy);
        return this;
    }

    public ReferenceBeanBuilder setProviderPort(Integer providerPort) {
        attributes.put(ReferenceAttributes.PROVIDER_PORT, providerPort);
        return this;
    }

//    public ReferenceBeanBuilder setRouter(String router) {
//        attributes.put(ReferenceAttributes.ROUTER, router);
//        return this;
//    }

    public ReferenceBeanBuilder setStub(String stub) {
        attributes.put(ReferenceAttributes.STUB, stub);
        return this;
    }

    public ReferenceBeanBuilder setCluster(String cluster) {
        attributes.put(ReferenceAttributes.CLUSTER, cluster);
        return this;
    }

    public ReferenceBeanBuilder setProxy(String proxy) {
        attributes.put(ReferenceAttributes.PROXY, proxy);
        return this;
    }

    public ReferenceBeanBuilder setConnections(Integer connections) {
        attributes.put(ReferenceAttributes.CONNECTIONS, connections);
        return this;
    }

    public ReferenceBeanBuilder setFilter(String filter) {
        attributes.put(ReferenceAttributes.FILTER, filter);
        return this;
    }

    public ReferenceBeanBuilder setLayer(String layer) {
        attributes.put(ReferenceAttributes.LAYER, layer);
        return this;
    }

//    @Deprecated
//    public ReferenceBeanBuilder setApplication(ApplicationConfig application) {
//        attributes.put(ReferenceAttributes.APPLICATION, application);
//        return this;
//    }

//    @Deprecated
//    public ReferenceBeanBuilder setModule(ModuleConfig module) {
//        attributes.put(ReferenceAttributes.MODULE, module);
//        return this;
//    }

    public ReferenceBeanBuilder setRegistry(String[] registryIds) {
        attributes.put(ReferenceAttributes.REGISTRY, registryIds);
        return this;
    }

    public ReferenceBeanBuilder setRegistry(RegistryConfig registry) {
        setRegistries(Arrays.asList(registry));
        return this;
    }

    public ReferenceBeanBuilder setRegistries(List<? extends RegistryConfig> registries) {
        attributes.put(ReferenceAttributes.REGISTRIES, registries);
        return this;
    }

    public ReferenceBeanBuilder setMethods(List<? extends MethodConfig> methods) {
        attributes.put(ReferenceAttributes.METHODS, methods);
        return this;
    }

    @Deprecated
    public ReferenceBeanBuilder setMonitor(MonitorConfig monitor) {
        attributes.put(ReferenceAttributes.MONITOR, monitor);
        return this;
    }

    @Deprecated
    public ReferenceBeanBuilder setMonitor(String monitor) {
        attributes.put(ReferenceAttributes.MONITOR, monitor);
        return this;
    }

    public ReferenceBeanBuilder setOwner(String owner) {
        attributes.put(ReferenceAttributes.OWNER, owner);
        return this;
    }

    public ReferenceBeanBuilder setCallbacks(Integer callbacks) {
        attributes.put(ReferenceAttributes.CALLBACKS, callbacks);
        return this;
    }

    public ReferenceBeanBuilder setScope(String scope) {
        attributes.put(ReferenceAttributes.SCOPE, scope);
        return this;
    }

    public ReferenceBeanBuilder setTag(String tag) {
        attributes.put(ReferenceAttributes.TAG, tag);
        return this;
    }

    public ReferenceBeanBuilder setTimeout(Integer timeout) {
        attributes.put(ReferenceAttributes.TIMEOUT, timeout);
        return this;
    }

    public ReferenceBeanBuilder setRetries(Integer retries) {
        attributes.put(ReferenceAttributes.RETRIES, retries);
        return this;
    }

    public ReferenceBeanBuilder setLoadBalance(String loadbalance) {
        attributes.put(ReferenceAttributes.LOAD_BALANCE, loadbalance);
        return this;
    }

    public ReferenceBeanBuilder setAsync(Boolean async) {
        attributes.put(ReferenceAttributes.ASYNC, async);
        return this;
    }

    public ReferenceBeanBuilder setActives(Integer actives) {
        attributes.put(ReferenceAttributes.ACTIVES, actives);
        return this;
    }

    public ReferenceBeanBuilder setSent(Boolean sent) {
        attributes.put(ReferenceAttributes.SENT, sent);
        return this;
    }

    public ReferenceBeanBuilder setMock(String mock) {
        attributes.put(ReferenceAttributes.MOCK, mock);
        return this;
    }

    public ReferenceBeanBuilder setMerger(String merger) {
        attributes.put(ReferenceAttributes.MERGER, merger);
        return this;
    }

    public ReferenceBeanBuilder setCache(String cache) {
        attributes.put(ReferenceAttributes.CACHE, cache);
        return this;
    }

    public ReferenceBeanBuilder setValidation(String validation) {
        attributes.put(ReferenceAttributes.VALIDATION, validation);
        return this;
    }

    public ReferenceBeanBuilder setParameters(Map<String, String> parameters) {
        attributes.put(ReferenceAttributes.PARAMETERS, parameters);
        return this;
    }

//    public ReferenceBeanBuilder setAuth(Boolean auth) {
//        attributes.put(ReferenceAttributes.AUTH, auth);
//        return this;
//    }
//
//    public ReferenceBeanBuilder setForks(Integer forks) {
//        attributes.put(ReferenceAttributes.FORKS, forks);
//        return this;
//    }
//
//    @Deprecated
//    public ReferenceBeanBuilder setConfigCenter(ConfigCenterConfig configCenter) {
//        attributes.put(ReferenceAttributes.CONFIG_CENTER, configCenter);
//        return this;
//    }
//
//    @Deprecated
//    public ReferenceBeanBuilder setMetadataReportConfig(MetadataReportConfig metadataReportConfig) {
//        attributes.put(ReferenceAttributes.METADATA_REPORT_CONFIG, metadataReportConfig);
//        return this;
//    }
//
//    @Deprecated
//    public ReferenceBeanBuilder setMetrics(MetricsConfig metrics) {
//        attributes.put(ReferenceAttributes.METRICS, metrics);
//        return this;
//    }

}
