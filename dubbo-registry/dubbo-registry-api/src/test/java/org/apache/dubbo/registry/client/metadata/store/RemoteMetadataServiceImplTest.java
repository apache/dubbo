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
package org.apache.dubbo.registry.client.metadata.store;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.EXPORTED_SERVICES_REVISION_PROPERTY_NAME;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RemoteMetadataServiceImplTest {
    private static RemoteMetadataServiceImpl remoteMetadataService;
    private static ApplicationModel scopeModel;
    private static final String REGISTRY_CLUSTER = "registry9103";

    @BeforeAll
    public static void setUp() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("A");
        scopeModel = ApplicationModel.defaultModel();
        scopeModel.getApplicationConfigManager().setApplication(applicationConfig);

        URL url = URL.valueOf("metadata://127.0.0.1:20880/TestService?version=1.0.0&metadata=mock&sync.report=true");
        MetadataReportConfig metadataReportConfig = mock(MetadataReportConfig.class);
        when(metadataReportConfig.getApplicationModel()).thenReturn(scopeModel);
        when(metadataReportConfig.toUrl()).thenReturn(url);
        when(metadataReportConfig.getScopeModel()).thenReturn(scopeModel);
        when(metadataReportConfig.getRegistry()).thenReturn(REGISTRY_CLUSTER);
        MetadataReportInstance metadataReportInstance = scopeModel.getBeanFactory().getBean(MetadataReportInstance.class);
        metadataReportInstance.init(metadataReportConfig);

        remoteMetadataService = new RemoteMetadataServiceImpl();
        remoteMetadataService.setScopeModel(scopeModel);
    }

    @AfterAll
    public static void clearUp() {
        ApplicationModel.reset();
    }

    @Test
    public void testPublishAndGetMetadata() {

        // test getMetadataReports
        Map<String, MetadataReport> metadataReports = remoteMetadataService.getMetadataReports();
        Assertions.assertTrue(metadataReports.containsKey(REGISTRY_CLUSTER));
        Assertions.assertTrue(metadataReports.get(REGISTRY_CLUSTER) instanceof MockMetadataReport);

        // test publishMetadata
        WritableMetadataService writableMetadataService = scopeModel.getDefaultExtension(WritableMetadataService.class);
        URL url = URL.valueOf("dubbo://30.225.21.30:20880/org.apache.dubbo.registry.service.DemoService?REGISTRY_CLUSTER=" + REGISTRY_CLUSTER);
        writableMetadataService.exportURL(url);
        remoteMetadataService.publishMetadata("A");

        // test getMetadata
        ServiceInstance serviceInstance = new DefaultServiceInstance();
        serviceInstance.setRegistryCluster(REGISTRY_CLUSTER);
        Map<String, MetadataInfo> metadataInfos = writableMetadataService.getMetadataInfos();
        MetadataInfo localMetadataInfo = metadataInfos.get(REGISTRY_CLUSTER);
        String revision = localMetadataInfo.calAndGetRevision();
        serviceInstance.getMetadata().put(EXPORTED_SERVICES_REVISION_PROPERTY_NAME, revision);

        MetadataInfo remoteMetadataInfo = remoteMetadataService.getMetadata(serviceInstance);
        Assertions.assertEquals(localMetadataInfo, remoteMetadataInfo);
    }

    @Test
    public void testPublishServiceDefinition() {
        URL providerURL = URL.valueOf("dubbo://127.0.0.1:8888/org.apache.dubbo.registry.service.DemoService?side=provider");
        remoteMetadataService.publishServiceDefinition(providerURL);

        URL consumerURL = URL.valueOf("dubbo://127.0.0.1:8888/org.apache.dubbo.registry.service.DemoService?side=consumer");
        remoteMetadataService.publishServiceDefinition(consumerURL);

        Map<String, MetadataReport> metadataReports = remoteMetadataService.getMetadataReports();
        MockMetadataReport metadataReport = (MockMetadataReport) metadataReports.get(REGISTRY_CLUSTER);
        Assertions.assertEquals(metadataReport.store.size(),2);

    }
}
