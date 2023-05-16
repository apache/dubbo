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

package org.apache.dubbo.generic;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.definition.model.MethodDefinition;
import org.apache.dubbo.metadata.definition.model.TypeDefinition;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.service.GenericService;
import org.apache.dubbo.service.ComplexObject;
import org.apache.dubbo.service.DemoService;
import org.apache.dubbo.service.DemoServiceImpl;

import com.alibaba.dubbo.config.ReferenceConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class GenericServiceTest {

    @BeforeEach
    public void beforeEach() {
        DubboBootstrap.reset();
    }

    @Test
    void testGeneric() {
        DemoService server = new DemoServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("dubbo://127.0.0.1:5342/" + DemoService.class.getName() + "?version=1.0.0");
        Exporter<DemoService> exporter = protocol.export(proxyFactory.getInvoker(server, DemoService.class, url));
        Invoker<DemoService> invoker = protocol.refer(DemoService.class, url);

        GenericService client = (GenericService) proxyFactory.getProxy(invoker, true);
        Object result = client.$invoke("sayHello", new String[]{"java.lang.String"}, new Object[]{"haha"});
        Assertions.assertEquals("hello haha", result);

        org.apache.dubbo.rpc.service.GenericService newClient = (org.apache.dubbo.rpc.service.GenericService) proxyFactory.getProxy(invoker, true);
        Object res = newClient.$invoke("sayHello", new String[]{"java.lang.String"}, new Object[]{"hehe"});
        Assertions.assertEquals("hello hehe", res);
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    void testGeneric2() {
        DemoService server = new DemoServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("dubbo://127.0.0.1:5342/" + DemoService.class.getName() + "?version=1.0.0&generic=true$timeout=3000");
        Exporter<DemoService> exporter = protocol.export(proxyFactory.getInvoker(server, DemoService.class, url));
        Invoker<GenericService> invoker = protocol.refer(GenericService.class, url);

        GenericService client = proxyFactory.getProxy(invoker, true);
        Object result = client.$invoke("sayHello", new String[]{"java.lang.String"}, new Object[]{"haha"});
        Assertions.assertEquals("hello haha", result);

        Invoker<DemoService> invoker2 = protocol.refer(DemoService.class, url);

        GenericService client2 = (GenericService) proxyFactory.getProxy(invoker2, true);
        Object result2 = client2.$invoke("sayHello", new String[]{"java.lang.String"}, new Object[]{"haha"});
        Assertions.assertEquals("hello haha", result2);

        invoker.destroy();
        exporter.unexport();
    }

    @Test
    void testGenericCompatible() {
        DubboBootstrap.getInstance()
            .application("test-app")
            .initialize();

        DemoService server = new DemoServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("dubbo://127.0.0.1:5342/" + DemoService.class.getName() + "?version=1.0.0&generic=true$timeout=3000");
        Exporter<DemoService> exporter = protocol.export(proxyFactory.getInvoker(server, DemoService.class, url));

        // simulate normal invoke
        ReferenceConfig<com.alibaba.dubbo.rpc.service.GenericService> oldReferenceConfig = new ReferenceConfig<>();
        oldReferenceConfig.setGeneric(true);
        oldReferenceConfig.setInterface(DemoService.class.getName());
        oldReferenceConfig.refresh();
        Invoker invoker = protocol.refer(oldReferenceConfig.getInterfaceClass(), url);
        com.alibaba.dubbo.rpc.service.GenericService client = (com.alibaba.dubbo.rpc.service.GenericService) proxyFactory.getProxy(invoker, true);

        Object result = client.$invoke("sayHello", new String[]{"java.lang.String"}, new Object[]{"haha"});
        Assertions.assertEquals("hello haha", result);

        invoker.destroy();
        exporter.unexport();
    }

    @Test
    void testGenericComplexCompute4FullServiceMetadata() {
        DemoService server = new DemoServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("dubbo://127.0.0.1:5342/" + DemoService.class.getName() + "?version=1.0.0&generic=true$timeout=3000");
        Exporter<DemoService> exporter = protocol.export(proxyFactory.getInvoker(server, DemoService.class, url));


        String var1 = "v1";
        int var2 = 234;
        long l = 555;
        String[] var3 = {"var31", "var32"};
        List<Integer> var4 = Arrays.asList(2, 4, 8);
        ComplexObject.TestEnum testEnum = ComplexObject.TestEnum.VALUE2;

        FullServiceDefinition fullServiceDefinition = ServiceDefinitionBuilder.buildFullDefinition(DemoService.class);
        MethodDefinition methodDefinition = getMethod("complexCompute", fullServiceDefinition.getMethods());
        Map mapObject = createComplexObject(fullServiceDefinition, var1, var2, l, var3, var4, testEnum);
        ComplexObject complexObject = map2bean(mapObject);

        Invoker<GenericService> invoker = protocol.refer(GenericService.class, url);


        GenericService client = proxyFactory.getProxy(invoker, true);
        Object result = client.$invoke(methodDefinition.getName(), methodDefinition.getParameterTypes(), new Object[]{"haha", mapObject});
        Assertions.assertEquals("haha###" + complexObject.toString(), result);


        Invoker<DemoService> invoker2 = protocol.refer(DemoService.class, url);
        GenericService client2 = (GenericService) proxyFactory.getProxy(invoker2, true);
        Object result2 = client2.$invoke("complexCompute", methodDefinition.getParameterTypes(), new Object[]{"haha2", mapObject});
        Assertions.assertEquals("haha2###" + complexObject.toString(), result2);

        invoker.destroy();
        exporter.unexport();
    }

    @Test
    void testGenericFindComplexObject4FullServiceMetadata() {
        DemoService server = new DemoServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("dubbo://127.0.0.1:5342/" + DemoService.class.getName() + "?version=1.0.0&generic=true$timeout=3000");
        Exporter<DemoService> exporter = protocol.export(proxyFactory.getInvoker(server, DemoService.class, url));


        String var1 = "v1";
        int var2 = 234;
        long l = 555;
        String[] var3 = {"var31", "var32"};
        List<Integer> var4 = Arrays.asList(2, 4, 8);
        ComplexObject.TestEnum testEnum = ComplexObject.TestEnum.VALUE2;
        //ComplexObject complexObject = createComplexObject(var1, var2, l, var3, var4, testEnum);

        Invoker<GenericService> invoker = protocol.refer(GenericService.class, url);

        GenericService client = proxyFactory.getProxy(invoker, true);
        Object result = client.$invoke("findComplexObject", new String[]{"java.lang.String", "int", "long", "java.lang.String[]", "java.util.List", "org.apache.dubbo.service.ComplexObject$TestEnum"},
            new Object[]{var1, var2, l, var3, var4, testEnum});
        Assertions.assertNotNull(result);
        ComplexObject r = map2bean((Map) result);
        Assertions.assertEquals(r, createComplexObject(var1, var2, l, var3, var4, testEnum));

        invoker.destroy();
        exporter.unexport();
    }

    MethodDefinition getMethod(String methodName, List<MethodDefinition> list) {
        for (MethodDefinition methodDefinition : list) {
            if (methodDefinition.getName().equals(methodName)) {
                return methodDefinition;
            }
        }
        return null;
    }

    Map<String, Object> createComplexObject(FullServiceDefinition fullServiceDefinition, String var1, int var2, long l, String[] var3, List<Integer> var4, ComplexObject.TestEnum testEnum) {
        List<TypeDefinition> typeDefinitions = fullServiceDefinition.getTypes();
        TypeDefinition topTypeDefinition = null;
        TypeDefinition innerTypeDefinition = null;
        TypeDefinition inner2TypeDefinition = null;
        TypeDefinition inner3TypeDefinition = null;
        for (TypeDefinition typeDefinition : typeDefinitions) {
            if (typeDefinition.getType().equals(ComplexObject.class.getCanonicalName())) {
                topTypeDefinition = typeDefinition;
            } else if (typeDefinition.getType().equals(ComplexObject.InnerObject.class.getCanonicalName())) {
                innerTypeDefinition = typeDefinition;
            } else if (typeDefinition.getType().equals(ComplexObject.InnerObject2.class.getCanonicalName())) {
                inner2TypeDefinition = typeDefinition;
            } else if (typeDefinition.getType().equals(ComplexObject.InnerObject3.class.getCanonicalName())) {
                inner3TypeDefinition = typeDefinition;
            }
        }
        Assertions.assertEquals("long", topTypeDefinition.getProperties().get("v"));
        Assertions.assertEquals("java.util.Map<java.lang.String,java.lang.String>", topTypeDefinition.getProperties().get("maps"));
        Assertions.assertEquals("org.apache.dubbo.service.ComplexObject.InnerObject", topTypeDefinition.getProperties().get("innerObject"));
        Assertions.assertEquals("java.util.List<java.lang.Integer>", topTypeDefinition.getProperties().get("intList"));
        Assertions.assertEquals("java.lang.String[]", topTypeDefinition.getProperties().get("strArrays"));
        Assertions.assertEquals("org.apache.dubbo.service.ComplexObject.InnerObject3[]", topTypeDefinition.getProperties().get("innerObject3"));
        Assertions.assertEquals("org.apache.dubbo.service.ComplexObject.TestEnum", topTypeDefinition.getProperties().get("testEnum"));
        Assertions.assertEquals("java.util.List<org.apache.dubbo.service.ComplexObject.InnerObject2>", topTypeDefinition.getProperties().get("innerObject2"));

        Assertions.assertSame("java.lang.String", innerTypeDefinition.getProperties().get("innerA"));
        Assertions.assertSame("int", innerTypeDefinition.getProperties().get("innerB"));

        Assertions.assertSame("java.lang.String", inner2TypeDefinition.getProperties().get("innerA2"));
        Assertions.assertSame("int", inner2TypeDefinition.getProperties().get("innerB2"));

        Assertions.assertSame("java.lang.String", inner3TypeDefinition.getProperties().get("innerA3"));

        Map<String, Object> result = new HashMap<>();
        result.put("v", l);
        Map maps = new HashMap<>(4);
        maps.put(var1 + "_k1", var1 + "_v1");
        maps.put(var1 + "_k2", var1 + "_v2");
        result.put("maps", maps);
        result.put("intList", var4);
        result.put("strArrays", var3);
        result.put("testEnum", testEnum.name());

        Map innerObjectMap = new HashMap<>(4);
        result.put("innerObject", innerObjectMap);
        innerObjectMap.put("innerA", var1);
        innerObjectMap.put("innerB", var2);

        List<Map> innerObject2List = new ArrayList<>();
        result.put("innerObject2", innerObject2List);
        Map innerObject2Tmp1 = new HashMap<>(4);
        innerObject2Tmp1.put("innerA2", var1 + "_21");
        innerObject2Tmp1.put("innerB2", var2 + 100000);
        Map innerObject2Tmp2 = new HashMap<>(4);
        innerObject2Tmp2.put("innerA2", var1 + "_22");
        innerObject2Tmp2.put("innerB2", var2 + 200000);
        innerObject2List.add(innerObject2Tmp1);
        innerObject2List.add(innerObject2Tmp2);

        Map innerObject3Tmp1 = new HashMap<>(4);
        innerObject3Tmp1.put("innerA3", var1 + "_31");
        Map innerObject3Tmp2 = new HashMap<>(4);
        innerObject3Tmp2.put("innerA3", var1 + "_32");
        Map innerObject3Tmp3 = new HashMap<>(4);
        innerObject3Tmp3.put("innerA3", var1 + "_32");
        result.put("innerObject3", new Map[]{innerObject3Tmp1, innerObject3Tmp2, innerObject3Tmp3});

        return result;
    }

    Map<String, Object> bean2Map(ComplexObject complexObject) {
        return JsonUtils.toJavaObject(JsonUtils.toJson(complexObject), Map.class);
    }

    ComplexObject map2bean(Map<String, Object> map) {
        return JsonUtils.toJavaObject(JsonUtils.toJson(map), ComplexObject.class);
    }

    ComplexObject createComplexObject(String var1, int var2, long l, String[] var3, List<Integer> var4, ComplexObject.TestEnum testEnum) {
        return new ComplexObject(var1, var2, l, var3, var4, testEnum);
    }

}
