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

import org.apache.dubbo.proto.HelloReply;
import org.apache.dubbo.rpc.model.MethodDescriptor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MethodDescriptorTest {
    @Test
    public void testMethodWithNoParameters() throws Exception {
        Method method = DescriptorService.class.getMethod("noParameterMethod");
        MethodDescriptor descriptor = new MethodDescriptor(method);
        assertEquals("", descriptor.getParamDesc());
        Assertions.assertEquals(0, descriptor.getParameterClasses().length);
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
    public void testIsNeedWrap() throws NoSuchMethodException {
        Method method = DescriptorService.class.getMethod("noParameterMethod");
        MethodDescriptor descriptor = new MethodDescriptor(method);
        Assertions.assertTrue(descriptor.isNeedWrap());

        method = DescriptorService.class.getMethod("sayHello", HelloReply.class);
        descriptor = new MethodDescriptor(method);
        Assertions.assertFalse(descriptor.isNeedWrap());
    }
}
