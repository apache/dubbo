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
package org.apache.dubbo.metadata.report.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.URLRevisionResolver;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.ServiceDefinition;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.ServiceMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.metadata.report.support.file.FileSystemMetadataReportFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.service.EchoService;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.metadata.report.support.Constants.SYNC_REPORT_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link ConfigCenterBasedMetadataReport} Test-Cases
 *
 * @since 2.7.8
 */
public class ConfigCenterBasedMetadataReportTest {

    private static final URL REPORT_SERVER_URL = URL.valueOf("file://")
            .addParameter(APPLICATION_KEY, "test")
            .addParameter(SYNC_REPORT_KEY, "true");

    private static final Class<EchoService> INTERFACE_CLASS = EchoService.class;

    private static final String INTERFACE_NAME = INTERFACE_CLASS.getName();

    private static final String APP_NAME = "test-service";

    private static final URL BASE_URL = URL
            .valueOf("dubbo://127.0.0.1:20880")
            .setPath(INTERFACE_NAME)
            .addParameter(APPLICATION_KEY, APP_NAME)
            .addParameter(SIDE_KEY, "provider");

    private ConfigCenterBasedMetadataReport metadataReport;

    @BeforeEach
    public void init() {
        ApplicationModel.getConfigManager().setApplication(new ApplicationConfig("test-service"));
        this.metadataReport = new FileSystemMetadataReportFactory().getMetadataReport(REPORT_SERVER_URL);
    }

    @AfterEach
    public void reset() throws Exception {
        ApplicationModel.reset();
        this.metadataReport.close();
    }

    /**
     * Test {@link MetadataReport#storeProviderMetadata(MetadataIdentifier, ServiceDefinition)} and
     * {@link MetadataReport#getServiceDefinition(MetadataIdentifier)}
     */
    @Test
    public void testStoreProviderMetadataAndGetServiceDefinition() {
        MetadataIdentifier metadataIdentifier = new MetadataIdentifier(BASE_URL);
        ServiceDefinition serviceDefinition = ServiceDefinitionBuilder.buildFullDefinition(INTERFACE_CLASS, BASE_URL.getParameters());
        metadataReport.storeProviderMetadata(metadataIdentifier, serviceDefinition);
        String serviceDefinitionJSON = metadataReport.getServiceDefinition(metadataIdentifier);
        assertEquals(serviceDefinitionJSON, new Gson().toJson(serviceDefinition));
    }

    /**
     * Test {@link MetadataReport#storeConsumerMetadata(MetadataIdentifier, Map)} and
     * {@link MetadataReport#getServiceDefinition(MetadataIdentifier)}
     */
    @Test
    public void testStoreConsumerMetadata() {
        MetadataIdentifier metadataIdentifier = new MetadataIdentifier(BASE_URL);
        metadataReport.storeConsumerMetadata(metadataIdentifier, BASE_URL.getParameters());
        String parametersJSON = metadataReport.getServiceDefinition(metadataIdentifier);
        assertEquals(parametersJSON, new Gson().toJson(BASE_URL.getParameters()));
    }

    /**
     * Test {@link MetadataReport#saveServiceMetadata(ServiceMetadataIdentifier, URL)} and
     * {@link MetadataReport#removeServiceMetadata(ServiceMetadataIdentifier)}
     */
    @Test
    public void testSaveServiceMetadataAndRemoveServiceMetadata() {
        ServiceMetadataIdentifier metadataIdentifier = new ServiceMetadataIdentifier(BASE_URL);
        metadataReport.saveServiceMetadata(metadataIdentifier, BASE_URL);
        String metadata = metadataReport.getMetadata(metadataIdentifier);
        assertEquals(URL.encode(BASE_URL.toFullString()), metadata);
        metadataReport.removeServiceMetadata(metadataIdentifier);
        assertNull(metadataReport.getMetadata(metadataIdentifier));
    }

    /**
     * Test {@link MetadataReport#saveSubscribedData(SubscriberMetadataIdentifier, Collection)} and
     * {@link MetadataReport#getSubscribedURLs(SubscriberMetadataIdentifier)}
     */
    @Test
    public void testSaveSubscribedDataAndGetSubscribedURLs() {
        SubscriberMetadataIdentifier metadataIdentifier = new SubscriberMetadataIdentifier(BASE_URL);
        Set<String> urls = singleton(BASE_URL).stream().map(URL::toIdentityString).collect(toSet());
        metadataReport.saveSubscribedData(metadataIdentifier, urls);
        Collection<String> subscribedURLs = metadataReport.getSubscribedURLs(metadataIdentifier);
        assertEquals(1, subscribedURLs.size());
        assertEquals(urls, subscribedURLs);
    }

    /**
     * Test {@link MetadataReport#saveExportedURLs(SortedSet)},
     * {@link MetadataReport#getExportedURLsContent(String, String)} and
     * {@link MetadataReport#getExportedURLs(String, String)}
     */
    @Test
    public void testSaveExportedURLsAndGetExportedURLs() {
        SortedSet<String> urls = singleton(BASE_URL).stream().map(URL::toIdentityString).collect(TreeSet::new, Set::add, Set::addAll);
        metadataReport.saveExportedURLs(urls);

        URLRevisionResolver urlRevisionResolver = URLRevisionResolver.INSTANCE;
        String revision = urlRevisionResolver.resolve(urls);
        assertEquals(urls, metadataReport.getExportedURLs(APP_NAME, revision));
    }
}
