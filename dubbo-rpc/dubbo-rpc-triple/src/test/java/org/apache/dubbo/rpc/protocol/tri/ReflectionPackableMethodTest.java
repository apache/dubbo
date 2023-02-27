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

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ReflectionMethodDescriptor;

import io.reactivex.Single;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReflectionPackableMethodTest {


    @Test
    void testUnaryFuture() throws Exception {
        Method method = DescriptorService.class.getMethod("unaryFuture");
        MethodDescriptor descriptor = new ReflectionMethodDescriptor(method);
        assertEquals(CompletableFuture.class, descriptor.getReturnClass());
        assertEquals(String.class, descriptor.getReturnTypes()[0]);
    }

    @Test
    void testMethodWithNoParameters() throws Exception {
        Method method = DescriptorService.class.getMethod("noParameterMethod");
        MethodDescriptor descriptor = new ReflectionMethodDescriptor(method);
        assertEquals("", descriptor.getParamDesc());
        Assertions.assertEquals(0, descriptor.getParameterClasses().length);
    }

    @Test
    void testMethodWithNoParametersAndReturnProtobuf() throws Exception {
        Method method = DescriptorService.class.getMethod("noParameterAndReturnProtobufMethod");
        MethodDescriptor descriptor = new ReflectionMethodDescriptor(method);
        assertEquals("", descriptor.getParamDesc());
        Assertions.assertEquals(0, descriptor.getParameterClasses().length);
        assertFalse(needWrap(descriptor));
    }

    @Test
    void testMethodWithNoParametersAndReturnJava() throws Exception {
        Method method = DescriptorService.class.getMethod("noParameterAndReturnJavaClassMethod");
        MethodDescriptor descriptor = new ReflectionMethodDescriptor(method);
        assertEquals("", descriptor.getParamDesc());
        Assertions.assertEquals(0, descriptor.getParameterClasses().length);
        assertTrue(needWrap(descriptor));
    }

    @Test
    void testWrapperBiStream() throws Exception {
        Method method = DescriptorService.class.getMethod("wrapBidirectionalStream", StreamObserver.class);
        ReflectionMethodDescriptor descriptor = new ReflectionMethodDescriptor(method);
        Assertions.assertEquals(1, descriptor.getParameterClasses().length);
        assertEquals(MethodDescriptor.RpcType.BI_STREAM, descriptor.getRpcType());
        assertTrue(needWrap(descriptor));
    }

    @Test
    void testBiStream() throws Exception {
        Method method = DescriptorService.class.getMethod("bidirectionalStream", StreamObserver.class);
        ReflectionMethodDescriptor descriptor = new ReflectionMethodDescriptor(method);
        Assertions.assertEquals(1, descriptor.getParameterClasses().length);
        assertSame(descriptor.getRpcType(), MethodDescriptor.RpcType.BI_STREAM);
        assertFalse(needWrap(descriptor));
    }

    @Test
    void testIsStream() throws NoSuchMethodException {
        Method method = DescriptorService.class.getMethod("noParameterMethod");

        ReflectionMethodDescriptor md1 = new ReflectionMethodDescriptor(method);
        Assertions.assertEquals(MethodDescriptor.RpcType.UNARY, md1.getRpcType());

        method = DescriptorService.class.getMethod("sayHello", HelloReply.class);
        ReflectionMethodDescriptor md2 = new ReflectionMethodDescriptor(method);
        Assertions.assertEquals(MethodDescriptor.RpcType.UNARY, md2.getRpcType());
    }

    @Test
    void testIsUnary() throws NoSuchMethodException {
        Method method = DescriptorService.class.getMethod("noParameterMethod");
        MethodDescriptor descriptor = new ReflectionMethodDescriptor(method);
        Assertions.assertEquals(MethodDescriptor.RpcType.UNARY, descriptor.getRpcType());

        method = DescriptorService.class.getMethod("sayHello", HelloReply.class);
        ReflectionMethodDescriptor md2 = new ReflectionMethodDescriptor(method);
        Assertions.assertEquals(MethodDescriptor.RpcType.UNARY, md2.getRpcType());
    }

    @Test
    void testIsServerStream() throws NoSuchMethodException {
        Method method = DescriptorService.class.getMethod("sayHelloServerStream", HelloReply.class,
                StreamObserver.class);
        ReflectionMethodDescriptor descriptor = new ReflectionMethodDescriptor(method);
        Assertions.assertFalse(needWrap(descriptor));

        Method method2 = DescriptorService.class.getMethod("sayHelloServerStream2", Object.class, StreamObserver.class);
        ReflectionMethodDescriptor descriptor2 = new ReflectionMethodDescriptor(method2);
        Assertions.assertTrue(needWrap(descriptor2));
    }

    @Test
    void testIsNeedWrap() throws NoSuchMethodException {
        Method method = DescriptorService.class.getMethod("noParameterMethod");
        MethodDescriptor descriptor = new ReflectionMethodDescriptor(method);
        Assertions.assertTrue(needWrap(descriptor));

        method = DescriptorService.class.getMethod("sayHello", HelloReply.class);
        descriptor = new ReflectionMethodDescriptor(method);
        Assertions.assertFalse(needWrap(descriptor));
    }

    @Test
    void testIgnoreMethod() throws NoSuchMethodException {
        Method method = DescriptorService.class.getMethod("iteratorServerStream", HelloReply.class);
        MethodDescriptor descriptor = new ReflectionMethodDescriptor(method);
        Assertions.assertFalse(needWrap(descriptor));

        Method method2 = DescriptorService.class.getMethod("reactorMethod", HelloReply.class);
        MethodDescriptor descriptor2 = new ReflectionMethodDescriptor(method2);
        Assertions.assertFalse(needWrap(descriptor2));

        Method method3 = DescriptorService.class.getMethod("reactorMethod2", Mono.class);
        MethodDescriptor descriptor3 = new ReflectionMethodDescriptor(method3);
        Assertions.assertFalse(needWrap(descriptor3));


        Method method4 = DescriptorService.class.getMethod("rxJavaMethod", Single.class);
        MethodDescriptor descriptor4 = new ReflectionMethodDescriptor(method4);
        Assertions.assertFalse(needWrap(descriptor4));
    }


    @Test
    void testMultiProtoParameter() throws Exception {
        Method method = DescriptorService.class.getMethod("testMultiProtobufParameters", HelloReply.class,
                HelloReply.class);
        assertThrows(IllegalStateException.class, () -> {
            MethodDescriptor descriptor = new ReflectionMethodDescriptor(method);
            needWrap(descriptor);
        });
    }

    @Test
    void testDiffParametersAndReturn() throws Exception {
        Method method = DescriptorService.class.getMethod("testDiffParametersAndReturn", HelloReply.class);
        assertThrows(IllegalStateException.class, () -> {
            MethodDescriptor descriptor = new ReflectionMethodDescriptor(method);
            needWrap(descriptor);
        });

        Method method2 = DescriptorService.class.getMethod("testDiffParametersAndReturn2", String.class);
        assertThrows(IllegalStateException.class, () -> {
            MethodDescriptor descriptor = new ReflectionMethodDescriptor(method2);
            needWrap(descriptor);
        });
    }

    @Test
    void testErrorServerStream() throws Exception {
        Method method = DescriptorService.class.getMethod("testErrorServerStream", StreamObserver.class,
                HelloReply.class);
        assertThrows(IllegalStateException.class, () -> {
            MethodDescriptor descriptor = new ReflectionMethodDescriptor(method);
            needWrap(descriptor);
        });

        Method method2 = DescriptorService.class.getMethod("testErrorServerStream2", HelloReply.class, HelloReply.class,
                StreamObserver.class);
        assertThrows(IllegalStateException.class, () -> {
            MethodDescriptor descriptor = new ReflectionMethodDescriptor(method2);
            needWrap(descriptor);
        });

        Method method3 = DescriptorService.class.getMethod("testErrorServerStream3", String.class,
                StreamObserver.class);
        assertThrows(IllegalStateException.class, () -> {
            MethodDescriptor descriptor = new ReflectionMethodDescriptor(method3);
            needWrap(descriptor);
        });

        Method method4 = DescriptorService.class.getMethod("testErrorServerStream4", String.class, String.class,
                StreamObserver.class);
        assertThrows(IllegalStateException.class, () -> {
            MethodDescriptor descriptor = new ReflectionMethodDescriptor(method4);
            needWrap(descriptor);
        });
    }

    @Test
    void testErrorBiStream() throws Exception {
        Method method = DescriptorService.class.getMethod("testErrorBiStream", HelloReply.class, StreamObserver.class);
        assertThrows(IllegalStateException.class, () -> {
            MethodDescriptor descriptor = new ReflectionMethodDescriptor(method);
            needWrap(descriptor);
        });

        Method method2 = DescriptorService.class.getMethod("testErrorBiStream2", String.class, StreamObserver.class);
        assertThrows(IllegalStateException.class, () -> {
            MethodDescriptor descriptor = new ReflectionMethodDescriptor(method2);
            needWrap(descriptor);
        });

        Method method3 = DescriptorService.class.getMethod("testErrorBiStream3", StreamObserver.class);
        assertThrows(IllegalStateException.class, () -> {
            MethodDescriptor descriptor = new ReflectionMethodDescriptor(method3);
            needWrap(descriptor);
        });

        Method method4 = DescriptorService.class.getMethod("testErrorBiStream4", StreamObserver.class, String.class);
        assertThrows(IllegalStateException.class, () -> {
            MethodDescriptor descriptor = new ReflectionMethodDescriptor(method4);
            needWrap(descriptor);
        });
    }

    public boolean needWrap(MethodDescriptor method) {
        Class<?>[] actualRequestTypes;
        Class<?> actualResponseType;

        switch (method.getRpcType()) {
            case CLIENT_STREAM:
            case BI_STREAM:
                actualRequestTypes = new Class<?>[]{(Class<?>) ((ParameterizedType) method.getMethod()
                        .getGenericReturnType()).getActualTypeArguments()[0]};
                actualResponseType = (Class<?>) ((ParameterizedType) method.getMethod()
                        .getGenericParameterTypes()[0]).getActualTypeArguments()[0];
                break;
            case SERVER_STREAM:
                actualRequestTypes = method.getMethod().getParameterTypes();
                actualResponseType = (Class<?>) ((ParameterizedType) method.getMethod()
                        .getGenericParameterTypes()[1]).getActualTypeArguments()[0];
                break;
            case UNARY:
                actualRequestTypes = method.getParameterClasses();
                actualResponseType = method.getReturnClass();
                break;
            default:
                throw new IllegalStateException("Can not reach here");
        }

        return ReflectionPackableMethod.needWrap(method, actualRequestTypes, actualResponseType);
    }

}
