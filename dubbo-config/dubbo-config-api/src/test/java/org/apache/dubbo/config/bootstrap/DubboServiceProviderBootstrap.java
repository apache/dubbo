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
package org.apache.dubbo.config.bootstrap;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.rest.UserService;
import org.apache.dubbo.config.bootstrap.rest.UserServiceImpl;

import java.util.Arrays;

/**
 * Dubbo Provider Bootstrap
 *
 * @since 2.7.5
 */
public class DubboServiceProviderBootstrap {

    public static void main(String[] args) {
        multipleRegistries();
    }

    private static void multipleRegistries() {
        ProtocolConfig restProtocol = new ProtocolConfig();
        restProtocol.setName("rest");
        restProtocol.setId("rest");
        restProtocol.setPort(-1);

        RegistryConfig interfaceRegistry = new RegistryConfig();
        interfaceRegistry.setId("interfaceRegistry");
        interfaceRegistry.setAddress("zookeeper://127.0.0.1:2181");

        RegistryConfig serviceRegistry = new RegistryConfig();
        serviceRegistry.setId("serviceRegistry");
        serviceRegistry.setAddress("zookeeper://127.0.0.1:2181?registry-type=service");

        ServiceConfig<EchoService> echoService = new ServiceConfig<>();
        echoService.setInterface(EchoService.class.getName());
        echoService.setRef(new EchoServiceImpl());
//        echoService.setRegistries(Arrays.asList(interfaceRegistry, serviceRegistry));

        ServiceConfig<UserService> userService = new ServiceConfig<>();
        userService.setInterface(UserService.class.getName());
        userService.setRef(new UserServiceImpl());
        userService.setProtocol(restProtocol);
//        userService.setRegistries(Arrays.asList(interfaceRegistry, serviceRegistry));

        ApplicationConfig applicationConfig = new ApplicationConfig("dubbo-provider-demo");
        applicationConfig.setMetadataType("remote");
        DubboBootstrap.getInstance()
                .application(applicationConfig)
                // Zookeeper in service registry type
//                .registry("zookeeper", builder -> builder.address("zookeeper://127.0.0.1:2181?registry.type=service"))
                // Nacos
//                .registry("zookeeper", builder -> builder.address("nacos://127.0.0.1:8848?registry.type=service"))
                .registries(Arrays.asList(interfaceRegistry, serviceRegistry))
//                .registry(RegistryBuilder.newBuilder().address("consul://127.0.0.1:8500?registry.type=service").build())
                .protocol(builder -> builder.port(-1).name("dubbo"))
                .metadataReport(new MetadataReportConfig("zookeeper://127.0.0.1:2181"))
                .service(echoService)
                .service(userService)
                .start()
                .await();
    }

    private static void testSCCallDubbo() {

    }

    private static void testDubboCallSC() {

    }

    private static void testDubboTansormation() {

    }

}
