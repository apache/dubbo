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
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;

/**
 *
 */
public class AbstractMetadataReportTest {

    private NewMetadataReport abstractMetadataReport;


    @BeforeEach
    public void before() {
        URL url = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        abstractMetadataReport = new NewMetadataReport(url);
    }

    @Test
    public void testGetProtocol() {
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic&side=provider");
        String protocol = abstractMetadataReport.getProtocol(url);
        Assertions.assertEquals(protocol, "provider");

        URL url2 = URL.valueOf("consumer://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        String protocol2 = abstractMetadataReport.getProtocol(url2);
        Assertions.assertEquals(protocol2, "consumer");
    }

    @Test
    public void testStoreProviderUsual() throws ClassNotFoundException, InterruptedException {
        String interfaceName = "org.apache.dubbo.metadata.integration.InterfaceNameTestService";
        String version = "1.0.0";
        String group = null;
        String application = "vic";
        MetadataIdentifier providerMetadataIdentifier = storePrivider(abstractMetadataReport, interfaceName, version, group, application);
        Thread.sleep(1500);
        Assertions.assertNotNull(abstractMetadataReport.store.get(providerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY)));
    }

    @Test
    public void testStoreProviderSync() throws ClassNotFoundException, InterruptedException {
        String interfaceName = "org.apache.dubbo.metadata.integration.InterfaceNameTestService";
        String version = "1.0.0";
        String group = null;
        String application = "vic";
        abstractMetadataReport.syncReport = true;
        MetadataIdentifier providerMetadataIdentifier = storePrivider(abstractMetadataReport, interfaceName, version, group, application);
        Assertions.assertNotNull(abstractMetadataReport.store.get(providerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY)));
    }

    @Test
    public void testFileExistAfterPut() throws InterruptedException, ClassNotFoundException {
        //just for one method
        URL singleUrl = URL.valueOf("redis://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.metadata.integration.InterfaceNameTestService?version=1.0.0&application=singleTest");
        NewMetadataReport singleMetadataReport = new NewMetadataReport(singleUrl);

        Assertions.assertFalse(singleMetadataReport.file.exists());

        String interfaceName = "org.apache.dubbo.metadata.integration.InterfaceNameTestService";
        String version = "1.0.0";
        String group = null;
        String application = "vic";
        MetadataIdentifier providerMetadataIdentifier = storePrivider(singleMetadataReport, interfaceName, version, group, application);

        Thread.sleep(2000);
        Assertions.assertTrue(singleMetadataReport.file.exists());
        Assertions.assertTrue(singleMetadataReport.properties.containsKey(providerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY)));
    }

    @Test
    public void testRetry() throws InterruptedException, ClassNotFoundException {
        String interfaceName = "org.apache.dubbo.metadata.integration.RetryTestService";
        String version = "1.0.0.retry";
        String group = null;
        String application = "vic.retry";
        URL storeUrl = URL.valueOf("retryReport://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestServiceForRetry?version=1.0.0.retry&application=vic.retry");
        RetryMetadataReport retryReport = new RetryMetadataReport(storeUrl, 2);
        retryReport.metadataReportRetry.retryPeriod = 400L;
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        Assertions.assertNull(retryReport.metadataReportRetry.retryScheduledFuture);
        Assertions.assertEquals(0,retryReport.metadataReportRetry.retryCounter.get());
        Assertions.assertTrue(retryReport.store.isEmpty());
        Assertions.assertTrue(retryReport.failedReports.isEmpty());


        storePrivider(retryReport, interfaceName, version, group, application);
        Thread.sleep(150);

        Assertions.assertTrue(retryReport.store.isEmpty());
        Assertions.assertFalse(retryReport.failedReports.isEmpty());
        Assertions.assertNotNull(retryReport.metadataReportRetry.retryScheduledFuture);
        Thread.sleep(2000L);
        Assertions.assertTrue(retryReport.metadataReportRetry.retryCounter.get() != 0);
        Assertions.assertTrue(retryReport.metadataReportRetry.retryCounter.get() >= 3);
        Assertions.assertFalse(retryReport.store.isEmpty());
        Assertions.assertTrue(retryReport.failedReports.isEmpty());
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
        Thread.sleep(80);

        Assertions.assertFalse(retryReport.metadataReportRetry.retryScheduledFuture.isCancelled());
        Assertions.assertFalse(retryReport.metadataReportRetry.retryExecutor.isShutdown());
        Thread.sleep(1000L);
        Assertions.assertTrue(retryReport.metadataReportRetry.retryScheduledFuture.isCancelled());
        Assertions.assertTrue(retryReport.metadataReportRetry.retryExecutor.isShutdown());

    }

    private MetadataIdentifier storePrivider(AbstractMetadataReport abstractMetadataReport, String interfaceName, String version, String group, String application) throws ClassNotFoundException {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":4444/" + interfaceName + "?version=" + version + "&application="
                + application + (group == null ? "" : "&group=" + group) + "&testPKey=8989");

        MetadataIdentifier providerMetadataIdentifier = new MetadataIdentifier(interfaceName, version, group, PROVIDER_SIDE,application);
        Class interfaceClass = Class.forName(interfaceName);
        FullServiceDefinition fullServiceDefinition = ServiceDefinitionBuilder.buildFullDefinition(interfaceClass, url.getParameters());

        abstractMetadataReport.storeProviderMetadata(providerMetadataIdentifier, fullServiceDefinition);

        return providerMetadataIdentifier;
    }

    private MetadataIdentifier storeConsumer(AbstractMetadataReport abstractMetadataReport, String interfaceName, String version, String group, String application, Map<String, String> tmp) throws ClassNotFoundException {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":4444/" + interfaceName + "?version=" + version + "&application="
                + application + (group == null ? "" : "&group=" + group) + "&testPKey=9090");

        tmp.putAll(url.getParameters());
        MetadataIdentifier consumerMetadataIdentifier = new MetadataIdentifier(interfaceName, version, group, CONSUMER_SIDE, application);

        abstractMetadataReport.storeConsumerMetadata(consumerMetadataIdentifier, tmp);

        return consumerMetadataIdentifier;
    }

    @Test
    public void testPublishAll() throws ClassNotFoundException, InterruptedException {

        Assertions.assertTrue(abstractMetadataReport.store.isEmpty());
        Assertions.assertTrue(abstractMetadataReport.allMetadataReports.isEmpty());
        String interfaceName = "org.apache.dubbo.metadata.integration.InterfaceNameTestService";
        String version = "1.0.0";
        String group = null;
        String application = "vic";
        MetadataIdentifier providerMetadataIdentifier1 = storePrivider(abstractMetadataReport, interfaceName, version, group, application);
        Thread.sleep(1000);
        Assertions.assertEquals(abstractMetadataReport.allMetadataReports.size(), 1);
        Assertions.assertTrue(((FullServiceDefinition) abstractMetadataReport.allMetadataReports.get(providerMetadataIdentifier1)).getParameters().containsKey("testPKey"));

        MetadataIdentifier providerMetadataIdentifier2 = storePrivider(abstractMetadataReport, interfaceName, version + "_2", group + "_2", application);
        Thread.sleep(1000);
        Assertions.assertEquals(abstractMetadataReport.allMetadataReports.size(), 2);
        Assertions.assertTrue(((FullServiceDefinition) abstractMetadataReport.allMetadataReports.get(providerMetadataIdentifier2)).getParameters().containsKey("testPKey"));
        Assertions.assertEquals(((FullServiceDefinition) abstractMetadataReport.allMetadataReports.get(providerMetadataIdentifier2)).getParameters().get("version"), version + "_2");

        Map<String, String> tmpMap = new HashMap<>();
        tmpMap.put("testKey", "value");
        MetadataIdentifier consumerMetadataIdentifier = storeConsumer(abstractMetadataReport, interfaceName, version + "_3", group + "_3", application, tmpMap);
        Thread.sleep(1000);
        Assertions.assertEquals(abstractMetadataReport.allMetadataReports.size(), 3);

        Map tmpMapResult = (Map) abstractMetadataReport.allMetadataReports.get(consumerMetadataIdentifier);
        Assertions.assertEquals(tmpMapResult.get("testPKey"), "9090");
        Assertions.assertEquals(tmpMapResult.get("testKey"), "value");
        Assertions.assertEquals(3,abstractMetadataReport.store.size());

        abstractMetadataReport.store.clear();

        Assertions.assertEquals(0,abstractMetadataReport.store.size());

        abstractMetadataReport.publishAll();
        Thread.sleep(200);

        Assertions.assertEquals(3,abstractMetadataReport.store.size());

        String v = abstractMetadataReport.store.get(providerMetadataIdentifier1.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY));
        Gson gson = new Gson();
        FullServiceDefinition data = gson.fromJson(v, FullServiceDefinition.class);
        checkParam(data.getParameters(), application, version);

        String v2 = abstractMetadataReport.store.get(providerMetadataIdentifier2.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY));
        gson = new Gson();
        data = gson.fromJson(v2, FullServiceDefinition.class);
        checkParam(data.getParameters(), application, version + "_2");

        String v3 = abstractMetadataReport.store.get(consumerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY));
        gson = new Gson();
        Map v3Map = gson.fromJson(v3, Map.class);
        checkParam(v3Map, application, version + "_3");
    }

    @Test
    public void testCalculateStartTime() {
        for (int i = 0; i < 300; i++) {
            long t = abstractMetadataReport.calculateStartTime() + System.currentTimeMillis();
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(t);
            Assertions.assertTrue(c.get(Calendar.HOUR_OF_DAY) >= 2);
            Assertions.assertTrue(c.get(Calendar.HOUR_OF_DAY) <= 6);
        }
    }

    private FullServiceDefinition toServiceDefinition(String v) {
        Gson gson = new Gson();
        FullServiceDefinition data = gson.fromJson(v, FullServiceDefinition.class);
        return data;
    }

    private void checkParam(Map<String, String> map, String application, String version) {
        Assertions.assertEquals(map.get("application"), application);
        Assertions.assertEquals(map.get("version"), version);
    }

    private Map<String, String> queryUrlToMap(String urlQuery) {
        if (urlQuery == null) {
            return Collections.emptyMap();
        }
        String[] pairs = urlQuery.split("&");
        Map<String, String> map = new HashMap<>();
        for (String pairStr : pairs) {
            String[] pair = pairStr.split("=");
            map.put(pair[0], pair[1]);
        }
        return map;
    }


    private static class NewMetadataReport extends AbstractMetadataReport {

        Map<String, String> store = new ConcurrentHashMap<>();

        public NewMetadataReport(URL metadataReportURL) {
            super(metadataReportURL);
        }

        @Override
        protected void doStoreProviderMetadata(MetadataIdentifier providerMetadataIdentifier, String serviceDefinitions) {
            store.put(providerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY), serviceDefinitions);
        }

        @Override
        protected void doStoreConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, String serviceParameterString) {
            store.put(consumerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY), serviceParameterString);
        }
    }

    private static class RetryMetadataReport extends AbstractMetadataReport {

        Map<String, String> store = new ConcurrentHashMap<>();
        int needRetryTimes;
        int executeTimes = 0;

        public RetryMetadataReport(URL metadataReportURL, int needRetryTimes) {
            super(metadataReportURL);
            this.needRetryTimes = needRetryTimes;
        }

        @Override
        protected void doStoreProviderMetadata(MetadataIdentifier providerMetadataIdentifier, String serviceDefinitions) {
            ++executeTimes;
            System.out.println("***" + executeTimes + ";" + System.currentTimeMillis());
            if (executeTimes <= needRetryTimes) {
                throw new RuntimeException("must retry:" + executeTimes);
            }
            store.put(providerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY), serviceDefinitions);
        }

        @Override
        protected void doStoreConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, String serviceParameterString) {
            ++executeTimes;
            if (executeTimes <= needRetryTimes) {
                throw new RuntimeException("must retry:" + executeTimes);
            }
            store.put(consumerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY), serviceParameterString);
        }

    }


}
