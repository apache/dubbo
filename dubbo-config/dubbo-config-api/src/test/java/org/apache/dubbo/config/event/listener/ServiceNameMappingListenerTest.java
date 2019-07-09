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
package org.apache.dubbo.config.event.listener;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link ServiceNameMappingListener} Test
 *
 * @since 2.7.4
 */
public class ServiceNameMappingListenerTest {

    private ServiceConfig<DemoServiceImpl> service = new ServiceConfig<DemoServiceImpl>();

    @BeforeEach
    public void init() {

        ApplicationConfig applicationConfig = new ApplicationConfig("app");

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setName("mockprotocol");

        RegistryConfig registry = new RegistryConfig();
        registry.setProtocol("mockprotocol");
        registry.setAddress("N/A");

        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());
        service.setApplication(applicationConfig);
        service.setProtocol(protocolConfig);
        service.setRegistry(registry);
    }

    @Test
    public void testOnEvent() {

        service.export();

    }
}
