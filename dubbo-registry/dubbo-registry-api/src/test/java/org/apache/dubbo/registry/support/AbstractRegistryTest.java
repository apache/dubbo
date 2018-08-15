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
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.common.Constants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * AbstractRegistryTest
 */
public class AbstractRegistryTest {

    private URL testUrl;
    private NotifyListener listener;
    private AbstractRegistry abstractRegistry;
    private boolean notifySuccess;
    private int threadNumber;

    @Before
    public void init() {
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":2233");
        testUrl = URL.valueOf("http://1.2.3.4:9090/registry?check=false&file=N/A&interface=com.test");
        threadNumber = 100;

        // init the object
        abstractRegistry = new AbstractRegistry(url) {
            @Override
            public boolean isAvailable() {
                return false;
            }
        };
        // init notify listener
        listener = urls -> notifySuccess = true;
        // notify flag
        notifySuccess = false;
    }

    @After
    public void after() {
        abstractRegistry.destroy();
    }

    /**
     * Test method for
     * {@link org.apache.dubbo.registry.support.AbstractRegistry#register(URL)}.
     *
     * @throws Exception
     */
    @Test
    public void testRegister() {
        // check parameters
        try {
            abstractRegistry.register(null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        // check if register successfully
        int beginSize = abstractRegistry.getRegistered().size();
        abstractRegistry.register(testUrl);
        Assert.assertEquals(beginSize + 1, abstractRegistry.getRegistered().size());
        // check register when the url is the same
        abstractRegistry.register(testUrl);
        Assert.assertEquals(beginSize + 1, abstractRegistry.getRegistered().size());
    }

    /**
     * Multi thread test method for
     * {@link org.apache.dubbo.registry.support.AbstractRegistry#register(URL)}.
     *
     * @throws Exception
     */
    @Test
    public void testRegisterMultiThread() {
        int threadBeginSize = abstractRegistry.getRegistered().size();
        CountDownLatch countDownLatch = new CountDownLatch(threadNumber);
        Thread[] threads = new Thread[threadNumber];
        for (int i = 0; i < threadNumber; i++){
            URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + i);
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    abstractRegistry.register(url);
                    countDownLatch.countDown();
                }
            });
        }
        for (int i = 0; i < threadNumber; i++){
            threads[i].run();
        }

        try {
            // wait for all thread done
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(threadBeginSize + threadNumber, abstractRegistry.getRegistered().size());
    }

    /**
     * Test method for
     * {@link org.apache.dubbo.registry.support.AbstractRegistry#unregister(URL)}.
     *
     * @throws Exception
     */
    @Test
    public void testUnregister() {
        // check parameters
        try {
            abstractRegistry.unregister(null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        int beginSize = abstractRegistry.getRegistered().size();
        // test unregister before register
        abstractRegistry.unregister(testUrl);
        Assert.assertEquals(beginSize, abstractRegistry.getRegistered().size());

        // test register then unregister
        abstractRegistry.register(testUrl);
        Assert.assertEquals(beginSize + 1, abstractRegistry.getRegistered().size());
        abstractRegistry.unregister(testUrl);
        Assert.assertEquals(beginSize, abstractRegistry.getRegistered().size());
        // test double unregister after register
        abstractRegistry.unregister(testUrl);
        Assert.assertEquals(beginSize, abstractRegistry.getRegistered().size());
    }

    /**
     * Multi thread test method for
     * {@link org.apache.dubbo.registry.support.AbstractRegistry#unregister(URL)}.
     *
     * @throws Exception
     */
    @Test
    public void testUnregisterMultiThread() {
        int threadBeginSize = abstractRegistry.getRegistered().size();
        CountDownLatch countDownLatch = new CountDownLatch(threadNumber);
        Thread[] threads = new Thread[threadNumber];

        for(int i = 0; i < threadNumber; i++){
            URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + i);
            abstractRegistry.register(url);
        }
        Assert.assertEquals(threadBeginSize + threadNumber, abstractRegistry.getRegistered().size());

        // unregister by multi thread
        for (int i = 0; i < threadNumber; i++){
            URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + i);
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    abstractRegistry.unregister(url);
                    countDownLatch.countDown();
                }
            });
        }
        for(int i = 0; i < threadNumber; i++){
            threads[i].run();
        }

        try {
            // wait for all thread done
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(threadBeginSize, abstractRegistry.getRegistered().size());
    }

    /**
     * Test method for
     * {@link org.apache.dubbo.registry.support.AbstractRegistry#subscribe(URL, NotifyListener)}.
     *
     * @throws Exception
     */
    @Test
    public void testSubscribe() {
        // check parameters
        try {
            abstractRegistry.subscribe(null, listener);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        // check parameters
        try {
            abstractRegistry.subscribe(testUrl, null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        // check parameters
        try {
            abstractRegistry.subscribe(null, null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        // check if subscribe successfully
        Assert.assertNull(abstractRegistry.getSubscribed().get(testUrl));
        abstractRegistry.subscribe(testUrl, listener);
        Assert.assertNotNull(abstractRegistry.getSubscribed().get(testUrl));
        Assert.assertTrue(abstractRegistry.getSubscribed().get(testUrl).contains(listener));
    }

    /**
     * Multi thread test method for
     * {@link org.apache.dubbo.registry.support.AbstractRegistry#subscribe(URL, NotifyListener)}.
     *
     * @throws Exception
     */
    @Test
    public void testSubscribeMultiThread() {
        CountDownLatch countDownLatch = new CountDownLatch(threadNumber);
        Thread[] threads = new Thread[threadNumber];
        NotifyListener[] listeners = new NotifyListener[threadNumber];
        for (int i = 0; i<threadNumber; i++) {
            listeners[i] = urls->notifySuccess = true;
        }
        for (int i = 0; i<threadNumber; i++) {
            NotifyListener notifyListener = listeners[i];
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    abstractRegistry.subscribe(testUrl, notifyListener);
                    countDownLatch.countDown();
                }
            });
            threads[i].run();
        }

        try {
            // wait for all thread done
            countDownLatch.await();
            for (int i = 0; i < threadNumber; i++) {
                Assert.assertNotNull(abstractRegistry.getSubscribed().get(testUrl));
                Assert.assertTrue(abstractRegistry.getSubscribed().get(testUrl).contains(listeners[i]));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test method for
     * {@link org.apache.dubbo.registry.support.AbstractRegistry#unsubscribe(URL, NotifyListener)}.
     *
     * @throws Exception
     */
    @Test
    public void testUnsubscribe() {
        // check parameters
        try {
            abstractRegistry.unsubscribe(null, listener);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        // check parameters
        try {
            abstractRegistry.unsubscribe(testUrl, null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        // check parameters
        try {
            abstractRegistry.unsubscribe(null, null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        Assert.assertNull(abstractRegistry.getSubscribed().get(testUrl));
        // check if unsubscribe successfully
        abstractRegistry.subscribe(testUrl, listener);
        abstractRegistry.unsubscribe(testUrl, listener);
        // Since we have subscribe testUrl, here should return a empty set instead of null
        Assert.assertNotNull(abstractRegistry.getSubscribed().get(testUrl));
        Assert.assertFalse(abstractRegistry.getSubscribed().get(testUrl).contains(listener));
    }

    /**
     * Multi thread test method for
     * {@link org.apache.dubbo.registry.support.AbstractRegistry#unsubscribe(URL, NotifyListener)}.
     *
     * @throws Exception
     */
    @Test
    public void testUnsubscribeMultiThread() {
        CountDownLatch countDownLatch = new CountDownLatch(threadNumber);
        Thread[] threads = new Thread[threadNumber];
        NotifyListener[] listeners = new NotifyListener[threadNumber];
        for (int i = 0; i < threadNumber; i++) {
            listeners[i] = urls->notifySuccess=true;
            abstractRegistry.subscribe(testUrl, listeners[i]);
        }
        for (int i = 0; i < threadNumber; i++){
            NotifyListener notifyListener = listeners[i];
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    abstractRegistry.unsubscribe(testUrl, notifyListener);
                    countDownLatch.countDown();
                }
            });
            threads[i].run();
        }

        try {
            // wait for all thread done
            countDownLatch.await();
            for(int i = 0; i < threadNumber; i++) {
                Assert.assertFalse(abstractRegistry.getSubscribed().get(testUrl).contains(listener));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test method for
     * {@link org.apache.dubbo.registry.support.AbstractRegistry#recover()}.
     */
    @Test
    public void testRecover() throws Exception {
        // test recover nothing
        abstractRegistry.recover();
        Assert.assertFalse(abstractRegistry.getRegistered().contains(testUrl));
        Assert.assertNull(abstractRegistry.getSubscribed().get(testUrl));

        // test recover
        abstractRegistry.register(testUrl);
        abstractRegistry.subscribe(testUrl, listener);
        abstractRegistry.recover();
        // check if recover successfully
        Assert.assertTrue(abstractRegistry.getRegistered().contains(testUrl));
        Assert.assertNotNull(abstractRegistry.getSubscribed().get(testUrl));
        Assert.assertTrue(abstractRegistry.getSubscribed().get(testUrl).contains(listener));
    }

    /**
     * Test method for
     * {@link org.apache.dubbo.registry.support.AbstractRegistry#notify(List)}.
     */
    @Test
    public void testNotify() {
        abstractRegistry.subscribe(testUrl, listener);
        // check parameters
        Assert.assertFalse(notifySuccess);
        abstractRegistry.notify(null);
        Assert.assertFalse(notifySuccess);

        List<URL> urls = new ArrayList<>();
        Assert.assertFalse(notifySuccess);
        abstractRegistry.notify(urls);
        Assert.assertFalse(notifySuccess);

        urls.add(testUrl);
        // check if notify successfully
        Assert.assertFalse(notifySuccess);
        abstractRegistry.notify(urls);
        Assert.assertTrue(notifySuccess);
    }

    /**
     * Test method for
     * {@link org.apache.dubbo.registry.support.AbstractRegistry#notify(URL, NotifyListener, List)}.
     *
     * @throws Exception
     */
    @Test
    public void testNotifyThreeArgs() {
        // check parameters
        try {
            abstractRegistry.notify(null, null, null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        // check parameters
        try {
            abstractRegistry.notify(testUrl, null, null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        // check parameters
        try {
            abstractRegistry.notify(null, listener, null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        Assert.assertFalse(notifySuccess);
        abstractRegistry.notify(testUrl, listener, null);
        Assert.assertFalse(notifySuccess);

        List<URL> urls = new ArrayList<>();
        urls.add(testUrl);
        // check if notify successfully
        Assert.assertFalse(notifySuccess);
        abstractRegistry.notify(testUrl, listener, urls);
        Assert.assertTrue(notifySuccess);
    }

    @Test
    public void filterEmptyTest() {
        // check parameters
        try {
            AbstractRegistry.filterEmpty(null, null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NullPointerException);
        }

        // check parameters
        List<URL> urls = new ArrayList<>();
        try {
            AbstractRegistry.filterEmpty(null, urls);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NullPointerException);
        }

        // check if the output is generated by a fixed way
        urls.add(testUrl.setProtocol(Constants.EMPTY_PROTOCOL));
        Assert.assertEquals(AbstractRegistry.filterEmpty(testUrl, null), urls);

        List<URL> testUrls = new ArrayList<>();
        Assert.assertEquals(AbstractRegistry.filterEmpty(testUrl, testUrls), urls);

        // check if the output equals the input urls
        testUrls.add(testUrl);
        Assert.assertEquals(AbstractRegistry.filterEmpty(testUrl, testUrls), testUrls);

    }


    @Test
    public void lookupTest(){
        // loop up before registry
        try {
            abstractRegistry.lookup(null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NullPointerException);
        }
        List<URL> urlList1 = abstractRegistry.lookup(testUrl);
        Assert.assertFalse(urlList1.contains(testUrl));
        // loop up after registry
        List<URL> urls = new ArrayList<>();
        urls.add(testUrl);
        abstractRegistry.notify(urls);
        List<URL> urlList2 = abstractRegistry.lookup(testUrl);
        Assert.assertTrue(urlList2.contains(testUrl));

    }

    @Test
    public void destroyTest(){
        abstractRegistry.register(testUrl);
        abstractRegistry.subscribe(testUrl, listener);
        Assert.assertEquals(1,abstractRegistry.getRegistered().size());
        Assert.assertEquals(1,abstractRegistry.getSubscribed().get(testUrl).size());
        // delete listener and register
        abstractRegistry.destroy();
        Assert.assertEquals(0,abstractRegistry.getRegistered().size());
        Assert.assertEquals(0,abstractRegistry.getSubscribed().get(testUrl).size());
    }

    @Test
    public void allTest(){
        // test all methods
        List<URL> urls = new ArrayList<>();
        urls.add(testUrl);
        // register, subscribe, notify, unsubscribe, unregister
        abstractRegistry.register(testUrl);
        Assert.assertTrue(abstractRegistry.getRegistered().contains(testUrl));
        abstractRegistry.subscribe(testUrl,listener);
        Assert.assertTrue(abstractRegistry.getSubscribed().containsKey(testUrl));
        Assert.assertFalse(notifySuccess);
        abstractRegistry.notify(urls);
        Assert.assertTrue(notifySuccess);
        abstractRegistry.unsubscribe(testUrl,listener);
        Assert.assertFalse(abstractRegistry.getSubscribed().containsKey(listener));
        abstractRegistry.unregister(testUrl);
        Assert.assertFalse(abstractRegistry.getRegistered().contains(testUrl));
    }
}