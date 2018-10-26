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
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.identifier.ConsumerMetadataIdentifier;
import org.apache.dubbo.metadata.identifier.ProviderMetadataIdentifier;
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
    public void testGetProtocol() {
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic&side=provider");
        String protocol = abstractServiceStore.getProtocol(url);
        Assert.assertEquals(protocol, "provider");

        URL url2 = URL.valueOf("consumer://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        String protocol2 = abstractServiceStore.getProtocol(url2);
        Assert.assertEquals(protocol2, "consumer");
    }

    @Test
    public void testStoreProviderUsual() throws ClassNotFoundException {
        String interfaceName = "org.apache.dubbo.metadata.integration.InterfaceNameTestService";
        String version = "1.0.0";
        String group = null;
        String application = "vic";
        ProviderMetadataIdentifier providerMetadataIdentifier = storePrivider(abstractServiceStore, interfaceName, version, group, application);
        Assert.assertNotNull(abstractServiceStore.store.get(providerMetadataIdentifier.getIdentifierKey()));
    }

    @Test
    public void testFileExistAfterPut() throws InterruptedException, ClassNotFoundException {
        //just for one method
        URL singleUrl = URL.valueOf("redis://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.metadata.integration.InterfaceNameTestService?version=1.0.0&application=singleTest");
        NewMetadataReport singleServiceStore = new NewMetadataReport(singleUrl);

        Assert.assertFalse(singleServiceStore.file.exists());

        String interfaceName = "org.apache.dubbo.metadata.integration.InterfaceNameTestService";
        String version = "1.0.0";
        String group = null;
        String application = "vic";
        ProviderMetadataIdentifier providerMetadataIdentifier = storePrivider(singleServiceStore, interfaceName, version, group, application);

        Thread.sleep(2000);
        Assert.assertTrue(singleServiceStore.file.exists());
        Assert.assertTrue(singleServiceStore.properties.containsKey(providerMetadataIdentifier.getIdentifierKey()));
    }

    @Test
    public void testRetry() throws InterruptedException, ClassNotFoundException {
        String interfaceName = "org.apache.dubbo.metadata.integration.RetryTestService";
        String version = "1.0.0.retry";
        String group = null;
        String application = "vic.retry";
        URL storeUrl = URL.valueOf("retryReport://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestServiceForRetry?version=1.0.0.retry&application=vic.retry");
        RetryMetadataReport retryReport = new RetryMetadataReport(storeUrl, 2);
        retryReport.metadataReportRetry.retryPeriod = 200L;
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        Assert.assertNull(retryReport.metadataReportRetry.retryScheduledFuture);
        Assert.assertTrue(retryReport.metadataReportRetry.retryTimes.get() == 0);
        Assert.assertTrue(retryReport.store.isEmpty());
        Assert.assertTrue(retryReport.failedReports.isEmpty());


        storePrivider(retryReport, interfaceName, version, group, application);

        Assert.assertTrue(retryReport.store.isEmpty());
        Assert.assertFalse(retryReport.failedReports.isEmpty());
        Assert.assertNotNull(retryReport.metadataReportRetry.retryScheduledFuture);
        Thread.sleep(1200L);
        Assert.assertTrue(retryReport.metadataReportRetry.retryTimes.get() != 0);
        Assert.assertTrue(retryReport.metadataReportRetry.retryTimes.get() >= 3);
        Assert.assertFalse(retryReport.store.isEmpty());
        Assert.assertTrue(retryReport.failedReports.isEmpty());
    }

    @Test
    public void testRetryCancel() throws InterruptedException, ClassNotFoundException {
        String interfaceName = "org.apache.dubbo.metadata.integration.RetryTestService";
        String version = "1.0.0.retrycancel";
        String group = null;
        String application = "vic.retry";
        URL storeUrl = URL.valueOf("retryReport://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestServiceForRetryCancel?version=1.0.0.retrycancel&application=vic.retry");
        RetryMetadataReport retryReport = new RetryMetadataReport(storeUrl, 2);
        retryReport.metadataReportRetry.retryPeriod = 150L;
        retryReport.metadataReportRetry.retryTimesIfNonFail = 2;

        storePrivider(retryReport, interfaceName, version, group, application);

        Assert.assertFalse(retryReport.metadataReportRetry.retryScheduledFuture.isCancelled());
        Assert.assertFalse(retryReport.metadataReportRetry.retryExecutor.isShutdown());
        Thread.sleep(1000L);
        Assert.assertTrue(retryReport.metadataReportRetry.retryScheduledFuture.isCancelled());
        Assert.assertTrue(retryReport.metadataReportRetry.retryExecutor.isShutdown());

    }

    private ProviderMetadataIdentifier storePrivider(AbstractMetadataReport abstractMetadataReport, String interfaceName, String version, String group, String application) throws ClassNotFoundException {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":4444/" + interfaceName + "?version=" + version + "&application="
                + application + (group == null ? "" : "&group=" + group));

        ProviderMetadataIdentifier providerMetadataIdentifier = new ProviderMetadataIdentifier(interfaceName, version, group);
        Class interfaceClass = Class.forName(interfaceName);
        FullServiceDefinition fullServiceDefinition = ServiceDefinitionBuilder.buildFullDefinition(interfaceClass, url.getParameters());

        abstractMetadataReport.storeProviderMetadata(providerMetadataIdentifier, fullServiceDefinition);

        return providerMetadataIdentifier;
    }


    private static class NewMetadataReport extends AbstractMetadataReport {

        Map<String, String> store = new ConcurrentHashMap<>();

        public NewMetadataReport(URL servicestoreURL) {
            super(servicestoreURL);
        }

        @Override
        protected void doStoreProviderMetadata(ProviderMetadataIdentifier providerMetadataIdentifier, String serviceDefinitions) {
            store.put(providerMetadataIdentifier.getIdentifierKey(), serviceDefinitions);
        }

        @Override
        protected void doStoreConsumerMetadata(ConsumerMetadataIdentifier consumerMetadataIdentifier, String serviceParameterString) {
            store.put(consumerMetadataIdentifier.getIdentifierKey(), serviceParameterString);
        }
    }

    private static class RetryMetadataReport extends AbstractMetadataReport {

        Map<String, String> store = new ConcurrentHashMap<>();
        int needRetryTimes;
        int executeTimes = 0;

        public RetryMetadataReport(URL servicestoreURL, int needRetryTimes) {
            super(servicestoreURL);
            this.needRetryTimes = needRetryTimes;
        }

        @Override
        protected void doStoreProviderMetadata(ProviderMetadataIdentifier providerMetadataIdentifier, String serviceDefinitions) {
            ++executeTimes;
            System.out.println("***" + executeTimes + ";" + System.currentTimeMillis());
            if (executeTimes <= needRetryTimes) {
                throw new RuntimeException("must retry:" + executeTimes);
            }
            store.put(providerMetadataIdentifier.getIdentifierKey(), serviceDefinitions);
        }

        @Override
        protected void doStoreConsumerMetadata(ConsumerMetadataIdentifier consumerMetadataIdentifier, String serviceParameterString) {
            ++executeTimes;
            if (executeTimes <= needRetryTimes) {
                throw new RuntimeException("must retry:" + executeTimes);
            }
            store.put(consumerMetadataIdentifier.getIdentifierKey(), serviceParameterString);
        }
    }


}
