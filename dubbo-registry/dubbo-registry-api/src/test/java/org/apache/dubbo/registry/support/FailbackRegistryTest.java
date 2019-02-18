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
package org.apache.dubbo.registry.support;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FailbackRegistryTest {
    static String service;
    static URL serviceUrl;
    static URL registryUrl;
    MockRegistry registry;
    private int FAILED_PERIOD = 200;
    private int sleeptime = 100;
    private int trytimes = 5;

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        service = "org.apache.dubbo.test.DemoService";
        serviceUrl = URL.valueOf("remote://127.0.0.1/demoservice?method=get");
        registryUrl = URL.valueOf("http://1.2.3.4:9090/registry?check=false&file=N/A").addParameter(Constants.REGISTRY_RETRY_PERIOD_KEY, String.valueOf(FAILED_PERIOD));
    }

    /**
     * Test method for retry
     *
     * @throws Exception
     */
    @Test
    public void testDoRetry() throws Exception {

        final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);

        // the latest latch just for 3. Because retry method has been removed.
        final CountDownLatch latch = new CountDownLatch(2);

        NotifyListener listner = new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {
                notified.set(Boolean.TRUE);
            }
        };
        registry = new MockRegistry(registryUrl, latch);
        registry.setBad(true);
        registry.register(serviceUrl);
        registry.unregister(serviceUrl);
        registry.subscribe(serviceUrl.setProtocol(Constants.CONSUMER_PROTOCOL).addParameters(CollectionUtils.toStringMap("check", "false")), listner);
        registry.unsubscribe(serviceUrl.setProtocol(Constants.CONSUMER_PROTOCOL).addParameters(CollectionUtils.toStringMap("check", "false")), listner);

        //Failure can not be called to listener.
        assertEquals(false, notified.get());
        assertEquals(2, latch.getCount());

        registry.setBad(false);

        for (int i = 0; i < trytimes; i++) {
            System.out.println("failback registry retry ,times:" + i);
            //System.out.println(latch.getCount());
            if (latch.getCount() == 0)
                break;
            Thread.sleep(sleeptime);
        }
//        Thread.sleep(100000);//for debug
        assertEquals(0, latch.getCount());
        //The failedsubcribe corresponding key will be cleared when unsubscribing
        assertEquals(false, notified.get());
    }

    @Test
    public void testDoRetry_subscribe() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);//All of them are called 4 times. A successful attempt to lose 1. subscribe will not be done

        registry = new MockRegistry(registryUrl, latch);
        registry.setBad(true);
        registry.register(serviceUrl);

        registry.setBad(false);

        for (int i = 0; i < trytimes; i++) {
            System.out.println("failback registry retry ,times:" + i);
            if (latch.getCount() == 0)
                break;
            Thread.sleep(sleeptime);
        }
        assertEquals(0, latch.getCount());
    }

    @Test
    public void testDoRetry_register() throws Exception {

        final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
        final CountDownLatch latch = new CountDownLatch(1);//All of them are called 4 times. A successful attempt to lose 1. subscribe will not be done

        NotifyListener listner = new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {
                notified.set(Boolean.TRUE);
            }
        };
        registry = new MockRegistry(registryUrl, latch);
        registry.setBad(true);
        registry.subscribe(serviceUrl.setProtocol(Constants.CONSUMER_PROTOCOL).addParameters(CollectionUtils.toStringMap("check", "false")), listner);

        //Failure can not be called to listener.
        assertEquals(false, notified.get());
        assertEquals(1, latch.getCount());

        registry.setBad(false);

        for (int i = 0; i < trytimes; i++) {
            System.out.println("failback registry retry ,times:" + i);
            //System.out.println(latch.getCount());
            if (latch.getCount() == 0)
                break;
            Thread.sleep(sleeptime);
        }
//        Thread.sleep(100000);
        assertEquals(0, latch.getCount());
        //The failedsubcribe corresponding key will be cleared when unsubscribing
        assertEquals(true, notified.get());
    }

    @Test
    public void testDoRetry_nofify() throws Exception {

        //Initial value 0
        final AtomicInteger count = new AtomicInteger(0);

        NotifyListener listner = new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {
                count.incrementAndGet();
                //The exception is thrown for the first time to see if the back will be called again to incrementAndGet
                if (count.get() == 1l) {
                    throw new RuntimeException("test exception please ignore");
                }
            }
        };
        registry = new MockRegistry(registryUrl, new CountDownLatch(0));
        registry.subscribe(serviceUrl.setProtocol(Constants.CONSUMER_PROTOCOL).addParameters(CollectionUtils.toStringMap("check", "false")), listner);

        assertEquals(1, count.get()); //Make sure that the subscribe call has just been called once count.incrementAndGet after the call is completed
        //Wait for the timer.
        for (int i = 0; i < trytimes; i++) {
            System.out.println("failback notify retry ,times:" + i);
            if (count.get() == 2)
                break;
            Thread.sleep(sleeptime);
        }
        assertEquals(2, count.get());
    }

    @Test
    public void testRecover() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(4);
        final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
        NotifyListener listener = new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {
                notified.set(Boolean.TRUE);
            }
        };

        MockRegistry mockRegistry = new MockRegistry(registryUrl, countDownLatch);
        mockRegistry.register(serviceUrl);
        mockRegistry.subscribe(serviceUrl, listener);
        Assertions.assertEquals(1, mockRegistry.getRegistered().size());
        Assertions.assertEquals(1, mockRegistry.getSubscribed().size());
        mockRegistry.recover();
        countDownLatch.await();
        Assertions.assertEquals(0, mockRegistry.getFailedRegistered().size());
        FailbackRegistry.Holder h = new FailbackRegistry.Holder(registryUrl, listener);
        Assertions.assertEquals(null, mockRegistry.getFailedSubscribed().get(h));
        Assertions.assertEquals(countDownLatch.getCount(), 0);
    }

    private static class MockRegistry extends FailbackRegistry {
        CountDownLatch latch;
        private boolean bad = false;

        /**
         * @param url
         */
        public MockRegistry(URL url, CountDownLatch latch) {
            super(url);
            this.latch = latch;
        }

        /**
         * @param bad the bad to set
         */
        public void setBad(boolean bad) {
            this.bad = bad;
        }

        @Override
        public void doRegister(URL url) {
            if (bad) {
                throw new RuntimeException("can not invoke!");
            }
            //System.out.println("do doRegister");
            latch.countDown();

        }

        @Override
        public void doUnregister(URL url) {
            if (bad) {
                throw new RuntimeException("can not invoke!");
            }
            //System.out.println("do doUnregister");
            latch.countDown();

        }

        @Override
        public void doSubscribe(URL url, NotifyListener listener) {
            if (bad) {
                throw new RuntimeException("can not invoke!");
            }
            //System.out.println("do doSubscribe");
            super.notify(url, listener, Arrays.asList(new URL[]{serviceUrl}));
            latch.countDown();
        }

        @Override
        public void doUnsubscribe(URL url, NotifyListener listener) {
            if (bad) {
                throw new RuntimeException("can not invoke!");
            }
            //System.out.println("do doUnsubscribe");
            latch.countDown();
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

    }
}
