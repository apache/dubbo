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

package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.protocol.tri.support.IGreeter;
import org.apache.dubbo.rpc.protocol.tri.support.IGreeterImpl;
import org.apache.dubbo.rpc.protocol.tri.support.MockStreamObserver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.rpc.protocol.tri.support.IGreeter.SERVER_MSG;


class TripleProtocolTest {

    @Test
    void testDemoProtocol() throws Exception {
        IGreeter serviceImpl = new IGreeterImpl();

        int availablePort = NetUtils.getAvailablePort();
        ApplicationModel applicationModel = ApplicationModel.defaultModel();

        URL providerUrl = URL.valueOf(
            "tri://127.0.0.1:" + availablePort + "/" + IGreeter.class.getName());

        ModuleServiceRepository serviceRepository = applicationModel.getDefaultModule()
            .getServiceRepository();
        ServiceDescriptor serviceDescriptor = serviceRepository.registerService(IGreeter.class);

        ProviderModel providerModel = new ProviderModel(
            providerUrl.getServiceKey(),
            serviceImpl,
            serviceDescriptor,
            new ServiceMetadata(), ClassUtils.getClassLoader(IGreeter.class));
        serviceRepository.registerProvider(providerModel);
        providerUrl = providerUrl.setServiceModel(providerModel);

        Protocol protocol = new TripleProtocol(providerUrl.getOrDefaultFrameworkModel());
        ProxyFactory proxy = applicationModel.getExtensionLoader(ProxyFactory.class)
            .getAdaptiveExtension();
        Invoker<IGreeter> invoker = proxy.getInvoker(serviceImpl, IGreeter.class, providerUrl);
        Exporter<IGreeter> export = protocol.export(invoker);

        URL consumerUrl = URL.valueOf(
            "tri://127.0.0.1:" + availablePort + "/" + IGreeter.class.getName());

        ConsumerModel consumerModel = new ConsumerModel(consumerUrl.getServiceKey(), null,
            serviceDescriptor, null,
            null, null);
        consumerUrl = consumerUrl.setServiceModel(consumerModel);
        IGreeter greeterProxy = proxy.getProxy(protocol.refer(IGreeter.class, consumerUrl));
        Thread.sleep(1000);

        // 1. test unaryStream
        String REQUEST_MSG = "hello world";
        Assertions.assertEquals(REQUEST_MSG, greeterProxy.echo(REQUEST_MSG));
        Assertions.assertEquals(REQUEST_MSG, serviceImpl.echoAsync(REQUEST_MSG).get());

        // 2. test serverStream
        MockStreamObserver outboundMessageSubscriber1 = new MockStreamObserver();
        greeterProxy.serverStream(REQUEST_MSG, outboundMessageSubscriber1);
        outboundMessageSubscriber1.getLatch().await(3000, TimeUnit.MILLISECONDS);
        Assertions.assertEquals(outboundMessageSubscriber1.getOnNextData(), REQUEST_MSG);
        Assertions.assertTrue(outboundMessageSubscriber1.isOnCompleted());

        // 3. test bidirectionalStream
        MockStreamObserver outboundMessageSubscriber2 = new MockStreamObserver();
        StreamObserver<String> inboundMessageObserver = greeterProxy.bidirectionalStream(
            outboundMessageSubscriber2);
        inboundMessageObserver.onNext(REQUEST_MSG);
        inboundMessageObserver.onCompleted();
        outboundMessageSubscriber2.getLatch().await(3000, TimeUnit.MILLISECONDS);
        // verify client
        Assertions.assertEquals(outboundMessageSubscriber2.getOnNextData(), SERVER_MSG);
        Assertions.assertTrue(outboundMessageSubscriber2.isOnCompleted());
        // verify server
        MockStreamObserver serverOutboundMessageSubscriber = (MockStreamObserver) ((IGreeterImpl) serviceImpl).getMockStreamObserver();
        serverOutboundMessageSubscriber.getLatch().await(1000, TimeUnit.MILLISECONDS);
        Assertions.assertEquals(REQUEST_MSG, serverOutboundMessageSubscriber.getOnNextData());
        Assertions.assertTrue(serverOutboundMessageSubscriber.isOnCompleted());

        export.unexport();
        protocol.destroy();
        // resource recycle.
        serviceRepository.destroy();
        System.out.println("serviceRepository destroyed");
    }
}
