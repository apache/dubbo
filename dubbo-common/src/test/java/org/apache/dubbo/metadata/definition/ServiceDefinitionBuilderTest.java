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
package org.apache.dubbo.metadata.definition;

import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.definition.model.MethodDefinition;
import org.apache.dubbo.metadata.definition.model.TypeDefinition;
import org.apache.dubbo.metadata.definition.service.ComplexObject;
import org.apache.dubbo.metadata.definition.service.DemoService;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * 2018/11/6
 */
class ServiceDefinitionBuilderTest {


    private static FrameworkModel frameworkModel;

    @BeforeAll
    public static void setup() {
        frameworkModel = new FrameworkModel();
        TypeDefinitionBuilder.initBuilders(frameworkModel);
    }

    @AfterAll
    public static void clear() {
        frameworkModel.destroy();
    }

    @Test
    void testBuilderComplexObject() {
        TypeDefinitionBuilder.initBuilders(FrameworkModel.defaultModel());
        FullServiceDefinition fullServiceDefinition = ServiceDefinitionBuilder.buildFullDefinition(DemoService.class);
        checkComplexObjectAsParam(fullServiceDefinition);
    }


    void checkComplexObjectAsParam(FullServiceDefinition fullServiceDefinition) {
        Assertions.assertTrue(fullServiceDefinition.getAnnotations().contains("@org.apache.dubbo.metadata.definition.service.annotation.MockTypeAnnotation(value=666)")
            // JDK 17 style
            || fullServiceDefinition.getAnnotations().contains("@org.apache.dubbo.metadata.definition.service.annotation.MockTypeAnnotation(666)"));

        List<MethodDefinition> methodDefinitions = fullServiceDefinition.getMethods();
        MethodDefinition complexCompute = null;
        MethodDefinition findComplexObject = null;
        MethodDefinition testAnnotation = null;
        for (MethodDefinition methodDefinition : methodDefinitions) {
            if ("complexCompute".equals(methodDefinition.getName())) {
                complexCompute = methodDefinition;
            } else if ("findComplexObject".equals(methodDefinition.getName())) {
                findComplexObject = methodDefinition;
            } else if ("testAnnotation".equals(methodDefinition.getName())) {
                testAnnotation = methodDefinition;
            }
        }
        Assertions.assertTrue(Arrays.equals(complexCompute.getParameterTypes(), new String[]{String.class.getName(), ComplexObject.class.getName()}));
        Assertions.assertEquals(complexCompute.getReturnType(), String.class.getName());

        Assertions.assertTrue(Arrays.equals(findComplexObject.getParameterTypes(), new String[]{String.class.getName(), "int", "long",
            String[].class.getCanonicalName(), "java.util.List<java.lang.Integer>", ComplexObject.TestEnum.class.getCanonicalName()}));
        Assertions.assertEquals(findComplexObject.getReturnType(), ComplexObject.class.getCanonicalName());

        Assertions.assertTrue(testAnnotation.getAnnotations().equals(Arrays.asList(
            "@org.apache.dubbo.metadata.definition.service.annotation.MockMethodAnnotation(value=777)",
            "@org.apache.dubbo.metadata.definition.service.annotation.MockMethodAnnotation2(value=888)"))
            // JDK 17 style
            || testAnnotation.getAnnotations().equals(Arrays.asList(
            "@org.apache.dubbo.metadata.definition.service.annotation.MockMethodAnnotation(777)",
            "@org.apache.dubbo.metadata.definition.service.annotation.MockMethodAnnotation2(888)")));
        Assertions.assertEquals(testAnnotation.getReturnType(), "void");


        List<TypeDefinition> typeDefinitions = fullServiceDefinition.getTypes();

        TypeDefinition topTypeDefinition = null;
        TypeDefinition innerTypeDefinition = null;
        TypeDefinition inner2TypeDefinition = null;
        TypeDefinition inner3TypeDefinition = null;
        TypeDefinition listTypeDefinition = null;
        for (TypeDefinition typeDefinition : typeDefinitions) {
            if (typeDefinition.getType().equals(ComplexObject.class.getCanonicalName())) {
                topTypeDefinition = typeDefinition;
            } else if (typeDefinition.getType().equals(ComplexObject.InnerObject.class.getCanonicalName())) {
                innerTypeDefinition = typeDefinition;
            } else if (typeDefinition.getType().equals(ComplexObject.InnerObject2.class.getCanonicalName())) {
                inner2TypeDefinition = typeDefinition;
            } else if (typeDefinition.getType().equals(ComplexObject.InnerObject3.class.getCanonicalName())) {
                inner3TypeDefinition = typeDefinition;
            } else if (typeDefinition.getType().equals("java.util.List<java.lang.Integer>")) {
                listTypeDefinition = typeDefinition;
            }
        }
        Assertions.assertEquals("long", topTypeDefinition.getProperties().get("v"));
        Assertions.assertEquals("java.util.Map<java.lang.String,java.lang.String>", topTypeDefinition.getProperties().get("maps"));
        Assertions.assertEquals(ComplexObject.InnerObject.class.getCanonicalName(), topTypeDefinition.getProperties().get("innerObject"));
        Assertions.assertEquals("java.util.List<java.lang.Integer>", topTypeDefinition.getProperties().get("intList"));
        Assertions.assertEquals("java.lang.String[]", topTypeDefinition.getProperties().get("strArrays"));
        Assertions.assertEquals("org.apache.dubbo.metadata.definition.service.ComplexObject.InnerObject3[]", topTypeDefinition.getProperties().get("innerObject3"));
        Assertions.assertEquals("org.apache.dubbo.metadata.definition.service.ComplexObject.TestEnum", topTypeDefinition.getProperties().get("testEnum"));
        Assertions.assertEquals("java.util.Set<org.apache.dubbo.metadata.definition.service.ComplexObject.InnerObject2>", topTypeDefinition.getProperties().get("innerObject2"));

        Assertions.assertSame("java.lang.String", innerTypeDefinition.getProperties().get("innerA"));
        Assertions.assertSame("int", innerTypeDefinition.getProperties().get("innerB"));

        Assertions.assertSame("java.lang.String", inner2TypeDefinition.getProperties().get("innerA2"));
        Assertions.assertSame("int", inner2TypeDefinition.getProperties().get("innerB2"));

        Assertions.assertSame("java.lang.String", inner3TypeDefinition.getProperties().get("innerA3"));

        Assertions.assertEquals(Integer.class.getCanonicalName(), listTypeDefinition.getItems().get(0));
    }

}
