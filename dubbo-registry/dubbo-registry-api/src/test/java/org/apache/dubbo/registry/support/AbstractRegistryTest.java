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

    private URL url1, url2;
    private NotifyListener listener;
    private NotifyListener listener2;
    private AbstractRegistry abstractRegistry;
    private boolean notifySuccess;

    @Before
    public void init() {
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":1111");
        url1 = URL.valueOf("http://127.0.0.1:9090/registry?check=false&file=N/A&interface=com.test");
        url2 = URL.valueOf("http://127.0.0.1:9091/registry?check=false&file=N/A&interface=com.test");

        //init the object
        abstractRegistry = new AbstractRegistry(url) {
            @Override
            public boolean isAvailable() {
                return false;
            }
        };
        //init notify listener
        listener = urls -> notifySuccess = true;
        listener2 = urls -> notifySuccess = false;
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
        abstractRegistry.register(url1);
        Assert.assertEquals(beginSize + 1, abstractRegistry.getRegistered().size());
        Assert.assertEquals("[" + url1 + "]", abstractRegistry.getRegistered().toString());

        //check repeat registration of the same service
        abstractRegistry.register(url1);
        Assert.assertEquals(beginSize + 1, abstractRegistry.getRegistered().size());
        Assert.assertEquals("[" + url1 + "]", abstractRegistry.getRegistered().toString());

        // add new register
        abstractRegistry.register(url2);
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

        // check if no register url2 but unregister
        abstractRegistry.unregister(url2);
        Assert.assertEquals(0, abstractRegistry.getRegistered().size());

        // check if unregister url successfully
        abstractRegistry.register(url1);
        abstractRegistry.register(url2);
        int beginSize = abstractRegistry.getRegistered().size();
        abstractRegistry.unregister(url1);
        Assert.assertEquals(beginSize - 1, abstractRegistry.getRegistered().size());

        // check if unregister a not exist url successfully
        abstractRegistry.unregister(url1);
        Assert.assertEquals(beginSize - 1, abstractRegistry.getRegistered().size());

        // unregister url2
        abstractRegistry.unregister(url2);
        Assert.assertEquals(0, abstractRegistry.getRegistered().size());
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
            abstractRegistry.subscribe(url1, null);
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

        // check if no one lintening url1 or url2
        Assert.assertEquals(0, abstractRegistry.getSubscribed().size());

        // check if subscribe successfully
        abstractRegistry.subscribe(url1, listener);
        Assert.assertEquals(1, abstractRegistry.getSubscribed().size());
        Assert.assertNotNull(abstractRegistry.getSubscribed().get(url1));
        Assert.assertTrue(abstractRegistry.getSubscribed().get(url1).contains(listener));

        // check if there is already one listener of url1
        abstractRegistry.subscribe(url1, listener);
        Assert.assertNotNull(abstractRegistry.getSubscribed().get(url1));
        Assert.assertTrue(abstractRegistry.getSubscribed().get(url1).contains(listener));

        // check there is no listener of url2
        Assert.assertNull(abstractRegistry.getSubscribed().get(url2));
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
            abstractRegistry.unsubscribe(url1, null);
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
        // check when there is no subscribe
        abstractRegistry.unsubscribe(url1, listener);
        Assert.assertNull(abstractRegistry.getSubscribed().get(url1));

        // check if unsubscribe successfully
        abstractRegistry.subscribe(url1, listener);
        abstractRegistry.unsubscribe(url1, listener);
        Assert.assertEquals(1, abstractRegistry.getSubscribed().size());
        Assert.assertFalse(abstractRegistry.getSubscribed().get(url1).contains(listener));

        // check repeat unsubscription
        abstractRegistry.unsubscribe(url1, listener);
        Assert.assertEquals(1, abstractRegistry.getSubscribed().size());
        Assert.assertFalse(abstractRegistry.getSubscribed().get(url1).contains(listener));
    }

    @Test
    public void recoverTest() throws Exception {
        // check if there is no registration or subscription
        abstractRegistry.recover();
        Assert.assertEquals(0, abstractRegistry.getRegistered().size());
        Assert.assertEquals(0, abstractRegistry.getSubscribed().size());

        // check if registered and subscribed
        abstractRegistry.register(url1);
        abstractRegistry.subscribe(url1, listener);
        abstractRegistry.recover();
        // check if recover successfully
        Assert.assertTrue(abstractRegistry.getRegistered().contains(url1));
        Assert.assertNotNull(abstractRegistry.getSubscribed().get(url1));
        Assert.assertTrue(abstractRegistry.getSubscribed().get(url1).contains(listener));

        // check if registration and subscription have been canceled
        abstractRegistry.register(url1);
        abstractRegistry.subscribe(url1, listener);
        abstractRegistry.unregister(url1);
        abstractRegistry.unsubscribe(url1, listener);
        abstractRegistry.recover();
        // check if recover unsuccessfully
        Assert.assertFalse(abstractRegistry.getRegistered().contains(url1));
        Assert.assertEquals(0, abstractRegistry.getSubscribed().get(url1).size());
        Assert.assertFalse(abstractRegistry.getSubscribed().get(url1).contains(listener));

        // check if registered but unsubscribed
        abstractRegistry.register(url1);
        abstractRegistry.subscribe(url1, listener);
        abstractRegistry.unsubscribe(url1, listener);
        abstractRegistry.recover();
        // check if recover unsuccessfully
        Assert.assertTrue(abstractRegistry.getRegistered().contains(url1));
        Assert.assertEquals(0, abstractRegistry.getSubscribed().get(url1).size());
        Assert.assertFalse(abstractRegistry.getSubscribed().get(url1).contains(listener));

        // check if unregistration but subscribed
        abstractRegistry.register(url1);
        abstractRegistry.unregister(url1);
        abstractRegistry.subscribe(url1, listener);
        abstractRegistry.recover();
        // check if recover unsuccessfully
        Assert.assertFalse(abstractRegistry.getRegistered().contains(url1));
        Assert.assertEquals(1, abstractRegistry.getSubscribed().get(url1).size());
        Assert.assertTrue(abstractRegistry.getSubscribed().get(url1).contains(listener));
    }

    @Test
    public void notifyTest() {
        // check if there is no registration or subscription
        List<URL> urls = new ArrayList<>();
        urls.add(url1);
        // check if notify successfully
        Assert.assertFalse(notifySuccess);
        //check parameters
        abstractRegistry.notify(null);
        Assert.assertFalse(notifySuccess);
        urls.clear();

        // normal case
        abstractRegistry.subscribe(url1, listener);
        urls.add(url1);
        // check if notify successfully
        Assert.assertFalse(notifySuccess);
        abstractRegistry.notify(urls);
        Assert.assertTrue(notifySuccess);
        urls.clear();

        // normal case2
        notifySuccess = false;
        abstractRegistry.subscribe(url1, listener2);
        urls.add(url1);
        // check if notify successfully
        Assert.assertFalse(notifySuccess);
        abstractRegistry.notify(urls);
        Assert.assertTrue(notifySuccess);// ？？
        urls.clear();

        // other case
        notifySuccess = false;
        abstractRegistry.notify(null);
        Assert.assertFalse(notifySuccess);
    }

    @Test
    public void notify3inTest() {
        List<URL> urls = new ArrayList<>();
        urls.add(url1);
        //check parameters
        try {
            abstractRegistry.notify(null, listener, urls);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        //check parameters
        try {
            abstractRegistry.notify(url1, null, urls);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        Assert.assertFalse(notifySuccess);
        abstractRegistry.notify(url1, listener, null);
        Assert.assertFalse(notifySuccess);
        // check if notify successfully
        Assert.assertFalse(notifySuccess);
        abstractRegistry.notify(url1, listener, urls);
        Assert.assertTrue(notifySuccess);
    }

    @Test
    public void destroyTest() {
        // check if there is no registration or subscription
        abstractRegistry.destroy();
        Assert.assertEquals(0, abstractRegistry.getRegistered().size());
        Assert.assertEquals(0, abstractRegistry.getSubscribed().size());

        // check if registered and subscribed
        abstractRegistry.register(url1);
        abstractRegistry.subscribe(url1, listener);
        abstractRegistry.destroy();
        // check if recover successfully
        Assert.assertEquals(0, abstractRegistry.getRegistered().size());
        Assert.assertEquals(1, abstractRegistry.getSubscribed().size());
        Assert.assertFalse(abstractRegistry.getSubscribed().get(url1).contains(listener));

        // check if registration and subscription have been canceled
        abstractRegistry.register(url1);
        abstractRegistry.subscribe(url1, listener);
        abstractRegistry.unregister(url1);
        abstractRegistry.unsubscribe(url1, listener);
        abstractRegistry.destroy();
        // check if recover unsuccessfully
        Assert.assertEquals(0, abstractRegistry.getRegistered().size());
        Assert.assertEquals(1, abstractRegistry.getSubscribed().size());
        Assert.assertFalse(abstractRegistry.getSubscribed().get(url1).contains(listener));

        // check if registered but unsubscribed
        abstractRegistry.register(url1);
        abstractRegistry.subscribe(url1, listener);
        abstractRegistry.unsubscribe(url1, listener);
        abstractRegistry.destroy();
        // check if recover unsuccessfully
        Assert.assertEquals(0, abstractRegistry.getRegistered().size());
        Assert.assertEquals(1, abstractRegistry.getSubscribed().size());
        Assert.assertFalse(abstractRegistry.getSubscribed().get(url1).contains(listener));

        // check if unregistration but subscribed
        abstractRegistry.register(url1);
        abstractRegistry.unregister(url1);
        abstractRegistry.subscribe(url1, listener);
        abstractRegistry.destroy();
        // check if recover unsuccessfully
        Assert.assertEquals(0, abstractRegistry.getRegistered().size());
        Assert.assertEquals(1, abstractRegistry.getSubscribed().size());
        Assert.assertFalse(abstractRegistry.getSubscribed().get(url1).contains(listener));
    }
}