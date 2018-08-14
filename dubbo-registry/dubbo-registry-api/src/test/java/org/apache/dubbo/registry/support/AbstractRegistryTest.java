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
import org.apache.dubbo.common.logger.Level;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.log4j.BasicConfigurator;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AbstractRegistryTest
 */
public class AbstractRegistryTest {

    private URL testUrl;
    private NotifyListener listener;
    private AbstractRegistry abstractRegistry;
    private boolean notifySuccess;

    private static final Map<String,String> parametersProvider=new LinkedHashMap<>();
    private static final Map<String,String> parametersConsumer=new LinkedHashMap<>();

    private URL mockUrl = new URL("dubbo", "127.0.0.0", 2200);
    static{
        parametersProvider.put("anyhost","true");
        parametersProvider.put("application","demo-provider");
        parametersProvider.put("dubbo","2.0.2");
        parametersProvider.put("generic","false");
        parametersProvider.put("interface","org.apache.dubbo.demo.DemoService");
        parametersProvider.put("methods","sayHello");
        parametersProvider.put("pid","1489");
        parametersProvider.put("side","provider");
        parametersProvider.put("timestamp",String.valueOf(System.currentTimeMillis()));

        parametersConsumer.put("application", "demo-consumer");
        parametersConsumer.put("category", "consumer");
        parametersConsumer.put("check", "false");
        parametersConsumer.put("dubbo", "2.0.2");
        parametersConsumer.put("interface", "org.apache.dubbo.demo.DemoService");
        parametersConsumer.put("methods", "sayHello");
        parametersConsumer.put("pid", "1676");
        parametersConsumer.put("qos.port", "333333");
        parametersConsumer.put("side", "consumer");
        parametersConsumer.put("timestamp", String.valueOf(System.currentTimeMillis()));


    }

    private List<URL> getList() {
        List<URL> list = new ArrayList<>();
        URL url_1 = new URL("dubbo", "127.0.0.0", 1000);
        URL url_2 = new URL("dubbo", "127.0.0.1", 1001);
        URL url_3 = new URL("dubbo", "127.0.0.2", 1002);
        list.add(url_1);
        list.add(url_2);
        list.add(url_3);
        return list;
    }

    @Before
    public void init() {
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":2233");
        testUrl = URL.valueOf("http://1.2.3.4:9090/registry?check=false&file=N/A&interface=com.test");

        //init the object
        abstractRegistry = new AbstractRegistry(url) {
            @Override
            public boolean isAvailable() {
                return false;
            }
        };
        //init notify listener
        listener = urls -> notifySuccess = true;
        //notify flag
        notifySuccess = false;

