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
package org.apache.dubbo.metadata.store.zookeeper;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.metadata.MappingChangedEvent;
import org.apache.dubbo.metadata.MappingListener;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.identifier.KeyTypeEnum;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.ServiceMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.remoting.zookeeper.curator5.ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.curator5.ZookeeperClientManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.metadata.ServiceNameMapping.DEFAULT_MAPPING_GROUP;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ZookeeperMetadataReportTest {

    private ZookeeperMetadataReport zookeeperMetadataReport;

    /**
     * Uses Map<String, String> to simulate ZK node storage:
     * key corresponds to the ZK node path, and value corresponds to the node content.
     */
    private final Map<String, String> zkDataStore = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        // 1. Create mock objects
        ZookeeperClientManager mockClientManager = mock(ZookeeperClientManager.class);
        ZookeeperClient mockZkClient = mock(ZookeeperClient.class);

        // 2. When connect(...) is called, return mockZkClient
        when(mockClientManager.connect(any(URL.class))).thenReturn(mockZkClient);

        // 3. Simulate createOrUpdate behavior: store path->data in zkDataStore
        doAnswer(invocation -> {
                    String path = invocation.getArgument(0, String.class);
                    String data = invocation.getArgument(1, String.class);
                    zkDataStore.put(path, data);
                    return null;
                })
                .when(mockZkClient)
                .createOrUpdate(anyString(), anyString(), eq(false));

        // 4. Simulate delete behavior: remove key=path from zkDataStore
        doAnswer(invocation -> {
                    String path = invocation.getArgument(0, String.class);
                    zkDataStore.remove(path);
                    return null;
                })
                .when(mockZkClient)
                .delete(anyString());

        // 5. Simulate getContent behavior: retrieve value by key=path from zkDataStore
        when(mockZkClient.getContent(anyString())).thenAnswer(invocation -> {
            String path = invocation.getArgument(0, String.class);
            return zkDataStore.get(path);
        });

        // 6. Construct a fake ZK URL and create ZookeeperMetadataReport using mockClientManager
        URL fakeZkUrl = URL.valueOf("zookeeper://127.0.0.1:2181");
        zookeeperMetadataReport = new ZookeeperMetadataReport(fakeZkUrl, mockClientManager);
    }

    private void deletePath(MetadataIdentifier metadataIdentifier, ZookeeperMetadataReport zookeeperMetadataReport) {
        String category = zookeeperMetadataReport.toRootDir() + metadataIdentifier.getUniqueKey(KeyTypeEnum.PATH);
        zookeeperMetadataReport.zkClient.delete(category);
    }

    @Test
    void testStoreProvider() throws ClassNotFoundException, InterruptedException {
        String interfaceName = "org.apache.dubbo.metadata.store.zookeeper.ZookeeperMetadataReport4TstService";
        String version = "1.0.0.zk.md";
        String group = null;
        String application = "vic.zk.md";
        MetadataIdentifier providerMetadataIdentifier =
                storePrivider(zookeeperMetadataReport, interfaceName, version, group, application);

        String fileContent = zookeeperMetadataReport.zkClient.getContent(
                zookeeperMetadataReport.getNodePath(providerMetadataIdentifier));
        Assertions.assertNotNull(fileContent);

        deletePath(providerMetadataIdentifier, zookeeperMetadataReport);
        fileContent = zookeeperMetadataReport.zkClient.getContent(
                zookeeperMetadataReport.getNodePath(providerMetadataIdentifier));
        Assertions.assertNull(fileContent);

        providerMetadataIdentifier = storePrivider(zookeeperMetadataReport, interfaceName, version, group, application);
        fileContent = zookeeperMetadataReport.zkClient.getContent(
                zookeeperMetadataReport.getNodePath(providerMetadataIdentifier));
        Assertions.assertNotNull(fileContent);

        FullServiceDefinition fullServiceDefinition = JsonUtils.toJavaObject(fileContent, FullServiceDefinition.class);
        Assertions.assertEquals("zkTest", fullServiceDefinition.getParameters().get("paramTest"));
    }

    @Test
    void testConsumer() throws ClassNotFoundException, InterruptedException {
        String interfaceName = "org.apache.dubbo.metadata.store.zookeeper.ZookeeperMetadataReport4TstService";
        String version = "1.0.0.zk.md";
        String group = null;
        String application = "vic.zk.md";
        MetadataIdentifier consumerMetadataIdentifier =
                storeConsumer(zookeeperMetadataReport, interfaceName, version, group, application);

        String fileContent = zookeeperMetadataReport.zkClient.getContent(
                zookeeperMetadataReport.getNodePath(consumerMetadataIdentifier));
        Assertions.assertNotNull(fileContent);

        deletePath(consumerMetadataIdentifier, zookeeperMetadataReport);
        fileContent = zookeeperMetadataReport.zkClient.getContent(
                zookeeperMetadataReport.getNodePath(consumerMetadataIdentifier));
        Assertions.assertNull(fileContent);

        consumerMetadataIdentifier = storeConsumer(zookeeperMetadataReport, interfaceName, version, group, application);
        fileContent = zookeeperMetadataReport.zkClient.getContent(
                zookeeperMetadataReport.getNodePath(consumerMetadataIdentifier));
        Assertions.assertNotNull(fileContent);
        Assertions.assertEquals("{\"paramConsumerTest\":\"zkCm\"}", fileContent);
    }

    @Test
    void testDoSaveMetadata() throws ExecutionException, InterruptedException {
        String interfaceName = "org.apache.dubbo.metadata.store.zookeeper.ZookeeperMetadataReport4TstService";
        String version = "1.0.0";
        String group = null;
        String application = "etc-metadata-report-consumer-test";
        String revision = "90980";
        String protocol = "xxx";
        URL url = generateURL(interfaceName, version, group, application);
        ServiceMetadataIdentifier serviceMetadataIdentifier =
                new ServiceMetadataIdentifier(interfaceName, version, group, "provider", revision, protocol);
        zookeeperMetadataReport.doSaveMetadata(serviceMetadataIdentifier, url);

        String fileContent = zookeeperMetadataReport.zkClient.getContent(
                zookeeperMetadataReport.getNodePath(serviceMetadataIdentifier));
        Assertions.assertNotNull(fileContent);

        Assertions.assertEquals(fileContent, URL.encode(url.toFullString()));
    }

    @Test
    void testDoRemoveMetadata() {
        String interfaceName = "org.apache.dubbo.metadata.store.zookeeper.ZookeeperMetadataReport4TstService";
        String version = "1.0.0";
        String group = null;
        String application = "etc-metadata-report-consumer-test";
        String revision = "90980";
        String protocol = "xxx";
        URL url = generateURL(interfaceName, version, group, application);
        ServiceMetadataIdentifier serviceMetadataIdentifier =
                new ServiceMetadataIdentifier(interfaceName, version, group, "provider", revision, protocol);
        zookeeperMetadataReport.doSaveMetadata(serviceMetadataIdentifier, url);
        String fileContent = zookeeperMetadataReport.zkClient.getContent(
                zookeeperMetadataReport.getNodePath(serviceMetadataIdentifier));

        Assertions.assertNotNull(fileContent);

        zookeeperMetadataReport.doRemoveMetadata(serviceMetadataIdentifier);

        fileContent = zookeeperMetadataReport.zkClient.getContent(
                zookeeperMetadataReport.getNodePath(serviceMetadataIdentifier));
        Assertions.assertNull(fileContent);
    }

    @Test
    void testDoGetExportedURLs() {
        String interfaceName = "org.apache.dubbo.metadata.store.zookeeper.ZookeeperMetadataReport4TstService";
        String version = "1.0.0";
        String group = null;
        String application = "etc-metadata-report-consumer-test";
        String revision = "90980";
        String protocol = "xxx";
        URL url = generateURL(interfaceName, version, group, application);
        ServiceMetadataIdentifier serviceMetadataIdentifier =
                new ServiceMetadataIdentifier(interfaceName, version, group, "provider", revision, protocol);
        zookeeperMetadataReport.doSaveMetadata(serviceMetadataIdentifier, url);

        List<String> r = zookeeperMetadataReport.doGetExportedURLs(serviceMetadataIdentifier);
        Assertions.assertEquals(1, r.size());

        String fileContent = r.get(0);
        Assertions.assertNotNull(fileContent);

        Assertions.assertEquals(fileContent, url.toFullString());
    }

    @Test
    void testDoSaveSubscriberData() {
        String interfaceName = "org.apache.dubbo.metadata.store.zookeeper.ZookeeperMetadataReport4TstService";
        String version = "1.0.0";
        String group = null;
        String application = "etc-metadata-report-consumer-test";
        String revision = "90980";
        URL url = generateURL(interfaceName, version, group, application);
        SubscriberMetadataIdentifier subscriberMetadataIdentifier =
                new SubscriberMetadataIdentifier(application, revision);
        String r = JsonUtils.toJson(Collections.singletonList(url.toString()));
        zookeeperMetadataReport.doSaveSubscriberData(subscriberMetadataIdentifier, r);

        String fileContent = zookeeperMetadataReport.zkClient.getContent(
                zookeeperMetadataReport.getNodePath(subscriberMetadataIdentifier));

        Assertions.assertNotNull(fileContent);

        Assertions.assertEquals(fileContent, r);
    }

    @Test
    void testDoGetSubscribedURLs() {
        String interfaceName = "org.apache.dubbo.metadata.store.zookeeper.ZookeeperMetadataReport4TstService";
        String version = "1.0.0";
        String group = null;
        String application = "etc-metadata-report-consumer-test";
        String revision = "90980";
        URL url = generateURL(interfaceName, version, group, application);
        SubscriberMetadataIdentifier subscriberMetadataIdentifier =
                new SubscriberMetadataIdentifier(application, revision);
        String r = JsonUtils.toJson(Collections.singletonList(url.toString()));
        zookeeperMetadataReport.doSaveSubscriberData(subscriberMetadataIdentifier, r);

        String fileContent = zookeeperMetadataReport.zkClient.getContent(
                zookeeperMetadataReport.getNodePath(subscriberMetadataIdentifier));

        Assertions.assertNotNull(fileContent);

        Assertions.assertEquals(fileContent, r);
    }

    private MetadataIdentifier storePrivider(
            MetadataReport zookeeperMetadataReport,
            String interfaceName,
            String version,
            String group,
            String application)
            throws ClassNotFoundException, InterruptedException {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":4444/" + interfaceName
                + "?paramTest=zkTest&version=" + version + "&application=" + application
                + (group == null ? "" : "&group=" + group));

        MetadataIdentifier providerMetadataIdentifier =
                new MetadataIdentifier(interfaceName, version, group, PROVIDER_SIDE, application);
        Class<?> interfaceClass = Class.forName(interfaceName);
        FullServiceDefinition fullServiceDefinition =
                ServiceDefinitionBuilder.buildFullDefinition(interfaceClass, url.getParameters());

        zookeeperMetadataReport.storeProviderMetadata(providerMetadataIdentifier, fullServiceDefinition);
        Thread.sleep(1000);
        return providerMetadataIdentifier;
    }

    private MetadataIdentifier storeConsumer(
            MetadataReport zookeeperMetadataReport,
            String interfaceName,
            String version,
            String group,
            String application)
            throws InterruptedException {
        MetadataIdentifier consumerMetadataIdentifier =
                new MetadataIdentifier(interfaceName, version, group, CONSUMER_SIDE, application);

        Map<String, String> tmp = new HashMap<>();
        tmp.put("paramConsumerTest", "zkCm");
        zookeeperMetadataReport.storeConsumerMetadata(consumerMetadataIdentifier, tmp);
        Thread.sleep(1000);

        return consumerMetadataIdentifier;
    }

    private URL generateURL(String interfaceName, String version, String group, String application) {
        return URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":8989/" + interfaceName
                + "?paramTest=etcdTest&version="
                + version + "&application="
                + application + (group == null ? "" : "&group=" + group));
    }

    @Test
    void testMapping() throws InterruptedException {
        String serviceKey = ZookeeperMetadataReportTest.class.getName();
        URL url = URL.valueOf("test://127.0.0.1:8888/" + serviceKey);
        String appNames = "demo1,demo2";

        CountDownLatch latch = new CountDownLatch(1);
        String pathKey = zookeeperMetadataReport.toRootDir() + DEFAULT_MAPPING_GROUP + "/" + serviceKey;
        zkDataStore.put(pathKey, ""); // Alternatively, do not put anything, default to null

        Set<String> serviceAppMapping = zookeeperMetadataReport.getServiceAppMapping(
                serviceKey,
                new MappingListener() {
                    @Override
                    public void onEvent(MappingChangedEvent event) {
                        Set<String> apps = event.getApps();
                        Assertions.assertEquals(2, apps.size());
                        Assertions.assertTrue(apps.contains("demo1"));
                        Assertions.assertTrue(apps.contains("demo2"));
                        latch.countDown();
                    }

                    @Override
                    public void stop() {}
                },
                url);
        Assertions.assertTrue(serviceAppMapping.isEmpty());

        zookeeperMetadataReport.registerServiceAppMapping(serviceKey, DEFAULT_MAPPING_GROUP, appNames, null);
        latch.countDown();
        latch.await();
    }

    @Test
    void testAppMetadata() {
        String serviceKey = ZookeeperMetadataReportTest.class.getName();
        String appName = "demo";
        URL url = URL.valueOf("test://127.0.0.1:8888/" + serviceKey);
        MetadataInfo metadataInfo = new MetadataInfo(appName);
        metadataInfo.addService(url);

        SubscriberMetadataIdentifier identifier =
                new SubscriberMetadataIdentifier(appName, metadataInfo.calAndGetRevision());
        MetadataInfo appMetadata = zookeeperMetadataReport.getAppMetadata(identifier, Collections.emptyMap());
        Assertions.assertNull(appMetadata);

        zookeeperMetadataReport.publishAppMetadata(identifier, metadataInfo);
        appMetadata = zookeeperMetadataReport.getAppMetadata(identifier, Collections.emptyMap());
        Assertions.assertNotNull(appMetadata);
        Assertions.assertEquals(appMetadata.calAndGetRevision(), metadataInfo.calAndGetRevision());
    }
}
