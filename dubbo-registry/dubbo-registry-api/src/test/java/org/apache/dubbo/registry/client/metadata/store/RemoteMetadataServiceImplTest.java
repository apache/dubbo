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
import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.EXPORTED_SERVICES_REVISION_PROPERTY_NAME;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RemoteMetadataServiceImplTest {

    private static final String REGISTRY_CLUSTER = "registry9103";
    private static final String SERVICE_NAME = "A";
    private RemoteMetadataServiceImpl remoteMetadataService;
    private MetadataReport metadataReport;
    private MetadataInfo metadataInfo;
    private String reversion;

    @BeforeEach
    public void setUp() {
        ApplicationModel applicationModel = spy(ApplicationModel.defaultModel());
        ScopeBeanFactory beanFactory = mock(ScopeBeanFactory.class);
        MetadataReportInstance metadataReportInstance = mock(MetadataReportInstance.class);
        metadataReport = mock(MetadataReport.class);

        Map<String, MetadataReport> clusterToMetadataReport = new HashMap<>();
        clusterToMetadataReport.put(REGISTRY_CLUSTER, metadataReport);
        when(metadataReportInstance.getMetadataReports(anyBoolean())).thenReturn(clusterToMetadataReport);
        when(applicationModel.getBeanFactory()).thenReturn(beanFactory);
        when(beanFactory.getBean(MetadataReportInstance.class)).thenReturn(metadataReportInstance);

        metadataInfo = new MetadataInfo();
        URL url = URL.valueOf("dubbo://30.225.21.30:20880/org.apache.dubbo.registry.service.DemoService");
        metadataInfo.addService(new MetadataInfo.ServiceInfo(url));
        reversion = metadataInfo.calAndGetRevision();
        Map<String, MetadataInfo> clusterToMetadataInfo = new HashMap<>();
        clusterToMetadataInfo.put(REGISTRY_CLUSTER, metadataInfo);

        WritableMetadataService writableMetadataService = mock(WritableMetadataService.class);
        when(applicationModel.getDefaultExtension(WritableMetadataService.class)).thenReturn(writableMetadataService);
        when(writableMetadataService.getMetadataInfos()).thenReturn(clusterToMetadataInfo);
        when(metadataReport.getAppMetadata(any(),any())).thenAnswer((Answer<MetadataInfo>) invocationOnMock -> {
            SubscriberMetadataIdentifier identifier = invocationOnMock.getArgument(0, SubscriberMetadataIdentifier.class);
            if (SERVICE_NAME.equals(identifier.getApplication()) && reversion.equals(identifier.getRevision())) {
                return metadataInfo;
            }
            return null;
        });


        remoteMetadataService = new RemoteMetadataServiceImpl();
        remoteMetadataService.setScopeModel(applicationModel);
    }

    @Test
    public void testPublishAndGetMetadata() {

        // test getMetadataReports
        Map<String, MetadataReport> metadataReports = remoteMetadataService.getMetadataReports();
        Assertions.assertTrue(metadataReports.containsKey(REGISTRY_CLUSTER));

        // test publishMetadata
        remoteMetadataService.publishMetadata(SERVICE_NAME);

        ArgumentCaptor<SubscriberMetadataIdentifier> identifierArgumentCaptor = ArgumentCaptor.forClass(SubscriberMetadataIdentifier.class);
        ArgumentCaptor<MetadataInfo> metadataInfoArgumentCaptor = ArgumentCaptor.forClass(MetadataInfo.class);
        verify(metadataReport, times(1)).publishAppMetadata(identifierArgumentCaptor.capture(), metadataInfoArgumentCaptor.capture());
        SubscriberMetadataIdentifier identifier = identifierArgumentCaptor.getValue();
        Assertions.assertEquals(identifier.getRevision(), reversion);
        Assertions.assertEquals(identifier.getApplication(), SERVICE_NAME);

        // test getMetadata
        DefaultServiceInstance serviceInstance = new DefaultServiceInstance(SERVICE_NAME, "127.0.0.1", 20880, ApplicationModel.defaultModel());
        serviceInstance.setRegistryCluster(REGISTRY_CLUSTER);
        serviceInstance.getMetadata().put(EXPORTED_SERVICES_REVISION_PROPERTY_NAME, reversion);

        MetadataInfo remoteMetadataInfo = remoteMetadataService.getMetadata(serviceInstance);
        Assertions.assertEquals(remoteMetadataInfo, metadataInfo);

        serviceInstance.setServiceName("FAIL_SERVICE_NAME");
        remoteMetadataInfo = remoteMetadataService.getMetadata(serviceInstance);
        Assertions.assertNull(remoteMetadataInfo);
    }

    @Test
    public void testPublishServiceDefinition() {
        // test provider publishServiceDefinition
        URL providerURL = URL.valueOf("dubbo://127.0.0.1:8888/org.apache.dubbo.registry.service.DemoService?side=provider");
        remoteMetadataService.publishServiceDefinition(providerURL);
        verify(metadataReport, times(1)).storeProviderMetadata(any(),any());

        // test consumer publishServiceDefinition
        URL consumerURL = URL.valueOf("dubbo://127.0.0.1:8888/org.apache.dubbo.registry.service.DemoService?side=consumer");
        remoteMetadataService.publishServiceDefinition(consumerURL);
        verify(metadataReport, times(1)).storeConsumerMetadata(any(),any());

    }
}
