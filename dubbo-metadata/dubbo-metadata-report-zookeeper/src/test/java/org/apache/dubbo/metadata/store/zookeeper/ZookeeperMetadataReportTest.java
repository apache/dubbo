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
import org.apache.dubbo.common.config.configcenter.ConfigItem;
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
import org.apache.dubbo.rpc.model.ApplicationModel;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.metadata.ServiceNameMapping.DEFAULT_MAPPING_GROUP;

/**
 * 2018/10/9
 */
public class ZookeeperMetadataReportTest {
    private ZookeeperMetadataReport zookeeperMetadataReport;
    private URL registryUrl;
    private ZookeeperMetadataReportFactory zookeeperMetadataReportFactory;
    private static String zookeeperConnectionAddress1;

    @BeforeAll
    public static void beforeAll() {
        zookeeperConnectionAddress1 = System.getProperty("zookeeper.connection.address.1");
    }

    @BeforeEach
    public void setUp() throws Exception {
        this.registryUrl = URL.valueOf(zookeeperConnectionAddress1);

        zookeeperMetadataReportFactory = new ZookeeperMetadataReportFactory(ApplicationModel.defaultModel());
        this.zookeeperMetadataReport = (ZookeeperMetadataReport) zookeeperMetadataReportFactory.getMetadataReport(registryUrl);
    }

    private void deletePath(MetadataIdentifier metadataIdentifier, ZookeeperMetadataReport zookeeperMetadataReport) {
        String category = zookeeperMetadataReport.toRootDir() + metadataIdentifier.getUniqueKey(KeyTypeEnum.PATH);
        zookeeperMetadataReport.zkClient.delete(category);
    }

    @Test
    public void testStoreProvider() throws ClassNotFoundException, InterruptedException {
        String interfaceName = "org.apache.dubbo.metadata.store.zookeeper.ZookeeperMetadataReport4TstService";
        String version = "1.0.0.zk.md";
        String group = null;
        String application = "vic.zk.md";
        MetadataIdentifier providerMetadataIdentifier = storePrivider(zookeeperMetadataReport, interfaceName, version, group, application);

        String fileContent = zookeeperMetadataReport.zkClient.getContent(zookeeperMetadataReport.getNodePath(providerMetadataIdentifier));
        fileContent = waitSeconds(fileContent, 3500, zookeeperMetadataReport.getNodePath(providerMetadataIdentifier));
        Assertions.assertNotNull(fileContent);

        deletePath(providerMetadataIdentifier, zookeeperMetadataReport);
        fileContent = zookeeperMetadataReport.zkClient.getContent(zookeeperMetadataReport.getNodePath(providerMetadataIdentifier));
        fileContent = waitSeconds(fileContent, 1000, zookeeperMetadataReport.getNodePath(providerMetadataIdentifier));
        Assertions.assertNull(fileContent);


        providerMetadataIdentifier = storePrivider(zookeeperMetadataReport, interfaceName, version, group, application);
        fileContent = zookeeperMetadataReport.zkClient.getContent(zookeeperMetadataReport.getNodePath(providerMetadataIdentifier));
        fileContent = waitSeconds(fileContent, 3500, zookeeperMetadataReport.getNodePath(providerMetadataIdentifier));
        Assertions.assertNotNull(fileContent);

        Gson gson = new Gson();
        FullServiceDefinition fullServiceDefinition = gson.fromJson(fileContent, FullServiceDefinition.class);
        Assertions.assertEquals(fullServiceDefinition.getParameters().get("paramTest"), "zkTest");
    }


    @Test
    public void testConsumer() throws ClassNotFoundException, InterruptedException {
        String interfaceName = "org.apache.dubbo.metadata.store.zookeeper.ZookeeperMetadataReport4TstService";
        String version = "1.0.0.zk.md";
        String group = null;
        String application = "vic.zk.md";
        MetadataIdentifier consumerMetadataIdentifier = storeConsumer(zookeeperMetadataReport, interfaceName, version, group, application);

        String fileContent = zookeeperMetadataReport.zkClient.getContent(zookeeperMetadataReport.getNodePath(consumerMetadataIdentifier));
        fileContent = waitSeconds(fileContent, 3500, zookeeperMetadataReport.getNodePath(consumerMetadataIdentifier));
        Assertions.assertNotNull(fileContent);

        deletePath(consumerMetadataIdentifier, zookeeperMetadataReport);
        fileContent = zookeeperMetadataReport.zkClient.getContent(zookeeperMetadataReport.getNodePath(consumerMetadataIdentifier));
        fileContent = waitSeconds(fileContent, 1000, zookeeperMetadataReport.getNodePath(consumerMetadataIdentifier));
        Assertions.assertNull(fileContent);

        consumerMetadataIdentifier = storeConsumer(zookeeperMetadataReport, interfaceName, version, group, application);
        fileContent = zookeeperMetadataReport.zkClient.getContent(zookeeperMetadataReport.getNodePath(consumerMetadataIdentifier));
        fileContent = waitSeconds(fileContent, 3000, zookeeperMetadataReport.getNodePath(consumerMetadataIdentifier));
        Assertions.assertNotNull(fileContent);
        Assertions.assertEquals(fileContent, "{\"paramConsumerTest\":\"zkCm\"}");
    }

