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
import org.apache.dubbo.remoting.http12.exception.UnimplementedException;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ReflectionServiceDescriptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DescriptorUtilsTest {

    @Test
    void shouldSelectServerStreamMethodByWrapperArgTypesWhenUnaryMethodHasSameName() throws Exception {
        ReflectionServiceDescriptor serviceDescriptor = new ReflectionServiceDescriptor(OverloadedService.class);
        TripleCustomerProtocolWrapper.TripleRequestWrapper request =
                TripleCustomerProtocolWrapper.TripleRequestWrapper.Builder.newBuilder()
                        .setSerializeType("hessian4")
                        .addArgTypes(String.class.getName())
                        .addArgTypes(StreamObserver.class.getName())
                        .build();

        MethodDescriptor methodDescriptor = DescriptorUtils.findTripleMethodDescriptor(
                serviceDescriptor, "sync", new ByteArrayInputStream(request.toByteArray()));

        assertEquals(MethodDescriptor.RpcType.SERVER_STREAM, methodDescriptor.getRpcType());
    }

    @Test
    void shouldSelectUnaryMethodByWrapperArgTypesWhenServerStreamMethodHasSameName() throws Exception {
        ReflectionServiceDescriptor serviceDescriptor = new ReflectionServiceDescriptor(OverloadedService.class);
        TripleCustomerProtocolWrapper.TripleRequestWrapper request =
                TripleCustomerProtocolWrapper.TripleRequestWrapper.Builder.newBuilder()
                        .setSerializeType("hessian4")
                        .addArgTypes(String.class.getName())
                        .build();

        MethodDescriptor methodDescriptor = DescriptorUtils.findTripleMethodDescriptor(
                serviceDescriptor, "sync", new ByteArrayInputStream(request.toByteArray()));

        assertEquals(MethodDescriptor.RpcType.UNARY, methodDescriptor.getRpcType());
    }

    @Test
    void shouldSelectNoArgMethodByEmptyWrapperArgTypesWhenMethodIsOverloaded() throws Exception {
        ReflectionServiceDescriptor serviceDescriptor = new ReflectionServiceDescriptor(NoArgOverloadedService.class);
        TripleCustomerProtocolWrapper.TripleRequestWrapper request =
                TripleCustomerProtocolWrapper.TripleRequestWrapper.Builder.newBuilder()
                        .setSerializeType("hessian4")
                        .build();

        MethodDescriptor methodDescriptor = DescriptorUtils.findTripleMethodDescriptor(
                serviceDescriptor, "overload", new ByteArrayInputStream(request.toByteArray()));

        assertEquals(0, methodDescriptor.getParameterClasses().length);
    }

    @Test
    void shouldRejectOverloadedMethodsWithoutMatchingWrapperArgTypes() {
        ReflectionServiceDescriptor serviceDescriptor = new ReflectionServiceDescriptor(OverloadedService.class);
        TripleCustomerProtocolWrapper.TripleRequestWrapper request =
                TripleCustomerProtocolWrapper.TripleRequestWrapper.Builder.newBuilder()
                        .setSerializeType("hessian4")
                        .build();

        assertThrows(
                UnimplementedException.class,
                () -> DescriptorUtils.findTripleMethodDescriptor(
                        serviceDescriptor, "sync", new ByteArrayInputStream(request.toByteArray())));
    }

    @Test
    void shouldFallbackToUnaryMethodForGeneratedPairWithoutWrapperArgTypes() throws Exception {
        ReflectionServiceDescriptor serviceDescriptor = new ReflectionServiceDescriptor(GeneratedUnaryService.class);
        TripleCustomerProtocolWrapper.TripleRequestWrapper request =
                TripleCustomerProtocolWrapper.TripleRequestWrapper.Builder.newBuilder()
                        .setSerializeType("hessian4")
                        .build();

        MethodDescriptor methodDescriptor = DescriptorUtils.findTripleMethodDescriptor(
                serviceDescriptor, "generated", new ByteArrayInputStream(request.toByteArray()));

        assertEquals(MethodDescriptor.RpcType.UNARY, methodDescriptor.getRpcType());
    }

    @Test
    void shouldRejectAmbiguousTwoMethodReflectionOverload() {
        ReflectionServiceDescriptor serviceDescriptor =
                new ReflectionServiceDescriptor(AmbiguousReflectionService.class);

        assertThrows(
                UnimplementedException.class,
                () -> DescriptorUtils.findReflectionMethodDescriptor(serviceDescriptor, "ambiguous"));
    }

    @Test
    void shouldSurfaceCorruptWrapperForGeneratedPair() {
        ReflectionServiceDescriptor serviceDescriptor = new ReflectionServiceDescriptor(GeneratedUnaryService.class);
        byte[] corruptWrapper = new byte[] {0x0A, 0x05, 'h', 'e'};

        assertThrows(
                IOException.class,
                () -> DescriptorUtils.findTripleMethodDescriptor(
                        serviceDescriptor, "generated", new ByteArrayInputStream(corruptWrapper)));
    }

    private interface OverloadedService {

        DataWrapper<String> sync(String value);

        void sync(String value, StreamObserver<String> response);
    }

    private interface NoArgOverloadedService {

        String overload();

        String overload(String value);
    }

    private interface GeneratedUnaryService {

        String generated(String value);

        void generated(String value, StreamObserver<String> response);
    }

    private interface AmbiguousReflectionService {

        String ambiguous(String value);

        void ambiguous(String value, StreamObserver<Integer> response);
    }
}
