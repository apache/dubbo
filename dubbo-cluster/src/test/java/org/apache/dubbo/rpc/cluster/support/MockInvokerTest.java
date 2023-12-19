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
package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.support.MockInvoker;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.rpc.Constants.MOCK_KEY;

class MockInvokerTest {

    @Test
    void testParseMockValue() throws Exception {
        Assertions.assertNull(org.apache.dubbo.rpc.support.MockInvoker.parseMockValue("null"));
        Assertions.assertNull(org.apache.dubbo.rpc.support.MockInvoker.parseMockValue("empty"));

        Assertions.assertTrue((Boolean) org.apache.dubbo.rpc.support.MockInvoker.parseMockValue("true"));
        Assertions.assertFalse((Boolean) org.apache.dubbo.rpc.support.MockInvoker.parseMockValue("false"));

        Assertions.assertEquals(123, org.apache.dubbo.rpc.support.MockInvoker.parseMockValue("123"));
        Assertions.assertEquals("foo", org.apache.dubbo.rpc.support.MockInvoker.parseMockValue("foo"));
        Assertions.assertEquals("foo", org.apache.dubbo.rpc.support.MockInvoker.parseMockValue("\"foo\""));
        Assertions.assertEquals("foo", org.apache.dubbo.rpc.support.MockInvoker.parseMockValue("\'foo\'"));

        Assertions.assertEquals(new HashMap<>(), org.apache.dubbo.rpc.support.MockInvoker.parseMockValue("{}"));
        Assertions.assertEquals(new ArrayList<>(), org.apache.dubbo.rpc.support.MockInvoker.parseMockValue("[]"));
        Assertions.assertEquals(
                "foo", org.apache.dubbo.rpc.support.MockInvoker.parseMockValue("foo", new Type[] {String.class}));
    }

    @Test
    void testInvoke() {
        URL url = URL.valueOf("remote://1.2.3.4/" + String.class.getName());
        url = url.addParameter(MOCK_KEY, "return ");
        org.apache.dubbo.rpc.support.MockInvoker mockInvoker =
                new org.apache.dubbo.rpc.support.MockInvoker(url, String.class);

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getSomething");
        Assertions.assertEquals(new HashMap<>(), mockInvoker.invoke(invocation).getObjectAttachments());
    }

    @Test
    void testGetDefaultObject() {
        // test methodA in DemoServiceAMock
        final Class<DemoServiceA> demoServiceAClass = DemoServiceA.class;
        URL url = URL.valueOf("remote://1.2.3.4/" + demoServiceAClass.getName());
        url = url.addParameter(MOCK_KEY, "force:true");
        org.apache.dubbo.rpc.support.MockInvoker mockInvoker =
                new org.apache.dubbo.rpc.support.MockInvoker(url, demoServiceAClass);

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("methodA");
        Assertions.assertEquals(new HashMap<>(), mockInvoker.invoke(invocation).getObjectAttachments());

        // test methodB in DemoServiceBMock
        final Class<DemoServiceB> demoServiceBClass = DemoServiceB.class;
        url = URL.valueOf("remote://1.2.3.4/" + demoServiceBClass.getName());
        url = url.addParameter(MOCK_KEY, "force:true");
        mockInvoker = new org.apache.dubbo.rpc.support.MockInvoker(url, demoServiceBClass);
        invocation = new RpcInvocation();
        invocation.setMethodName("methodB");
        Assertions.assertEquals(new HashMap<>(), mockInvoker.invoke(invocation).getObjectAttachments());
    }

    @Test
    void testInvokeThrowsRpcException1() {
        URL url = URL.valueOf("remote://1.2.3.4/" + String.class.getName());
        org.apache.dubbo.rpc.support.MockInvoker mockInvoker = new org.apache.dubbo.rpc.support.MockInvoker(url, null);

        Assertions.assertThrows(RpcException.class, () -> mockInvoker.invoke(new RpcInvocation()));
    }

    @Test
    void testInvokeThrowsRpcException2() {
        URL url = URL.valueOf("remote://1.2.3.4/" + String.class.getName());
        url = url.addParameter(MOCK_KEY, "fail");
        org.apache.dubbo.rpc.support.MockInvoker mockInvoker =
                new org.apache.dubbo.rpc.support.MockInvoker(url, String.class);

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getSomething");
        Assertions.assertThrows(RpcException.class, () -> mockInvoker.invoke(invocation));
    }

    @Test
    void testInvokeThrowsRpcException3() {
        URL url = URL.valueOf("remote://1.2.3.4/" + String.class.getName());
        url = url.addParameter(MOCK_KEY, "throw");
        org.apache.dubbo.rpc.support.MockInvoker mockInvoker =
                new org.apache.dubbo.rpc.support.MockInvoker(url, String.class);

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getSomething");
        Assertions.assertThrows(RpcException.class, () -> mockInvoker.invoke(invocation));
    }

    @Test
    void testGetThrowable() {
        Assertions.assertThrows(
                RpcException.class, () -> org.apache.dubbo.rpc.support.MockInvoker.getThrowable("Exception.class"));
    }

    @Test
    void testGetMockObject() {
        Assertions.assertEquals(
                "",
                org.apache.dubbo.rpc.support.MockInvoker.getMockObject(
                        ApplicationModel.defaultModel().getExtensionDirector(), "java.lang.String", String.class));

        Assertions.assertThrows(
                IllegalStateException.class,
                () -> org.apache.dubbo.rpc.support.MockInvoker.getMockObject(
                        ApplicationModel.defaultModel().getExtensionDirector(), "true", String.class));
        Assertions.assertThrows(
                IllegalStateException.class,
                () -> org.apache.dubbo.rpc.support.MockInvoker.getMockObject(
                        ApplicationModel.defaultModel().getExtensionDirector(), "default", String.class));
        Assertions.assertThrows(
                IllegalStateException.class,
                () -> org.apache.dubbo.rpc.support.MockInvoker.getMockObject(
                        ApplicationModel.defaultModel().getExtensionDirector(), "java.lang.String", Integer.class));
        Assertions.assertThrows(
                IllegalStateException.class,
                () -> org.apache.dubbo.rpc.support.MockInvoker.getMockObject(
                        ApplicationModel.defaultModel().getExtensionDirector(),
                        "java.io.Serializable",
                        Serializable.class));
    }

    @Test
    void testNormalizeMock() {
        Assertions.assertNull(org.apache.dubbo.rpc.support.MockInvoker.normalizeMock(null));

        Assertions.assertEquals("", org.apache.dubbo.rpc.support.MockInvoker.normalizeMock(""));
        Assertions.assertEquals("", org.apache.dubbo.rpc.support.MockInvoker.normalizeMock("fail:"));
        Assertions.assertEquals("", org.apache.dubbo.rpc.support.MockInvoker.normalizeMock("force:"));
        Assertions.assertEquals("throw", org.apache.dubbo.rpc.support.MockInvoker.normalizeMock("throw"));
        Assertions.assertEquals("default", org.apache.dubbo.rpc.support.MockInvoker.normalizeMock("fail"));
        Assertions.assertEquals("default", org.apache.dubbo.rpc.support.MockInvoker.normalizeMock("force"));
        Assertions.assertEquals("default", org.apache.dubbo.rpc.support.MockInvoker.normalizeMock("true"));
        Assertions.assertEquals("default", org.apache.dubbo.rpc.support.MockInvoker.normalizeMock("default"));
        Assertions.assertEquals("return null", org.apache.dubbo.rpc.support.MockInvoker.normalizeMock("return"));
        Assertions.assertEquals("return null", MockInvoker.normalizeMock("return null"));
    }
}
