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
import java.util.Map;
import java.util.Set;

/**
 * AbstractRegistryTest
 */
public class AbstractRegistryTest {

    private URL url;
    private URL testUrl;
    private URL testUrl2;
    private URL testUrl3;
    private NotifyListener listener;
    private AbstractRegistry abstractRegistry;
    private boolean notifySuccess;

    @Before
    public void init() {
        url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":2233");
        testUrl = URL.valueOf("http://1.2.3.4:9090/registry?check=false&file=N/A&interface=com.test");
        // Url format: "protocol://host:port@username:password/path?key=value&key=value"
        //      protocol   = "http"
        //      path       = "registry"
        //      host:ip    = 1.2.3.4:9090
        //      parameters = {("check", "false"), ("file", "N/A"), ("interface", "com.test")}
        testUrl2 = URL.valueOf("http://0.0.0.0:8080/registry?check=false&file=~/.dubbo/test2.cache&interface=com.test2");
        testUrl3 = URL.valueOf("empty://192.168.199.118:20880/com.example.HelloService?anyhost=true&application=dubbo-demo-server&category=configurators&check=false&dubbo=2.0.1&generic=false&interface=com.example.HelloService&methods=sayHello&pid=7185&revision=1.0.0&side=provider&timestamp=1534089043849&version=1.0.0");
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
    public void lookupTest() {
        Assert.assertTrue(abstractRegistry.getNotified().size() == 0);
        Assert.assertTrue(abstractRegistry.getSubscribed().size() == 0);

        try {
            // get throw nullptr
            List<URL> lookup_rst = abstractRegistry.lookup(null);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NullPointerException);
        }
        // on the first lookup, there are no notifiedUrls
        // a listener is subscribed to the testUrl:
        //      when notify it, the reference in lookup is set
        // to notice: the lookup url comes from consumer url, the notify urls comes from provider
        List<URL> lookup_rst = abstractRegistry.lookup(testUrl);
        Assert.assertTrue(lookup_rst.size() == 0);
        Assert.assertTrue(abstractRegistry.getNotified().size() == 0);
        Assert.assertTrue(abstractRegistry.getSubscribed().size() == 1);

        // now we notify
        List<URL> urls = new ArrayList<URL>();
        // urls is empty, should do nothing
        abstractRegistry.notify(urls);
        Assert.assertTrue(abstractRegistry.getNotified().size() == 0);
        URL fakeUrl = URL.valueOf("http://1.2.3.5:9091/registry?check=false&file=N/A&interface=com.faketest");
        urls.add(fakeUrl);
        abstractRegistry.notify(urls);
        Assert.assertTrue(abstractRegistry.getNotified().size() == 0);

        urls = new ArrayList<URL>();
        urls.add(testUrl);
        abstractRegistry.notify(urls);
        Assert.assertTrue(abstractRegistry.getNotified().size() == 1);
        lookup_rst = abstractRegistry.lookup(testUrl);
        Assert.assertTrue(lookup_rst.size() == 1);

    }


    @Test
    public void getCacheUrlsTest(){
        try{
            abstractRegistry.getCacheUrls(null);
            Assert.fail();
        }catch (Exception e){
            Assert.assertTrue(e instanceof NullPointerException);
        }
        Assert.assertEquals(1, abstractRegistry.getCacheUrls(testUrl).size());
    }

    @Test
    public void toStringTest(){
        Assert.assertEquals(url.toFullString(), abstractRegistry.toString());
    }

    @Test
    public void getCacheFileTest(){
        abstractRegistry.getCacheFile();
    }

    @Test
    public void getCachePropertiesTest(){
        abstractRegistry.getCacheProperties();
    }

    @Test
    public void getLastCacheChangedTest(){
        abstractRegistry.getLastCacheChanged();
    }

    @Test
    public void filterEmptyTest(){
        List<URL> urls = new ArrayList<URL>() {};
        try{
            abstractRegistry.filterEmpty(null, urls);
            Assert.fail();
        }catch (Exception e){
            Assert.assertTrue(e instanceof NullPointerException);
        }
        abstractRegistry.filterEmpty(testUrl, urls);
        urls.add(testUrl);
        abstractRegistry.filterEmpty(testUrl, urls);
    }

    @Test
    public void destroyTest(){
        abstractRegistry.register(testUrl);
        abstractRegistry.register(testUrl2);
        abstractRegistry.subscribe(testUrl, listener);
        abstractRegistry.subscribe(testUrl3, listener);
        abstractRegistry.destroy();
        Assert.assertEquals(0, abstractRegistry.getRegistered().size());
        for (Map.Entry<URL, Set<NotifyListener>> map : abstractRegistry.getSubscribed().entrySet()) {
            Assert.assertEquals(0, map.getValue().size());
        }
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
        //check parameters
        abstractRegistry.notify(testUrl, listener, null);
        Assert.assertTrue(abstractRegistry.getNotified().size() == 0);

        List<URL> urls = new ArrayList<>();
        urls.add(testUrl);
        // check if notify successfully
        Assert.assertFalse(notifySuccess);
        abstractRegistry.notify(testUrl, listener, urls);
        Assert.assertTrue(notifySuccess);
    }
}