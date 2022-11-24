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

package org.apache.dubbo.rpc.protocol.tri.call;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor.RpcType;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ReflectionMethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.DescriptorService;
import org.apache.dubbo.rpc.protocol.tri.HelloReply;
import org.apache.dubbo.rpc.protocol.tri.stream.TripleServerStream;
import org.apache.dubbo.rpc.protocol.tri.TripleFlowControlFrame;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ReflectionServerCallTest {

    @Test
    void doStartCall() throws NoSuchMethodException {
        Invoker<?> invoker = Mockito.mock(Invoker.class);
        TripleServerStream serverStream = Mockito.mock(TripleServerStream.class);
        ProviderModel providerModel = Mockito.mock(ProviderModel.class);
        ReflectionMethodDescriptor methodDescriptor = Mockito.mock(
            ReflectionMethodDescriptor.class);
        URL url = Mockito.mock(URL.class);
        when(invoker.getUrl())
            .thenReturn(url);
        when(url.getServiceModel())
            .thenReturn(providerModel);

        String service = "testService";
        String methodName = "method";
        try {
            ReflectionAbstractServerCall call = new ReflectionAbstractServerCall(invoker, serverStream,
                new FrameworkModel(), "",
                service, methodName,
                Collections.emptyList(),
                ImmediateEventExecutor.INSTANCE);
            fail();
        } catch (Exception e) {
            // pass
        }

        ServiceDescriptor serviceDescriptor = Mockito.mock(ServiceDescriptor.class);
        when(providerModel.getServiceModel())
            .thenReturn(serviceDescriptor);
        when(serviceDescriptor.getMethods(anyString()))
            .thenReturn(Collections.singletonList(methodDescriptor));
        when(methodDescriptor.getRpcType())
            .thenReturn(RpcType.UNARY);
        Method method = DescriptorService.class.getMethod("sayHello", HelloReply.class);
        when(methodDescriptor.getMethod())
            .thenReturn(method);
        when(methodDescriptor.getParameterClasses())
            .thenReturn(method.getParameterTypes());
        when(methodDescriptor.getReturnClass())
            .thenAnswer(invocation -> method.getReturnType());

        ReflectionAbstractServerCall call2 = new ReflectionAbstractServerCall(invoker, serverStream,
            new FrameworkModel(), "",
            service, methodName,
            Collections.emptyList(),
            ImmediateEventExecutor.INSTANCE);
        call2.onHeader(Collections.emptyMap());
        TripleFlowControlFrame tripleFlowControlFrame = new TripleFlowControlFrame(new byte[0],null,null);
        call2.onMessage(tripleFlowControlFrame);
        call2.onComplete();
    }
}