    @Test
    public void testDoSaveMetadata() throws ExecutionException, InterruptedException {
        String interfaceName = "org.apache.dubbo.metadata.store.zookeeper.ZookeeperMetadataReport4TstService";
        String version = "1.0.0";
        String group = null;
        String application = "etc-metadata-report-consumer-test";
        String revision = "90980";
        String protocol = "xxx";
        URL url = generateURL(interfaceName, version, group, application);
        ServiceMetadataIdentifier serviceMetadataIdentifier = new ServiceMetadataIdentifier(interfaceName, version,
            group, "provider", revision, protocol);
        zookeeperMetadataReport.doSaveMetadata(serviceMetadataIdentifier, url);

        String fileContent = zookeeperMetadataReport.zkClient.getContent(zookeeperMetadataReport.getNodePath(serviceMetadataIdentifier));
        Assertions.assertNotNull(fileContent);

        Assertions.assertEquals(fileContent, URL.encode(url.toFullString()));
    }

    @Test
    public void testDoRemoveMetadata() throws ExecutionException, InterruptedException {
        String interfaceName = "org.apache.dubbo.metadata.store.zookeeper.ZookeeperMetadataReport4TstService";
        String version = "1.0.0";
        String group = null;
        String application = "etc-metadata-report-consumer-test";
        String revision = "90980";
        String protocol = "xxx";
        URL url = generateURL(interfaceName, version, group, application);
        ServiceMetadataIdentifier serviceMetadataIdentifier = new ServiceMetadataIdentifier(interfaceName, version,
            group, "provider", revision, protocol);
        zookeeperMetadataReport.doSaveMetadata(serviceMetadataIdentifier, url);
        String fileContent = zookeeperMetadataReport.zkClient.getContent(zookeeperMetadataReport.getNodePath(serviceMetadataIdentifier));

        Assertions.assertNotNull(fileContent);


        zookeeperMetadataReport.doRemoveMetadata(serviceMetadataIdentifier);

        fileContent = zookeeperMetadataReport.zkClient.getContent(zookeeperMetadataReport.getNodePath(serviceMetadataIdentifier));
        Assertions.assertNull(fileContent);
    }

    @Test
    public void testDoGetExportedURLs() throws ExecutionException, InterruptedException {
        String interfaceName = "org.apache.dubbo.metadata.store.zookeeper.ZookeeperMetadataReport4TstService";
        String version = "1.0.0";
        String group = null;
        String application = "etc-metadata-report-consumer-test";
        String revision = "90980";
        String protocol = "xxx";
        URL url = generateURL(interfaceName, version, group, application);
        ServiceMetadataIdentifier serviceMetadataIdentifier = new ServiceMetadataIdentifier(interfaceName, version,
            group, "provider", revision, protocol);
        zookeeperMetadataReport.doSaveMetadata(serviceMetadataIdentifier, url);

        List<String> r = zookeeperMetadataReport.doGetExportedURLs(serviceMetadataIdentifier);
        Assertions.assertTrue(r.size() == 1);

        String fileContent = r.get(0);
        Assertions.assertNotNull(fileContent);

        Assertions.assertEquals(fileContent, url.toFullString());
    }

    @Test
    public void testDoSaveSubscriberData() throws ExecutionException, InterruptedException {
        String interfaceName = "org.apache.dubbo.metadata.store.zookeeper.ZookeeperMetadataReport4TstService";
        String version = "1.0.0";
        String group = null;
        String application = "etc-metadata-report-consumer-test";
        String revision = "90980";
        String protocol = "xxx";
        URL url = generateURL(interfaceName, version, group, application);
        SubscriberMetadataIdentifier subscriberMetadataIdentifier = new SubscriberMetadataIdentifier(application, revision);
        Gson gson = new Gson();
        String r = gson.toJson(Arrays.asList(url));
        zookeeperMetadataReport.doSaveSubscriberData(subscriberMetadataIdentifier, r);

        String fileContent = zookeeperMetadataReport.zkClient.getContent(zookeeperMetadataReport.getNodePath(subscriberMetadataIdentifier));

        Assertions.assertNotNull(fileContent);

        Assertions.assertEquals(fileContent, r);
    }

