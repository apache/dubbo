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
package org.apache.dubbo.config.integration.single.exportmetadata;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.ServiceListener;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.integration.IntegrationTest;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.ExporterListener;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.test.check.registrycenter.config.ZookeeperRegistryCenterConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.dubbo.rpc.Constants.SCOPE_LOCAL;

/**
 * The testcases are only for checking the process of exporting metadata service.
 */
class SingleRegistryCenterExportMetadataIntegrationTest implements IntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(SingleRegistryCenterExportMetadataIntegrationTest.class);

    /**
     * Define the provider application name.
     */
    private static String PROVIDER_APPLICATION_NAME = "single-registry-center-export-metadata";

    /**
     * The name for getting the specified instance, which is loaded using SPI.
     */
    private static String SPI_NAME = "singleConfigCenterExportMetadata";

    /**
     * Define the protocol's name.
     */
    private static String PROTOCOL_NAME = "injvm";
    /**
     * Define the {@link ServiceConfig} instance.
     */
    private ServiceConfig<SingleRegistryCenterExportMetadataService> serviceConfig;

    /**
     * The listener to record exported services
     */
    private SingleRegistryCenterExportMetadataServiceListener serviceListener;

    /**
     * The listener to record exported exporters.
     */
    private SingleRegistryCenterExportMetadataExporterListener exporterListener;

    @BeforeEach
    public void setUp() throws Exception {
        logger.info(getClass().getSimpleName() + " testcase is beginning...");
        DubboBootstrap.reset();
        // initialize service config
        serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(SingleRegistryCenterExportMetadataService.class);
        serviceConfig.setRef(new SingleRegistryCenterExportMetadataServiceImpl());
        serviceConfig.setAsync(false);
        serviceConfig.setScope(SCOPE_LOCAL);

        // initailize bootstrap
        DubboBootstrap.getInstance()
            .application(new ApplicationConfig(PROVIDER_APPLICATION_NAME))
            .protocol(new ProtocolConfig(PROTOCOL_NAME))
            .service(serviceConfig);
        RegistryConfig registryConfig = new RegistryConfig(ZookeeperRegistryCenterConfig.getConnectionAddress());
        DubboBootstrap.getInstance().registry(registryConfig);
    }

    /**
     * Define {@link ServiceListener}, {@link ExporterListener} and {@link Filter} for helping check.
     * <p>Use SPI to load them before exporting.
     * <p>After that, there are some checkpoints need to verify as follow:
     * <ul>
     *     <li>There is nothing in ServiceListener or not</li>
     *     <li>There is nothing in ExporterListener or not</li>
     *     <li>ServiceConfig is exported or not</li>
     * </ul>
     */
    private void beforeExport() {
        // ---------------initialize--------------- //
        serviceListener = (SingleRegistryCenterExportMetadataServiceListener) ExtensionLoader.getExtensionLoader(ServiceListener.class).getExtension(SPI_NAME);
        exporterListener = (SingleRegistryCenterExportMetadataExporterListener) ExtensionLoader.getExtensionLoader(ExporterListener.class).getExtension(SPI_NAME);

        // ---------------checkpoints--------------- //
        // There is nothing in ServiceListener
        Assertions.assertTrue(serviceListener.getExportedServices().isEmpty());
        // There is nothing in ExporterListener
        Assertions.assertTrue(exporterListener.getExportedExporters().isEmpty());
        // ServiceConfig isn't exported
        Assertions.assertFalse(serviceConfig.isExported());
    }

    /**
     * {@inheritDoc}
     */
    @Test
    @Override
    public void integrate() {
        beforeExport();
        DubboBootstrap.getInstance().start();
        afterExport();
    }

    /**
     * There are some checkpoints need to check after exported as follow:
     * <ul>
     *     <li>The metadata service is only one or not</li>
     *     <li>The exported service is MetadataService or not</li>
     *     <li>The MetadataService is exported or not</li>
     *     <li>The exported exporters are right or not</li>
     * </ul>
     */
    private void afterExport() {
        // The metadata service is only one
        Assertions.assertEquals(serviceListener.getExportedServices().size(), 1);
        // The exported service is MetadataService
        Assertions.assertEquals(serviceListener.getExportedServices().get(0).getInterfaceClass(),
            MetadataService.class);
        // The MetadataService is exported
        Assertions.assertTrue(serviceListener.getExportedServices().get(0).isExported());
        // There are two exported exporters
        // 1. Metadata Service exporter with Injvm protocol
        // 2. SingleRegistryCenterExportMetadataService exporter with Injvm protocol
        Assertions.assertEquals(exporterListener.getExportedExporters().size(), 2);
        List<Exporter<?>> injvmExporters = exporterListener.getExportedExporters()
            .stream()
            .filter(
                exporter -> PROTOCOL_NAME.equalsIgnoreCase(exporter.getInvoker().getUrl().getProtocol())
            ).collect(Collectors.toList());
        // Make sure there are 2 injvmExporters
        Assertions.assertEquals(injvmExporters.size(), 2);
    }

    @AfterEach
    public void tearDown() throws IOException {
        DubboBootstrap.reset();
        PROVIDER_APPLICATION_NAME = null;
        serviceConfig = null;
        // The exported service has been unexported
        Assertions.assertTrue(serviceListener.getExportedServices().isEmpty());
        serviceListener = null;
        logger.info(getClass().getSimpleName() + " testcase is ending...");
    }
}