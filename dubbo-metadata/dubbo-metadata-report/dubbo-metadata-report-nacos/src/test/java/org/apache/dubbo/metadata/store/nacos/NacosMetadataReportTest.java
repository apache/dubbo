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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;

import com.alibaba.nacos.api.config.ConfigService;
import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
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

    @BeforeEach
    public void setUp() {
        // timeout in 15 seconds.
        URL url = URL.valueOf("nacos://127.0.0.1:8848")
                .addParameter(SESSION_TIMEOUT_KEY, 15000);
        nacosMetadataReportFactory = new NacosMetadataReportFactory();
        this.nacosMetadataReport = (NacosMetadataReport) nacosMetadataReportFactory.createMetadataReport(url);
        this.configService = nacosMetadataReport.buildConfigService(url);
    }

    @AfterEach
    public void tearDown() throws Exception {
    }

    @Test
    public void testStoreProvider() throws Exception {
        String version = "1.0.0";
        String group = null;
        String application = "nacos-metdata-report-test";
        MetadataIdentifier providerIdentifier =
                storeProvider(nacosMetadataReport, TEST_SERVICE, version, group, application);
        String serverContent = configService.getConfig(providerIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY), group, 5000L);
        Assertions.assertNotNull(serverContent);

        Gson gson = new Gson();
        FullServiceDefinition fullServiceDefinition = gson.fromJson(serverContent, FullServiceDefinition.class);
        Assertions.assertEquals(fullServiceDefinition.getParameters().get("paramTest"), "nacosTest");
    }

    @Test
    public void testStoreConsumer() throws Exception {
        String version = "1.0.0";
        String group = null;
        String application = "nacos-metadata-report-consumer-test";
        MetadataIdentifier consumerIdentifier = storeConsumer(nacosMetadataReport, TEST_SERVICE, version, group, application);

        String serverContent = configService.getConfig(consumerIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY), group, 5000L);
        Assertions.assertNotNull(serverContent);
        Assertions.assertEquals(serverContent, "{\"paramConsumerTest\":\"nacosConsumer\"}");
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
        Thread.sleep(1000);
        return providerMetadataIdentifier;
    }

    private MetadataIdentifier storeConsumer(NacosMetadataReport nacosMetadataReport, String interfaceName,
                                             String version, String group, String application) throws InterruptedException {
        MetadataIdentifier consumerIdentifier = new MetadataIdentifier(interfaceName, version, group, CONSUMER_SIDE, application);
        Map<String, String> tmp = new HashMap<>();
        tmp.put("paramConsumerTest", "nacosConsumer");
        nacosMetadataReport.storeConsumerMetadata(consumerIdentifier, tmp);
        Thread.sleep(1000);
        return consumerIdentifier;
    }
}