    @Test
    public void testDoGetSubscribedURLs() throws ExecutionException, InterruptedException {
        String interfaceName = "org.apache.dubbo.metadata.store.zookeeper.ZookeeperMetadataReport4TstService";
        String version = "1.0.0";
        String group = null;
        String application = "etc-metadata-report-consumer-test";
        String revision = "90980";
        String protocol = "xxx";
        URL url = generateURL(interfaceName, version, group, application);
        SubscriberMetadataIdentifier subscriberMetadataIdentifier = new SubscriberMetadataIdentifier(application, revision);
        Gson gson = new Gson();
        String r = gson.toJson(Arrays.asList(url));
        zookeeperMetadataReport.doSaveSubscriberData(subscriberMetadataIdentifier, r);

        String fileContent = zookeeperMetadataReport.zkClient.getContent(zookeeperMetadataReport.getNodePath(subscriberMetadataIdentifier));

        Assertions.assertNotNull(fileContent);

        Assertions.assertEquals(fileContent, r);
    }


    private MetadataIdentifier storePrivider(MetadataReport zookeeperMetadataReport, String interfaceName, String version, String group, String application) throws ClassNotFoundException, InterruptedException {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":4444/" + interfaceName + "?paramTest=zkTest&version=" + version + "&application="
            + application + (group == null ? "" : "&group=" + group));

        MetadataIdentifier providerMetadataIdentifier = new MetadataIdentifier(interfaceName, version, group, PROVIDER_SIDE, application);
        Class interfaceClass = Class.forName(interfaceName);
        FullServiceDefinition fullServiceDefinition = ServiceDefinitionBuilder.buildFullDefinition(interfaceClass, url.getParameters());

        zookeeperMetadataReport.storeProviderMetadata(providerMetadataIdentifier, fullServiceDefinition);
        Thread.sleep(2000);
        return providerMetadataIdentifier;
    }

    private MetadataIdentifier storeConsumer(MetadataReport zookeeperMetadataReport, String interfaceName, String version, String group, String application) throws ClassNotFoundException, InterruptedException {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":4444/" + interfaceName + "?version=" + version + "&application="
            + application + (group == null ? "" : "&group=" + group));

        MetadataIdentifier consumerMetadataIdentifier = new MetadataIdentifier(interfaceName, version, group, CONSUMER_SIDE, application);
        Class interfaceClass = Class.forName(interfaceName);

        Map<String, String> tmp = new HashMap<>();
        tmp.put("paramConsumerTest", "zkCm");
        zookeeperMetadataReport.storeConsumerMetadata(consumerMetadataIdentifier, tmp);
        Thread.sleep(2000);

        return consumerMetadataIdentifier;
    }

    private String waitSeconds(String value, long moreTime, String path) throws InterruptedException {
        if (value == null) {
            Thread.sleep(moreTime);
            return zookeeperMetadataReport.zkClient.getContent(path);
        }
        return value;
    }

    private URL generateURL(String interfaceName, String version, String group, String application) {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":8989/" + interfaceName +
            "?paramTest=etcdTest&version=" + version + "&application="
            + application + (group == null ? "" : "&group=" + group));
        return url;
    }


    @Test
    public void testMapping() throws InterruptedException {
        String serviceKey = ZookeeperMetadataReportTest.class.getName();
        URL url = URL.valueOf("test://127.0.0.1:8888/" + serviceKey);
        String appNames = "demo1,demo2";

        CountDownLatch latch = new CountDownLatch(1);
        Set<String> serviceAppMapping = zookeeperMetadataReport.getServiceAppMapping(serviceKey, new MappingListener() {
            @Override
            public void onEvent(MappingChangedEvent event) {
                Set<String> apps = event.getApps();
                Assertions.assertEquals(apps.size(), 2);
                Assertions.assertTrue(apps.contains("demo1"));
                Assertions.assertTrue(apps.contains("demo2"));
                latch.countDown();
            }

            @Override
            public void stop() {

            }
        }, url);
        Assertions.assertTrue(serviceAppMapping.isEmpty());

        ConfigItem configItem = zookeeperMetadataReport.getConfigItem(serviceKey, DEFAULT_MAPPING_GROUP);
        zookeeperMetadataReport.registerServiceAppMapping(serviceKey, DEFAULT_MAPPING_GROUP, appNames, configItem.getTicket());
        latch.await();
    }

    @Test
    public void testAppMetadata() {
        String serviceKey = ZookeeperMetadataReportTest.class.getName();
        String appName = "demo";
        URL url = URL.valueOf("test://127.0.0.1:8888/" + serviceKey);
        MetadataInfo metadataInfo = new MetadataInfo(appName);
        metadataInfo.addService(url);

        SubscriberMetadataIdentifier identifier = new SubscriberMetadataIdentifier(appName, metadataInfo.calAndGetRevision());
        MetadataInfo appMetadata = zookeeperMetadataReport.getAppMetadata(identifier, Collections.emptyMap());
        Assertions.assertNull(appMetadata);

        zookeeperMetadataReport.publishAppMetadata(identifier, metadataInfo);
        appMetadata = zookeeperMetadataReport.getAppMetadata(identifier, Collections.emptyMap());
        Assertions.assertNotNull(appMetadata);
        Assertions.assertEquals(appMetadata.calAndGetRevision(), metadataInfo.calAndGetRevision());

    }
}
