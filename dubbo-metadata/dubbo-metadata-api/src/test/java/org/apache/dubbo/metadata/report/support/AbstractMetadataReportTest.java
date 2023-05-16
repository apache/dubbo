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

package org.apache.dubbo.metadata.report.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.MappingListener;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.report.identifier.KeyTypeEnum;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.ServiceMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractMetadataReportTest {

    private NewMetadataReport abstractMetadataReport;
    private ApplicationModel applicationModel;

    @BeforeEach
    public void before() {
        // set the simple name of current class as the application name
        FrameworkModel frameworkModel = FrameworkModel.defaultModel();
        applicationModel = frameworkModel.newApplication();
        applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig(getClass().getSimpleName()));

        URL url = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        abstractMetadataReport = new NewMetadataReport(url, applicationModel);

    }

    @AfterEach
    public void reset() {
        // reset
        ApplicationModel.reset();
    }

    @Test
    void testGetProtocol() {
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic&side=provider");
        String protocol = abstractMetadataReport.getProtocol(url);
        assertEquals("provider", protocol);

        URL url2 = URL.valueOf("consumer://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        String protocol2 = abstractMetadataReport.getProtocol(url2);
        assertEquals("consumer", protocol2);
    }

    @Test
    void testStoreProviderUsual() throws ClassNotFoundException {
        String interfaceName = "org.apache.dubbo.metadata.store.InterfaceNameTestService";
        String version = "1.0.0";
        String group = null;
        String application = "vic";
        ThreadPoolExecutor reportCacheExecutor = (ThreadPoolExecutor) abstractMetadataReport.getReportCacheExecutor();

        long completedTaskCount1 = reportCacheExecutor.getCompletedTaskCount();
        MetadataIdentifier providerMetadataIdentifier = storeProvider(abstractMetadataReport, interfaceName, version, group, application);
        await().until(() -> reportCacheExecutor.getCompletedTaskCount() > completedTaskCount1);
        Assertions.assertNotNull(abstractMetadataReport.store.get(providerMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY)));
    }

    @Test
    void testStoreProviderSync() throws ClassNotFoundException {
        String interfaceName = "org.apache.dubbo.metadata.store.InterfaceNameTestService";
        String version = "1.0.0";
        String group = null;
        String application = "vic";
        abstractMetadataReport.syncReport = true;
        MetadataIdentifier providerMetadataIdentifier = storeProvider(abstractMetadataReport, interfaceName, version, group, application);
        Assertions.assertNotNull(abstractMetadataReport.store.get(providerMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY)));
    }

    @Test
    void testFileExistAfterPut() throws ClassNotFoundException {
        //just for one method
        URL singleUrl = URL.valueOf("redis://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.metadata.store.InterfaceNameTestService?version=1.0.0&application=singleTest");
        NewMetadataReport singleMetadataReport = new NewMetadataReport(singleUrl, applicationModel);

        assertFalse(singleMetadataReport.file.exists());

        String interfaceName = "org.apache.dubbo.metadata.store.InterfaceNameTestService";
        String version = "1.0.0";
        String group = null;
        String application = "vic";
        ThreadPoolExecutor reportCacheExecutor = (ThreadPoolExecutor) singleMetadataReport.getReportCacheExecutor();

        long completedTaskCount1 = reportCacheExecutor.getCompletedTaskCount();
        MetadataIdentifier providerMetadataIdentifier = storeProvider(singleMetadataReport, interfaceName, version, group, application);
        await().until(() -> reportCacheExecutor.getCompletedTaskCount() > completedTaskCount1);

        assertTrue(singleMetadataReport.file.exists());
        assertTrue(singleMetadataReport.properties.containsKey(providerMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY)));
    }

    @Test
    void testRetry() throws ClassNotFoundException {
        String interfaceName = "org.apache.dubbo.metadata.store.RetryTestService";
        String version = "1.0.0.retry";
        String group = null;
        String application = "vic.retry";
        URL storeUrl = URL.valueOf("retryReport://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestServiceForRetry?version=1.0.0.retry&application=vic.retry");
        RetryMetadataReport retryReport = new RetryMetadataReport(storeUrl, 2, applicationModel);
        retryReport.metadataReportRetry.retryPeriod = 400L;
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        Assertions.assertNull(retryReport.metadataReportRetry.retryScheduledFuture);
        assertEquals(0, retryReport.metadataReportRetry.retryCounter.get());
        assertTrue(retryReport.store.isEmpty());
        assertTrue(retryReport.failedReports.isEmpty());


        ThreadPoolExecutor reportCacheExecutor = (ThreadPoolExecutor) retryReport.getReportCacheExecutor();
        ScheduledThreadPoolExecutor retryExecutor = (ScheduledThreadPoolExecutor) retryReport.getMetadataReportRetry().getRetryExecutor();

        long completedTaskCount1 = reportCacheExecutor.getCompletedTaskCount();
        long completedTaskCount2 = retryExecutor.getCompletedTaskCount();
        storeProvider(retryReport, interfaceName, version, group, application);
        await().until(() -> reportCacheExecutor.getCompletedTaskCount() > completedTaskCount1);

        assertTrue(retryReport.store.isEmpty());
        assertFalse(retryReport.failedReports.isEmpty());
        assertNotNull(retryReport.metadataReportRetry.retryScheduledFuture);

        await().until(() -> retryExecutor.getCompletedTaskCount() > completedTaskCount2 + 2);
        assertNotEquals(0, retryReport.metadataReportRetry.retryCounter.get());
        assertTrue(retryReport.metadataReportRetry.retryCounter.get() >= 3);
        assertFalse(retryReport.store.isEmpty());
        assertTrue(retryReport.failedReports.isEmpty());
    }

    @Test
    void testRetryCancel() throws ClassNotFoundException {
        String interfaceName = "org.apache.dubbo.metadata.store.RetryTestService";
        String version = "1.0.0.retrycancel";
        String group = null;
        String application = "vic.retry";
        URL storeUrl = URL.valueOf("retryReport://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestServiceForRetryCancel?version=1.0.0.retrycancel&application=vic.retry");
        RetryMetadataReport retryReport = new RetryMetadataReport(storeUrl, 2, applicationModel);
        retryReport.metadataReportRetry.retryPeriod = 150L;
        retryReport.metadataReportRetry.retryTimesIfNonFail = 2;

        ScheduledThreadPoolExecutor retryExecutor = (ScheduledThreadPoolExecutor) retryReport.getMetadataReportRetry().getRetryExecutor();
        long completedTaskCount = retryExecutor.getCompletedTaskCount();
        storeProvider(retryReport, interfaceName, version, group, application);

        // Wait for the assignment of retryScheduledFuture to complete
        await().until(() -> retryReport.metadataReportRetry.retryScheduledFuture != null);
        assertFalse(retryReport.metadataReportRetry.retryScheduledFuture.isCancelled());
        assertFalse(retryReport.metadataReportRetry.retryExecutor.isShutdown());
        await().until(() -> retryExecutor.getCompletedTaskCount() > completedTaskCount + 2);
        assertTrue(retryReport.metadataReportRetry.retryScheduledFuture.isCancelled());
        assertTrue(retryReport.metadataReportRetry.retryExecutor.isShutdown());

    }

    private MetadataIdentifier storeProvider(AbstractMetadataReport abstractMetadataReport, String interfaceName, String version, String group, String application) throws ClassNotFoundException {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":4444/" + interfaceName + "?version=" + version + "&application="
            + application + (group == null ? "" : "&group=" + group) + "&testPKey=8989");

        MetadataIdentifier providerMetadataIdentifier = new MetadataIdentifier(interfaceName, version, group, PROVIDER_SIDE, application);
        Class interfaceClass = Class.forName(interfaceName);
        FullServiceDefinition fullServiceDefinition = ServiceDefinitionBuilder.buildFullDefinition(interfaceClass, url.getParameters());

        abstractMetadataReport.storeProviderMetadata(providerMetadataIdentifier, fullServiceDefinition);

        return providerMetadataIdentifier;
    }

    private MetadataIdentifier storeConsumer(AbstractMetadataReport abstractMetadataReport, String interfaceName, String version, String group, String application, Map<String, String> tmp) {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":4444/" + interfaceName + "?version=" + version + "&application="
            + application + (group == null ? "" : "&group=" + group) + "&testPKey=9090");

        tmp.putAll(url.getParameters());
        MetadataIdentifier consumerMetadataIdentifier = new MetadataIdentifier(interfaceName, version, group, CONSUMER_SIDE, application);

        abstractMetadataReport.storeConsumerMetadata(consumerMetadataIdentifier, tmp);

        return consumerMetadataIdentifier;
    }

    @Test
    void testPublishAll() throws ClassNotFoundException {
        ThreadPoolExecutor reportCacheExecutor = (ThreadPoolExecutor) abstractMetadataReport.getReportCacheExecutor();

        assertTrue(abstractMetadataReport.store.isEmpty());
        assertTrue(abstractMetadataReport.allMetadataReports.isEmpty());
        String interfaceName = "org.apache.dubbo.metadata.store.InterfaceNameTestService";
        String version = "1.0.0";
        String group = null;
        String application = "vic";
        long completedTaskCount1 = reportCacheExecutor.getCompletedTaskCount();
        MetadataIdentifier providerMetadataIdentifier1 = storeProvider(abstractMetadataReport, interfaceName, version, group, application);
        await().until(() -> reportCacheExecutor.getCompletedTaskCount() > completedTaskCount1);
        assertEquals(1, abstractMetadataReport.allMetadataReports.size());
        assertTrue(((FullServiceDefinition) abstractMetadataReport.allMetadataReports.get(providerMetadataIdentifier1)).getParameters().containsKey("testPKey"));

        long completedTaskCount2 = reportCacheExecutor.getCompletedTaskCount();
        MetadataIdentifier providerMetadataIdentifier2 = storeProvider(abstractMetadataReport, interfaceName, version + "_2", group + "_2", application);
        await().until(() -> reportCacheExecutor.getCompletedTaskCount() > completedTaskCount2);
        assertEquals(2, abstractMetadataReport.allMetadataReports.size());
        assertTrue(((FullServiceDefinition) abstractMetadataReport.allMetadataReports.get(providerMetadataIdentifier2)).getParameters().containsKey("testPKey"));
        assertEquals(((FullServiceDefinition) abstractMetadataReport.allMetadataReports.get(providerMetadataIdentifier2)).getParameters().get("version"), version + "_2");

        Map<String, String> tmpMap = new HashMap<>();
        tmpMap.put("testKey", "value");
        long completedTaskCount3 = reportCacheExecutor.getCompletedTaskCount();
        MetadataIdentifier consumerMetadataIdentifier = storeConsumer(abstractMetadataReport, interfaceName, version + "_3", group + "_3", application, tmpMap);
        await().until(() -> reportCacheExecutor.getCompletedTaskCount() > completedTaskCount3);
        assertEquals(3, abstractMetadataReport.allMetadataReports.size());

        Map tmpMapResult = (Map) abstractMetadataReport.allMetadataReports.get(consumerMetadataIdentifier);
        assertEquals("9090", tmpMapResult.get("testPKey"));
        assertEquals("value", tmpMapResult.get("testKey"));
        assertEquals(3, abstractMetadataReport.store.size());

        abstractMetadataReport.store.clear();

        assertEquals(0, abstractMetadataReport.store.size());

        long completedTaskCount4 = reportCacheExecutor.getCompletedTaskCount();
        abstractMetadataReport.publishAll();
        await().until(() -> reportCacheExecutor.getCompletedTaskCount() > completedTaskCount4);

        assertEquals(3, abstractMetadataReport.store.size());

        String v = abstractMetadataReport.store.get(providerMetadataIdentifier1.getUniqueKey(KeyTypeEnum.UNIQUE_KEY));
        FullServiceDefinition data = JsonUtils.toJavaObject(v, FullServiceDefinition.class);
        checkParam(data.getParameters(), application, version);

        String v2 = abstractMetadataReport.store.get(providerMetadataIdentifier2.getUniqueKey(KeyTypeEnum.UNIQUE_KEY));
        data = JsonUtils.toJavaObject(v2, FullServiceDefinition.class);
        checkParam(data.getParameters(), application, version + "_2");

        String v3 = abstractMetadataReport.store.get(consumerMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY));
        Map v3Map = JsonUtils.toJavaObject(v3, Map.class);
        checkParam(v3Map, application, version + "_3");
    }

    @Test
    void testCalculateStartTime() {
        for (int i = 0; i < 300; i++) {
            long t = abstractMetadataReport.calculateStartTime() + System.currentTimeMillis();
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(t);
            assertTrue(c.get(Calendar.HOUR_OF_DAY) >= 2);
            assertTrue(c.get(Calendar.HOUR_OF_DAY) <= 6);
        }
    }


    private void checkParam(Map<String, String> map, String application, String version) {
        assertEquals(map.get("application"), application);
        assertEquals(map.get("version"), version);
    }

    private static class NewMetadataReport extends AbstractMetadataReport {

        Map<String, String> store = new ConcurrentHashMap<>();

        public NewMetadataReport(URL metadataReportURL, ApplicationModel applicationModel) {
            super(metadataReportURL);
            this.applicationModel = applicationModel;
        }

        @Override
        protected void doStoreProviderMetadata(MetadataIdentifier providerMetadataIdentifier, String serviceDefinitions) {
            store.put(providerMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), serviceDefinitions);
        }

        @Override
        protected void doStoreConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, String serviceParameterString) {
            store.put(consumerMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), serviceParameterString);
        }

        @Override
        protected void doSaveMetadata(ServiceMetadataIdentifier metadataIdentifier, URL url) {
            throw new UnsupportedOperationException("This extension does not support working as a remote metadata center.");
        }

        @Override
        protected void doRemoveMetadata(ServiceMetadataIdentifier metadataIdentifier) {
            throw new UnsupportedOperationException("This extension does not support working as a remote metadata center.");
        }

        @Override
        protected List<String> doGetExportedURLs(ServiceMetadataIdentifier metadataIdentifier) {
            throw new UnsupportedOperationException("This extension does not support working as a remote metadata center.");
        }

        @Override
        protected void doSaveSubscriberData(SubscriberMetadataIdentifier subscriberMetadataIdentifier, String urls) {

        }

        @Override
        protected String doGetSubscribedURLs(SubscriberMetadataIdentifier metadataIdentifier) {
            throw new UnsupportedOperationException("This extension does not support working as a remote metadata center.");
        }

        @Override
        public String getServiceDefinition(MetadataIdentifier consumerMetadataIdentifier) {
            throw new UnsupportedOperationException("This extension does not support working as a remote metadata center.");
        }

        @Override
        public void removeServiceAppMappingListener(String serviceKey, MappingListener listener) {
            throw new UnsupportedOperationException("This extension does not support working as a remote metadata center.");
        }
    }

    private static class RetryMetadataReport extends AbstractMetadataReport {

        Map<String, String> store = new ConcurrentHashMap<>();
        int needRetryTimes;
        int executeTimes = 0;

        public RetryMetadataReport(URL metadataReportURL, int needRetryTimes, ApplicationModel applicationModel) {
            super(metadataReportURL);
            this.needRetryTimes = needRetryTimes;
            this.applicationModel = applicationModel;
        }

        @Override
        protected void doStoreProviderMetadata(MetadataIdentifier providerMetadataIdentifier, String serviceDefinitions) {
            ++executeTimes;
            System.out.println("***" + executeTimes + ";" + System.currentTimeMillis());
            if (executeTimes <= needRetryTimes) {
                throw new RuntimeException("must retry:" + executeTimes);
            }
            store.put(providerMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), serviceDefinitions);
        }

        @Override
        protected void doStoreConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, String serviceParameterString) {
            ++executeTimes;
            if (executeTimes <= needRetryTimes) {
                throw new RuntimeException("must retry:" + executeTimes);
            }
            store.put(consumerMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), serviceParameterString);
        }

        @Override
        protected void doSaveMetadata(ServiceMetadataIdentifier metadataIdentifier, URL url) {
            throw new UnsupportedOperationException("This extension does not support working as a remote metadata center.");
        }

        @Override
        protected void doRemoveMetadata(ServiceMetadataIdentifier metadataIdentifier) {
            throw new UnsupportedOperationException("This extension does not support working as a remote metadata center.");
        }

        @Override
        protected List<String> doGetExportedURLs(ServiceMetadataIdentifier metadataIdentifier) {
            throw new UnsupportedOperationException("This extension does not support working as a remote metadata center.");
        }

        @Override
        protected void doSaveSubscriberData(SubscriberMetadataIdentifier subscriberMetadataIdentifier, String urls) {

        }

        @Override
        protected String doGetSubscribedURLs(SubscriberMetadataIdentifier metadataIdentifier) {
            throw new UnsupportedOperationException("This extension does not support working as a remote metadata center.");
        }

        @Override
        public String getServiceDefinition(MetadataIdentifier consumerMetadataIdentifier) {
            throw new UnsupportedOperationException("This extension does not support working as a remote metadata center.");
        }

        @Override
        public void removeServiceAppMappingListener(String serviceKey, MappingListener listener) {
            throw new UnsupportedOperationException("This extension does not support working as a remote metadata center.");
        }

    }


}
