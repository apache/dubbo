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
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.metadata.report.identifier.KeyTypeEnum;
import org.apache.dubbo.metadata.report.identifier.ServiceMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.metadata.test.JTestMetadataReport4Test;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.SortedSet;


/**
 * 2019-08-27
 */
public class RemoteWritableMetadataServiceDelegateTest {
    static URL metadataURL = URL.valueOf("JTest://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.Tes33tService?version=1.0.0&application=vic");

    RemoteWritableMetadataServiceDelegate metadataReportService;

    String interfaceName = "org.apache.dubbo.metadata.store.InterfaceNameTestService80", version = "0.6.9", group = null;
    URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/?interface=" + interfaceName + "&version="
            + version + "&application=vicpubprovder&side=provider");

    @BeforeAll
    public static void beforeAll() {
        MetadataReportInstance.init(metadataURL);
    }

    @BeforeEach
    public void before() {
        metadataReportService = new RemoteWritableMetadataServiceDelegate();
    }


    @Test
    public void testInstance() {
        WritableMetadataService metadataReportService1 = WritableMetadataService.getExtension("remote");
        WritableMetadataService metadataReportService2 = WritableMetadataService.getExtension("remote");
        Assertions.assertSame(metadataReportService1, metadataReportService2);
        Assertions.assertTrue(metadataReportService1 instanceof RemoteWritableMetadataServiceDelegate);
    }

    @Test
    public void testPublishServiceDefinition() throws InterruptedException {
        String interfaceName = "org.apache.dubbo.metadata.store.InterfaceNameTestService2", version = "0.9.9", group = null;
        URL tmpUrl = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/?interface=" + interfaceName + "&version="
                + version + "&application=vicpubprovder&side=provider");
        metadataReportService.publishServiceDefinition(tmpUrl);
        Thread.sleep(150);
        String v = metadataReportService.getServiceDefinition(interfaceName, version, group);
        Assertions.assertNotNull(v);
    }

    @Test
    public void testExportURL() throws InterruptedException {
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.Test567Service?version=1.0.44&application=vicpubprovder&side=provider");
        metadataReportService.exportURL(url);
        Thread.sleep(100);
        Assertions.assertTrue(getInMemoryWriableMetadataService().exportedServiceURLs.size() == 1);
        Assertions.assertEquals(getInMemoryWriableMetadataService().exportedServiceURLs.get(url.getServiceKey()).first(), url);
    }

    @Test
    public void testSubscribeURL() throws InterruptedException {
        URL url = URL.valueOf("subscriber://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.Test0678Service?version=1.3.144&application=vicpubprovder&side=provider");
        int origSize = getInMemoryWriableMetadataService().subscribedServiceURLs.size();
        metadataReportService.subscribeURL(url);
        Thread.sleep(100);
        int size = getInMemoryWriableMetadataService().subscribedServiceURLs.size();
        Assertions.assertTrue(size - origSize == 1);
        Assertions.assertEquals(getInMemoryWriableMetadataService().subscribedServiceURLs.get(url.getServiceKey()).first(), url);
    }

    @Test
    public void testUnExportURL() throws InterruptedException {
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.Test0567Service?version=1.2.44&application=vicpubprovder&side=provider");
        int origSize = getInMemoryWriableMetadataService().exportedServiceURLs.size();
        metadataReportService.exportURL(url);
        Thread.sleep(100);
        int size = getInMemoryWriableMetadataService().exportedServiceURLs.size();
        Assertions.assertTrue(size - origSize == 1);
        Assertions.assertEquals(getInMemoryWriableMetadataService().exportedServiceURLs.get(url.getServiceKey()).first(), url);

        metadataReportService.unexportURL(url);
        int unexportSize = getInMemoryWriableMetadataService().exportedServiceURLs.size();
        Assertions.assertTrue(size - unexportSize == 1);
    }

    @Test
    public void testUnSubscribeURL() throws InterruptedException {
        URL url = URL.valueOf("subscriber://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.Test0678Service?version=1.5.477&application=vicpubprovder&side=provider");
        int origSize = getInMemoryWriableMetadataService().subscribedServiceURLs.size();
        metadataReportService.subscribeURL(url);
        Thread.sleep(100);
        int size = getInMemoryWriableMetadataService().subscribedServiceURLs.size();
        Assertions.assertTrue(size - origSize == 1);
        Assertions.assertEquals(getInMemoryWriableMetadataService().subscribedServiceURLs.get(url.getServiceKey()).first(), url);

        metadataReportService.unsubscribeURL(url);
        Thread.sleep(100);
        Assertions.assertTrue(getInMemoryWriableMetadataService().subscribedServiceURLs.size() == 0);
    }

    @Test
    public void testRefreshMetadataService() throws InterruptedException {
        URL publishUrl = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestRefreshMetadataService?version=1.6.8&application=vicpubprovder&side=provider");
        URL publishUrl2 = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestRefreshMetadata2Service?version=1.6.5&application=vicpubprovder&side=provider");
        metadataReportService.exportURL(publishUrl);
        metadataReportService.exportURL(publishUrl2);
        String exportedRevision = "9999";
        JTestMetadataReport4Test jTestMetadataReport4Test = (JTestMetadataReport4Test) MetadataReportInstance.getMetadataReport(true);
        int origSize = jTestMetadataReport4Test.store.size();
        int num = countNum();
        Assertions.assertTrue(metadataReportService.refreshMetadata(exportedRevision, "1109"));
        Thread.sleep(200);
        int size = jTestMetadataReport4Test.store.size();
        Assertions.assertTrue(size - origSize == num);
        Assertions.assertEquals(jTestMetadataReport4Test.store.get(getServiceMetadataIdentifier(publishUrl, exportedRevision).getUniqueKey(KeyTypeEnum.UNIQUE_KEY)), publishUrl.toFullString());
        Assertions.assertEquals(jTestMetadataReport4Test.store.get(getServiceMetadataIdentifier(publishUrl2, exportedRevision).getUniqueKey(KeyTypeEnum.UNIQUE_KEY)), publishUrl2.toFullString());
    }


    // unstable test
