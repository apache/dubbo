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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.registry.NotifyListener;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.dubbo.registry.Constants.CONSUMER_PROTOCOL;
import static org.apache.dubbo.registry.Constants.REGISTRY_RETRY_PERIOD_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FailbackRegistryTest {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private URL serviceUrl;
    private URL registryUrl;
    private MockRegistry registry;
    private final int FAILED_PERIOD = 200;
    private final int sleepTime = 100;
    private final int tryTimes = 5;

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        String failedPeriod = String.valueOf(FAILED_PERIOD);
        serviceUrl = URL.valueOf("remote://127.0.0.1/demoservice?method=get").addParameter(REGISTRY_RETRY_PERIOD_KEY, failedPeriod);
        registryUrl = URL.valueOf("http://1.2.3.4:9090/registry?check=false&file=N/A").addParameter(REGISTRY_RETRY_PERIOD_KEY, failedPeriod);
    }

    /**
     * Test method for retry
     *
     * @throws Exception
     */
    @Test
    void testDoRetry() throws Exception {

        final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);

        // the latest latch just for 3. Because retry method has been removed.
        final CountDownLatch latch = new CountDownLatch(2);

        NotifyListener listener = urls -> notified.set(Boolean.TRUE);
        URL subscribeUrl = serviceUrl.setProtocol(CONSUMER_PROTOCOL).addParameters(CollectionUtils.toStringMap("check", "false"));
        registry = new MockRegistry(registryUrl, serviceUrl, latch);
        registry.setBad(true);
        registry.register(serviceUrl);
        registry.unregister(serviceUrl);
        registry.subscribe(subscribeUrl, listener);
        registry.unsubscribe(subscribeUrl, listener);

        //Failure can not be called to listener.
        assertEquals(false, notified.get());
        assertEquals(2, latch.getCount());

        registry.setBad(false);

        for (int i = 0; i < 20; i++) {
            logger.info("failback registry retry, times:" + i);
            //System.out.println(latch.getCount());
            if (latch.getCount() == 0)
                break;
            Thread.sleep(sleepTime);
        }
        assertEquals(0, latch.getCount());
        //The failed subscribe corresponding key will be cleared when unsubscribing
        assertEquals(false, notified.get());
    }

    @Test
    void testDoRetryRegister() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);//All of them are called 4 times. A successful attempt to lose 1. subscribe will not be done

        registry = new MockRegistry(registryUrl, serviceUrl, latch);
        registry.setBad(true);
        registry.register(serviceUrl);

        registry.setBad(false);

        for (int i = 0; i < tryTimes; i++) {
            System.out.println("failback registry retry ,times:" + i);
            if (latch.getCount() == 0)
                break;
            Thread.sleep(sleepTime);
        }
        assertEquals(0, latch.getCount());
    }

    @Test
    void testDoRetrySubscribe() throws Exception {

        final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
        final CountDownLatch latch = new CountDownLatch(1);//All of them are called 4 times. A successful attempt to lose 1. subscribe will not be done

        NotifyListener listener = urls -> notified.set(Boolean.TRUE);
        registry = new MockRegistry(registryUrl, serviceUrl, latch);
        registry.setBad(true);
        registry.subscribe(serviceUrl.setProtocol(CONSUMER_PROTOCOL).addParameters(CollectionUtils.toStringMap("check", "false")), listener);

        //Failure can not be called to listener.
        assertEquals(false, notified.get());
        assertEquals(1, latch.getCount());

        registry.setBad(false);

        for (int i = 0; i < tryTimes; i++) {
            System.out.println("failback registry retry ,times:" + i);
            if (latch.getCount() == 0)
                break;
            Thread.sleep(sleepTime);
        }
        assertEquals(0, latch.getCount());
        //The failed subscribe corresponding key will be cleared when unsubscribing
        assertEquals(true, notified.get());
    }

    @Test
    void testRecover() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(6);
        final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
        NotifyListener listener = urls -> notified.set(Boolean.TRUE);

        MockRegistry mockRegistry = new MockRegistry(registryUrl, serviceUrl, countDownLatch);
        mockRegistry.register(serviceUrl);
        mockRegistry.subscribe(serviceUrl, listener);
        Assertions.assertEquals(1, mockRegistry.getRegistered().size());
        Assertions.assertEquals(1, mockRegistry.getSubscribed().size());
        mockRegistry.recover();
        countDownLatch.await();
        Assertions.assertEquals(0, mockRegistry.getFailedRegistered().size());
        FailbackRegistry.Holder h = new FailbackRegistry.Holder(registryUrl, listener);
        Assertions.assertNull(mockRegistry.getFailedSubscribed().get(h));
        Assertions.assertEquals(countDownLatch.getCount(), 0);
    }

    private static class MockRegistry extends FailbackRegistry {
        private final URL serviceUrl;
        CountDownLatch latch;
        private volatile boolean bad = false;

        /**
         * @param url
         * @param serviceUrl
         */
        public MockRegistry(URL url, URL serviceUrl, CountDownLatch latch) {
            super(url);
            this.serviceUrl = serviceUrl;
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
            latch.countDown();

        }

        @Override
        public void doUnregister(URL url) {
            if (bad) {
                throw new RuntimeException("can not invoke!");
            }
            latch.countDown();

        }

        @Override
        public void doSubscribe(URL url, NotifyListener listener) {
            if (bad) {
                throw new RuntimeException("can not invoke!");
            }
            super.notify(url, listener, Arrays.asList(new URL[]{serviceUrl}));
            latch.countDown();
        }

        @Override
        public void doUnsubscribe(URL url, NotifyListener listener) {
            if (bad) {
                throw new RuntimeException("can not invoke!");
            }
            latch.countDown();
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public void removeFailedRegisteredTask(URL url) {
            if (bad) {
                throw new RuntimeException("can not invoke!");
            }
            super.removeFailedRegisteredTask(url);
            latch.countDown();
        }

        @Override
        public void removeFailedSubscribedTask(URL url, NotifyListener listener) {
            if (bad) {
                throw new RuntimeException("can not invoke!");
            }
            super.removeFailedSubscribedTask(url, listener);
            latch.countDown();
        }

    }
}
