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
    //add another testUrl
    private URL testUrl2;
    private URL registerTestUrl1,unregisterTestUrl1,unregisterTestUrl2,unregisterTestUrlfordynamic1,unregisterTestUrlfordynamic2;
    private URL subscribeTestUrlforcatagory1,subscribeTestUrlforcatagory2,subscribeTestUrlforcatagory3,subscribeTestUrlforcatagory4,subscribeTestUrlforcatagory5;
    private NotifyListener listener;
    private AbstractRegistry abstractRegistry;
    private boolean notifySuccess;

    @Before
    public void init() {
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":2233");
        testUrl = URL.valueOf("http://1.2.3.4:9090/registry?check=false&file=N/A&interface=com.test");
        testUrl2 = URL.valueOf("http://2.2.3.4:9090/registry?check=true&file=N/A&interface=com.test");
        registerTestUrl1 = URL.valueOf("http://1.2.3.4:9090/registry?check=true&file=N/A&interface=com.test");
        unregisterTestUrl1 = URL.valueOf("http://1.2.3.4:9090/registry?check=false&file=N/A");
        unregisterTestUrlfordynamic1 = URL.valueOf("http://1.2.3.4:9090/registry?check=false&file=N/A&interface=com.test&dynamic=false");
        unregisterTestUrlfordynamic2 = URL.valueOf("http://1.2.3.4:9090/registry?check=false&file=N/A");
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

        // check register when the same URL but different parameters to coexist
        abstractRegistry.register(registerTestUrl1);
        Assert.assertEquals(beginSize + 2, abstractRegistry.getRegistered().size());
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

        // check if unregister url  according to the not full url match
        beginSize = abstractRegistry.getRegistered().size();
        abstractRegistry.register(testUrl);
        abstractRegistry.unregister(unregisterTestUrl1);
        Assert.assertEquals(beginSize+1 , abstractRegistry.getRegistered().size());

      /*  // check if unregister url  according to the not full url match
        abstractRegistry.register(unregisterTestUrlfordynamic1);
        abstractRegistry.unregister(unregisterTestUrlfordynamic2);
        Assert.assertEquals(beginSize+1 , abstractRegistry.getRegistered().size());*/


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

        //check subscribe when the url is the same
        int size=abstractRegistry.getSubscribed().size();
        abstractRegistry.subscribe(testUrl, listener);
        Assert.assertEquals(size , abstractRegistry.getSubscribed().size());


       /* // check parameter of interface and category
        abstractRegistry.getSubscribed().clear();
        abstractRegistry.getRegistered().clear();
        abstractRegistry.register(URL.valueOf("http://1.2.3.4:9090/registry?interface=com.test&category=provider"));
        abstractRegistry.register(URL.valueOf("http://1.2.3.4:9090/registry?interface=com.test&category=comsumer"));
        abstractRegistry.register(URL.valueOf("http://1.2.3.4:9090/registry?interface=com.alibaba&category=provider"));
        abstractRegistry.register(URL.valueOf("http://1.2.3.4:9090/registry?interface=com.alibaba&category=comsumer"));
       // abstractRegistry.subscribe(URL.valueOf("http://1.2.3.4:9090/registry?interface=com.test&category=*"),listener);

        abstractRegistry.subscribe(URL.valueOf("http://1.2.3.4:9090/registry?interface=com.test&category=provider"),listener);
        abstractRegistry.subscribe(URL.valueOf("http://1.2.3.4:9090/registry?interface=com.test&category=comsumer"),listener);


        Assert.assertEquals(2 , abstractRegistry.getSubscribed().size());
        abstractRegistry.subscribe(URL.valueOf("http://1.2.3.4:9090/registry?interface=*&category=*"),listener);
        Assert.assertEquals(4 , abstractRegistry.getSubscribed().size());*/


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

        // check unsubscribe when the url is the same
        abstractRegistry.unsubscribe(testUrl, listener);
        Assert.assertFalse(abstractRegistry.getSubscribed().get(testUrl).contains(listener));

        // check unsubscribe when the url is not exist
        abstractRegistry.unsubscribe(URL.valueOf("http://1.2.3.4:9090/registry?interface=com.test&category=provider"), listener);
        Assert.assertNull(abstractRegistry.getSubscribed().get(URL.valueOf("http://1.2.3.4:9090/registry?interface=com.test&category=provider")));



       /* abstractRegistry.getSubscribed().clear();
        abstractRegistry.getRegistered().clear();
        abstractRegistry.register(URL.valueOf("http://1.2.3.4:9090/registry?interface=com.test&category=provider"));
        abstractRegistry.register(URL.valueOf("http://1.2.3.4:9090/registry?interface=com.test&category=comsumer"));
        abstractRegistry.register(URL.valueOf("http://1.2.3.4:9090/registry?interface=com.alibaba&category=provider"));
        abstractRegistry.register(URL.valueOf("http://1.2.3.4:9090/registry?interface=com.alibaba&category=comsumer"));
        abstractRegistry.subscribe(URL.valueOf("http://1.2.3.4:9090/registry?interface=com.test&category=*"),listener);
    //    Assert.assertEquals(2 , abstractRegistry.getSubscribed().size());
        abstractRegistry.subscribe(URL.valueOf("http://1.2.3.4:9090/registry?interface=*&category=*"),listener);
        int size=abstractRegistry.getSubscribed().size();
        abstractRegistry.unsubscribe(URL.valueOf("http://1.2.3.4:9090/registry?interface=*&category=*"),listener);
        Assert.assertEquals(size-4 , abstractRegistry.getSubscribed().size());
      // Assert.assertFalse(abstractRegistry.getSubscribed().get(testUrl).contains(listener));*/
    }

    @Test
    public void recoverTest() throws Exception {
        //check if recover unsuccessfully
        abstractRegistry.recover();
        Assert.assertFalse(abstractRegistry.getRegistered().contains(testUrl));
        Assert.assertNull(abstractRegistry.getSubscribed().get(testUrl));

        // check if recover successfully
        abstractRegistry.register(testUrl);
        abstractRegistry.subscribe(testUrl, listener);
        abstractRegistry.recover();
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

        //check if notify successfully when urls.size()>1
        notifySuccess = false;
        urls.add(testUrl2);
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
        try {
            List<URL> urls2 = new ArrayList<>();
            urls2.add(testUrl);
            abstractRegistry.notify(null, null, urls2);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        //check parameters
        try {
            abstractRegistry.notify(null, listener, null);
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

        //check if notify successfully when ruls.size()>1
        urls.add(testUrl2);
        notifySuccess = false;
        abstractRegistry.notify(testUrl,listener,urls);
        Assert.assertTrue(notifySuccess);

    }
    @Test
    public void destoryTest(){
        // check if destory successfully
        abstractRegistry.register(testUrl);
        abstractRegistry.subscribe(testUrl, listener);
        abstractRegistry.destroy();
        Assert.assertFalse(abstractRegistry.getRegistered().contains(testUrl));

        //check if destory a not exist url successfully
        abstractRegistry.destroy();
        Assert.assertFalse(abstractRegistry.getRegistered().contains(testUrl2));

        //check if destory when the url is already destoried
        abstractRegistry.destroy();
        Assert.assertFalse(abstractRegistry.getRegistered().contains(testUrl));


    }
}