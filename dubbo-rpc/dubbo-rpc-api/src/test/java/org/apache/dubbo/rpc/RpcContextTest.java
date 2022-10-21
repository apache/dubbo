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

        RpcContext rpcContext = RpcContext.getClientAttachment();
        Assertions.assertNotNull(rpcContext);

        RpcContext.removeClientAttachment();
        // if null, will return the initialize value.
        //Assertions.assertNull(RpcContext.getContext());
        Assertions.assertNotNull(RpcContext.getClientAttachment());
        Assertions.assertNotEquals(rpcContext, RpcContext.getClientAttachment());

        RpcContext serverRpcContext = RpcContext.getServerContext();
        Assertions.assertNotNull(serverRpcContext);

        RpcContext.removeServerContext();
        Assertions.assertNotEquals(serverRpcContext, RpcContext.getServerContext());

    }

    @Test
    public void testAddress() {
        RpcContext context = RpcContext.getServiceContext();
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

        RpcContext context = RpcContext.getServiceContext();

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

        RpcContext context = RpcContext.getClientAttachment();
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

        RpcContext context = RpcContext.getClientAttachment();
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
        RpcContext.removeContext();
    }

    @Test
    public void testAsync() {

        RpcContext rpcContext = RpcContext.getServiceContext();
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
        CompletableFuture<String> rpcFuture = RpcContext.getClientAttachment().asyncCall(() -> {
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
        RpcContext rpcContext = RpcContext.getClientAttachment();

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
    @Test
    public void say() {
        final String key = "user-attachment";
        final String value = "attachment-value";
        RpcContext.getServerContext().setObjectAttachment(key, value);
        final String returned = (String) RpcContext.getServerContext().getObjectAttachment(key);
        System.out.println(returned);
        RpcContext.getServerContext().remove(key);
    }

    @Test
    public void testRestore() {

    }

    @Test
    public void testRpcServerContextAttachment() {
        RpcContextAttachment attachment = RpcContext.getServerContext();
        attachment.setAttachment("key_1","value_1");
        attachment.setAttachment("key_2","value_2");
        Assertions.assertEquals("value_1", attachment.getAttachment("key_1"));
        Assertions.assertEquals(null, attachment.getAttachment("aaa"));
        attachment.removeAttachment("key_1");
        Assertions.assertEquals(null, attachment.getAttachment("key_1"));
        Assertions.assertEquals("value_2", attachment.getAttachment("key_2"));
        Object obj = new Object();
        attachment.setObjectAttachment("key_3", obj);
        Assertions.assertEquals(obj, attachment.getObjectAttachment("key_3"));
        attachment.removeAttachment("key_3");
        Assertions.assertEquals(null, attachment.getObjectAttachment("key_3"));

        Assertions.assertThrows(RuntimeException.class, () -> attachment.copyOf(true));
        Assertions.assertThrows(RuntimeException.class, attachment::isValid);

    }

    @Test
    public void testRpcServerContextClearAttachment() {
        RpcServerContextAttachment attachment = new RpcServerContextAttachment();
        attachment.setAttachment("key_1","value_1");
        attachment.setAttachment("key_2","value_2");
        attachment.setAttachment("key_3","value_3");
        attachment.clearAttachments();
        Assertions.assertEquals(null, attachment.getAttachment("key_1"));
        Assertions.assertEquals(null, attachment.getAttachment("key_2"));
        Assertions.assertEquals(null, attachment.getAttachment("key_3"));
    }

    @Test
    public void testAsyncContext() {
        RpcContextAttachment attachment = RpcContext.getServerContext();
        AsyncContext asyncContext = new AsyncContextImpl();
        attachment.setAsyncContext(asyncContext);
        asyncContext.start();
        Assertions.assertTrue(asyncContext.isAsyncStarted());
        Assertions.assertTrue(attachment.stopAsync());
    }

    @Test
    public void testObjectAttachmentMap() {
        RpcServerContextAttachment attachment  = new RpcServerContextAttachment();
        RpcServerContextAttachment.ObjectAttachmentMap objectAttachmentMap = new RpcServerContextAttachment.ObjectAttachmentMap(attachment);
        objectAttachmentMap.put("key_1", "value_1");
        Assertions.assertEquals(true, objectAttachmentMap.containsKey("key_1"));
        Assertions.assertEquals(true, objectAttachmentMap.containsValue("value_1"));
        Assertions.assertEquals("value_1", objectAttachmentMap.get("key_1"));
        Assertions.assertEquals(null, objectAttachmentMap.get("key_2"));
        objectAttachmentMap.remove("key_1");
        Assertions.assertEquals(null, objectAttachmentMap.get("key_1"));
    }

    @Test
    public void testClearAttachmentMap() {
        RpcServerContextAttachment attachment  = new RpcServerContextAttachment();
        RpcServerContextAttachment.ObjectAttachmentMap objectAttachmentMap = new RpcServerContextAttachment.ObjectAttachmentMap(attachment);
        objectAttachmentMap.put("key_1", "value_1");
        objectAttachmentMap.put("key_2", "value_2");
        objectAttachmentMap.put("key_3", "value_3");
        Assertions.assertEquals(3, objectAttachmentMap.size());
        objectAttachmentMap.clear();
        Assertions.assertEquals(0, objectAttachmentMap.size());
        Assertions.assertEquals(true, objectAttachmentMap.isEmpty());
    }
}
