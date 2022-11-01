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
package org.apache.dubbo.remoting.transport.netty4;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.remoting.api.pu.DefaultPuHandler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ConnectionTest {
    @Test
    void connectSyncTest() throws Throwable {
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("empty://127.0.0.1:" + port + "?foo=bar");
        NettyPortUnificationServer server = null;
        try {
            server = new NettyPortUnificationServer(url, new DefaultPuHandler());
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

    @Test
    void testMultiConnect() throws Throwable {
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("empty://127.0.0.1:" + port + "?foo=bar");
        NettyPortUnificationServer server = null;
        ExecutorService service = Executors.newFixedThreadPool(10);

        try {
            server = new NettyPortUnificationServer(url, new DefaultPuHandler());

            Connection connection = new Connection(url);

            final CountDownLatch latch = new CountDownLatch(10);
            for (int i = 0; i < 10; i++) {
                Runnable runnable = () -> {
                    try {
                        Assertions.assertTrue(connection.isAvailable());
                    } catch (Exception e) {
                        // ignore
                    } finally {
                        latch.countDown();
                    }
                };

                service.execute(runnable);
            }

            latch.await();
        } finally {
            try {
                server.close();
            } catch (Throwable e) {
                // ignored
            }

            service.shutdown();
        }
    }
}
