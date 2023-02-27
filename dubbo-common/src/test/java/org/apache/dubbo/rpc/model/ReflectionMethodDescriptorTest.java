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

package org.apache.dubbo.rpc.model;

import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.rpc.model.MethodDescriptor.RpcType;
import org.apache.dubbo.rpc.support.DemoService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;

class ReflectionMethodDescriptorTest {

    private final ReflectionMethodDescriptor method;

    {
        try {
            method = new ReflectionMethodDescriptor(
                DemoService.class.getDeclaredMethod("sayHello", String.class));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void getMethodName() {
        Assertions.assertEquals("sayHello", method.getMethodName());
    }

    @Test
    void getMethod() {
        Assertions.assertEquals("sayHello", method.getMethod().getName());
    }

    @Test
    void getCompatibleParamSignatures() {
        Assertions.assertArrayEquals(new String[]{String.class.getName()},
            method.getCompatibleParamSignatures());
    }

    @Test
    void getParameterClasses() {
        Assertions.assertArrayEquals(new Class[]{String.class}, method.getParameterClasses());
    }

    @Test
    void getParamDesc() {
        Assertions.assertEquals(ReflectUtils.getDesc(String.class), method.getParamDesc());
    }

    @Test
    void getReturnClass() {
        Assertions.assertEquals(String.class, method.getReturnClass());
    }

    @Test
    void getReturnTypes() {
        Assertions.assertArrayEquals(new Type[]{String.class, String.class},
            method.getReturnTypes());
    }

    @Test
    void getRpcType() {
        Assertions.assertEquals(RpcType.UNARY, method.getRpcType());
    }

    @Test
    void isGeneric() {
        Assertions.assertFalse(method.isGeneric());
    }

    @Test
    void addAttribute() {
        String attr = "attr";
        method.addAttribute(attr, attr);
        Assertions.assertEquals(attr, method.getAttribute(attr));
    }

    @Test
    void testEquals() {
        try {
            MethodDescriptor method2 = new ReflectionMethodDescriptor(
                DemoService.class.getDeclaredMethod("sayHello", String.class));
            method.addAttribute("attr", "attr");
            method2.addAttribute("attr", "attr");
            Assertions.assertEquals(method, method2);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void testHashCode() {
        try {
            MethodDescriptor method2 = new ReflectionMethodDescriptor(
                DemoService.class.getDeclaredMethod("sayHello", String.class));
            method.addAttribute("attr", "attr");
            method2.addAttribute("attr", "attr");
            Assertions.assertEquals(method.hashCode(), method2.hashCode());
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}