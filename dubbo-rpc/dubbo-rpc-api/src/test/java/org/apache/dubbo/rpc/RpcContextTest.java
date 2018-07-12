package org.apache.dubbo.rpc;

import org.apache.dubbo.common.URL;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RpcContextTest {

    @Test
    public void testGetContext() {

        RpcContext rpcContext = RpcContext.getContext();
        Assert.assertNotNull(rpcContext);

        RpcContext.removeContext();
        // if null, will return the initialize value.
        //Assert.assertNull(RpcContext.getContext());
        Assert.assertNotNull(RpcContext.getContext());
        Assert.assertNotEquals(rpcContext, RpcContext.getContext());

        RpcContext serverRpcContext = RpcContext.getServerContext();
        Assert.assertNotNull(serverRpcContext);

        RpcContext.removeServerContext();
        Assert.assertNotEquals(serverRpcContext, RpcContext.getServerContext());

    }

    @Test
    public void testAddress() {
        RpcContext context = RpcContext.getContext();
        context.setLocalAddress("127.0.0.1", 20880);
        Assert.assertTrue(context.getLocalAddress().getPort() == 20880);
        Assert.assertEquals("127.0.0.1:20880", context.getLocalAddressString());

        context.setRemoteAddress("127.0.0.1", 20880);
        Assert.assertTrue(context.getRemoteAddress().getPort() == 20880);
        Assert.assertEquals("127.0.0.1:20880", context.getRemoteAddressString());

        context.setRemoteAddress("127.0.0.1", -1);
        context.setLocalAddress("127.0.0.1", -1);
        Assert.assertTrue(context.getRemoteAddress().getPort() == 0);
        Assert.assertTrue(context.getLocalAddress().getPort() == 0);
        Assert.assertEquals("127.0.0.1", context.getRemoteHostName());
        Assert.assertEquals("127.0.0.1", context.getLocalHostName());
    }

    @Test
    public void testCheckSide() {

        RpcContext context = RpcContext.getContext();

        //TODO fix npe
        //context.isProviderSide();

        context.setUrl(URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1"));
        Assert.assertFalse(context.isConsumerSide());
        Assert.assertTrue(context.isProviderSide());

        context.setUrl(URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&side=consumer"));
        Assert.assertTrue(context.isConsumerSide());
        Assert.assertFalse(context.isProviderSide());
    }

    @Test
    public void testAttachments() {

        RpcContext context = RpcContext.getContext();
        Map<String, String> map = new HashMap<String, String>();
        map.put("_11", "1111");
        map.put("_22", "2222");
        map.put(".33", "3333");

        context.setAttachments(map);
        Assert.assertEquals(map, context.getAttachments());

        Assert.assertEquals("1111", context.getAttachment("_11"));
        context.setAttachment("_11", "11.11");
        Assert.assertEquals("11.11", context.getAttachment("_11"));

        context.setAttachment(null, "22222");
        context.setAttachment("_22", null);
        Assert.assertEquals("22222", context.getAttachment(null));
        Assert.assertNull(context.getAttachment("_22"));

        Assert.assertNull(context.getAttachment("_33"));
        Assert.assertEquals("3333", context.getAttachment(".33"));

        context.clearAttachments();
        Assert.assertNull(context.getAttachment("_11"));
    }

    @Test
    public void testObject() {

        RpcContext context = RpcContext.getContext();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("_11", "1111");
        map.put("_22", "2222");
        map.put(".33", "3333");

        map.forEach(context::set);

        Assert.assertEquals(map, context.get());

        Assert.assertEquals("1111", context.get("_11"));
        context.set("_11", "11.11");
        Assert.assertEquals("11.11", context.get("_11"));

        context.set(null, "22222");
        context.set("_22", null);
        Assert.assertEquals("22222", context.get(null));
        Assert.assertNull(context.get("_22"));

        Assert.assertNull(context.get("_33"));
        Assert.assertEquals("3333", context.get(".33"));

        map.keySet().forEach(context::remove);
        Assert.assertNull(context.get("_11"));
    }

    @Test
    public void testAsync() {

        CompletableFuture<Object> future = new CompletableFuture<>();
        AsyncContext asyncContext = new AsyncContextImpl(future);

        RpcContext rpcContext = RpcContext.getContext();
        Assert.assertFalse(rpcContext.isAsyncStarted());

        rpcContext.setAsyncContext(asyncContext);
        Assert.assertFalse(rpcContext.isAsyncStarted());

        RpcContext.startAsync();
        Assert.assertTrue(rpcContext.isAsyncStarted());

        asyncContext.write(new Object());
        Assert.assertTrue(future.isDone());

        rpcContext.stopAsync();
        Assert.assertTrue(rpcContext.isAsyncStarted());
    }

}
