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
package org.apache.dubbo.rpc.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

import static org.apache.dubbo.rpc.Constants.MOCK_KEY;

public class MockInvokerTest {

    @Test
    public void testParseMockValue() throws Exception {
        Assertions.assertNull(MockInvoker.parseMockValue("null"));
        Assertions.assertNull(MockInvoker.parseMockValue("empty"));

        Assertions.assertTrue((Boolean) MockInvoker.parseMockValue("true"));
        Assertions.assertFalse((Boolean) MockInvoker.parseMockValue("false"));

        Assertions.assertEquals(123, MockInvoker.parseMockValue("123"));
        Assertions.assertEquals("foo", MockInvoker.parseMockValue("foo"));
        Assertions.assertEquals("foo", MockInvoker.parseMockValue("\"foo\""));
        Assertions.assertEquals("foo", MockInvoker.parseMockValue("\'foo\'"));

        Assertions.assertEquals(
                new HashMap<>(), MockInvoker.parseMockValue("{}"));
        Assertions.assertEquals(
                new ArrayList<>(), MockInvoker.parseMockValue("[]"));
        Assertions.assertEquals("foo",
                MockInvoker.parseMockValue("foo", new Type[]{String.class}));
    }

    @Test
    public void testInvoke() {
        URL url = URL.valueOf("remote://1.2.3.4/" + String.class.getName());
        url = url.addParameter(MOCK_KEY, "return ");
        MockInvoker mockInvoker = new MockInvoker(url, String.class);

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getSomething");
        Assertions.assertEquals(new HashMap<>(),
                mockInvoker.invoke(invocation).getObjectAttachments());
    }

    @Test
    public void testInvokeThrowsRpcException1() {
        URL url = URL.valueOf("remote://1.2.3.4/" + String.class.getName());
        MockInvoker mockInvoker = new MockInvoker(url, null);

        Assertions.assertThrows(RpcException.class,
                () -> mockInvoker.invoke(new RpcInvocation()));
    }

    @Test
    public void testInvokeThrowsRpcException2() {
        URL url = URL.valueOf("remote://1.2.3.4/" + String.class.getName());
        url = url.addParameter(MOCK_KEY, "fail");
        MockInvoker mockInvoker = new MockInvoker(url, String.class);

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getSomething");
        Assertions.assertThrows(RpcException.class,
                () -> mockInvoker.invoke(invocation));
    }

    @Test
    public void testInvokeThrowsRpcException3() {
        URL url = URL.valueOf("remote://1.2.3.4/" + String.class.getName());
        url = url.addParameter(MOCK_KEY, "throw");
        MockInvoker mockInvoker = new MockInvoker(url, String.class);

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getSomething");
        Assertions.assertThrows(RpcException.class,
                () -> mockInvoker.invoke(invocation));
    }

    @Test
    public void testGetThrowable() {
        Assertions.assertThrows(RpcException.class,
                () -> MockInvoker.getThrowable("Exception.class"));
    }

    @Test
    public void testGetMockObject() {
        Assertions.assertEquals("",
                MockInvoker.getMockObject("java.lang.String", String.class));

        Assertions.assertThrows(IllegalStateException.class, () -> MockInvoker
                .getMockObject("true", String.class));
        Assertions.assertThrows(IllegalStateException.class, () -> MockInvoker
                .getMockObject("default", String.class));
        Assertions.assertThrows(IllegalStateException.class, () -> MockInvoker
                .getMockObject("java.lang.String", Integer.class));
        Assertions.assertThrows(IllegalStateException.class, () -> MockInvoker
                .getMockObject("java.io.Serializable", Serializable.class));
    }

    @Test
    public void testNormalizeMock() {
        Assertions.assertNull(MockInvoker.normalizeMock(null));

        Assertions.assertEquals("", MockInvoker.normalizeMock(""));
        Assertions.assertEquals("", MockInvoker.normalizeMock("fail:"));
        Assertions.assertEquals("", MockInvoker.normalizeMock("force:"));
        Assertions.assertEquals("throw", MockInvoker.normalizeMock("throw"));
        Assertions.assertEquals("default", MockInvoker.normalizeMock("fail"));
        Assertions.assertEquals("default", MockInvoker.normalizeMock("force"));
        Assertions.assertEquals("default", MockInvoker.normalizeMock("true"));
        Assertions.assertEquals("default",
                MockInvoker.normalizeMock("default"));
        Assertions.assertEquals("return null",
                MockInvoker.normalizeMock("return"));
        Assertions.assertEquals("return null",
                MockInvoker.normalizeMock("return null"));
    }
}
