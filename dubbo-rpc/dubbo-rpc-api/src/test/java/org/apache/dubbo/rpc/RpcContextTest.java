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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
        Assertions.assertEquals(20880, context.getLocalAddress().getPort());
        Assertions.assertEquals("127.0.0.1:20880", context.getLocalAddressString());

        context.setRemoteAddress("127.0.0.1", 20880);
        Assertions.assertEquals(20880, context.getRemoteAddress().getPort());
        Assertions.assertEquals("127.0.0.1:20880", context.getRemoteAddressString());

        context.setRemoteAddress("127.0.0.1", -1);
        context.setLocalAddress("127.0.0.1", -1);
        Assertions.assertEquals(0, context.getRemoteAddress().getPort());
        Assertions.assertEquals(0, context.getLocalAddress().getPort());
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
        Map<String, Object> map = new HashMap<>();
        map.put("_11", "1111");
        map.put("_22", "2222");
        map.put(".33", "3333");

        context.setObjectAttachments(map);
        Assertions.assertEquals(map, context.getObjectAttachments());

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
        Assertions.assertTrue(((AsyncContextImpl) asyncContext).getInternalFuture().isDone());

        rpcContext.stopAsync();
        Assertions.assertTrue(rpcContext.isAsyncStarted());
        RpcContext.removeContext();
    }

    @Test
    public void testAsyncCall() {
        CompletableFuture<String> rpcFuture = RpcContext.getContext().asyncCall(() -> {
            throw new NullPointerException();
        });

        rpcFuture.whenComplete((rpcResult, throwable) -> {
            System.out.println(throwable.toString());
            Assertions.assertNull(rpcResult);
            Assertions.assertTrue(throwable instanceof RpcException);
            Assertions.assertTrue(throwable.getCause() instanceof NullPointerException);
        });

        Assertions.assertThrows(ExecutionException.class, rpcFuture::get);

        rpcFuture = rpcFuture.exceptionally(throwable -> "mock success");

        Assertions.assertEquals("mock success", rpcFuture.join());
    }

    @Test
    public void testObjectAttachment() {
        RpcContext rpcContext = RpcContext.getContext();

        rpcContext.setAttachment("objectKey1", "value1");
        rpcContext.setAttachment("objectKey2", "value2");
        rpcContext.setAttachment("objectKey3", 1); // object

        Assertions.assertEquals("value1", rpcContext.getObjectAttachment("objectKey1"));
        Assertions.assertEquals("value2", rpcContext.getAttachment("objectKey2"));
        Assertions.assertNull(rpcContext.getAttachment("objectKey3"));
        Assertions.assertEquals(1, rpcContext.getObjectAttachment("objectKey3"));
        Assertions.assertEquals(3, rpcContext.getObjectAttachments().size());

        rpcContext.clearAttachments();
        Assertions.assertEquals(0, rpcContext.getObjectAttachments().size());

        HashMap<String, Object> map = new HashMap<>();
        map.put("mapKey1", 1);
        map.put("mapKey2", "mapValue2");
        rpcContext.setObjectAttachments(map);
        Assertions.assertEquals(map, rpcContext.getObjectAttachments());
    }
}
