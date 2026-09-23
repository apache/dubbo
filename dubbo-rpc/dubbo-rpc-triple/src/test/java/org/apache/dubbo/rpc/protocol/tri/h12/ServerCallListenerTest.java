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
import org.apache.dubbo.remoting.http12.FlowControlStreamObserver;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.exception.HttpRequestTimeout;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.tri.TripleConstants;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServerCallListenerTest {

    @Test
    void cancellationDoesNotCloseRequestImmediately() {
        Invoker<?> invoker = mock(Invoker.class);

        HttpRequest unaryRequest = mock(HttpRequest.class);
        new UnaryServerCallListener(invocation(unaryRequest), invoker, mock(StreamObserver.class)).onCancel(1);
        org.mockito.Mockito.verifyNoInteractions(unaryRequest);

        HttpRequest serverStreamRequest = mock(HttpRequest.class);
        StreamObserver<Object> serverStreamResponse = mock(StreamObserver.class);
        new ServerStreamServerCallListener(invocation(serverStreamRequest), invoker, serverStreamResponse).onCancel(1);
        org.mockito.Mockito.verifyNoInteractions(serverStreamRequest);
        verify(serverStreamResponse).onError(any(HttpStatusException.class));

        HttpRequest biStreamRequest = mock(HttpRequest.class);
        FlowControlStreamObserver<Object> biStreamResponse = mock(FlowControlStreamObserver.class);
        StreamObserver<Object> biStreamRequestObserver = mock(StreamObserver.class);
        RpcInvocation biStreamInvocation = invocation(biStreamRequest);
        when(invoker.invoke(biStreamInvocation))
                .thenReturn(AsyncRpcResult.newDefaultAsyncResult(biStreamRequestObserver, biStreamInvocation));
        new BiStreamServerCallListener(biStreamInvocation, invoker, biStreamResponse).onCancel(1);
        org.mockito.Mockito.verifyNoInteractions(biStreamRequest);
        verify(biStreamRequestObserver).onError(any(HttpStatusException.class));
    }

    @Test
    void closeRequestWhenInvocationReturnsException() {
        HttpRequest request = mock(HttpRequest.class);
        RpcInvocation invocation = invocation(request);
        Invoker<?> invoker = mock(Invoker.class);
        when(invoker.invoke(invocation))
                .thenReturn(AsyncRpcResult.newDefaultAsyncResult(new RuntimeException("test"), invocation));
        StreamObserver<Object> responseObserver = mock(StreamObserver.class);

        new UnaryServerCallListener(invocation, invoker, responseObserver).onComplete();

        verify(responseObserver).onError(any(RuntimeException.class));
    }

    @Test
    void closeRequestWhenInvocationThrowsException() {
        HttpRequest request = mock(HttpRequest.class);
        RpcInvocation invocation = invocation(request);
        Invoker<?> invoker = mock(Invoker.class);
        when(invoker.invoke(invocation)).thenThrow(new RuntimeException("test"));
        StreamObserver<Object> responseObserver = mock(StreamObserver.class);

        new UnaryServerCallListener(invocation, invoker, responseObserver).onComplete();

        verify(responseObserver).onError(any(RuntimeException.class));
    }

    @Test
    void closeRequestWhenAsyncInvocationFails() {
        HttpRequest request = mock(HttpRequest.class);
        RpcInvocation invocation = invocation(request);
        Invoker<?> invoker = mock(Invoker.class);
        CompletableFuture<AppResponse> future = new CompletableFuture<>();
        when(invoker.invoke(invocation)).thenReturn(new AsyncRpcResult(future, invocation));
        StreamObserver<Object> responseObserver = mock(StreamObserver.class);

        new UnaryServerCallListener(invocation, invoker, responseObserver).onComplete();
        future.completeExceptionally(new RuntimeException("test"));

        verify(responseObserver).onError(any(RuntimeException.class));
    }

    @Test
    void closeRequestWhenAsyncInvocationReturnsException() {
        HttpRequest request = mock(HttpRequest.class);
        RpcInvocation invocation = invocation(request);
        Invoker<?> invoker = mock(Invoker.class);
        CompletableFuture<AppResponse> future = new CompletableFuture<>();
        when(invoker.invoke(invocation)).thenReturn(new AsyncRpcResult(future, invocation));
        StreamObserver<Object> responseObserver = mock(StreamObserver.class);

        new UnaryServerCallListener(invocation, invoker, responseObserver).onComplete();
        AppResponse response = new AppResponse(invocation);
        response.setException(new RuntimeException("test"));
        future.complete(response);

        verify(responseObserver).onError(any(RuntimeException.class));
    }

    @Test
    void closeRequestWhenInvocationTimesOut() {
        HttpRequest request = mock(HttpRequest.class);
        RpcInvocation invocation = invocation(request);
        invocation.put("timeout", -1L);
        Invoker<?> invoker = mock(Invoker.class);
        CompletableFuture<AppResponse> future = new CompletableFuture<>();
        when(invoker.invoke(invocation)).thenReturn(new AsyncRpcResult(future, invocation));
        StreamObserver<Object> responseObserver = mock(StreamObserver.class);

        new UnaryServerCallListener(invocation, invoker, responseObserver).onComplete();
        future.complete(new AppResponse(invocation));

        verify(responseObserver).onError(any(HttpRequestTimeout.class));
    }

    private static RpcInvocation invocation(HttpRequest request) {
        RpcInvocation invocation = new RpcInvocation();
        invocation.put(TripleConstants.HTTP_REQUEST_KEY, request);
        return invocation;
    }
}
