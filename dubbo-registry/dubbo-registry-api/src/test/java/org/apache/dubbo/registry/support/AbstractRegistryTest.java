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
    private URL testUrl2;
    private NotifyListener listener;
    private NotifyListener listener2;
    private AbstractRegistry abstractRegistry;
    private boolean notifySuccess;

    @Before
    public void init() {
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":2233");
        testUrl = URL.valueOf("http://1.2.3.4:9090/registry?check=false&file=N/A&interface=com.test");
        testUrl2 = URL.valueOf("http://1.2.3.4:9090/registry?check=false&file=N/A&interface=com.test2");

        //init the object
        abstractRegistry = new AbstractRegistry(url) {
            @Override
            public boolean isAvailable() {
                return false;
            }
        };
        //init notify listener
        listener = urls -> notifySuccess = true;
        listener2 = urls -> notifySuccess = true;
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
        Assert.assertTrue(abstractRegistry.getRegistered().contains(testUrl));
        Assert.assertEquals(beginSize + 1, abstractRegistry.getRegistered().size());
        //check register when the url is the same
        abstractRegistry.register(testUrl);
        Assert.assertEquals(beginSize + 1, abstractRegistry.getRegistered().size());
        abstractRegistry.register(testUrl);
        Assert.assertEquals(beginSize + 1, abstractRegistry.getRegistered().size());
        //check register multiple url
        abstractRegistry.getRegistered().clear();
        beginSize = abstractRegistry.getRegistered().size();
        List<URL> urlList = new ArrayList<>();
        urlList.add(testUrl);
        urlList.add(testUrl2);
        for(URL url: urlList){
            abstractRegistry.register(url);
        }
        Assert.assertEquals(beginSize+ urlList.size(), abstractRegistry.getRegistered().size());
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
        Assert.assertTrue(abstractRegistry.getRegistered().contains(testUrl));
        abstractRegistry.unregister(testUrl);
        Assert.assertFalse(abstractRegistry.getRegistered().contains(testUrl));
        Assert.assertEquals(beginSize - 1, abstractRegistry.getRegistered().size());
        // check if unregister a not exist url successfully
        abstractRegistry.unregister(testUrl);
        Assert.assertEquals(beginSize - 1, abstractRegistry.getRegistered().size());

        // check multiple url unregister.
        abstractRegistry.getRegistered().clear();
        abstractRegistry.register(testUrl);
        abstractRegistry.register(testUrl2);
        List<URL> list = new ArrayList<>();
        list.add(testUrl);
        list.add(testUrl2);
        for(URL url : list){
            abstractRegistry.unregister(url);
        }
        Assert.assertEquals(0,abstractRegistry.getRegistered().size());
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
        int beginSize = abstractRegistry.getSubscribed().size();
        abstractRegistry.subscribe(testUrl, listener);
        Assert.assertNotNull(abstractRegistry.getSubscribed().get(testUrl));
        Assert.assertTrue(abstractRegistry.getSubscribed().get(testUrl).contains(listener));
        Assert.assertEquals(beginSize + 1, abstractRegistry.getSubscribed().size());
        //check subscribe when the url and listener are the same
        abstractRegistry.subscribe(testUrl, listener);
        Assert.assertEquals(beginSize + 1, abstractRegistry.getSubscribed().size());

        // check same url multiple listener
        abstractRegistry.subscribe(testUrl, listener2);
        Assert.assertEquals(2,abstractRegistry.getSubscribed().get(testUrl).size());
        Assert.assertTrue(abstractRegistry.getSubscribed().get(testUrl).contains(listener2));
        // check same url:
        abstractRegistry.subscribe(testUrl2,listener);
        Assert.assertTrue(abstractRegistry.getSubscribed().get(testUrl2).contains(listener));
        Assert.assertTrue(abstractRegistry.getSubscribed().size()==2);
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
        int beginSize = abstractRegistry.getRegistered().size();
        abstractRegistry.recover();
        Assert.assertEquals(beginSize, abstractRegistry.getRegistered().size());
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
        notifySuccess = false;
        abstractRegistry.notify(null);
        Assert.assertFalse(notifySuccess);
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
}