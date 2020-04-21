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
package org.apache.dubbo.metadata.store.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.report.identifier.KeyTypeEnum;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;

import com.alibaba.nacos.api.config.ConfigService;
import com.google.gson.Gson;
import org.apache.dubbo.metadata.report.identifier.ServiceMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;

//FIXME: waiting for embedded Nacos suport, then we can open the switch.
@Disabled("https://github.com/alibaba/nacos/issues/1188")
public class NacosMetadataReportTest {

    private static final String SESSION_TIMEOUT_KEY = "session";

    private static final String TEST_SERVICE = "org.apache.dubbo.metadata.store.nacos.NacosMetadata4TstService";

    private NacosMetadataReport nacosMetadataReport;

    private NacosMetadataReportFactory nacosMetadataReportFactory;

    private ConfigService configService;

    private static final String NACOS_GROUP = "metadata_test";

    /**
     * timeout(ms) for nacos session
     */
    private static final int SESSION_TIMEOUT = 15 * 1000;

    /**
     * timeout(ms) for query operation on nacos
     */
    private static final int NACOS_READ_TIMEOUT = 5 * 1000;

    /**
     * interval(ms) to make nacos cache refresh
     */
    private static final int INTERVAL_TO_MAKE_NACOS_REFRESH = 1000;

    /**
     * version for test
     */
    private static final String VERSION = "1.0.0";

    /**
     * group for test
     */
    private static final String METADATA_GROUP = null;

    /**
     * application name for test
     */
    private static final String APPLICATION_NAME = "nacos-metdata-report-test";

    /**
     * revision for test
     */
    private static final String REVISION = "90980";

    /**
     * protocol for test
     */
    private static final String PROTOCOL = "xxx";

    @BeforeEach
    public void setUp() {
        URL url = URL.valueOf("nacos://127.0.0.1:8848?group=" + NACOS_GROUP)
                .addParameter(SESSION_TIMEOUT_KEY, SESSION_TIMEOUT);
        nacosMetadataReportFactory = new NacosMetadataReportFactory();
        this.nacosMetadataReport = (NacosMetadataReport) nacosMetadataReportFactory.createMetadataReport(url);
        this.configService = nacosMetadataReport.buildConfigService(url);
    }

    @AfterEach
    public void tearDown() throws Exception {
    }


    @Test
    public void testStoreProvider() throws Exception {
        MetadataIdentifier providerIdentifier =
                storeProvider(nacosMetadataReport, TEST_SERVICE, VERSION, METADATA_GROUP, APPLICATION_NAME);
        String serverContent = configService.getConfig(providerIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), NACOS_GROUP, NACOS_READ_TIMEOUT);
        Assertions.assertNotNull(serverContent);

        Gson gson = new Gson();
        FullServiceDefinition fullServiceDefinition = gson.fromJson(serverContent, FullServiceDefinition.class);
        Assertions.assertEquals(fullServiceDefinition.getParameters().get("paramTest"), "nacosTest");

