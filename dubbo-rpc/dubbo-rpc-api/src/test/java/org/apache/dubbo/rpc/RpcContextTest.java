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
package org.apache.dubbo.rpc;

import org.apache.dubbo.common.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class RpcContextTest {

    @Test
    public void testGetContext() {

        RpcContext rpcContext = RpcContext.getContext();
        Assertions.assertNotNull(rpcContext);

        RpcContext.removeContext();
        // if null, will return the initialize value.
        //Assertions.assertNull(RpcContext.getContext());
        Assertions.assertNotNull(RpcContext.getContext());
        Assertions.assertNotEquals(rpcContext, RpcContext.getContext());

        RpcContext serverRpcContext = RpcContext.getServerContext();
        Assertions.assertNotNull(serverRpcContext);

        RpcContext.removeServerContext();
        Assertions.assertNotEquals(serverRpcContext, RpcContext.getServerContext());

    }

    @Test
    public void testAddress() {
        RpcContext context = RpcContext.getContext();
        context.setLocalAddress("127.0.0.1", 20880);
        Assertions.assertTrue(context.getLocalAddress().getPort() == 20880);
        Assertions.assertEquals("127.0.0.1:20880", context.getLocalAddressString());

        context.setRemoteAddress("127.0.0.1", 20880);
        Assertions.assertTrue(context.getRemoteAddress().getPort() == 20880);
        Assertions.assertEquals("127.0.0.1:20880", context.getRemoteAddressString());

        context.setRemoteAddress("127.0.0.1", -1);
        context.setLocalAddress("127.0.0.1", -1);
        Assertions.assertTrue(context.getRemoteAddress().getPort() == 0);
        Assertions.assertTrue(context.getLocalAddress().getPort() == 0);
        Assertions.assertEquals("127.0.0.1", context.getRemoteHostName());
        Assertions.assertEquals("127.0.0.1", context.getLocalHostName());
    }

    @Test
    public void testCheckSide() {

        RpcContext context = RpcContext.getContext();

        //TODO fix npe
        //context.isProviderSide();

        context.setUrl(URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1"));
        Assertions.assertFalse(context.isConsumerSide());
        Assertions.assertTrue(context.isProviderSide());

        context.setUrl(URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&side=consumer"));
        Assertions.assertTrue(context.isConsumerSide());
        Assertions.assertFalse(context.isProviderSide());
    }

    @Test
    public void testAttachments() {

        RpcContext context = RpcContext.getContext();
        Map<String, String> map = new HashMap<String, String>();
        map.put("_11", "1111");
        map.put("_22", "2222");
        map.put(".33", "3333");

        context.setAttachments(map);
        Assertions.assertEquals(map, context.getAttachments());

        Assertions.assertEquals("1111", context.getAttachment("_11"));
        context.setAttachment("_11", "11.11");
        Assertions.assertEquals("11.11", context.getAttachment("_11"));

        context.setAttachment(null, "22222");
        context.setAttachment("_22", null);
        Assertions.assertEquals("22222", context.getAttachment(null));
        Assertions.assertNull(context.getAttachment("_22"));

        Assertions.assertNull(context.getAttachment("_33"));
        Assertions.assertEquals("3333", context.getAttachment(".33"));

        context.clearAttachments();
        Assertions.assertNull(context.getAttachment("_11"));
    }

    @Test
    public void testObject() {

        RpcContext context = RpcContext.getContext();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("_11", "1111");
        map.put("_22", "2222");
        map.put(".33", "3333");

        map.forEach(context::set);

        Assertions.assertEquals(map, context.get());

        Assertions.assertEquals("1111", context.get("_11"));
        context.set("_11", "11.11");
        Assertions.assertEquals("11.11", context.get("_11"));

        context.set(null, "22222");
        context.set("_22", null);
        Assertions.assertEquals("22222", context.get(null));
        Assertions.assertNull(context.get("_22"));

        Assertions.assertNull(context.get("_33"));
        Assertions.assertEquals("3333", context.get(".33"));

        map.keySet().forEach(context::remove);
        Assertions.assertNull(context.get("_11"));
    }

    @Test
    public void testAsync() {

        RpcContext rpcContext = RpcContext.getContext();
        Assertions.assertFalse(rpcContext.isAsyncStarted());

        AsyncContext asyncContext = RpcContext.startAsync();
        Assertions.assertTrue(rpcContext.isAsyncStarted());

        asyncContext.write(new Object());
        Assertions.assertTrue(((AsyncContextImpl)asyncContext).getInternalFuture().isDone());

        rpcContext.stopAsync();
        Assertions.assertTrue(rpcContext.isAsyncStarted());
    }

}
