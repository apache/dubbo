package com.alibaba.dubbo.remoting.exchange.support;

import org.junit.Test;

import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.TimeoutException;
import com.alibaba.dubbo.remoting.exchange.Request;
import com.alibaba.dubbo.remoting.exchange.Response;
import com.alibaba.dubbo.remoting.exchange.support.header.MockChannel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DefaultFutureTest {

    @Test
    public void testReceivedTimeout() throws InterruptedException {
        Channel channel = new MockChannel();
        DefaultFuture future = new DefaultFuture(channel, new Request(1), 1000);
        assertEquals(1, DefaultFuture.pendingTimeout());
        Thread.sleep(1100);
        assertTrue(future.isDone());
        assertFalse(DefaultFuture.hasFuture(channel));
        assertEquals(0, DefaultFuture.pendingTimeout());

        try {
            future.get();
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof TimeoutException);
        }
    }

    @Test
    public void testReceivedOnTime() throws Exception {
        Channel channel = new MockChannel();
        Request request = new Request(1);
        Response response = new Response(1);
        response.setResult("bar");
        DefaultFuture future = new DefaultFuture(channel, request, 1000);

        assertEquals(1, DefaultFuture.pendingTimeout());
        DefaultFuture.received(channel, response);
        assertTrue(future.isDone());
        assertFalse(DefaultFuture.hasFuture(channel));
        Thread.sleep(200);
        assertEquals(0, DefaultFuture.pendingTimeout());
        assertEquals(future.get(), "bar");
    }
}