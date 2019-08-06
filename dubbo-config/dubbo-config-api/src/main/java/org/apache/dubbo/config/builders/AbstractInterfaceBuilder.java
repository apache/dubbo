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
package org.apache.dubbo.config.builders;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.rpc.Filter;

import java.util.ArrayList;
import java.util.List;

/**
 * AbstractBuilder
 *
 * @since 2.7
 */
public abstract class AbstractInterfaceBuilder<T extends AbstractInterfaceConfig, B extends AbstractInterfaceBuilder<T, B>>
        extends AbstractMethodBuilder<T, B> {
    /**
     * Local impl class name for the service interface
     */
    protected String local;

    /**
     * Local stub class name for the service interface
     */
    protected String stub;

    /**
     * Service monitor
     */
    protected MonitorConfig monitor;

    /**
     * Strategies for generating dynamic agents，there are two strategies can be choosed: jdk and javassist
     */
    protected String proxy;

    /**
     * Cluster type
     */
    protected String cluster;

    /**
     * The {@link Filter} when the provider side exposed a service or the customer side references a remote service used,
     * if there are more than one, you can use commas to separate them
     */
    protected String filter;

    /**
     * The Listener when the provider side exposes a service or the customer side references a remote service used
     * if there are more than one, you can use commas to separate them
     */
    protected String listener;

    /**
     * The owner of the service providers
     */
    protected String owner;

    /**
     * Connection limits, 0 means shared connection, otherwise it defines the connections delegated to the current service
     */
    protected Integer connections;

    /**
     * The layer of service providers
     */
    protected String layer;

    /**
     * The application info
     */
    protected ApplicationConfig application;

    /**
     * The module info
     */
    protected ModuleConfig module;

    /**
     * Registry centers
     */
    protected List<RegistryConfig> registries;

    protected String registryIds;

    // connection events
    protected String onconnect;

    /**
     * Disconnection events
     */
    protected String ondisconnect;
    protected MetadataReportConfig metadataReportConfig;

    protected ConfigCenterConfig configCenter;

    // callback limits
    private Integer callbacks;
    // the scope for referring/exporting a service, if it's local, it means searching in current JVM only.
    private String scope;

    private String tag;

    /**
     * @param local
     * @see org.apache.dubbo.config.builders.AbstractInterfaceBuilder#stub(String)
     * @deprecated Replace to <code>stub(String)</code>
     */
    @Deprecated
    public B local(String local) {
        this.local = local;
        return getThis();
    }

    /**
     * @param local
     * @see org.apache.dubbo.config.builders.AbstractInterfaceBuilder#stub(Boolean)
     * @deprecated Replace to <code>stub(Boolean)</code>
     */
    @Deprecated
    public B local(Boolean local) {
        if (local != null) {
            this.local = local.toString();
        } else {
            this.local = null;
        }
        return getThis();
    }

    public B stub(String stub) {
        this.stub = stub;
        return getThis();
    }

    public B stub(Boolean stub) {
        if (stub != null) {
            this.stub = stub.toString();
        } else {
            this.stub = null;
        }
        return getThis();
    }

    public B monitor(MonitorConfig monitor) {
        this.monitor = monitor;
        return getThis();
    }

    public B monitor(String monitor) {
        this.monitor = new MonitorConfig(monitor);
        return getThis();
    }

    public B proxy(String proxy) {
        this.proxy = proxy;
        return getThis();
    }

    public B cluster(String cluster) {
        this.cluster = cluster;
        return getThis();
    }

    public B filter(String filter) {
        this.filter = filter;
        return getThis();
    }

    public B listener(String listener) {
        this.listener = listener;
        return getThis();
    }

    public B owner(String owner) {
        this.owner = owner;
        return getThis();
    }

    public B connections(Integer connections) {
        this.connections = connections;
        return getThis();
    }

    public B layer(String layer) {
        this.layer = layer;
        return getThis();
    }

    public B application(ApplicationConfig application) {
        this.application = application;
        return getThis();
    }

    public B module(ModuleConfig module) {
        this.module = module;
        return getThis();
    }

    public B addRegistries(List<RegistryConfig> registries) {
        if (this.registries == null) {
            this.registries = new ArrayList<>();
        }
        this.registries.addAll(registries);
        return getThis();
    }

    public B addRegistry(RegistryConfig registry) {
        if (this.registries == null) {
            this.registries = new ArrayList<>();
        }
        this.registries.add(registry);
        return getThis();
    }

    public B registryIds(String registryIds) {
        this.registryIds = registryIds;
        return getThis();
    }

    public B onconnect(String onconnect) {
        this.onconnect = onconnect;
        return getThis();
    }

    public B ondisconnect(String ondisconnect) {
        this.ondisconnect = ondisconnect;
        return getThis();
    }

    public B metadataReportConfig(MetadataReportConfig metadataReportConfig) {
        this.metadataReportConfig = metadataReportConfig;
        return getThis();
    }

    public B configCenter(ConfigCenterConfig configCenter) {
        this.configCenter = configCenter;
        return getThis();
    }

    public B callbacks(Integer callbacks) {
        this.callbacks = callbacks;
        return getThis();
    }

    public B scope(String scope) {
        this.scope = scope;
        return getThis();
    }

    public B tag(String tag) {
        this.tag = tag;
        return getThis();
    }

    @Override
    public void build(T instance) {
        super.build(instance);

        if (!StringUtils.isEmpty(local)) {
            instance.setLocal(local);
        }
        if (!StringUtils.isEmpty(stub)) {
            instance.setStub(stub);
        }
        if (monitor != null) {
            instance.setMonitor(monitor);
        }
        if (!StringUtils.isEmpty(proxy)) {
            instance.setProxy(proxy);
        }
        if (!StringUtils.isEmpty(cluster)) {
            instance.setCluster(cluster);
        }
        if (!StringUtils.isEmpty(filter)) {
            instance.setFilter(filter);
        }
        if (!StringUtils.isEmpty(listener)) {
            instance.setListener(listener);
        }
        if (!StringUtils.isEmpty(owner)) {
            instance.setOwner(owner);
        }
        if (connections != null) {
            instance.setConnections(connections);
        }
        if (!StringUtils.isEmpty(layer)) {
            instance.setLayer(layer);
        }
        if (application != null) {
            instance.setApplication(application);
        }
        if (module != null) {
            instance.setModule(module);
        }
        if (registries != null) {
            instance.setRegistries(registries);
        }
        if (!StringUtils.isEmpty(registryIds)) {
            instance.setRegistryIds(registryIds);
        }
        if (!StringUtils.isEmpty(onconnect)) {
            instance.setOnconnect(onconnect);
        }
        if (!StringUtils.isEmpty(ondisconnect)) {
            instance.setOndisconnect(ondisconnect);
        }
        if (metadataReportConfig != null) {
            instance.setMetadataReportConfig(metadataReportConfig);
        }
        if (configCenter != null) {
            instance.setConfigCenter(configCenter);
        }
        if (callbacks != null) {
            instance.setCallbacks(callbacks);
        }
        if (!StringUtils.isEmpty(scope)) {
            instance.setScope(scope);
        }
        if (StringUtils.isNotEmpty(tag)) {
            instance.setTag(tag);
        }
    }
}
