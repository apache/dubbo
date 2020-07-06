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
package org.apache.dubbo.metadata.store;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.metadata.report.identifier.KeyTypeEnum;
import org.apache.dubbo.metadata.report.identifier.ServiceMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.metadata.test.JTestMetadataReport4Test;
import org.apache.dubbo.rpc.model.ApplicationModel;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * 2018/9/14
 */
public class RemoteWritableMetadataServiceTest {
    URL url = URL.valueOf("JTest://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
    RemoteWritableMetadataService metadataReportService1;

    @BeforeEach
    public void before() {
        metadataReportService1 = new RemoteWritableMetadataService();
        MetadataReportInstance.init(url);
    }

    @Test
    public void testPublishProviderNoInterfaceName() {
        URL publishUrl = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vicpubprovder&side=provider");
        metadataReportService1.publishServiceDefinition(publishUrl);

        Assertions.assertTrue(metadataReportService1.getMetadataReport() instanceof JTestMetadataReport4Test);

        JTestMetadataReport4Test jTestMetadataReport4Test = (JTestMetadataReport4Test) metadataReportService1.getMetadataReport();
        Assertions.assertTrue(!jTestMetadataReport4Test.store.containsKey(JTestMetadataReport4Test.getProviderKey(publishUrl)));

    }

    @Test
    public void testPublishProviderWrongInterface() {

        URL publishUrl = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vicpu&interface=ccc&side=provider");
        metadataReportService1.publishServiceDefinition(publishUrl);

        Assertions.assertTrue(metadataReportService1.getMetadataReport() instanceof JTestMetadataReport4Test);

        JTestMetadataReport4Test jTestMetadataReport4Test = (JTestMetadataReport4Test) metadataReportService1.getMetadataReport();
        Assertions.assertTrue(!jTestMetadataReport4Test.store.containsKey(JTestMetadataReport4Test.getProviderKey(publishUrl)));

    }

    @Test
    public void testPublishProviderContainInterface() throws InterruptedException {

        URL publishUrl = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.3&application=vicpubp&interface=org.apache.dubbo.metadata.store.InterfaceNameTestService&side=provider");
        metadataReportService1.publishServiceDefinition(publishUrl);
        Thread.sleep(300);

        Assertions.assertTrue(metadataReportService1.getMetadataReport() instanceof JTestMetadataReport4Test);

        JTestMetadataReport4Test jTestMetadataReport4Test = (JTestMetadataReport4Test) metadataReportService1.getMetadataReport();
        Assertions.assertTrue(jTestMetadataReport4Test.store.containsKey(JTestMetadataReport4Test.getProviderKey(publishUrl)));

        String value = jTestMetadataReport4Test.store.get(JTestMetadataReport4Test.getProviderKey(publishUrl));
        FullServiceDefinition fullServiceDefinition = toServiceDefinition(value);
        Map<String, String> map = fullServiceDefinition.getParameters();
        Assertions.assertEquals(map.get("application"), "vicpubp");
        Assertions.assertEquals(map.get("version"), "1.0.3");
        Assertions.assertEquals(map.get("interface"), "org.apache.dubbo.metadata.store.InterfaceNameTestService");
    }

    @Test
    public void testPublishConsumer() throws InterruptedException {

        URL publishUrl = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.x&application=vicpubconsumer&side=consumer");
        metadataReportService1.publishServiceDefinition(publishUrl);
        Thread.sleep(300);

        Assertions.assertTrue(metadataReportService1.getMetadataReport() instanceof JTestMetadataReport4Test);

        JTestMetadataReport4Test jTestMetadataReport4Test = (JTestMetadataReport4Test) metadataReportService1.getMetadataReport();
        Assertions.assertTrue(jTestMetadataReport4Test.store.containsKey(JTestMetadataReport4Test.getConsumerKey(publishUrl)));

        String value = jTestMetadataReport4Test.store.get(JTestMetadataReport4Test.getConsumerKey(publishUrl));
        Gson gson = new Gson();
        Map<String, String> map = gson.fromJson(value, Map.class);
        Assertions.assertEquals(map.get("application"), "vicpubconsumer");
        Assertions.assertEquals(map.get("version"), "1.0.x");

    }

    @Test
    public void testPublishServiceDefinition() {
        URL publishUrl = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vicpubprovder&side=provider");
        metadataReportService1.publishServiceDefinition(publishUrl);

        Assertions.assertTrue(metadataReportService1.getMetadataReport() instanceof JTestMetadataReport4Test);

        JTestMetadataReport4Test jTestMetadataReport4Test = (JTestMetadataReport4Test) metadataReportService1.getMetadataReport();
        Assertions.assertTrue(!jTestMetadataReport4Test.store.containsKey(JTestMetadataReport4Test.getProviderKey(publishUrl)));

    }

    @Test
    public void testUnexportURL() {

    }

    @Test
    public void testRefreshMetadataService() throws InterruptedException {
        URL publishUrl = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestRefreshMetadataService?version=1.0.8&application=vicpubprovder&side=provider");
        URL publishUrl2 = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestRefreshMetadata2Service?version=1.0.5&application=vicpubprovder&side=provider");
        metadataReportService1.exportURL(publishUrl);
        metadataReportService1.exportURL(publishUrl2);
        String exportedRevision = "9999";
        JTestMetadataReport4Test jTestMetadataReport4Test = (JTestMetadataReport4Test) metadataReportService1.getMetadataReport();
        int origSize = jTestMetadataReport4Test.store.size();
        Assertions.assertTrue(metadataReportService1.refreshMetadata(exportedRevision, "1109"));
        Thread.sleep(200);
        int size = jTestMetadataReport4Test.store.size();
        Assertions.assertEquals(origSize, size);
        Assertions.assertNull(jTestMetadataReport4Test.store.get(getServiceMetadataIdentifier(publishUrl, exportedRevision).getUniqueKey(KeyTypeEnum.UNIQUE_KEY)));
        Assertions.assertNull(jTestMetadataReport4Test.store.get(getServiceMetadataIdentifier(publishUrl2, exportedRevision).getUniqueKey(KeyTypeEnum.UNIQUE_KEY)));
    }

    @Test
    public void testRefreshMetadataSubscription() throws InterruptedException {
        URL subscriberUrl1 = URL.valueOf("subscriber://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestRefreshMetadata00Service?version=1.0.8&application=vicpubprovder&side=provider");
        URL subscriberUrl2 = URL.valueOf("subscriber://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestRefreshMetadata09Service?version=1.0.5&application=vicpubprovder&side=provider");
        metadataReportService1.subscribeURL(subscriberUrl1);
        metadataReportService1.subscribeURL(subscriberUrl2);
        String exportedRevision = "9999";
        String subscriberRevision = "2099";
        String applicationName = "wriableMetadataService";
        JTestMetadataReport4Test jTestMetadataReport4Test = (JTestMetadataReport4Test) metadataReportService1.getMetadataReport();
        int origSize = jTestMetadataReport4Test.store.size();
        ApplicationModel.setApplication(applicationName);
        Assertions.assertTrue(metadataReportService1.refreshMetadata(exportedRevision, subscriberRevision));
        Thread.sleep(200);
        int size = jTestMetadataReport4Test.store.size();
        Assertions.assertEquals(origSize, size);
        String r = jTestMetadataReport4Test.store.get(getSubscriberMetadataIdentifier(
                subscriberRevision).getUniqueKey(KeyTypeEnum.UNIQUE_KEY));
        Assertions.assertNull(r);
    }

    private ServiceMetadataIdentifier getServiceMetadataIdentifier(URL publishUrl, String exportedRevision) {
        ServiceMetadataIdentifier serviceMetadataIdentifier = new ServiceMetadataIdentifier(publishUrl);
        serviceMetadataIdentifier.setRevision(exportedRevision);
        serviceMetadataIdentifier.setProtocol(publishUrl.getProtocol());
        return serviceMetadataIdentifier;
    }

    private SubscriberMetadataIdentifier getSubscriberMetadataIdentifier(String subscriberRevision) {
        SubscriberMetadataIdentifier subscriberMetadataIdentifier = new SubscriberMetadataIdentifier();
        subscriberMetadataIdentifier.setRevision(subscriberRevision);
        subscriberMetadataIdentifier.setApplication(ApplicationModel.getApplication());
        return subscriberMetadataIdentifier;
    }

    private FullServiceDefinition toServiceDefinition(String urlQuery) {
        Gson gson = new Gson();
        return gson.fromJson(urlQuery, FullServiceDefinition.class);
    }

}