        //Clear test data
        configService.removeConfig(providerIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), NACOS_GROUP);
    }

    @Test
    public void testStoreConsumer() throws Exception {
        MetadataIdentifier consumerIdentifier = storeConsumer(nacosMetadataReport, TEST_SERVICE, VERSION, METADATA_GROUP, APPLICATION_NAME);

        String serverContent = configService.getConfig(consumerIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), NACOS_GROUP, NACOS_READ_TIMEOUT);
        Assertions.assertNotNull(serverContent);
        Assertions.assertEquals(serverContent, "{\"paramConsumerTest\":\"nacosConsumer\"}");

        //clear test data
        configService.removeConfig(consumerIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), NACOS_GROUP);
    }

    @Test
    public void testDoSaveServiceMetadata() throws Exception {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":4444/" + TEST_SERVICE +
                "?paramTest=nacosTest&version=" + VERSION + "&application="
                + APPLICATION_NAME + (METADATA_GROUP == null ? "" : "&group=" + METADATA_GROUP));
        ServiceMetadataIdentifier serviceMetadataIdentifier = new ServiceMetadataIdentifier(TEST_SERVICE, VERSION,
                METADATA_GROUP, "provider", REVISION, PROTOCOL);
        nacosMetadataReport.doSaveMetadata(serviceMetadataIdentifier, url);
        Thread.sleep(INTERVAL_TO_MAKE_NACOS_REFRESH);
        String serviceMetaData = configService.getConfig(serviceMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), NACOS_GROUP, NACOS_READ_TIMEOUT);
        Assertions.assertNotNull(serviceMetaData);
        Assertions.assertEquals(serviceMetaData, URL.encode(url.toFullString()));

        //clear test data
        configService.removeConfig(serviceMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), NACOS_GROUP);
    }

    @Test
    public void testDoRemoveServiceMetadata() throws Exception {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":4444/" + TEST_SERVICE +
                "?paramTest=nacosTest&version=" + VERSION + "&application="
                + APPLICATION_NAME + (METADATA_GROUP == null ? "" : "&group=" + METADATA_GROUP));
        ServiceMetadataIdentifier serviceMetadataIdentifier = new ServiceMetadataIdentifier(TEST_SERVICE, VERSION,
                METADATA_GROUP, "provider", REVISION, PROTOCOL);
        nacosMetadataReport.doSaveMetadata(serviceMetadataIdentifier, url);
        Thread.sleep(INTERVAL_TO_MAKE_NACOS_REFRESH);
        String serviceMetaData = configService.getConfig(serviceMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), NACOS_GROUP, NACOS_READ_TIMEOUT);
        Assertions.assertNotNull(serviceMetaData);

        nacosMetadataReport.doRemoveMetadata(serviceMetadataIdentifier);
        Thread.sleep(INTERVAL_TO_MAKE_NACOS_REFRESH);
        serviceMetaData = configService.getConfig(serviceMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), NACOS_GROUP, NACOS_READ_TIMEOUT);
        Assertions.assertNull(serviceMetaData);
    }

    @Test
    public void testDoGetExportedURLs() throws InterruptedException, NacosException {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":4444/" + TEST_SERVICE +
                "?paramTest=nacosTest&version=" + VERSION + "&application="
                + APPLICATION_NAME + (METADATA_GROUP == null ? "" : "&group=" + METADATA_GROUP));
        ServiceMetadataIdentifier serviceMetadataIdentifier = new ServiceMetadataIdentifier(TEST_SERVICE, VERSION,
                METADATA_GROUP, "provider", REVISION, PROTOCOL);

        nacosMetadataReport.doSaveMetadata(serviceMetadataIdentifier, url);
        Thread.sleep(INTERVAL_TO_MAKE_NACOS_REFRESH);

        List<String> exportedURLs = nacosMetadataReport.doGetExportedURLs(serviceMetadataIdentifier);
        Assertions.assertTrue(exportedURLs.size() == 1);

        String exportedUrl = exportedURLs.get(0);
        Assertions.assertNotNull(exportedUrl);
        Assertions.assertEquals(exportedUrl, url.toFullString());

        //clear test data
        configService.removeConfig(serviceMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), NACOS_GROUP);
    }

    @Test
    public void testDoSaveSubscriberData() throws InterruptedException, NacosException {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":4444/" + TEST_SERVICE +
                "?paramTest=nacosTest&version=" + VERSION + "&application="
                + APPLICATION_NAME + (METADATA_GROUP == null ? "" : "&group=" + METADATA_GROUP));
        SubscriberMetadataIdentifier subscriberMetadataIdentifier = new SubscriberMetadataIdentifier(APPLICATION_NAME, REVISION);
        Gson gson = new Gson();
        String urlListJsonString = gson.toJson(Arrays.asList(url));
        nacosMetadataReport.doSaveSubscriberData(subscriberMetadataIdentifier, urlListJsonString);
        Thread.sleep(INTERVAL_TO_MAKE_NACOS_REFRESH);

        String subscriberMetadata = configService.getConfig(subscriberMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), NACOS_GROUP, NACOS_READ_TIMEOUT);
        Assertions.assertNotNull(subscriberMetadata);
        Assertions.assertEquals(subscriberMetadata, urlListJsonString);

        //clear test data
        configService.removeConfig(subscriberMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), NACOS_GROUP);

    }

    private MetadataIdentifier storeProvider(NacosMetadataReport nacosMetadataReport, String interfaceName, String version,
                                             String group, String application)
            throws ClassNotFoundException, InterruptedException {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":4444/" + interfaceName +
                "?paramTest=nacosTest&version=" + version + "&application="
                + application + (group == null ? "" : "&group=" + group));

        MetadataIdentifier providerMetadataIdentifier =
                new MetadataIdentifier(interfaceName, version, group, PROVIDER_SIDE, application);
        Class interfaceClass = Class.forName(interfaceName);
        FullServiceDefinition fullServiceDefinition =
                ServiceDefinitionBuilder.buildFullDefinition(interfaceClass, url.getParameters());

        nacosMetadataReport.storeProviderMetadata(providerMetadataIdentifier, fullServiceDefinition);
        Thread.sleep(INTERVAL_TO_MAKE_NACOS_REFRESH);
        return providerMetadataIdentifier;
    }

    private MetadataIdentifier storeConsumer(NacosMetadataReport nacosMetadataReport, String interfaceName,
                                             String version, String group, String application) throws InterruptedException {
        MetadataIdentifier consumerIdentifier = new MetadataIdentifier(interfaceName, version, group, CONSUMER_SIDE, application);
        Map<String, String> tmp = new HashMap<>();
        tmp.put("paramConsumerTest", "nacosConsumer");
        nacosMetadataReport.storeConsumerMetadata(consumerIdentifier, tmp);
        Thread.sleep(INTERVAL_TO_MAKE_NACOS_REFRESH);
        return consumerIdentifier;
    }

}
