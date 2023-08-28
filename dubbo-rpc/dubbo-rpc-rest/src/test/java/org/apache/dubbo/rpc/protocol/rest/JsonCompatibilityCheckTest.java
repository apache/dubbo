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
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.rest.compatibility.RestDemoService;
import org.apache.dubbo.rpc.protocol.rest.compatibility.RestDemoServiceImpl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.rpc.protocol.rest.constans.RestConstant.JSON_CHECK_LEVEL;
import static org.apache.dubbo.rpc.protocol.rest.constans.RestConstant.JSON_CHECK_LEVEL_DISABLED;
import static org.apache.dubbo.rpc.protocol.rest.constans.RestConstant.JSON_CHECK_LEVEL_STRICT;
import static org.apache.dubbo.rpc.protocol.rest.constans.RestConstant.JSON_CHECK_LEVEL_WARN;


public class JsonCompatibilityCheckTest {

    private final int availablePort = NetUtils.getAvailablePort();
    private final URL exportUrl = URL.valueOf("rest://127.0.0.1:" + availablePort + "/rest?interface=org.apache.dubbo.rpc.protocol.rest.compatibility.RestDemoService");
    private final ModuleServiceRepository repository = ApplicationModel.defaultModel().getDefaultModule().getServiceRepository();

    private final Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension("rest");
    private final ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();


    @Test
    public void testJsonCheckDisabled() {

        RestDemoService server = new RestDemoServiceImpl();

        URL url = this.registerProvider(exportUrl, server, RestDemoService.class);

        url = url.addParameter(JSON_CHECK_LEVEL, JSON_CHECK_LEVEL_DISABLED);

        Exporter<RestDemoService> exporter = protocol.export(proxy.getInvoker(server, RestDemoService.class, url));

        exporter.unexport();
    }

    @Test
    public void testJsonCheckWarn() {
        RestDemoService server = new RestDemoServiceImpl();

        URL url = this.registerProvider(exportUrl, server, RestDemoService.class);

        url = url.addParameter(JSON_CHECK_LEVEL, JSON_CHECK_LEVEL_WARN);

        Exporter<RestDemoService> exporter = protocol.export(proxy.getInvoker(server, RestDemoService.class, url));

        exporter.unexport();
    }

    @Test
    public void testJsonCheckStrict() {
        RestDemoService server = new RestDemoServiceImpl();

        URL url = this.registerProvider(exportUrl, server, RestDemoService.class);

        URL newUrl = url.addParameter(JSON_CHECK_LEVEL, JSON_CHECK_LEVEL_STRICT);

        Assertions.assertThrowsExactly(RpcException.class, () -> {
            Exporter<RestDemoService> exporter = null;
            try {
                exporter = protocol.export(proxy.getInvoker(server, RestDemoService.class, newUrl));
            } finally {
                if (exporter != null) exporter.unexport();
            }
        });
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
