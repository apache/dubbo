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
package org.apache.dubbo.rpc.stub;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.support.IGreeter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class StubInvocationUtilTest {
    private Invoker<IGreeter> invoker;
    private MethodDescriptor method;
    private String request = "request";
    private String response = "response";
    private Result result;

    private Invoker<IGreeter> createMockInvoker(URL url) {
        Invoker<IGreeter> invoker = Mockito.mock(Invoker.class);
        when(invoker.getUrl()).thenReturn(url);
        when(invoker.getInterface()).thenReturn(IGreeter.class);
        return invoker;
    }

    private URL createMockURL(ConsumerModel consumerModel) {
        URL url = Mockito.mock(URL.class);
        when(url.getServiceModel()).thenReturn(consumerModel);
        when(url.getServiceInterface()).thenReturn(IGreeter.class.getName());
        when(url.getProtocolServiceKey()).thenReturn(IGreeter.class.getName());
        return url;
    }

    private ConsumerModel createMockConsumerModel(ServiceDescriptor serviceDescriptor) {
        ConsumerModel consumerModel = Mockito.mock(ConsumerModel.class);
        when(consumerModel.getServiceModel()).thenReturn(serviceDescriptor);
        return consumerModel;
    }

    private Result createMockResult(String response) throws Throwable {
        Result result = Mockito.mock(Result.class);
        when(result.recreate()).thenReturn(response);
        return result;
    }

    private MethodDescriptor createMockMethodDescriptor() {
        MethodDescriptor method = Mockito.mock(MethodDescriptor.class);
        when(method.getParameterClasses()).thenReturn(new Class[] {String.class});
        when(method.getMethodName()).thenReturn("sayHello");
        return method;
    }

    @BeforeEach
    void init() throws Throwable {
        ServiceDescriptor serviceDescriptor = Mockito.mock(ServiceDescriptor.class);
        ConsumerModel consumerModel = createMockConsumerModel(serviceDescriptor);
        URL url = createMockURL(consumerModel);
        invoker = createMockInvoker(url);
        result = createMockResult("response");
        method = createMockMethodDescriptor();
    }

    @Test
    void unaryCall() {
        when(invoker.invoke(any(Invocation.class))).thenReturn(result);
        Object ret = StubInvocationUtil.unaryCall(invoker, method, request);
        Assertions.assertEquals(response, ret);
    }

    @Test
    void unaryCall2() {
        when(invoker.invoke(any(Invocation.class)))
                .thenThrow(new RuntimeException("a"))
                .thenThrow(new Error("b"));
        try {
            StubInvocationUtil.unaryCall(invoker, method, request);
            fail();
        } catch (Throwable t) {
            // pass
        }
        try {
            StubInvocationUtil.unaryCall(invoker, method, request);
            fail();
        } catch (Throwable t) {
            // pass
        }
    }

    @Test
    void testUnaryCall() throws Throwable {
        when(invoker.invoke(any(Invocation.class))).then(invocationOnMock -> result);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> atomicReference = new AtomicReference<>();
        StreamObserver<Object> responseObserver = new StreamObserver<Object>() {
            @Override
            public void onNext(Object data) {
                atomicReference.set(data);
            }

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        };
        StubInvocationUtil.unaryCall(invoker, method, request, responseObserver);
        latch.await(1, TimeUnit.SECONDS);
        Assertions.assertEquals(response, atomicReference.get());
    }

    @Test
    void biOrClientStreamCall() throws InterruptedException {
        when(invoker.invoke(any(Invocation.class))).then(invocationOnMock -> {
            Invocation invocation = (Invocation) invocationOnMock.getArguments()[0];
            StreamObserver<Object> observer =
                    (StreamObserver<Object>) invocation.getArguments()[0];
            observer.onNext(response);
            observer.onCompleted();
            when(result.recreate()).then(invocationOnMock1 -> new StreamObserver<Object>() {
                @Override
                public void onNext(Object data) {
                    observer.onNext(data);
                }

                @Override
                public void onError(Throwable throwable) {}

                @Override
                public void onCompleted() {
                    observer.onCompleted();
                }
            });
            return result;
        });
        CountDownLatch latch = new CountDownLatch(11);
        StreamObserver<Object> responseObserver = new StreamObserver<Object>() {
            @Override
            public void onNext(Object data) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        };
        StreamObserver<Object> observer = StubInvocationUtil.biOrClientStreamCall(invoker, method, responseObserver);
        for (int i = 0; i < 10; i++) {
            observer.onNext(request);
        }
        observer.onCompleted();
        Assertions.assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    void serverStreamCall() throws InterruptedException {
        when(invoker.invoke(any(Invocation.class))).then(invocationOnMock -> {
            Invocation invocation = (Invocation) invocationOnMock.getArguments()[0];
            StreamObserver<Object> observer =
                    (StreamObserver<Object>) invocation.getArguments()[1];
            for (int i = 0; i < 10; i++) {
                observer.onNext(response);
            }
            observer.onCompleted();
            return result;
        });
        CountDownLatch latch = new CountDownLatch(11);
        StreamObserver<Object> responseObserver = new StreamObserver<Object>() {
            @Override
            public void onNext(Object data) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        };
        StubInvocationUtil.serverStreamCall(invoker, method, request, responseObserver);
        Assertions.assertTrue(latch.await(1, TimeUnit.SECONDS));
    }
}
