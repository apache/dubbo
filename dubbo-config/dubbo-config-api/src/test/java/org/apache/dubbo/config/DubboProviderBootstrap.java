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
package org.apache.dubbo.config;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.metadata.ConfigurableMetadataServiceExporter;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;
import org.apache.dubbo.metadata.MetadataServiceExporter;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceDiscoveryFactory;

/**
 * Dubbo Provider Bootstrap
 */
public class DubboProviderBootstrap {

    public static void main(String[] args) throws Exception {

        ApplicationConfig application = new ApplicationConfig();
        application.setName("dubbo-provider-demo");

        URL connectionURL = URL.valueOf("zookeeper://127.0.0.1:2181?registry.type=service");

        // 连接注册中心配置
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress(connectionURL.toString());

        // 服务提供者协议配置
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName("dubbo");
        protocol.setPort(NetUtils.getAvailablePort());

        DemoService demoService = new DemoServiceImpl();

        ServiceConfig<DemoService> service = new ServiceConfig<>();
        service.setApplication(application);
        service.setRegistry(registry); // 多个注册中心可以用setRegistries()
        service.setProtocol(protocol); // 多个协议可以用setProtocols()
        service.setInterface(DemoService.class);
        service.setRef(demoService);
        service.setVersion("1.0.0");

        // 暴露及注册服务
        service.export();

        MetadataServiceExporter exporter = new ConfigurableMetadataServiceExporter();

        // 暴露 MetadataService 服务
        exporter.export();

        ServiceDiscoveryFactory factory = ServiceDiscoveryFactory.getExtension(connectionURL);

        ServiceDiscovery serviceDiscovery = factory.getServiceDiscovery(connectionURL);

        serviceDiscovery.initialize(connectionURL);

        DefaultServiceInstance serviceInstance = new DefaultServiceInstance(application.getName(), "127.0.0.1", protocol.getPort());

        serviceDiscovery.register(serviceInstance);

        System.in.read();

        serviceDiscovery.destroy();
    }
}
