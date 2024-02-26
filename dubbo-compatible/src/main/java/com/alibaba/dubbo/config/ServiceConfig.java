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
package com.alibaba.dubbo.config;

import org.apache.dubbo.config.annotation.Service;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class ServiceConfig<T> extends org.apache.dubbo.config.ServiceConfig<T> {

    public ServiceConfig() {}

    public ServiceConfig(Service service) {
        super(service);
    }

    public void setProvider(com.alibaba.dubbo.config.ProviderConfig provider) {
        super.setProvider(provider);
    }

    public void setApplication(com.alibaba.dubbo.config.ApplicationConfig application) {
        super.setApplication(application);
    }

    public void setModule(com.alibaba.dubbo.config.ModuleConfig module) {
        super.setModule(module);
    }

    public void setRegistry(com.alibaba.dubbo.config.RegistryConfig registry) {
        super.setRegistry(registry);
    }

    public void addMethod(com.alibaba.dubbo.config.MethodConfig methodConfig) {
        super.addMethod(methodConfig);
    }

    public com.alibaba.dubbo.config.MonitorConfig getMonitor() {
        org.apache.dubbo.config.MonitorConfig monitorConfig = super.getMonitor();
        if (monitorConfig == null) {
            return null;
        }
        if (monitorConfig instanceof com.alibaba.dubbo.config.MonitorConfig) {
            return (com.alibaba.dubbo.config.MonitorConfig) monitorConfig;
        }
        throw new IllegalArgumentException("Monitor has not been set with type com.alibaba.dubbo.config.MonitorConfig. "
                + "Found " + monitorConfig.getClass().getName() + " instead.");
    }

    public void setMonitor(com.alibaba.dubbo.config.MonitorConfig monitor) {
        super.setMonitor(monitor);
    }

    public void setProtocol(com.alibaba.dubbo.config.ProtocolConfig protocol) {
        super.setProtocol(protocol);
    }

    public void setMock(Boolean mock) {
        if (mock == null) {
            setMock((String) null);
        } else {
            setMock(String.valueOf(mock));
        }
    }

    public void setProviders(List<ProviderConfig> providers) {
        setProtocols(convertProviderToProtocol(providers));
    }

    private static List<ProtocolConfig> convertProviderToProtocol(List<ProviderConfig> providers) {
        if (providers == null || providers.isEmpty()) {
            return null;
        }
        List<ProtocolConfig> protocols = new ArrayList<ProtocolConfig>(providers.size());
        for (ProviderConfig provider : providers) {
            protocols.add(convertProviderToProtocol(provider));
        }
        return protocols;
    }

    private static ProtocolConfig convertProviderToProtocol(ProviderConfig provider) {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName(provider.getProtocol().getName());
        protocol.setServer(provider.getServer());
        protocol.setClient(provider.getClient());
        protocol.setCodec(provider.getCodec());
        protocol.setHost(provider.getHost());
        protocol.setPort(provider.getPort());
        protocol.setPath(provider.getPath());
        protocol.setPayload(provider.getPayload());
        protocol.setThreads(provider.getThreads());
        protocol.setParameters(provider.getParameters());
        return protocol;
    }

    private static ProviderConfig convertProtocolToProvider(ProtocolConfig protocol) {
        ProviderConfig provider = new ProviderConfig();
        provider.setProtocol(protocol);
        provider.setServer(protocol.getServer());
        provider.setClient(protocol.getClient());
        provider.setCodec(protocol.getCodec());
        provider.setHost(protocol.getHost());
        provider.setPort(protocol.getPort());
        provider.setPath(protocol.getPath());
        provider.setPayload(protocol.getPayload());
        provider.setThreads(protocol.getThreads());
        provider.setParameters(protocol.getParameters());
        return provider;
    }
}
