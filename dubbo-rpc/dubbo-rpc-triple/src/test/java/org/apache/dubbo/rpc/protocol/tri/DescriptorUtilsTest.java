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

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ReflectionMethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DescriptorUtilsTest {

    @Test
    void testFindReflectionMethodDescriptorForGenericAndEcho() {
        ServiceDescriptor serviceDescriptor = Mockito.mock(ServiceDescriptor.class);

        MethodDescriptor genericDescriptor =
                DescriptorUtils.findReflectionMethodDescriptor(serviceDescriptor, CommonConstants.$INVOKE);
        MethodDescriptor echoDescriptor =
                DescriptorUtils.findReflectionMethodDescriptor(serviceDescriptor, CommonConstants.$ECHO);

        Assertions.assertEquals(CommonConstants.$INVOKE, genericDescriptor.getMethodName());
        Assertions.assertTrue(genericDescriptor.isGeneric());
        Assertions.assertEquals(CommonConstants.$ECHO, echoDescriptor.getMethodName());
    }

    @Test
    void testFindReflectionMethodDescriptorForEmptyMethodList() {
        ServiceDescriptor serviceDescriptor = Mockito.mock(ServiceDescriptor.class);
        Mockito.when(serviceDescriptor.getMethods("missing")).thenReturn(Collections.emptyList());

        MethodDescriptor result = DescriptorUtils.findReflectionMethodDescriptor(serviceDescriptor, "missing");

        Assertions.assertNull(result);
    }

    @Test
    void testFindReflectionMethodDescriptorForSingleMethod() throws NoSuchMethodException {
        ServiceDescriptor serviceDescriptor = Mockito.mock(ServiceDescriptor.class);
        MethodDescriptor unaryDescriptor =
                new ReflectionMethodDescriptor(TestService.class.getDeclaredMethod("unary", String.class));
        Mockito.when(serviceDescriptor.getMethods("unary")).thenReturn(Collections.singletonList(unaryDescriptor));

        MethodDescriptor result = DescriptorUtils.findReflectionMethodDescriptor(serviceDescriptor, "unary");

        Assertions.assertSame(unaryDescriptor, result);
    }

    @Test
    void testFindReflectionMethodDescriptorPrefersUnaryMethodFromOverloads() throws NoSuchMethodException {
        ServiceDescriptor serviceDescriptor = Mockito.mock(ServiceDescriptor.class);
        MethodDescriptor unaryDescriptor =
                new ReflectionMethodDescriptor(TestService.class.getDeclaredMethod("streaming", String.class));
        MethodDescriptor serverStreamDescriptor = new ReflectionMethodDescriptor(
                TestService.class.getDeclaredMethod("streaming", String.class, StreamObserver.class));
        Mockito.when(serviceDescriptor.getMethods("streaming"))
                .thenReturn(Arrays.asList(unaryDescriptor, serverStreamDescriptor));

        MethodDescriptor result = DescriptorUtils.findReflectionMethodDescriptor(serviceDescriptor, "streaming");

        Assertions.assertSame(unaryDescriptor, result);
    }

    interface TestService {
        String unary(String value);

        String streaming(String value);

        void streaming(String value, StreamObserver<String> streamObserver);
    }
}
