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
package org.apache.dubbo.remoting.api;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

class ConnectionTest {

    @Test
    public void testRefCnt0() throws InterruptedException {
        Connection connection = new Connection(URL.valueOf("empty://127.0.0.1:8080?foo=bar"));
        CountDownLatch latch = new CountDownLatch(1);
        connection.getClosePromise().addListener(future -> latch.countDown());
        connection.release();
        latch.await();
        Assertions.assertEquals(0, latch.getCount());
    }

    @Test
    public void testRefCnt1() {
        Connection connection = new Connection(URL.valueOf("empty://127.0.0.1:8080?foo=bar"));
        CountDownLatch latch = new CountDownLatch(1);
        connection.retain();
        connection.getClosePromise().addListener(future -> latch.countDown());
        connection.release();
        Assertions.assertEquals(1, latch.getCount());
    }

    @Test
    public void testRefCnt2() throws InterruptedException {
        Connection connection = new Connection(URL.valueOf("empty://127.0.0.1:8080?foo=bar"));
        CountDownLatch latch = new CountDownLatch(1);
        connection.retain();
        connection.getClosePromise().addListener(future -> latch.countDown());
        connection.release(2);
        latch.await();
        Assertions.assertEquals(0, latch.getCount());
    }

    @Test
    public void connectSyncTest() throws Throwable {
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("empty://127.0.0.1:" + port + "?foo=bar");
        PortUnificationServer server = null;
        try {
            server = new PortUnificationServer(url);
            server.bind();

            Connection connection = new Connection(url);
            Assertions.assertTrue(connection.isAvailable());

            server.close();
            Assertions.assertFalse(connection.isAvailable());

            server.bind();
            // auto reconnect
            Assertions.assertTrue(connection.isAvailable());

            connection.close();
            Assertions.assertFalse(connection.isAvailable());
        } finally {
            try {
                server.close();
            } catch (Throwable e) {
                // ignored
            }
        }


    }
}