//    @Test
//    public void testRefreshMetadataSubscription() throws InterruptedException {
//        URL subscriberUrl1 = URL.valueOf("subscriber://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestRefreshMetadata00Service?version=2.0.8&application=vicpubprovder&side=provider");
//        URL subscriberUrl2 = URL.valueOf("subscriber://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestRefreshMetadata09Service?version=2.0.5&application=vicpubprovder&side=provider");
//        metadataReportService.subscribeURL(subscriberUrl1);
//        metadataReportService.subscribeURL(subscriberUrl2);
//        String exportedRevision = "9999";
//        String subscriberRevision = "2099";
//        String applicationName = "wriableMetadataService";
//        JTestMetadataReport4Test jTestMetadataReport4Test = (JTestMetadataReport4Test) MetadataReportInstance.getMetadataReport(true);
//        int origSize = jTestMetadataReport4Test.store.size();
//        ApplicationModel.setApplication(applicationName);
//        Assertions.assertTrue(metadataReportService.refreshMetadata(exportedRevision, subscriberRevision));
//        Thread.sleep(200);
//        int size = jTestMetadataReport4Test.store.size();
//        Assertions.assertTrue(size - origSize == 1);
//        String r = jTestMetadataReport4Test.store.get(getSubscriberMetadataIdentifier(
//                subscriberRevision).getUniqueKey(KeyTypeEnum.UNIQUE_KEY));
//        Assertions.assertNotNull(r);
//    }


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

    private InMemoryWritableMetadataService getInMemoryWriableMetadataService() {
        return (InMemoryWritableMetadataService) metadataReportService.defaultWritableMetadataService;
    }

    private int countNum() {
        int num = 0;
        for (SortedSet<URL> tmp : getInMemoryWriableMetadataService().exportedServiceURLs.values()) {
            num += tmp.size();
        }
        if (!getInMemoryWriableMetadataService().subscribedServiceURLs.values().isEmpty()) {
            num++;
        }
        return num;
    }
}
