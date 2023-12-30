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
package org.apache.dubbo.rpc.protocol.rest;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.*;
import org.apache.dubbo.rpc.protocol.rest.rest.RestDemoService;
import org.apache.dubbo.rpc.protocol.rest.rest.RestDemoServiceImpl;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.remoting.Constants.SERVER_KEY;

public class ResteasyResponseTest {

    private Protocol protocol =
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension("rest");
    private ProxyFactory proxy =
            ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    private final int availablePort = NetUtils.getAvailablePort();
    private final URL exportUrl = URL.valueOf("rest://127.0.0.1:" + availablePort
            + "/rest?interface=org.apache.dubbo.rpc.protocol.rest.rest.RestDemoService");
    private final ModuleServiceRepository repository =
            ApplicationModel.defaultModel().getDefaultModule().getServiceRepository();

    @AfterEach
    public void tearDown() {
        protocol.destroy();
        FrameworkModel.destroyAll();
    }

    @Test
    void testResponse() {
        RestDemoService server = new RestDemoServiceImpl();
        URL url = this.registerProvider(exportUrl, server, RestDemoService.class);

        URL nettyUrl = url.addParameter(SERVER_KEY, "netty").addParameter("timeout", 3000000);

        protocol.export(proxy.getInvoker(new RestDemoServiceImpl(), RestDemoService.class, nettyUrl));

        RestDemoService demoService = this.proxy.getProxy(protocol.refer(RestDemoService.class, nettyUrl));

        Response response = demoService.findUserById(10);

        Assertions.assertNotNull(response);
    }

    @Test
    void testResponseCustomStatusCode() {
        RestDemoService server = new RestDemoServiceImpl();
        URL url = this.registerProvider(exportUrl, server, RestDemoService.class);

        URL nettyUrl = url.addParameter(SERVER_KEY, "netty").addParameter("timeout", 3000000);

        protocol.export(proxy.getInvoker(new RestDemoServiceImpl(), RestDemoService.class, nettyUrl));

        RestDemoService demoService = this.proxy.getProxy(protocol.refer(RestDemoService.class, nettyUrl));

        Response response = demoService.deleteUserById("uid");

        Assertions.assertEquals(response.getStatus(), 200);
    }

    private URL registerProvider(URL url, Object impl, Class<?> interfaceClass) {
        ServiceDescriptor serviceDescriptor = repository.registerService(interfaceClass);
        ProviderModel providerModel = new ProviderModel(url.getServiceKey(), impl, serviceDescriptor, null, null);
        repository.registerProvider(providerModel);
        return url.setServiceModel(providerModel);
    }
}
