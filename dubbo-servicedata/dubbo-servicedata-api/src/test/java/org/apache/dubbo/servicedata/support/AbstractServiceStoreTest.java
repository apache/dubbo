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
package org.apache.dubbo.servicedata.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class AbstractServiceStoreTest {

    private NewServiceStore abstractServiceStore;
    private NewServiceStore singleServiceStore;

    @Before
    public void before() {
        URL url = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        abstractServiceStore = new NewServiceStore(url);
        URL singleUrl = URL.valueOf("redis://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=singleTest");
        singleServiceStore = new NewServiceStore(singleUrl);
    }

    @Test
    public void testPutUsual() {
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        abstractServiceStore.put(url);
        Assert.assertNotNull(abstractServiceStore.store.get(url.getServiceKey()));
    }

    @Test
    public void testPutNoServiceKeyUrl() {
        URL urlTmp = URL.valueOf("rmi://wrongHost:90?application=vic");
        abstractServiceStore.put(urlTmp);
        Assert.assertNull(urlTmp.getServiceKey());
        // key is null, will add failed list.
        Assert.assertFalse(abstractServiceStore.failedServiceStore.isEmpty());
    }

    @Test
    public void testPutNotFullServiceKeyUrl() {
        URL urlTmp = URL.valueOf("rmi://wrongHost:90/org.dubbo.TestService");
        abstractServiceStore.put(urlTmp);
        Assert.assertNotNull(abstractServiceStore.store.get(urlTmp.getServiceKey()));
    }

    @Test
    public void testFileExistAfterPut() throws InterruptedException {
        Assert.assertFalse(singleServiceStore.file.exists());
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        singleServiceStore.put(url);
        Thread.sleep(2000);
        Assert.assertTrue(singleServiceStore.file.exists());
        Assert.assertTrue(singleServiceStore.properties.containsKey(url.getServiceKey()));
    }

    @Test
    public void testPeek() {
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        abstractServiceStore.put(url);
        URL result = abstractServiceStore.peek(url);
        Assert.assertEquals(url, result);
    }


    private static class NewServiceStore extends AbstractServiceStore {

        Map<String, String> store = new ConcurrentHashMap<>();

        public NewServiceStore(URL servicestoreURL) {
            super(servicestoreURL);
        }

        @Override
        protected void doPutService(URL url) {
            store.put(url.getServiceKey(), url.toParameterString());
        }

        @Override
        protected URL doPeekService(URL url) {
            String queryV = store.get(url.getServiceKey());
            URL urlTmp = url.clearParameters().addParameterString(queryV);
            return urlTmp;
        }
    }


}
