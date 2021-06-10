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
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UnaryClientStreamTest {
    @Test
    @SuppressWarnings("all")
    public void testInit() {
        URL url = new ServiceConfigURL("test", "1.2.3.4", 8080);
        final Executor executor = Mockito.mock(Executor.class);
        final UnaryClientStream stream = UnaryClientStream.unary(url, executor);
        final StreamObserver<Object> observer = stream.asStreamObserver();
        RpcInvocation inv = Mockito.mock(RpcInvocation.class);
        // no invoker
        Assertions.assertThrows(NullPointerException.class, () -> observer.onNext(inv));
        final Invoker mockInvoker = Mockito.mock(Invoker.class);
        when(mockInvoker.getUrl()).thenReturn(url);
        when(inv.getInvoker()).thenReturn(mockInvoker);
        // no subscriber
        Assertions.assertThrows(NullPointerException.class, () -> observer.onNext(inv));
        verify(mockInvoker, times(2)).getUrl();

        TransportObserver transportObserver = Mockito.mock(TransportObserver.class);
        stream.subscribe(transportObserver);
        // no method descriptor
        Assertions.assertThrows(NullPointerException.class, () -> observer.onNext(inv));
        Mockito.verify(transportObserver).tryOnMetadata(any(), anyBoolean());

        MethodDescriptor md = Mockito.mock(MethodDescriptor.class);
        when(md.isNeedWrap()).thenReturn(true);
        when(md.isStream()).thenReturn(false);
        stream.method(md);
        // bad request
        Assertions.assertThrows(NullPointerException.class, () -> observer.onNext(inv));

        String[] params = new String[]{"rainbow ponies!"};
        when(inv.getArguments()).thenReturn(params);
        // no serialization
        Assertions.assertThrows(NullPointerException.class, () -> observer.onNext(inv));
//        Map<String,Object> attachemnts=new HashMap<>();
//        when(inv.getObjectAttachments()).thenReturn(attachemnts);
//        attachemnts.put(Constants.SERIALIZATION_KEY, "hessian2");
//        observer.onNext(inv);
    }

}
