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

import com.google.gson.Gson;
import junit.framework.TestCase;
import org.apache.dubbo.metadata.definition.common.ClassExtendsMap;
import org.apache.dubbo.metadata.definition.common.ColorEnum;
import org.apache.dubbo.metadata.definition.common.OuterClass;
import org.apache.dubbo.metadata.definition.common.ResultWithRawCollections;
import org.apache.dubbo.metadata.definition.common.TestService;
import org.apache.dubbo.metadata.definition.model.ServiceDefinition;
import org.apache.dubbo.metadata.definition.model.TypeDefinition;
import org.junit.Test;

/**
 * TypeDefinitionBuilder
 * <p>
 * 16/9/22.
 */
public class MetadataTest {

    /**
     * 测试内部类，内部类的 class name 应当用 $ 分隔，例如： xxx.xx.Xxx$InnerClass
     */
    @Test
    public void testInnerClassType() {
        // 使用 TypeDefinitionBuilder 生成 TypeDefiniton
        TypeDefinitionBuilder builder = new TypeDefinitionBuilder();
        TypeDefinition td = builder.build(OuterClass.InnerClass.class, OuterClass.InnerClass.class);
        System.out.println(">> testInnerClassType: " + new Gson().toJson(td));

        TestCase.assertEquals("com.taobao.jaket.common.OuterClass$InnerClass", td.getType());
        TestCase.assertEquals(1, td.getProperties().size());
        TestCase.assertNotNull(td.getProperties().get("name"));

        // 使用 MetadataUtils 生成 ServiceDefinition
        ServiceDefinition sd = MetadataUtils.generateMetadata(TestService.class);
        System.out.println(">> testInnerClassType: " + new Gson().toJson(sd));

        TestCase.assertEquals(TestService.class.getName(), sd.getCanonicalName());
        TestCase.assertEquals(TestService.class.getMethods().length, sd.getMethods().size());
        boolean containsType = false;
        for (TypeDefinition type : sd.getTypes()) {
            if (type.getType().equals("com.taobao.jaket.common.OuterClass$InnerClass")) {
                containsType = true;
                break;
            }
        }
        TestCase.assertTrue(containsType);
    }

    /**
     * 测试返回结果包含无泛型的Map
     */
    @Test
    public void testRawMap() {
        // 使用 TypeDefinitionBuilder 生成 TypeDefiniton
        TypeDefinitionBuilder builder = new TypeDefinitionBuilder();
        TypeDefinition td = builder.build(ResultWithRawCollections.class, ResultWithRawCollections.class);
        System.out.println(">> testRawMap: " + new Gson().toJson(td));

        TestCase.assertEquals("com.taobao.jaket.common.ResultWithRawCollections", td.getType());
        TestCase.assertEquals(2, td.getProperties().size());
        TestCase.assertEquals("java.util.Map", td.getProperties().get("map").getType());
        TestCase.assertEquals("java.util.List", td.getProperties().get("list").getType());

        // 使用 MetadataUtils 生成 ServiceDefinition
        ServiceDefinition sd = MetadataUtils.generateMetadata(TestService.class);
        System.out.println(">> testRawMap: " + new Gson().toJson(sd));

        TestCase.assertEquals(TestService.class.getName(), sd.getCanonicalName());
        TestCase.assertEquals(TestService.class.getMethods().length, sd.getMethods().size());
        boolean containsType = false;
        for (TypeDefinition type : sd.getTypes()) {
            if (type.getType().equals("com.taobao.jaket.common.ResultWithRawCollections")) {
                containsType = true;
                break;
            }
        }
        TestCase.assertTrue(containsType);
    }

    @Test
    public void testEnum() {
        // 使用 TypeDefinitionBuilder 生成 TypeDefiniton
        TypeDefinitionBuilder builder = new TypeDefinitionBuilder();
        TypeDefinition td = builder.build(ColorEnum.class, ColorEnum.class);
        System.out.println(">> testEnum: " + new Gson().toJson(td));

        TestCase.assertEquals("com.taobao.jaket.common.ColorEnum", td.getType());
        TestCase.assertEquals(3, td.getEnums().size());
        TestCase.assertTrue(td.getEnums().contains("RED"));
        TestCase.assertTrue(td.getEnums().contains("YELLOW"));
        TestCase.assertTrue(td.getEnums().contains("BLUE"));

        // 使用 MetadataUtils 生成 ServiceDefinition
        ServiceDefinition sd = MetadataUtils.generateMetadata(TestService.class);
        System.out.println(">> testEnum: " + new Gson().toJson(sd));

        TestCase.assertEquals(TestService.class.getName(), sd.getCanonicalName());
        TestCase.assertEquals(TestService.class.getMethods().length, sd.getMethods().size());
        boolean containsType = false;
        for (TypeDefinition type : sd.getTypes()) {
            if (type.getType().equals("com.taobao.jaket.common.ColorEnum")) {
                containsType = true;
                break;
            }
        }
        TestCase.assertTrue(containsType);
    }

    @Test
    public void testExtendsMap() {
        // 使用 TypeDefinitionBuilder 生成 TypeDefiniton
        TypeDefinitionBuilder builder = new TypeDefinitionBuilder();
        TypeDefinition td = builder.build(ClassExtendsMap.class, ClassExtendsMap.class);
        System.out.println(">> testExtendsMap: " + new Gson().toJson(td));

        TestCase.assertEquals("com.taobao.jaket.common.ClassExtendsMap", td.getType());
        TestCase.assertEquals(0, td.getProperties().size());

        // 使用 MetadataUtils 生成 ServiceDefinition
        ServiceDefinition sd = MetadataUtils.generateMetadata(TestService.class);
        System.out.println(">> testExtendsMap: " + new Gson().toJson(sd));

        TestCase.assertEquals(TestService.class.getName(), sd.getCanonicalName());
        TestCase.assertEquals(TestService.class.getMethods().length, sd.getMethods().size());
        boolean containsType = false;
        for (TypeDefinition type : sd.getTypes()) {
            if (type.getType().equals("com.taobao.jaket.common.ClassExtendsMap")) {
                containsType = true;
                break;
            }
        }
        // 被认定成 map 类型，不会放到 ServiceDefinition 的 types 节点中
        TestCase.assertFalse(containsType);
    }
}
