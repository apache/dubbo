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
package org.apache.dubbo.remoting.http.rest;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class TestRestClient {
    private final Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension("rest");
    private final ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    private final ModuleServiceRepository repository = ApplicationModel.defaultModel().getDefaultModule().getServiceRepository();


    @Test
    void testAnotherUserRestProtocolByDifferentRestClient() {
        testAnotherUserRestProtocol(org.apache.dubbo.remoting.Constants.OK_HTTP);
        testAnotherUserRestProtocol(org.apache.dubbo.remoting.Constants.APACHE_HTTP_CLIENT);
        testAnotherUserRestProtocol(org.apache.dubbo.remoting.Constants.URL_CONNECTION);
    }

    void testAnotherUserRestProtocol(String restClient) {
        URL url = URL.valueOf("rest://127.0.0.1:" + NetUtils.getAvailablePort()
            + "/?version=1.0.0&interface=org.apache.dubbo.remoting.http.rest.AnotherUserRestService&"
            + org.apache.dubbo.remoting.Constants.CLIENT_KEY + "=" + restClient);

        AnotherUserRestServiceImpl server = new AnotherUserRestServiceImpl();

        url = this.registerProvider(url, server, AnotherUserRestService.class);

        Exporter<AnotherUserRestService> exporter = protocol.export(proxy.getInvoker(server, AnotherUserRestService.class, url));
        Invoker<AnotherUserRestService> invoker = protocol.refer(AnotherUserRestService.class, url);


        AnotherUserRestService client = proxy.getProxy(invoker);
        User result = client.getUser(123l);

        Assertions.assertEquals(123l, result.getId());

        Assertions.assertEquals("context", client.getContext());

        byte[] bytes = {1, 2, 3, 4};
        Assertions.assertTrue(Arrays.equals(bytes, client.bytes(bytes)));

        Assertions.assertEquals(1l, client.number(1l));


        invoker.destroy();
        exporter.unexport();
    }

    private URL registerProvider(URL url, Object impl, Class<?> interfaceClass) {
        ServiceDescriptor serviceDescriptor = repository.registerService(interfaceClass);
        ProviderModel providerModel = new ProviderModel(
            url.getServiceKey(),
            impl,
            serviceDescriptor,
            null,
            null);
        repository.registerProvider(providerModel);
        return url.setServiceModel(providerModel);
    }
}
