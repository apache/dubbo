/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.remoting;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.remoting.exchange.Exchangers;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * ProformanceClient
 * 这个测试类会报线程池的异常，因为DefaultChannelHandler中关于线程池的判断产生并发问题（connected事件异步执行，判断已经过了，这时关闭了线程池，然后线程池执行，报错，此问题通过指定Constants.CHANNEL_HANDLER_KEY=connection即可.）
 *
 * @author william.liangf
 */
public class PerformanceClientCloseTest extends TestCase {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceClientCloseTest.class);

    @Test
    public void testClient() throws Throwable {
        // 读取参数
        if (PerformanceUtils.getProperty("server", null) == null) {
            logger.warn("Please set -Dserver=127.0.0.1:9911");
            return;
        }
        final String server = System.getProperty("server", "127.0.0.1:9911");
        final String transporter = PerformanceUtils.getProperty(Constants.TRANSPORTER_KEY, Constants.DEFAULT_TRANSPORTER);
        final String serialization = PerformanceUtils.getProperty(Constants.SERIALIZATION_KEY, Constants.DEFAULT_REMOTING_SERIALIZATION);
        final int timeout = PerformanceUtils.getIntProperty(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
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