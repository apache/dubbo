///*
// * Licensed to the Apache Software Foundation (ASF) under one or more
// * contributor license agreements.  See the NOTICE file distributed with
// * this work for additional information regarding copyright ownership.
// * The ASF licenses this file to You under the Apache License, Version 2.0
// * (the "License"); you may not use this file except in compliance with
// * the License.  You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package org.apache.dubbo.qos.command.impl;
//
//import org.apache.dubbo.common.URL;
//import org.apache.dubbo.qos.command.CommandContext;
//import org.apache.dubbo.registry.Registry;
//import org.apache.dubbo.rpc.Invoker;
//import org.apache.dubbo.rpc.model.ApplicationModel;
//import org.apache.dubbo.rpc.model.ProviderModel;
//
//import org.junit.jupiter.api.Test;
//
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.equalTo;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//public class OnlineTest {
//    @Test
//    public void testExecute() throws Exception {
//        ProviderModel providerModel = mock(ProviderModel.class);
//        when(providerModel.getServiceKey()).thenReturn("org.apache.dubbo.BarService");
//        ApplicationModel.initProviderModel("org.apache.dubbo.BarService", providerModel);
//
//        Invoker providerInvoker = mock(Invoker.class);
//        URL registryUrl = mock(URL.class);
//        when(registryUrl.toFullString()).thenReturn("test://localhost:8080");
//        URL providerUrl = mock(URL.class);
//        when(providerUrl.getServiceKey()).thenReturn("org.apache.dubbo.BarService");
//        when(providerUrl.toFullString()).thenReturn("dubbo://localhost:8888/org.apache.dubbo.BarService");
//        when(providerInvoker.getUrl()).thenReturn(providerUrl);
//        ApplicationModel.registerProviderInvoker(providerInvoker, registryUrl, providerUrl);
//
//        Registry registry = mock(Registry.class);
//        TestRegistryFactory.registry = registry;
//
//        Online online = new Online();
//        String output = online.execute(mock(CommandContext.class), new String[]{"org.apache.dubbo.BarService"});
//        assertThat(output, equalTo("OK"));
//        for (ProviderInvokerWrapper wrapper : ApplicationModel.getProviderInvokers("org.apache.dubbo.BarService")) {
//            assertTrue(wrapper.isReg());
//        }
//    }
//}
