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
package com.alibaba.dubbo.rpc.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.RpcInvocation;

import junit.framework.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

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
        Assert.assertTrue(id1 == id2); // verify if it's idempotent
        Assert.assertTrue(id1 >= 0);
        Assert.assertEquals("bb", attachments.get("aa"));
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
        Assert.assertNull(RpcUtils.getInvocationId(inv));
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
        Assert.assertTrue(RpcUtils.getInvocationId(inv) >= 0l);
    }

    /**
     * scenario: explicitly configure to not add attachment
     * verify: no id attribute added in attachment
     */
    @Test
    public void testAttachInvocationIdIfAsync_forceNotAttache() {
        URL url = URL.valueOf("dubbo://localhost/?test.async=true&" + Constants.AUTO_ATTACH_INVOCATIONID_KEY + "=false");
        Invocation inv = new RpcInvocation("test", new Class[]{}, new String[]{});
        RpcUtils.attachInvocationIdIfAsync(url, inv);
        Assert.assertNull(RpcUtils.getInvocationId(inv));
    }

    /**
     * scenario: explicitly configure to add attachment
     * verify: id attribute added in attachment
     */
    @Test
    public void testAttachInvocationIdIfAsync_forceAttache() {
        URL url = URL.valueOf("dubbo://localhost/?" + Constants.AUTO_ATTACH_INVOCATIONID_KEY + "=true");
        Invocation inv = new RpcInvocation("test", new Class[]{}, new String[]{});
        RpcUtils.attachInvocationIdIfAsync(url, inv);
        Assert.assertNotNull(RpcUtils.getInvocationId(inv));
    }
}
