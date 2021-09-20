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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.RpcServiceContext;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.protocol.tri.support.IGreeter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ClientStreamTest {
    private Protocol protocol = ApplicationModel.defaultModel().getAdaptiveExtension(Protocol.class);
    private ProxyFactory proxy = ApplicationModel.defaultModel().getAdaptiveExtension(ProxyFactory.class);

    @Test
    @SuppressWarnings("all")
    public void testInit() throws InterruptedException {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        IGreeter serviceImpl = new IGreeter() {

            @Override
            public String echo(String request) {
                return request;
            }

            @Override
            public void serverStream(String str,
                                     StreamObserver<String> observer) {
                observer.onNext(str);
                executorService.schedule(()-> serverStream(str, observer), 1, TimeUnit.SECONDS);
            }
        };

        int availablePort = NetUtils.getAvailablePort();

        URL url = URL.valueOf("tri://127.0.0.1:" + availablePort + "/" + IGreeter.class.getName());

        ModuleServiceRepository
            serviceRepository = ApplicationModel.defaultModel().getDefaultModule().getServiceRepository();
        ServiceDescriptor serviceDescriptor = serviceRepository.registerService(IGreeter.class);

        ProviderModel providerModel = new ProviderModel(
            url.getServiceKey(),
            serviceImpl,
            serviceDescriptor,
            null,
            new ServiceMetadata());
        serviceRepository.registerProvider(providerModel);
        url = url.setServiceModel(providerModel);

        protocol.export(proxy.getInvoker(serviceImpl, IGreeter.class, url));

        ConsumerModel
            consumerModel = new ConsumerModel(url.getServiceKey(), null, serviceDescriptor, null,
            new ServiceMetadata(), null);
        url = url.setServiceModel(consumerModel);
        serviceImpl = proxy.getProxy(protocol.refer(IGreeter.class, url));
        StreamObserver<String> streamObserver = Mockito.mock(StreamObserver.class);
        serviceImpl.serverStream("hello world", streamObserver);
        TimeUnit.SECONDS.sleep(1);
        // client stream cancel call
        RpcContext.getServiceContext().getCancellableContext().cancel(streamObserver);

        TimeUnit.SECONDS.sleep(1);
        Mockito.verify(streamObserver, Mockito.times(1)).onNext(any());

        TimeUnit.SECONDS.sleep(1);
        // resource recycle.
        serviceRepository.destroy();
    }

}
