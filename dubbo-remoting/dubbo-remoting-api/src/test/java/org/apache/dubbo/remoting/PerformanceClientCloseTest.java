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
package org.apache.dubbo.remoting;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.exchange.ExchangeClient;
import org.apache.dubbo.remoting.exchange.Exchangers;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;

/**
 * ProformanceClient
 * The test class will report abnormal thread pool, because the judgment on the thread pool concurrency problems produced in DefaultChannelHandler (connected event has been executed asynchronously, judgment, then closed the thread pool, thread pool and execution error, this problem can be specified through the Constants.CHANNEL_HANDLER_KEY=connection.)
 */
public class PerformanceClientCloseTest  {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceClientCloseTest.class);

    @Test
    public void testClient() throws Throwable {
        // read server info from property
        if (PerformanceUtils.getProperty("server", null) == null) {
            logger.warn("Please set -Dserver=127.0.0.1:9911");
            return;
        }
        final String server = System.getProperty("server", "127.0.0.1:9911");
        final String transporter = PerformanceUtils.getProperty(Constants.TRANSPORTER_KEY, Constants.DEFAULT_TRANSPORTER);
        final String serialization = PerformanceUtils.getProperty(Constants.SERIALIZATION_KEY, Constants.DEFAULT_REMOTING_SERIALIZATION);
        final int timeout = PerformanceUtils.getIntProperty(TIMEOUT_KEY, DEFAULT_TIMEOUT);
        final int concurrent = PerformanceUtils.getIntProperty("concurrent", 1);
        final int runs = PerformanceUtils.getIntProperty("runs", Integer.MAX_VALUE);
        final String onerror = PerformanceUtils.getProperty("onerror", "continue");

        final String url = "exchange://" + server + "?transporter=" + transporter
                + "&serialization=" + serialization
//            + "&"+Constants.CHANNEL_HANDLER_KEY+"=connection"
                + "&timeout=" + timeout;

        final AtomicInteger count = new AtomicInteger();
        final AtomicInteger error = new AtomicInteger();
        for (int n = 0; n < concurrent; n++) {
            new Thread(new Runnable() {
                public void run() {
                    for (int i = 0; i < runs; i++) {
                        ExchangeClient client = null;
                        try {
                            client = Exchangers.connect(url);
                            int c = count.incrementAndGet();
                            if (c % 100 == 0) {
                                System.out.println("count: " + count.get() + ", error: " + error.get());
                            }
                        } catch (Exception e) {
                            error.incrementAndGet();
                            e.printStackTrace();
                            System.out.println("count: " + count.get() + ", error: " + error.get());
                            if ("exit".equals(onerror)) {
                                System.exit(-1);
                            } else if ("break".equals(onerror)) {
                                break;
                            } else if ("sleep".equals(onerror)) {
                                try {
                                    Thread.sleep(30000);
                                } catch (InterruptedException e1) {
                                }
                            }
                        } finally {
                            if (client != null) {
                                client.close();
                            }
                        }
                    }
                }
            }).start();
        }
        synchronized (PerformanceServerTest.class) {
            while (true) {
                try {
                    PerformanceServerTest.class.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

}
