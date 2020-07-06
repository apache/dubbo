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
package org.apache.dubbo.registry.client.metadata.proxy;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import static java.lang.String.valueOf;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.COMPOSITE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.metadata.report.support.Constants.SYNC_REPORT_KEY;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.EXPORTED_SERVICES_REVISION_PROPERTY_NAME;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.METADATA_SERVICE_URL_PARAMS_PROPERTY_NAME;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CompositeMetadataServiceProxyFactory} Test-Cases
 *
 * @since 2.7.8
 */
public class CompositeMetadataServiceProxyFactoryTest {

    private static final URL METADATA_REPORT_URL = URL.valueOf("file://")
            .addParameter(APPLICATION_KEY, "test")
            .addParameter(SYNC_REPORT_KEY, "true");

    private static final String APP_NAME = "test-service";

    private MetadataServiceProxyFactory factory;

    private DefaultServiceInstance instance;

    @BeforeEach
    public void init() {
        ApplicationModel.getConfigManager().setApplication(new ApplicationConfig(APP_NAME));
        MetadataReportInstance.init(METADATA_REPORT_URL);
        factory = MetadataServiceProxyFactory.getExtension(COMPOSITE_METADATA_STORAGE_TYPE);
        instance = createServiceInstance();
    }

    @AfterEach
    public void reset() throws Exception {
        ApplicationModel.reset();
        MetadataReportInstance.getMetadataReport().close();
    }

    private DefaultServiceInstance createServiceInstance() {
        DefaultServiceInstance serviceInstance = new DefaultServiceInstance(valueOf(System.nanoTime()), "A", "127.0.0.1", 8080);
        Map<String, String> metadata = new HashMap<>();
        metadata.put(EXPORTED_SERVICES_REVISION_PROPERTY_NAME, "X");
        metadata.put(METADATA_SERVICE_URL_PARAMS_PROPERTY_NAME, "{\"dubbo\":{\"application\":\"dubbo-provider-demo\",\"deprecated\":\"false\",\"group\":\"dubbo-provider-demo\",\"version\":\"1.0.0\",\"timestamp\":\"1564845042651\",\"dubbo\":\"2.0.2\",\"host\":\"192.168.0.102\",\"port\":\"20880\"}}");
        serviceInstance.setMetadata(metadata);
        return serviceInstance;
    }

    @Test
    public void testGetProxy() {
        MetadataService metadataService = factory.getProxy(instance);
        MetadataService metadataService2 = factory.getProxy(instance);
        assertSame(metadataService, metadataService2);
    }

    @Test
    public void testGetExportedURLs() {
        MetadataService metadataService = factory.getProxy(instance);
        SortedSet<String> exportedURLs = metadataService.getExportedURLs();
        assertTrue(exportedURLs.isEmpty());
    }
}
