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
package org.apache.dubbo.descriptor;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.proto.HelloReply;
import org.apache.dubbo.rpc.model.MethodDescriptor;

import io.reactivex.Single;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MethodDescriptorTest {
    @Test
    public void testMethodWithNoParameters() throws Exception {
        Method method = DescriptorService.class.getMethod("noParameterMethod");
        MethodDescriptor descriptor = new MethodDescriptor(method);
        assertEquals("", descriptor.getParamDesc());
        Assertions.assertEquals(0, descriptor.getParameterClasses().length);
    }

    @Test
    public void testMethodWithNoParametersAndReturnProtobuf() throws Exception {
        Method method = DescriptorService.class.getMethod("noParameterAndReturnProtobufMethod");
        MethodDescriptor descriptor = new MethodDescriptor(method);
        assertEquals("", descriptor.getParamDesc());
        Assertions.assertEquals(0, descriptor.getParameterClasses().length);
        assertTrue(descriptor.isUnary());
        assertFalse(descriptor.isNeedWrap());
    }

    @Test
    public void testMethodWithNoParametersAndReturnJava() throws Exception {
        Method method = DescriptorService.class.getMethod("noParameterAndReturnJavaClassMethod");
        MethodDescriptor descriptor = new MethodDescriptor(method);
        assertEquals("", descriptor.getParamDesc());
        Assertions.assertEquals(0, descriptor.getParameterClasses().length);
        assertTrue(descriptor.isUnary());
        assertTrue(descriptor.isNeedWrap());
    }

    @Test
    public void testWrapperBiStream() throws Exception {
        Method method = DescriptorService.class.getMethod("wrapBidirectionalStream", StreamObserver.class);
        MethodDescriptor descriptor = new MethodDescriptor(method);
        Assertions.assertEquals(1, descriptor.getParameterClasses().length);
        assertTrue(descriptor.isStream());
        assertSame(descriptor.getRpcType(), MethodDescriptor.RpcType.BIDIRECTIONAL_STREAM);
        assertTrue(descriptor.isNeedWrap());
    }

    @Test
    public void testBiStream() throws Exception {
        Method method = DescriptorService.class.getMethod("bidirectionalStream", StreamObserver.class);
        MethodDescriptor descriptor = new MethodDescriptor(method);
        Assertions.assertEquals(1, descriptor.getParameterClasses().length);
        assertTrue(descriptor.isStream());
        assertSame(descriptor.getRpcType(), MethodDescriptor.RpcType.BIDIRECTIONAL_STREAM);
        assertFalse(descriptor.isNeedWrap());
    }

    @Test
    public void testIsStream() throws NoSuchMethodException {
        Method method = DescriptorService.class.getMethod("noParameterMethod");
        MethodDescriptor descriptor = new MethodDescriptor(method);
        Assertions.assertFalse(descriptor.isStream());

        method = DescriptorService.class.getMethod("sayHello", HelloReply.class);
        descriptor = new MethodDescriptor(method);
        Assertions.assertFalse(descriptor.isStream());
    }

    @Test
    public void testIsUnary() throws NoSuchMethodException {
        Method method = DescriptorService.class.getMethod("noParameterMethod");
        MethodDescriptor descriptor = new MethodDescriptor(method);
        Assertions.assertTrue(descriptor.isUnary());

        method = DescriptorService.class.getMethod("sayHello", HelloReply.class);
        descriptor = new MethodDescriptor(method);
        Assertions.assertTrue(descriptor.isUnary());
    }

    @Test
    public void testIsServerStream() throws NoSuchMethodException {
        Method method = DescriptorService.class.getMethod("sayHelloServerStream", HelloReply.class,
            StreamObserver.class);
        MethodDescriptor descriptor = new MethodDescriptor(method);
        Assertions.assertFalse(descriptor.isUnary());
        Assertions.assertFalse(descriptor.isNeedWrap());

        Method method2 = DescriptorService.class.getMethod("sayHelloServerStream2", Object.class, StreamObserver.class);
        MethodDescriptor descriptor2 = new MethodDescriptor(method2);
        Assertions.assertFalse(descriptor2.isUnary());
        Assertions.assertTrue(descriptor2.isNeedWrap());
    }

    @Test
    public void testIsNeedWrap() throws NoSuchMethodException {
        Method method = DescriptorService.class.getMethod("noParameterMethod");
        MethodDescriptor descriptor = new MethodDescriptor(method);
        Assertions.assertTrue(descriptor.isNeedWrap());

        method = DescriptorService.class.getMethod("sayHello", HelloReply.class);
        descriptor = new MethodDescriptor(method);
        Assertions.assertFalse(descriptor.isNeedWrap());
    }

    @Test
    public void testIgnoreMethod() throws NoSuchMethodException {
        Method method = DescriptorService.class.getMethod("iteratorServerStream", HelloReply.class);
        MethodDescriptor descriptor = new MethodDescriptor(method);
        Assertions.assertFalse(descriptor.isNeedWrap());

        Method method2 = DescriptorService.class.getMethod("reactorMethod", HelloReply.class);
        MethodDescriptor descriptor2 = new MethodDescriptor(method2);
        Assertions.assertFalse(descriptor2.isNeedWrap());

        Method method3 = DescriptorService.class.getMethod("reactorMethod2", Mono.class);
        MethodDescriptor  descriptor3 = new MethodDescriptor(method3);
        Assertions.assertFalse(descriptor3.isNeedWrap());


        Method method4 = DescriptorService.class.getMethod("rxJavaMethod", Single.class);
        MethodDescriptor  descriptor4 = new MethodDescriptor(method4);
        Assertions.assertFalse(descriptor4.isNeedWrap());
    }


    @Test
    public void testMultiProtoParameter() throws Exception {
        Method method = DescriptorService.class.getMethod("testMultiProtobufParameters", HelloReply.class,
            HelloReply.class);
        assertThrows(IllegalStateException.class,
            () -> {
                MethodDescriptor descriptor = new MethodDescriptor(method);
            });
    }

    @Test
    public void testDiffParametersAndReturn() throws Exception {
        Method method = DescriptorService.class.getMethod("testDiffParametersAndReturn", HelloReply.class);
        assertThrows(IllegalStateException.class,
            () -> {
                MethodDescriptor descriptor = new MethodDescriptor(method);
            });

        Method method2 = DescriptorService.class.getMethod("testDiffParametersAndReturn2", String.class);
        assertThrows(IllegalStateException.class,
            () -> {
                MethodDescriptor descriptor = new MethodDescriptor(method2);
            });
    }

    @Test
    public void testErrorServerStream() throws Exception {
        Method method = DescriptorService.class.getMethod("testErrorServerStream", StreamObserver.class,
            HelloReply.class);
        assertThrows(IllegalStateException.class,
            () -> {
                MethodDescriptor descriptor = new MethodDescriptor(method);
            });

        Method method2 = DescriptorService.class.getMethod("testErrorServerStream2", HelloReply.class, HelloReply
            .class, StreamObserver.class);
        assertThrows(IllegalStateException.class,
            () -> {
                MethodDescriptor descriptor = new MethodDescriptor(method2);
            });

        Method method3 = DescriptorService.class.getMethod("testErrorServerStream3", String.class,
            StreamObserver.class);
        assertThrows(IllegalStateException.class,
            () -> {
                MethodDescriptor descriptor = new MethodDescriptor(method3);
            });

        Method method4 = DescriptorService.class.getMethod("testErrorServerStream4", String.class, String.class,
            StreamObserver.class);
        assertThrows(IllegalStateException.class,
            () -> {
                MethodDescriptor descriptor = new MethodDescriptor(method4);
            });
    }

    @Test
    public void testErrorBiStream() throws Exception {
        Method method = DescriptorService.class.getMethod("testErrorBiStream", HelloReply.class, StreamObserver
            .class);
        assertThrows(IllegalStateException.class,
            () -> {
                MethodDescriptor descriptor = new MethodDescriptor(method);
            });

        Method method2 = DescriptorService.class.getMethod("testErrorBiStream2", String.class, StreamObserver.class);
        assertThrows(IllegalStateException.class,
            () -> {
                MethodDescriptor descriptor = new MethodDescriptor(method2);
            });

        Method method3 = DescriptorService.class.getMethod("testErrorBiStream3", StreamObserver.class);
        assertThrows(IllegalStateException.class,
            () -> {
                MethodDescriptor descriptor = new MethodDescriptor(method3);
            });

        Method method4 = DescriptorService.class.getMethod("testErrorBiStream4", StreamObserver.class, String.class);
        assertThrows(IllegalStateException.class,
            () -> {
                MethodDescriptor descriptor = new MethodDescriptor(method4);
            });
    }


}
