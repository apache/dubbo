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
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.StubMethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.stream.TripleServerStream;

import io.netty.util.concurrent.ImmediateEventExecutor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class StubServerCallTest {

    @Test
    void doStartCall() throws IOException, ClassNotFoundException {
        Invoker<?> invoker = Mockito.mock(Invoker.class);
        TripleServerStream tripleServerStream = Mockito.mock(TripleServerStream.class);
        ProviderModel providerModel = Mockito.mock(ProviderModel.class);
        ServiceDescriptor serviceDescriptor = Mockito.mock(ServiceDescriptor.class);
        StubMethodDescriptor methodDescriptor = Mockito.mock(StubMethodDescriptor.class);
        URL url = Mockito.mock(URL.class);
        when(invoker.getUrl())
            .thenReturn(url);
        when(url.getServiceModel())
            .thenReturn(providerModel);
        when(providerModel.getServiceModel())
            .thenReturn(serviceDescriptor);
        when(serviceDescriptor.getMethods(anyString()))
            .thenReturn(Collections.singletonList(methodDescriptor));
        when(methodDescriptor.getRpcType())
            .thenReturn(RpcType.UNARY);
        when(methodDescriptor.parseRequest(any(byte[].class)))
            .thenReturn("test");
        String service = "testService";
        String method = "method";
        StubAbstractServerCall call = new StubAbstractServerCall(invoker, tripleServerStream,
            new FrameworkModel(), "",
            service, method,
            ImmediateEventExecutor.INSTANCE);
        call.onHeader(Collections.emptyMap());
        call.onMessage(new byte[0], false);
        call.onComplete();
    }
}
