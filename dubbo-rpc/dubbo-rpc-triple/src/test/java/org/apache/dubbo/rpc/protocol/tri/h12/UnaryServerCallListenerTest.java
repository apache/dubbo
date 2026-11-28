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
package org.apache.dubbo.rpc.protocol.tri.h12;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.InvokeMode;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TriRpcStatus;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class UnaryServerCallListenerTest {
    @Test
    @SuppressWarnings("unchecked")
    void errorAttachmentsReachTransportForImmediateAndAsynchronousResults() {
        for (boolean asynchronous : new boolean[] {false, true}) {
            RpcInvocation invocation = new RpcInvocation();
            invocation.setInvokeMode(InvokeMode.ASYNC);
            CompletableFuture<AppResponse> future = new CompletableFuture<>();
            AppResponse response = new AppResponse(TriRpcStatus.UNAVAILABLE.asException());
            response.setAttachment("grpc-retry-pushback-ms", "800");
            if (!asynchronous) {
                future.complete(response);
            }
            Invoker<Object> invoker = mock(Invoker.class);
            when(invoker.invoke(invocation)).thenAnswer(ignored -> new AsyncRpcResult(future, invocation));
            StreamObserver<Object> observer =
                    mock(StreamObserver.class, withSettings().extraInterfaces(AttachmentHolder.class));
            try {
                new UnaryServerCallListener(invocation, invoker, observer).invoke();
                if (asynchronous) {
                    future.complete(response);
                }
                verify((AttachmentHolder) observer).setResponseAttachments(response.getObjectAttachments());
                verify(observer).onError(response.getException());
                verify(observer, never()).onNext(any());
            } finally {
                RpcContext.removeContext();
            }
        }
    }
}