        LoggerFactory.setLevel(Level.INFO);
        Level level = LoggerFactory.getLevel();
        // BasicConfigurator.configure();
    }



    @Test
    public void registerTest() {
        //check parameters
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
        //check register when the url is the same
        abstractRegistry.register(testUrl);
        Assert.assertEquals(beginSize + 1, abstractRegistry.getRegistered().size());
    }

    @Test
    public void unregisterTest() {
        //check parameters
        try {
            abstractRegistry.unregister(null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        // check if unregister url successfully
        abstractRegistry.register(testUrl);
        int beginSize = abstractRegistry.getRegistered().size();
        abstractRegistry.unregister(testUrl);
        Assert.assertEquals(beginSize - 1, abstractRegistry.getRegistered().size());
        // check if unregister a not exist url successfully
        abstractRegistry.unregister(testUrl);
        Assert.assertEquals(beginSize - 1, abstractRegistry.getRegistered().size());
    }

    @Test
    public void subscribeTest() {
        //check parameters
        try {
            abstractRegistry.subscribe(null, listener);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        //check parameters
        try {
            abstractRegistry.subscribe(testUrl, null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        //check parameters
        try {
            abstractRegistry.subscribe(null, null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        // check if subscribe successfully
        abstractRegistry.subscribe(testUrl, listener);
        Assert.assertNotNull(abstractRegistry.getSubscribed().get(testUrl));
        Assert.assertTrue(abstractRegistry.getSubscribed().get(testUrl).contains(listener));
    }

    @Test
    public void unsubscribeTest() {
        //check parameters
        try {
            abstractRegistry.unsubscribe(null, listener);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        //check parameters
        try {
            abstractRegistry.unsubscribe(testUrl, null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        //check parameters
        try {
            abstractRegistry.unsubscribe(null, null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        // check if unsubscribe successfully
        abstractRegistry.subscribe(testUrl, listener);
        abstractRegistry.unsubscribe(testUrl, listener);
        Assert.assertFalse(abstractRegistry.getSubscribed().get(testUrl).contains(listener));
    }

    @Test
    public void recoverTest() throws Exception {
        abstractRegistry.register(testUrl);
        abstractRegistry.subscribe(testUrl, listener);
        abstractRegistry.recover();
        // check if recover successfully
        Assert.assertTrue(abstractRegistry.getRegistered().contains(testUrl));
        Assert.assertNotNull(abstractRegistry.getSubscribed().get(testUrl));
        Assert.assertTrue(abstractRegistry.getSubscribed().get(testUrl).contains(listener));
    }

    @Test
    public void notifyTest() {
        abstractRegistry.subscribe(testUrl, listener);
        List<URL> urls = new ArrayList<>();
        urls.add(testUrl);
        // check if notify successfully
        Assert.assertFalse(notifySuccess);
        abstractRegistry.notify(urls);
        Assert.assertTrue(notifySuccess);
    }

    @Test
    public void notify2Test() {
        //check parameters
        try {
            abstractRegistry.notify(null, null, null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        //check parameters
        try {
            abstractRegistry.notify(testUrl, null, null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        List<URL> urls = new ArrayList<>();
        urls.add(testUrl);
        // check if notify successfully
        Assert.assertFalse(notifySuccess);
        abstractRegistry.notify(testUrl, listener, urls);
        Assert.assertTrue(notifySuccess);
    }



    /**
     * test register
     */
    @Test
    public void testRegister() {
        //test one url
        abstractRegistry.register(mockUrl);
        assert abstractRegistry.getRegistered().contains(mockUrl);
        //test multiple urls
        abstractRegistry.getRegistered().clear();
        List<URL> urlList = getList();
        for (URL url : urlList) {
            abstractRegistry.register(url);
        }
        Assert.assertThat(abstractRegistry.getRegistered().size(), Matchers.equalTo(urlList.size()));
    }

    @Test
    public void testRegisterIfURLNULL() {
        //test one url
        try {
            abstractRegistry.register(null);
            Assert.fail("register url == null");
        }catch (IllegalArgumentException illegalArgumentException){
            Assert.assertEquals("register url == null",illegalArgumentException.getMessage());
        }
    }
    /**
     * test unregister
     */
    @Test
    public void testUnregister() {
        //test one unregister
        URL url = new URL("dubbo", "127.0.0.0", 2200);
        abstractRegistry.getRegistered().add(url);
        abstractRegistry.unregister(url);
        Assert.assertThat(false, Matchers.equalTo(abstractRegistry.getRegistered().contains(url)));
        //test multiple unregisters
        abstractRegistry.getRegistered().clear();
        List<URL> urlList = getList();
        for (URL urlSub : urlList) {
            abstractRegistry.getRegistered().add(urlSub);
        }
        for (URL urlSub : urlList) {
            abstractRegistry.unregister(urlSub);
        }
        Assert.assertThat(0, Matchers.equalTo(abstractRegistry.getRegistered().size()));
    }

    @Test
    public void testUnregisterIfURLNULL() {
        try {
            abstractRegistry.unregister(null);
            Assert.fail("unregister url == null");
        }catch (IllegalArgumentException illegalArgumentException){
            Assert.assertEquals("unregister url == null",illegalArgumentException.getMessage());
        }
    }
    /**
     * test subscribe and unsubscribe
     */
    @Test
    public void testSubscribeAndUnsubscribe() {
        //test subscribe
        final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
        NotifyListener listener = urls -> notified.set(Boolean.TRUE);
        URL url = new URL("dubbo", "127.0.0.0", 2200);
        abstractRegistry.subscribe(url, listener);
        Set<NotifyListener> subscribeListeners = abstractRegistry.getSubscribed().get(url);
        Assert.assertThat(true, Matchers.equalTo(subscribeListeners.contains(listener)));
        //test unsubscribe
        abstractRegistry.unsubscribe(url, listener);
        Set<NotifyListener> unsubscribeListeners = abstractRegistry.getSubscribed().get(url);
        Assert.assertThat(false, Matchers.equalTo(unsubscribeListeners.contains(listener)));
    }
    @Test
    public void testSubscribeIfURLNull(){
        final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
        NotifyListener listener = urls -> notified.set(Boolean.TRUE);
        URL url = new URL("dubbo", "127.0.0.0", 2200);
        try {
            abstractRegistry.subscribe(null,listener);
            Assert.fail("subscribe url == null");
        }catch (IllegalArgumentException illegalArgumentException){
            Assert.assertEquals("subscribe url == null",illegalArgumentException.getMessage());
        }
    }
    @Test
    public void testSubscribeIfListenerNull(){
        final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
        NotifyListener listener = urls -> notified.set(Boolean.TRUE);
        URL url = new URL("dubbo", "127.0.0.0", 2200);
        try {
            abstractRegistry.subscribe(url,null);
            Assert.fail("listener url == null");
        }catch (IllegalArgumentException illegalArgumentException){
            Assert.assertEquals("subscribe listener == null",illegalArgumentException.getMessage());
            return;
        }
    }

    @Test
    public void testUnsubscribeIfURLNull(){
        final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
        NotifyListener listener = urls -> notified.set(Boolean.TRUE);
        URL url = new URL("dubbo", "127.0.0.0", 2200);
        try {
            abstractRegistry.unsubscribe(null,listener);
            Assert.fail("unsubscribe url == null");
        }catch (IllegalArgumentException illegalArgumentException){
            Assert.assertEquals("unsubscribe url == null",illegalArgumentException.getMessage());
        }
    }

    @Test
    public void testUnsubscribeIfNotifyNull(){
        final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
        NotifyListener listener = urls -> notified.set(Boolean.TRUE);
        URL url = new URL("dubbo", "127.0.0.0", 2200);
        try {
            abstractRegistry.unsubscribe(url,null);
            Assert.fail("unsubscribe listener == null");
        }catch (IllegalArgumentException illegalArgumentException){
            Assert.assertEquals("unsubscribe listener == null",illegalArgumentException.getMessage());
        }
    }
    /**
     * test recover
     */
    @Test
    public void testRecover() {
        List<URL> list = getList();
        try {
            abstractRegistry.recover();
            Assert.assertEquals(0, abstractRegistry.getRegistered().size());
            for (URL url : list) {
                abstractRegistry.register(url);
            }
            Assert.assertEquals(3, abstractRegistry.getRegistered().size());
            abstractRegistry.recover();
            Assert.assertEquals(3, abstractRegistry.getRegistered().size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * test notifyList
     */
    @Test
    public void testNotifyList() {
        final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
        NotifyListener listner1 = urls -> notified.set(Boolean.TRUE);
        URL url1 = new URL("dubbo", "127.0.0.0", 2200, parametersConsumer);
        abstractRegistry.subscribe(url1, listner1);
        NotifyListener listner2 = urls -> notified.set(Boolean.TRUE);
        URL url2 = new URL("dubbo", "127.0.0.1", 2201, parametersConsumer);
        abstractRegistry.subscribe(url2, listner2);
        NotifyListener listner3 = urls -> notified.set(Boolean.TRUE);
        URL url3 = new URL("dubbo", "127.0.0.2", 2202, parametersConsumer);
        abstractRegistry.subscribe(url3, listner3);
        List<URL> urls = new ArrayList<>();
        urls.add(url1);
        urls.add(url2);
        urls.add(url3);
        abstractRegistry.notify(urls);
        Map<URL, Map<String, List<URL>>> map = abstractRegistry.getNotified();
        Assert.assertThat(true, Matchers.equalTo(map.containsKey(url1)));
        Assert.assertThat(true, Matchers.equalTo(map.containsKey(url2)));
        Assert.assertThat(true, Matchers.equalTo(map.containsKey(url3)));
    }
    /**
     * test notify
     */
    @Test
    public void testNotify() {
        final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
        NotifyListener listner1 = urls -> notified.set(Boolean.TRUE);
        URL url1 = new URL("dubbo", "127.0.0.0", 2200, parametersConsumer);
        abstractRegistry.subscribe(url1, listner1);
        NotifyListener listner2 = urls -> notified.set(Boolean.TRUE);
        URL url2 = new URL("dubbo", "127.0.0.1", 2201, parametersConsumer);
        abstractRegistry.subscribe(url2, listner2);
        NotifyListener listner3 = urls -> notified.set(Boolean.TRUE);
        URL url3 = new URL("dubbo", "127.0.0.2", 2202, parametersConsumer);
        abstractRegistry.subscribe(url3, listner3);
        List<URL> urls = new ArrayList<>();
        urls.add(url1);
        urls.add(url2);
        urls.add(url3);
        abstractRegistry.notify(url1, listner1, urls);
        Map<URL, Map<String, List<URL>>> map = abstractRegistry.getNotified();
        Assert.assertThat(true, Matchers.equalTo(map.containsKey(url1)));
        Assert.assertThat(false, Matchers.equalTo(map.containsKey(url2)));
        Assert.assertThat(false, Matchers.equalTo(map.containsKey(url3)));
    }

    @Test
    public void testNotifyIfURLNULL() {
        final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
        NotifyListener listner1 = urls -> notified.set(Boolean.TRUE);
        URL url1 = new URL("dubbo", "127.0.0.0", 2200, parametersConsumer);
        abstractRegistry.subscribe(url1, listner1);
        NotifyListener listner2 = urls -> notified.set(Boolean.TRUE);
        URL url2 = new URL("dubbo", "127.0.0.1", 2201, parametersConsumer);
        abstractRegistry.subscribe(url2, listner2);
        NotifyListener listner3 = urls -> notified.set(Boolean.TRUE);
        URL url3 = new URL("dubbo", "127.0.0.2", 2202, parametersConsumer);
        abstractRegistry.subscribe(url3, listner3);
        List<URL> urls = new ArrayList<>();
        urls.add(url1);
        urls.add(url2);
        urls.add(url3);
        try {
            abstractRegistry.notify(null, listner1, urls);
            Assert.fail("notify url == null");
        }catch (IllegalArgumentException illegalArgumentException){
            Assert.assertEquals("notify url == null",illegalArgumentException.getMessage());
        }
    }

    @Test
    public void testNotifyIfNotifyNULL() {
        final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
        NotifyListener listner1 = urls -> notified.set(Boolean.TRUE);
        URL url1 = new URL("dubbo", "127.0.0.0", 2200, parametersConsumer);
        abstractRegistry.subscribe(url1, listner1);
        NotifyListener listner2 = urls -> notified.set(Boolean.TRUE);
        URL url2 = new URL("dubbo", "127.0.0.1", 2201, parametersConsumer);
        abstractRegistry.subscribe(url2, listner2);
        NotifyListener listner3 = urls -> notified.set(Boolean.TRUE);
        URL url3 = new URL("dubbo", "127.0.0.2", 2202, parametersConsumer);
        abstractRegistry.subscribe(url3, listner3);
        List<URL> urls = new ArrayList<>();
        urls.add(url1);
        urls.add(url2);
        urls.add(url3);
        try {
            abstractRegistry.notify(url1, null, urls);
            Assert.fail("notify listener == null");
        }catch (IllegalArgumentException illegalArgumentException){
            Assert.assertEquals("notify listener == null",illegalArgumentException.getMessage());
        }
    }



}