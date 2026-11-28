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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.nested.TripleConfig;
import org.apache.dubbo.rpc.Constants;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that a single message larger than the HTTP/2 stream-level flow control window can
 * still be delivered: the consumer must return WINDOW_UPDATE for the partially received
 * message, otherwise the peer stalls once the window is exhausted and both sides wait on
 * each other until the call times out.
 */
class TripleFlowControlTest {

    /**
     * The gRPC response size reported in issue #16427: about 92 KiB, far above the 64 KiB
     * stream window configured below and far below the default 8 MiB max response body size.
     */
    private static final int PAYLOAD_SIZE = 94_486;

    private static final int INITIAL_STREAM_WINDOW_SIZE = 64 * 1024 - 1;

    private ProtocolConfig protocolConfig;
    private Exporter<IGreeter> export;
    private IGreeter greeterProxy;

    @BeforeEach
    void setUp() throws Exception {
        IGreeterImpl serviceImpl = new IGreeterImpl();
        int availablePort = NetUtils.getAvailablePort();
        ApplicationModel applicationModel = ApplicationModel.defaultModel();

        // TripleHttp3ProtocolTest leaks H3_ENABLED=true into the global configuration and
        // the static Http3Exchanger state; force both back to the plain HTTP/2 path,
        // otherwise the window size configured below does not apply
        Map<String, String> settings = new HashMap<>();
        settings.put(Constants.H3_SETTINGS_HTTP3_ENABLED, "false");
        applicationModel.modelEnvironment().updateAppConfigMap(settings);
        applicationModel.getApplicationConfigManager().getSsl().ifPresent(ssl -> applicationModel
                .getApplicationConfigManager()
                .removeConfig(ssl));
        new TripleProtocol(applicationModel.getFrameworkModel());

        protocolConfig = new ProtocolConfig("tri");
        TripleConfig tripleConfig = new TripleConfig();
        tripleConfig.setInitialWindowSize(INITIAL_STREAM_WINDOW_SIZE);
        protocolConfig.setTriple(tripleConfig);
        applicationModel.getApplicationConfigManager().addProtocol(protocolConfig);

        URL providerUrl = URL.valueOf("tri://127.0.0.1:" + availablePort + "/" + IGreeter.class.getName());

        ModuleServiceRepository serviceRepository =
                applicationModel.getDefaultModule().getServiceRepository();
        ServiceDescriptor serviceDescriptor = serviceRepository.registerService(IGreeter.class);

        ProviderModel providerModel = new ProviderModel(
                providerUrl.getServiceKey(),
                serviceImpl,
                serviceDescriptor,
                new ServiceMetadata(),
                ClassUtils.getClassLoader(IGreeter.class));
        serviceRepository.registerProvider(providerModel);
        providerUrl = providerUrl.setServiceModel(providerModel);

        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension("tri");

        ProxyFactory proxy =
                applicationModel.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Invoker<IGreeter> invoker = proxy.getInvoker(serviceImpl, IGreeter.class, providerUrl);
        export = protocol.export(invoker);

        URL consumerUrl =
                URL.valueOf("tri://127.0.0.1:" + availablePort + "/" + IGreeter.class.getName() + "?timeout=5000");
        ConsumerModel consumerModel =
                new ConsumerModel(consumerUrl.getServiceKey(), null, serviceDescriptor, null, null, null);
        consumerUrl = consumerUrl.setServiceModel(consumerModel);
        greeterProxy = proxy.getProxy(protocol.refer(IGreeter.class, consumerUrl));
        Thread.sleep(1000);
    }

    @AfterEach
    void tearDown() {
        export.unexport();
        ExtensionLoader.getExtensionLoader(Protocol.class).getExtension("tri").destroy();
        ApplicationModel.defaultModel()
                .getDefaultModule()
                .getServiceRepository()
                .destroy();
        ApplicationModel.defaultModel().getApplicationConfigManager().removeConfig(protocolConfig);
    }

    @Test
    void testMessageLargerThanStreamWindow() throws Exception {
        String payload = buildPayload();

        // unary: the request and the response each exceed the window, exercising the
        // client-to-server and server-to-client directions in one call
        CompletableFuture<String> reply = CompletableFuture.supplyAsync(() -> greeterProxy.echo(payload));
        Assertions.assertEquals(payload, reply.get(10, TimeUnit.SECONDS));

        // server stream: a large message delivered to a streaming observer
        MockStreamObserver responseObserver = new MockStreamObserver();
        greeterProxy.serverStream(payload, responseObserver);
        Assertions.assertTrue(responseObserver.getLatch().await(10, TimeUnit.SECONDS), "onNext not delivered");
        Assertions.assertEquals(payload, responseObserver.getOnNextData());
        Assertions.assertTrue(responseObserver.isOnCompleted());
    }

    private static String buildPayload() {
        StringBuilder builder = new StringBuilder(PAYLOAD_SIZE);
        int chunk = 0;
        while (builder.length() < PAYLOAD_SIZE) {
            builder.append("flow-control-payload-").append(chunk++).append(';');
        }
        return builder.toString();
    }
}
