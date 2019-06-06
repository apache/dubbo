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

package org.apache.dubbo.metadata.store.etcd;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;

import com.google.gson.Gson;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.launcher.EtcdClusterFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;

/**
 * Unit test for etcd metadata report
 */
public class EtcdMetadataReportTest {

    private static final String TEST_SERVICE = "org.apache.dubbo.metadata.store.etcd.EtcdMetadata4TstService";

    private EtcdCluster etcdCluster = EtcdClusterFactory.buildCluster(getClass().getSimpleName(), 1, false, false);
    private Client etcdClientForTest;
    private EtcdMetadataReport etcdMetadataReport;
    private URL registryUrl;
    private EtcdMetadataReportFactory etcdMetadataReportFactory;

    @BeforeEach
    public void setUp() {
        etcdCluster.start();
        etcdClientForTest = Client.builder().endpoints(etcdCluster.getClientEndpoints()).build();
        List<URI> clientEndPoints = etcdCluster.getClientEndpoints();
        this.registryUrl = URL.valueOf("etcd://" + clientEndPoints.get(0).getHost() + ":" + clientEndPoints.get(0).getPort());
        etcdMetadataReportFactory = new EtcdMetadataReportFactory();
        this.etcdMetadataReport = (EtcdMetadataReport) etcdMetadataReportFactory.createMetadataReport(registryUrl);
    }

    @AfterEach
    public void tearDown() throws Exception {
        etcdCluster.close();
    }

    @Test
    @Disabled("Disabled because https://github.com/apache/dubbo/issues/4185")
    public void testStoreProvider() throws Exception {
        String version = "1.0.0";
        String group = null;
        String application = "etcd-metdata-report-test";
        MetadataIdentifier providerIdentifier =
                storeProvider(etcdMetadataReport, TEST_SERVICE, version, group, application);

        CompletableFuture<GetResponse> response = etcdClientForTest.getKVClient().get(ByteSequence.from(
                etcdMetadataReport.getNodeKey(providerIdentifier), StandardCharsets.UTF_8));
        String fileContent = response.get().getKvs().get(0).getValue().toString(StandardCharsets.UTF_8);
        Assertions.assertNotNull(fileContent);

        Gson gson = new Gson();
        FullServiceDefinition fullServiceDefinition = gson.fromJson(fileContent, FullServiceDefinition.class);
        Assertions.assertEquals(fullServiceDefinition.getParameters().get("paramTest"), "etcdTest");
    }

    @Test
    public void testStoreConsumer() throws Exception {
        String version = "1.0.0";
        String group = null;
        String application = "etc-metadata-report-consumer-test";
        MetadataIdentifier consumerIdentifier = storeConsumer(etcdMetadataReport, TEST_SERVICE, version, group, application);

        CompletableFuture<GetResponse> response = etcdClientForTest.getKVClient().get(ByteSequence.from(
                etcdMetadataReport.getNodeKey(consumerIdentifier), StandardCharsets.UTF_8));
        String fileContent = response.get().getKvs().get(0).getValue().toString(StandardCharsets.UTF_8);
        Assertions.assertNotNull(fileContent);
        Assertions.assertEquals(fileContent, "{\"paramConsumerTest\":\"etcdConsumer\"}");
    }

    private MetadataIdentifier storeProvider(EtcdMetadataReport etcdMetadataReport, String interfaceName, String version,
                                             String group, String application)
            throws ClassNotFoundException, InterruptedException {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":4444/" + interfaceName +
                "?paramTest=etcdTest&version=" + version + "&application="
                + application + (group == null ? "" : "&group=" + group));

        MetadataIdentifier providerMetadataIdentifier =
                new MetadataIdentifier(interfaceName, version, group, PROVIDER_SIDE, application);
        Class interfaceClass = Class.forName(interfaceName);
        FullServiceDefinition fullServiceDefinition =
                ServiceDefinitionBuilder.buildFullDefinition(interfaceClass, url.getParameters());

        etcdMetadataReport.storeProviderMetadata(providerMetadataIdentifier, fullServiceDefinition);
        Thread.sleep(1000);
        return providerMetadataIdentifier;
    }

    private MetadataIdentifier storeConsumer(EtcdMetadataReport etcdMetadataReport, String interfaceName,
                                             String version, String group, String application) throws InterruptedException {

        MetadataIdentifier consumerIdentifier = new MetadataIdentifier(interfaceName, version, group, CONSUMER_SIDE, application);
        Map<String, String> tmp = new HashMap<>();
        tmp.put("paramConsumerTest", "etcdConsumer");
        etcdMetadataReport.storeConsumerMetadata(consumerIdentifier, tmp);
        Thread.sleep(1000);
        return consumerIdentifier;
    }
}
