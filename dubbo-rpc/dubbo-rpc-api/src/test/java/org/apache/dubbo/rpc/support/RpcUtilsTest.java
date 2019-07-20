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
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import static org.apache.dubbo.rpc.Constants.AUTO_ATTACH_INVOCATIONID_KEY;

public class RpcUtilsTest {

    /**
     * regular scenario: async invocation in URL
     * verify: 1. whether invocationId is set correctly, 2. idempotent or not
     */
    @Test
    public void testAttachInvocationIdIfAsync_normal() {
        URL url = URL.valueOf("dubbo://localhost/?test.async=true");
        Map<String, String> attachments = new HashMap<String, String>();
        attachments.put("aa", "bb");
        Invocation inv = new RpcInvocation("test", new Class[]{}, new String[]{}, attachments);
        RpcUtils.attachInvocationIdIfAsync(url, inv);
        long id1 = RpcUtils.getInvocationId(inv);
        RpcUtils.attachInvocationIdIfAsync(url, inv);
        long id2 = RpcUtils.getInvocationId(inv);
        assertEquals(id1, id2); // verify if it's idempotent
        assertTrue(id1 >= 0);
        assertEquals("bb", attachments.get("aa"));
    }

    /**
     * scenario: sync invocation, no attachment added by default
     * verify: no id attribute added in attachment
     */
    @Test
    public void testAttachInvocationIdIfAsync_sync() {
        URL url = URL.valueOf("dubbo://localhost/");
        Invocation inv = new RpcInvocation("test", new Class[]{}, new String[]{});
        RpcUtils.attachInvocationIdIfAsync(url, inv);
        assertNull(RpcUtils.getInvocationId(inv));
    }

    /**
     * scenario: async invocation, add attachment by default
     * verify: no error report when the original attachment is null
     */
    @Test
    public void testAttachInvocationIdIfAsync_nullAttachments() {
        URL url = URL.valueOf("dubbo://localhost/?test.async=true");
        Invocation inv = new RpcInvocation("test", new Class[]{}, new String[]{});
        RpcUtils.attachInvocationIdIfAsync(url, inv);
        assertTrue(RpcUtils.getInvocationId(inv) >= 0l);
    }

    /**
     * scenario: explicitly configure to not add attachment
     * verify: no id attribute added in attachment
     */
    @Test
    public void testAttachInvocationIdIfAsync_forceNotAttache() {
        URL url = URL.valueOf("dubbo://localhost/?test.async=true&" + AUTO_ATTACH_INVOCATIONID_KEY + "=false");
        Invocation inv = new RpcInvocation("test", new Class[]{}, new String[]{});
        RpcUtils.attachInvocationIdIfAsync(url, inv);
        assertNull(RpcUtils.getInvocationId(inv));
    }

    /**
     * scenario: explicitly configure to add attachment
     * verify: id attribute added in attachment
     */
    @Test
    public void testAttachInvocationIdIfAsync_forceAttache() {
        URL url = URL.valueOf("dubbo://localhost/?" + AUTO_ATTACH_INVOCATIONID_KEY + "=true");
        Invocation inv = new RpcInvocation("test", new Class[]{}, new String[]{});
        RpcUtils.attachInvocationIdIfAsync(url, inv);
        assertNotNull(RpcUtils.getInvocationId(inv));
    }

    @Test
    public void testGetReturnTypes() throws Exception {
        Invoker invoker = mock(Invoker.class);
        given(invoker.getUrl()).willReturn(URL.valueOf("test://127.0.0.1:1/org.apache.dubbo.rpc.support.DemoService?interface=org.apache.dubbo.rpc.support.DemoService"));
        Invocation inv = new RpcInvocation("testReturnType", new Class<?>[]{String.class}, null, null, invoker);

        java.lang.reflect.Type[] types = RpcUtils.getReturnTypes(inv);
        Assertions.assertEquals(2, types.length);
        Assertions.assertEquals(String.class, types[0]);
        Assertions.assertEquals(String.class, types[1]);

        Invocation inv1 = new RpcInvocation("testReturnType1", new Class<?>[]{String.class}, null, null, invoker);
        java.lang.reflect.Type[] types1 = RpcUtils.getReturnTypes(inv1);
        Assertions.assertEquals(2, types1.length);
        Assertions.assertEquals(List.class, types1[0]);
        Assertions.assertEquals(DemoService.class.getMethod("testReturnType1", new Class<?>[]{String.class}).getGenericReturnType(), types1[1]);

        Invocation inv2 = new RpcInvocation("testReturnType2", new Class<?>[]{String.class}, null, null, invoker);
        java.lang.reflect.Type[] types2 = RpcUtils.getReturnTypes(inv2);
        Assertions.assertEquals(2, types2.length);
        Assertions.assertEquals(String.class, types2[0]);
        Assertions.assertEquals(String.class, types2[1]);

        Invocation inv3 = new RpcInvocation("testReturnType3", new Class<?>[]{String.class}, null, null, invoker);
        java.lang.reflect.Type[] types3 = RpcUtils.getReturnTypes(inv3);
        Assertions.assertEquals(2, types3.length);
        Assertions.assertEquals(List.class, types3[0]);
        java.lang.reflect.Type genericReturnType3 = DemoService.class.getMethod("testReturnType3", new Class<?>[]{String.class}).getGenericReturnType();
        Assertions.assertEquals(((ParameterizedType) genericReturnType3).getActualTypeArguments()[0], types3[1]);

        Invocation inv4 = new RpcInvocation("testReturnType4", new Class<?>[]{String.class}, null, null, invoker);
        java.lang.reflect.Type[] types4 = RpcUtils.getReturnTypes(inv4);
        Assertions.assertEquals(2, types4.length);
        Assertions.assertNull(types4[0]);
        Assertions.assertNull(types4[1]);

        Invocation inv5 = new RpcInvocation("testReturnType5", new Class<?>[]{String.class}, null, null, invoker);
        java.lang.reflect.Type[] types5 = RpcUtils.getReturnTypes(inv5);
        Assertions.assertEquals(2, types5.length);
        Assertions.assertEquals(Map.class, types5[0]);
        java.lang.reflect.Type genericReturnType5 = DemoService.class.getMethod("testReturnType5", new Class<?>[]{String.class}).getGenericReturnType();
        Assertions.assertEquals(((ParameterizedType) genericReturnType5).getActualTypeArguments()[0], types5[1]);
    }
}
