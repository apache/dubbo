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
package org.apache.dubbo.qos.command.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.command.support.DemoService;
import org.apache.dubbo.qos.command.support.impl.DemoServiceImpl;
import org.apache.dubbo.rpc.model.ServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ReadyTest {

    private static Ready ready;
    private static CommandContext commandContext;

    @BeforeAll
    public static void setUp(){
        ready = new Ready();
        commandContext = Mockito.mock(CommandContext.class);
    }

    @AfterAll
    public static void clear(){
        DubboBootstrap.reset();
    }

    @Test
    public void appReadyTest() {
        DubboBootstrap.getInstance().setReady(false);
        String msgUnready = ready.execute(commandContext, new String[]{});
        Assertions.assertEquals(msgUnready, "false");

        DubboBootstrap.getInstance().setReady(true);
        String msgReady = ready.execute(commandContext, new String[]{});
        Assertions.assertEquals(msgReady, "true");
    }

    @Test
    public void serviceReadyTest(){

        String serviceName = DemoService.class.getName();
        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setApplication(new ApplicationConfig("app"));
        serviceConfig.setRegistry(new RegistryConfig("127.0.0.1:2181"));

        ServiceRepository repository = ApplicationModel.getServiceRepository();
        ServiceDescriptor serviceDescriptor = repository.registerService(DemoService.class);

        repository.registerProvider(
                URL.buildKey(serviceName, "",""),
                new DemoServiceImpl(),
                serviceDescriptor,
                serviceConfig,
                new ServiceMetadata()
        );


        ApplicationModel.allProviderModels().forEach(providerModel -> providerModel.addStatedUrl(new ProviderModel.RegisterStatedURL(
                URL.valueOf("test://127.0.0.1/test"),
                URL.valueOf( "127.0.0.1:2181"),
                true)));
        ready = new Ready();
        String msgTrue = ready.execute(commandContext, new String[]{serviceName});
        Assertions.assertTrue(msgTrue.contains("TRUE"));
        /**
         * msgTrue is:
         +------------------------------------------------+------+
         |              Provider Service Name             |STATUS|
         +------------------------------------------------+------+
         |org.apache.dubbo.qos.command.support.DemoService| TRUE |
         +------------------------------------------------+------+
         */


        ApplicationModel.allProviderModels().forEach(providerModel -> providerModel.getStatedUrl().clear());
        String msgFalse = ready.execute(commandContext, new String[]{serviceName});
        Assertions.assertTrue(msgFalse.contains("FALSE"));
        /**
         * msgFalse is:
         +------------------------------------------------+------+
         |              Provider Service Name             |STATUS|
         +------------------------------------------------+------+
         |org.apache.dubbo.qos.command.support.DemoService| FALSE|
         +------------------------------------------------+------+
         */
    }
}
