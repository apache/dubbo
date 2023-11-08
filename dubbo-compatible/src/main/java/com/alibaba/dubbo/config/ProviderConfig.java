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

@Deprecated
public class ProviderConfig extends org.apache.dubbo.config.ProviderConfig {
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

    public void setMonitor(com.alibaba.dubbo.config.MonitorConfig monitor) {
        super.setMonitor(monitor);
    }

    public void setProtocol(com.alibaba.dubbo.config.ProtocolConfig protocol) {
        super.setProtocol(protocol);
    }

    @Override
    public void setProtocol(String protocol) {
        setProtocol(new com.alibaba.dubbo.config.ProtocolConfig(protocol));
    }

    public void setMock(Boolean mock) {
        if (mock == null) {
            setMock((String) null);
        } else {
            setMock(String.valueOf(mock));
        }
    }
}
