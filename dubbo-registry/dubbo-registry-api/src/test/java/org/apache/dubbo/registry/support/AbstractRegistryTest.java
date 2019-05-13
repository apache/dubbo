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
import org.apache.dubbo.registry.NotifyListener;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;

/**
 * AbstractRegistryTest
 */
public class AbstractRegistryTest {

    private URL testUrl;
    private URL mockUrl;
    private NotifyListener listener;
    private AbstractRegistry abstractRegistry;
    private boolean notifySuccess;
    private Map<String, String> parametersConsumer = new LinkedHashMap<>();

    @BeforeEach
    public void init() {
        URL url = URL.valueOf("dubbo://192.168.0.2:2233");
        //sync update cache file
        url = url.addParameter("save.file", true);
        testUrl = URL.valueOf("http://192.168.0.3:9090/registry?check=false&file=N/A&interface=com.test");
        mockUrl = new URL("dubbo", "192.168.0.1", 2200);

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

    @AfterEach
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
    public void testRegister() throws Exception {
        //test one url
        abstractRegistry.register(mockUrl);
        assert abstractRegistry.getRegistered().contains(mockUrl);
        //test multiple urls
        for (URL url : abstractRegistry.getRegistered()) {
            abstractRegistry.unregister(url);
        }
        List<URL> urlList = getList();
        for (URL url : urlList) {
            abstractRegistry.register(url);
        }
        MatcherAssert.assertThat(abstractRegistry.getRegistered().size(), Matchers.equalTo(urlList.size()));
    }

    @Test
    public void testRegisterIfURLNULL() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            abstractRegistry.register(null);
            Assertions.fail("register url == null");
        });
    }

    /**
     * Test method for
     * {@link org.apache.dubbo.registry.support.AbstractRegistry#unregister(URL)}.
     *
     * @throws Exception
     */
    @Test
    public void testUnregister() throws Exception {
        //test one unregister
        URL url = new URL("dubbo", "192.168.0.1", 2200);
        abstractRegistry.register(url);
        abstractRegistry.unregister(url);
        MatcherAssert.assertThat(false, Matchers.equalTo(abstractRegistry.getRegistered().contains(url)));
        //test multiple unregisters
        for (URL u : abstractRegistry.getRegistered()) {
            abstractRegistry.unregister(u);
        }
        List<URL> urlList = getList();
        for (URL urlSub : urlList) {
            abstractRegistry.register(urlSub);
        }
        for (URL urlSub : urlList) {
            abstractRegistry.unregister(urlSub);
        }
        MatcherAssert.assertThat(0, Matchers.equalTo(abstractRegistry.getRegistered().size()));
    }

    @Test
    public void testUnregisterIfUrlNull() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            abstractRegistry.unregister(null);
            Assertions.fail("unregister url == null");
        });
    }

    /**
     * test subscribe and unsubscribe
     */
    @Test
    public void testSubscribeAndUnsubscribe() throws Exception {
        //test subscribe
        final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
        NotifyListener listener = urls -> notified.set(Boolean.TRUE);
        URL url = new URL("dubbo", "192.168.0.1", 2200);
        abstractRegistry.subscribe(url, listener);
        Set<NotifyListener> subscribeListeners = abstractRegistry.getSubscribed().get(url);
        MatcherAssert.assertThat(true, Matchers.equalTo(subscribeListeners.contains(listener)));
        //test unsubscribe
        abstractRegistry.unsubscribe(url, listener);
        Set<NotifyListener> unsubscribeListeners = abstractRegistry.getSubscribed().get(url);
        MatcherAssert.assertThat(false, Matchers.equalTo(unsubscribeListeners.contains(listener)));
    }

    @Test
    public void testSubscribeIfUrlNull() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
            NotifyListener listener = urls -> notified.set(Boolean.TRUE);
            URL url = new URL("dubbo", "192.168.0.1", 2200);
            abstractRegistry.subscribe(null, listener);
            Assertions.fail("subscribe url == null");
        });
    }

    @Test
    public void testSubscribeIfListenerNull() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
            NotifyListener listener = urls -> notified.set(Boolean.TRUE);
            URL url = new URL("dubbo", "192.168.0.1", 2200);
            abstractRegistry.subscribe(url, null);
            Assertions.fail("listener url == null");
        });
    }

    @Test
    public void testUnsubscribeIfUrlNull() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
            NotifyListener listener = urls -> notified.set(Boolean.TRUE);
            abstractRegistry.unsubscribe(null, listener);
            Assertions.fail("unsubscribe url == null");
        });
    }

    @Test
    public void testUnsubscribeIfNotifyNull() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
            URL url = new URL("dubbo", "192.168.0.1", 2200);
            abstractRegistry.unsubscribe(url, null);
            Assertions.fail("unsubscribe listener == null");
        });
    }

    /**
     * Test method for
     * {@link org.apache.dubbo.registry.support.AbstractRegistry#subscribe(URL, NotifyListener)}.
     *
     * @throws Exception
     */
    @Test
    public void testSubscribe() throws Exception {
        // check parameters
        try {
            abstractRegistry.subscribe(testUrl, null);
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }
        // check parameters
        try {
            abstractRegistry.subscribe(null, null);
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }
        // check if subscribe successfully
        Assertions.assertNull(abstractRegistry.getSubscribed().get(testUrl));
        abstractRegistry.subscribe(testUrl, listener);
        Assertions.assertNotNull(abstractRegistry.getSubscribed().get(testUrl));
        Assertions.assertTrue(abstractRegistry.getSubscribed().get(testUrl).contains(listener));
    }

    /**
     * Test method for
     * {@link org.apache.dubbo.registry.support.AbstractRegistry#unsubscribe(URL, NotifyListener)}.
     *
     * @throws Exception
     */
    @Test
    public void testUnsubscribe() throws Exception {
        // check parameters
        try {
            abstractRegistry.unsubscribe(testUrl, null);
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }
        // check parameters
        try {
            abstractRegistry.unsubscribe(null, null);
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }

        Assertions.assertNull(abstractRegistry.getSubscribed().get(testUrl));
        // check if unsubscribe successfully
        abstractRegistry.subscribe(testUrl, listener);
        abstractRegistry.unsubscribe(testUrl, listener);
        // Since we have subscribe testUrl, here should return a empty set instead of null
        Assertions.assertNotNull(abstractRegistry.getSubscribed().get(testUrl));
        Assertions.assertFalse(abstractRegistry.getSubscribed().get(testUrl).contains(listener));
    }

    /**
     * Test method for
     * {@link org.apache.dubbo.registry.support.AbstractRegistry#recover()}.
     */
    @Test
    public void testRecover() throws Exception {
        // test recover nothing
        abstractRegistry.recover();
        Assertions.assertFalse(abstractRegistry.getRegistered().contains(testUrl));
        Assertions.assertNull(abstractRegistry.getSubscribed().get(testUrl));

        // test recover
        abstractRegistry.register(testUrl);
        abstractRegistry.subscribe(testUrl, listener);
        abstractRegistry.recover();
        // check if recover successfully
        Assertions.assertTrue(abstractRegistry.getRegistered().contains(testUrl));
        Assertions.assertNotNull(abstractRegistry.getSubscribed().get(testUrl));
        Assertions.assertTrue(abstractRegistry.getSubscribed().get(testUrl).contains(listener));

    }

    @Test
    public void testRecover2() throws Exception {
        List<URL> list = getList();
        abstractRegistry.recover();
        Assertions.assertEquals(0, abstractRegistry.getRegistered().size());
        for (URL url : list) {
            abstractRegistry.register(url);
        }
        Assertions.assertEquals(3, abstractRegistry.getRegistered().size());
        abstractRegistry.recover();
        Assertions.assertEquals(3, abstractRegistry.getRegistered().size());
    }

    /**
     * Test method for
     * {@link org.apache.dubbo.registry.support.AbstractRegistry#notify(List)}.
     */
    @Test
    public void testNotify() throws Exception {
        final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
        NotifyListener listener1 = urls -> notified.set(Boolean.TRUE);
        URL url1 = new URL("dubbo", "192.168.0.1", 2200, parametersConsumer);
        abstractRegistry.subscribe(url1, listener1);
        NotifyListener listener2 = urls -> notified.set(Boolean.TRUE);
        URL url2 = new URL("dubbo", "192.168.0.2", 2201, parametersConsumer);
        abstractRegistry.subscribe(url2, listener2);
        NotifyListener listener3 = urls -> notified.set(Boolean.TRUE);
        URL url3 = new URL("dubbo", "192.168.0.3", 2202, parametersConsumer);
        abstractRegistry.subscribe(url3, listener3);
        List<URL> urls = new ArrayList<>();
        urls.add(url1);
        urls.add(url2);
        urls.add(url3);
        abstractRegistry.notify(url1, listener1, urls);
        Map<URL, Map<String, List<URL>>> map = abstractRegistry.getNotified();
        MatcherAssert.assertThat(true, Matchers.equalTo(map.containsKey(url1)));
        MatcherAssert.assertThat(false, Matchers.equalTo(map.containsKey(url2)));
        MatcherAssert.assertThat(false, Matchers.equalTo(map.containsKey(url3)));
    }

    /**
     * test notifyList
     */
    @Test
    public void testNotifyList() throws Exception {
        final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
        NotifyListener listener1 = urls -> notified.set(Boolean.TRUE);
        URL url1 = new URL("dubbo", "192.168.0.1", 2200, parametersConsumer);
        abstractRegistry.subscribe(url1, listener1);
        NotifyListener listener2 = urls -> notified.set(Boolean.TRUE);
        URL url2 = new URL("dubbo", "192.168.0.2", 2201, parametersConsumer);
        abstractRegistry.subscribe(url2, listener2);
        NotifyListener listener3 = urls -> notified.set(Boolean.TRUE);
        URL url3 = new URL("dubbo", "192.168.0.3", 2202, parametersConsumer);
        abstractRegistry.subscribe(url3, listener3);
        List<URL> urls = new ArrayList<>();
        urls.add(url1);
        urls.add(url2);
        urls.add(url3);
        abstractRegistry.notify(urls);
        Map<URL, Map<String, List<URL>>> map = abstractRegistry.getNotified();
        MatcherAssert.assertThat(true, Matchers.equalTo(map.containsKey(url1)));
        MatcherAssert.assertThat(true, Matchers.equalTo(map.containsKey(url2)));
        MatcherAssert.assertThat(true, Matchers.equalTo(map.containsKey(url3)));
    }

    @Test
    public void testNotifyIfURLNull() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
            NotifyListener listener1 = urls -> notified.set(Boolean.TRUE);
            URL url1 = new URL("dubbo", "192.168.0.1", 2200, parametersConsumer);
            abstractRegistry.subscribe(url1, listener1);
            NotifyListener listener2 = urls -> notified.set(Boolean.TRUE);
            URL url2 = new URL("dubbo", "192.168.0.2", 2201, parametersConsumer);
            abstractRegistry.subscribe(url2, listener2);
            NotifyListener listener3 = urls -> notified.set(Boolean.TRUE);
            URL url3 = new URL("dubbo", "192.168.0.3", 2202, parametersConsumer);
            abstractRegistry.subscribe(url3, listener3);
            List<URL> urls = new ArrayList<>();
            urls.add(url1);
            urls.add(url2);
            urls.add(url3);
            abstractRegistry.notify(null, listener1, urls);
            Assertions.fail("notify url == null");
        });
    }

    @Test
    public void testNotifyIfNotifyNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
            NotifyListener listener1 = urls -> notified.set(Boolean.TRUE);
            URL url1 = new URL("dubbo", "192.168.0.1", 2200, parametersConsumer);
            abstractRegistry.subscribe(url1, listener1);
            NotifyListener listener2 = urls -> notified.set(Boolean.TRUE);
            URL url2 = new URL("dubbo", "192.168.0.2", 2201, parametersConsumer);
            abstractRegistry.subscribe(url2, listener2);
            NotifyListener listener3 = urls -> notified.set(Boolean.TRUE);
            URL url3 = new URL("dubbo", "192.168.0.3", 2202, parametersConsumer);
            abstractRegistry.subscribe(url3, listener3);
            List<URL> urls = new ArrayList<>();
            urls.add(url1);
            urls.add(url2);
            urls.add(url3);
            abstractRegistry.notify(url1, null, urls);
            Assertions.fail("notify listener == null");
        });
    }


    /**
     * Test method for
     * {@link org.apache.dubbo.registry.support.AbstractRegistry#notify(URL, NotifyListener, List)}.
     *
     * @throws Exception
     */
    @Test
    public void testNotifyArgs() throws Exception {
        // check parameters
        try {
            abstractRegistry.notify(null, null, null);
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }
        // check parameters
        try {
            abstractRegistry.notify(testUrl, null, null);
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }
        // check parameters
        try {
            abstractRegistry.notify(null, listener, null);
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }

        Assertions.assertFalse(notifySuccess);
        abstractRegistry.notify(testUrl, listener, null);
        Assertions.assertFalse(notifySuccess);

        List<URL> urls = new ArrayList<>();
        urls.add(testUrl);
        // check if notify successfully
        Assertions.assertFalse(notifySuccess);
        abstractRegistry.notify(testUrl, listener, urls);
        Assertions.assertTrue(notifySuccess);
    }

    @Test
    public void filterEmptyTest() throws Exception {
        // check parameters
        try {
            AbstractRegistry.filterEmpty(null, null);
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof NullPointerException);
        }

        // check parameters
        List<URL> urls = new ArrayList<>();
        try {
            AbstractRegistry.filterEmpty(null, urls);
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof NullPointerException);
        }

        // check if the output is generated by a fixed way
        urls.add(testUrl.setProtocol(EMPTY_PROTOCOL));
        Assertions.assertEquals(AbstractRegistry.filterEmpty(testUrl, null), urls);

        List<URL> testUrls = new ArrayList<>();
        Assertions.assertEquals(AbstractRegistry.filterEmpty(testUrl, testUrls), urls);

        // check if the output equals the input urls
        testUrls.add(testUrl);
        Assertions.assertEquals(AbstractRegistry.filterEmpty(testUrl, testUrls), testUrls);

    }


    @Test
    public void lookupTest() throws Exception {
        // loop up before registry
        try {
            abstractRegistry.lookup(null);
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof NullPointerException);
        }
        List<URL> urlList1 = abstractRegistry.lookup(testUrl);
        Assertions.assertFalse(urlList1.contains(testUrl));
        // loop up after registry
        List<URL> urls = new ArrayList<>();
        urls.add(testUrl);
        abstractRegistry.notify(urls);
        List<URL> urlList2 = abstractRegistry.lookup(testUrl);
        Assertions.assertTrue(urlList2.contains(testUrl));

    }

    @Test
    public void destroyTest() throws Exception {
        abstractRegistry.register(testUrl);
        abstractRegistry.subscribe(testUrl, listener);
        Assertions.assertEquals(1, abstractRegistry.getRegistered().size());
        Assertions.assertEquals(1, abstractRegistry.getSubscribed().get(testUrl).size());
        // delete listener and register
        abstractRegistry.destroy();
        Assertions.assertEquals(0, abstractRegistry.getRegistered().size());
        Assertions.assertEquals(0, abstractRegistry.getSubscribed().get(testUrl).size());
    }

    @Test
    public void allTest() throws Exception {
        // test all methods
        List<URL> urls = new ArrayList<>();
        urls.add(testUrl);
        // register, subscribe, notify, unsubscribe, unregister
        abstractRegistry.register(testUrl);
        Assertions.assertTrue(abstractRegistry.getRegistered().contains(testUrl));
        abstractRegistry.subscribe(testUrl, listener);
        Assertions.assertTrue(abstractRegistry.getSubscribed().containsKey(testUrl));
        Assertions.assertFalse(notifySuccess);
        abstractRegistry.notify(urls);
        Assertions.assertTrue(notifySuccess);
        abstractRegistry.unsubscribe(testUrl, listener);
        Assertions.assertFalse(abstractRegistry.getSubscribed().containsKey(listener));
        abstractRegistry.unregister(testUrl);
        Assertions.assertFalse(abstractRegistry.getRegistered().contains(testUrl));
    }

    private List<URL> getList() {
        List<URL> list = new ArrayList<>();
        URL url1 = new URL("dubbo", "192.168.0.1", 1000);
        URL url2 = new URL("dubbo", "192.168.0.2", 1001);
        URL url3 = new URL("dubbo", "192.168.0.3", 1002);
        list.add(url1);
        list.add(url2);
        list.add(url3);
        return list;
    }

    @Test
    public void getCacheUrlsTest() {
        List<URL> urls = new ArrayList<>();
        urls.add(testUrl);
        // check if notify successfully
        Assertions.assertFalse(notifySuccess);
        abstractRegistry.notify(testUrl, listener, urls);
        Assertions.assertTrue(notifySuccess);
        List<URL> cacheUrl = abstractRegistry.getCacheUrls(testUrl);
        Assertions.assertTrue(cacheUrl.size() == 1);
        URL nullUrl = URL.valueOf("http://1.2.3.4:9090/registry?check=false&file=N/A&interface=com.testa");
        cacheUrl = abstractRegistry.getCacheUrls(nullUrl);
        Assertions.assertTrue(Objects.isNull(cacheUrl));
    }
}
