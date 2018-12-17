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

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * 2018/11/6
 */
public class ServiceDefinitionBuildderTest {

    @Test
    public void testBuilderComplextObject() {
        FullServiceDefinition fullServiceDefinition = ServiceDefinitionBuilder.buildFullDefinition(DemoService.class);
        checkComplextObjectAsParam(fullServiceDefinition);
    }


    void checkComplextObjectAsParam(FullServiceDefinition fullServiceDefinition) {
        List<MethodDefinition> methodDefinitions = fullServiceDefinition.getMethods();
        MethodDefinition complexCompute = null;
        MethodDefinition findComplexObject = null;
        for (MethodDefinition methodDefinition : methodDefinitions) {
            if ("complexCompute".equals(methodDefinition.getName())) {
                complexCompute = methodDefinition;
            } else if ("findComplexObject".equals(methodDefinition.getName())) {
                findComplexObject = methodDefinition;
            }
        }
        Assert.assertTrue(Arrays.equals(complexCompute.getParameterTypes(), new String[]{String.class.getName(), ComplexObject.class.getName()}));
        Assert.assertEquals(complexCompute.getReturnType(), String.class.getName());

        Assert.assertTrue(Arrays.equals(findComplexObject.getParameterTypes(), new String[]{String.class.getName(), "int", "long",
                String[].class.getCanonicalName(), "java.util.List<java.lang.Integer>", ComplexObject.TestEnum.class.getCanonicalName()}));
        Assert.assertEquals(findComplexObject.getReturnType(), ComplexObject.class.getCanonicalName());


        List<TypeDefinition> typeDefinitions = fullServiceDefinition.getTypes();

        TypeDefinition topTypeDefinition = null;
        TypeDefinition innerTypeDefinition = null;
        TypeDefinition inner2TypeDefinition = null;
        TypeDefinition inner3TypeDefinition = null;
        for (TypeDefinition typeDefinition : typeDefinitions) {
            if (typeDefinition.getType().equals(ComplexObject.class.getName())) {
                topTypeDefinition = typeDefinition;
            } else if (typeDefinition.getType().equals(ComplexObject.InnerObject.class.getName())) {
                innerTypeDefinition = typeDefinition;
            } else if (typeDefinition.getType().contains(ComplexObject.InnerObject2.class.getName())) {
                inner2TypeDefinition = typeDefinition;
            } else if (typeDefinition.getType().equals(ComplexObject.InnerObject3.class.getName())) {
                inner3TypeDefinition = typeDefinition;
            }
        }
        Assert.assertEquals(topTypeDefinition.getProperties().get("v").getType(), "long");
        Assert.assertEquals(topTypeDefinition.getProperties().get("maps").getType(), "java.util.Map<java.lang.String, java.lang.String>");
        Assert.assertEquals(topTypeDefinition.getProperties().get("innerObject").getType(), ComplexObject.InnerObject.class.getName());
        Assert.assertEquals(topTypeDefinition.getProperties().get("intList").getType(), "java.util.List<java.lang.Integer>");
        Assert.assertEquals(topTypeDefinition.getProperties().get("strArrays").getType(), "java.lang.String[]");
        Assert.assertEquals(topTypeDefinition.getProperties().get("innerObject3").getType(), "org.apache.dubbo.metadata.definition.service.ComplexObject.InnerObject3[]");
        Assert.assertEquals(topTypeDefinition.getProperties().get("testEnum").getType(), "org.apache.dubbo.metadata.definition.service.ComplexObject.TestEnum");
        Assert.assertEquals(topTypeDefinition.getProperties().get("innerObject2").getType(), "java.util.Set<org.apache.dubbo.metadata.definition.service.ComplexObject$InnerObject2>");

        Assert.assertSame(innerTypeDefinition.getProperties().get("innerA").getType(), "java.lang.String");
        Assert.assertSame(innerTypeDefinition.getProperties().get("innerB").getType(), "int");

        Assert.assertSame(inner2TypeDefinition.getProperties().get("innerA2").getType(), "java.lang.String");
        Assert.assertSame(inner2TypeDefinition.getProperties().get("innerB2").getType(), "int");

        Assert.assertSame(inner3TypeDefinition.getProperties().get("innerA3").getType(), "java.lang.String");

    }

}
