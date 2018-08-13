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
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * AbstractRegistryTest
 */
public class AbstractRegistryTest {

    private URL testUrl;
    private NotifyListener listener;
    private AbstractRegistry abstractRegistry;
    private boolean notifySuccess;

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