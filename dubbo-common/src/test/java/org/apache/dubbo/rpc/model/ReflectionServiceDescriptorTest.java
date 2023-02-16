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
import org.apache.dubbo.metadata.definition.TypeDefinitionBuilder;
import org.apache.dubbo.rpc.support.DemoService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

class ReflectionServiceDescriptorTest {

    private final ReflectionServiceDescriptor service = new ReflectionServiceDescriptor(
        DemoService.class);

    @Test
    void addMethod() {
        ReflectionServiceDescriptor service2 = new ReflectionServiceDescriptor(
            DemoService.class);
        MethodDescriptor method = Mockito.mock(MethodDescriptor.class);
        when(method.getMethodName()).thenReturn("sayHello2");
        service2.addMethod(method);
        Assertions.assertEquals(1, service2.getMethods("sayHello2").size());
    }

    @Test
    void getFullServiceDefinition() {
        TypeDefinitionBuilder.initBuilders(new FrameworkModel());
        Assertions.assertNotNull(service.getFullServiceDefinition("demoService"));
    }

    @Test
    void getInterfaceName() {
        Assertions.assertEquals(DemoService.class.getName(), service.getInterfaceName());
    }

    @Test
    void getServiceInterfaceClass() {
        Assertions.assertEquals(DemoService.class, service.getServiceInterfaceClass());
    }

    @Test
    void getAllMethods() {
        Assertions.assertFalse(service.getAllMethods().isEmpty());
    }

    @Test
    void getMethod() {
        String desc = ReflectUtils.getDesc(String.class);
        Assertions.assertNotNull(service.getMethod("sayHello", desc));
    }

    @Test
    void testGetMethod() {
        Assertions.assertNotNull(service.getMethod("sayHello", new Class[]{String.class}));
    }

    @Test
    void getMethods() {
        Assertions.assertEquals(1, service.getMethods("sayHello").size());
    }

    @Test
    void testEquals() {
        ReflectionServiceDescriptor service2 = new ReflectionServiceDescriptor(
            DemoService.class);
        ReflectionServiceDescriptor service3 = new ReflectionServiceDescriptor(
            DemoService.class);
        Assertions.assertEquals(service2, service3);
    }

    @Test
    void testHashCode() {
        ReflectionServiceDescriptor service2 = new ReflectionServiceDescriptor(
            DemoService.class);
        ReflectionServiceDescriptor service3 = new ReflectionServiceDescriptor(
            DemoService.class);
        Assertions.assertEquals(service2.hashCode(), service3.hashCode());
    }
}
