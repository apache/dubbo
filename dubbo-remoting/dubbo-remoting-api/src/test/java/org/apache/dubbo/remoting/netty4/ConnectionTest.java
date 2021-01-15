package org.apache.dubbo.remoting.netty4;

import org.apache.dubbo.common.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

class ConnectionTest {

    @Test
    public void testRefCnt0() throws InterruptedException {
        Connection connection = new Connection(URL.valueOf("empty://127.0.0.1:8080?foo=bar"));
        CountDownLatch latch = new CountDownLatch(1);
        connection.getCloseFuture().addListener(future -> latch.countDown());
        connection.release();
        latch.await();
        Assertions.assertEquals(0, latch.getCount());
    }

    @Test
    public void testRefCnt1() throws InterruptedException {
        Connection connection = new Connection(URL.valueOf("empty://127.0.0.1:8080?foo=bar"));
        CountDownLatch latch = new CountDownLatch(1);
        connection.retain();
        connection.getCloseFuture().addListener(future -> latch.countDown());
        connection.release();
        Assertions.assertEquals(1, latch.getCount());
    }

    @Test
    public void testRefCnt2() throws InterruptedException {
        Connection connection = new Connection(URL.valueOf("empty://127.0.0.1:8080?foo=bar"));
        CountDownLatch latch = new CountDownLatch(1);
        connection.retain();
        connection.getCloseFuture().addListener(future -> latch.countDown());
        connection.release(2);
        latch.await();
        Assertions.assertEquals(0, latch.getCount());
    }
}