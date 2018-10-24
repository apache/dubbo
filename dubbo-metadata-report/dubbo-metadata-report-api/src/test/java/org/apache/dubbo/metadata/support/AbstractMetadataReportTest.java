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
package org.apache.dubbo.metadata.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class AbstractMetadataReportTest {

    private NewMetadataReport abstractServiceStore;


    @Before
    public void before() {
        URL url = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        abstractServiceStore = new NewMetadataReport(url);
    }

    @Test
    public void testGetProtocol(){
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic&side=provider");
        String protocol = abstractServiceStore.getProtocol(url);
        Assert.assertEquals(protocol, "provider");

        URL url2 = URL.valueOf("consumer://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        String protocol2 = abstractServiceStore.getProtocol(url2);
        Assert.assertEquals(protocol2, "consumer");
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
        //just for one method
        URL singleUrl = URL.valueOf("redis://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=singleTest");
        NewMetadataReport singleServiceStore = new NewMetadataReport(singleUrl);

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


    private static class NewMetadataReport extends AbstractMetadataReport {

        Map<String, String> store = new ConcurrentHashMap<>();

        public NewMetadataReport(URL servicestoreURL) {
            super(servicestoreURL);
        }

        @Override
        protected void doPut(URL url) {
            store.put(url.getServiceKey(), url.toParameterString());
        }

        @Override
        protected URL doPeek(URL url) {
            String queryV = store.get(url.getServiceKey());
            URL urlTmp = url.clearParameters().addParameterString(queryV);
            return urlTmp;
        }
    }


}
