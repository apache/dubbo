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

package org.apache.dubbo.rpc.protocol.grpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.protocol.grpc.support.DubboGreeterGrpc;
import org.apache.dubbo.rpc.protocol.grpc.support.GrpcGreeterImpl;
import org.apache.dubbo.rpc.protocol.grpc.support.HelloReply;
import org.apache.dubbo.rpc.protocol.grpc.support.HelloRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GrpcProtocolTest {
    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    @Test
    public void testDemoProtocol() throws Exception {
        DubboGreeterGrpc.IGreeter serviceImpl = new GrpcGreeterImpl();

        int availablePort = NetUtils.getAvailablePort();

        URL url = URL.valueOf("grpc://127.0.0.1:" + availablePort + "/" + DubboGreeterGrpc.IGreeter.class.getName());

        ServiceDescriptor serviceDescriptor = ApplicationModel.getServiceRepository().registerService(DubboGreeterGrpc.IGreeter.class);
        ApplicationModel.getServiceRepository().registerProvider(
            url.getServiceKey(),
            serviceImpl,
            serviceDescriptor,
            null,
            new ServiceMetadata()
        );


        MockReferenceConfig mockReferenceConfig = new MockReferenceConfig();
        mockReferenceConfig.setInterface(DubboGreeterGrpc.IGreeter.class);

        ServiceMetadata serviceMetadata = new ServiceMetadata();
        serviceMetadata.setServiceKey(URL.buildKey(DubboGreeterGrpc.IGreeter.class.getName(), null, null));

        ApplicationModel.getServiceRepository().registerConsumer(
            url.getServiceKey(),
            serviceDescriptor,
            mockReferenceConfig,
            null,
            serviceMetadata
        );

        protocol.export(proxy.getInvoker(serviceImpl, DubboGreeterGrpc.IGreeter.class, url));
        serviceImpl = proxy.getProxy(protocol.refer(DubboGreeterGrpc.IGreeter.class, url));

        HelloReply hello = serviceImpl.sayHello(HelloRequest.newBuilder().setName("World").build());
        Assertions.assertEquals("Hello World", hello.getMessage());

        // resource recycle.
        ApplicationModel.getServiceRepository().destroy();
    }

    class MockReferenceConfig extends ReferenceConfigBase {

        @Override
        public Object get() {
            return null;
        }

        @Override
        public void destroy() {

        }
    }
}
