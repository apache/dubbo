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
package org.apache.dubbo.integration.single.exportmetadata;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.ServiceListener;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.integration.IntegrationTest;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registrycenter.DefaultSingleRegistryCenter;
import org.apache.dubbo.registrycenter.SingleRegistryCenter;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.ExporterListener;
import org.apache.dubbo.rpc.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.apache.dubbo.rpc.Constants.SCOPE_LOCAL;

/**
 * The testcases are only for checking the process of exporting metadata service.
 */
public class SingleRegistryCenterExportMetadataIntegrationTest implements IntegrationTest {

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

    /**
     * Define a registry center.
     */
    private SingleRegistryCenter registryCenter;

    @BeforeEach
    public void setUp() throws Exception {
        logger.info(getClass().getSimpleName() + " testcase is beginning...");
        DubboBootstrap.reset();
        registryCenter = new DefaultSingleRegistryCenter(NetUtils.getAvailablePort());
        registryCenter.startup();
        // initialize service config
        serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(SingleRegistryCenterExportMetadataService.class);
        serviceConfig.setRef(new SingleRegistryCenterExportMetadataServiceImpl());
        serviceConfig.setAsync(false);
        serviceConfig.setScope(SCOPE_LOCAL);

        // initailize bootstrap
        DubboBootstrap.getInstance()
            .application(new ApplicationConfig(PROVIDER_APPLICATION_NAME))
            .registry(registryCenter.getRegistryConfig())
            .protocol(new ProtocolConfig(PROTOCOL_NAME))
            .service(serviceConfig);
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
        // 1. Metadata Service exporter with dubbo protocol
        // 2. SingleRegistryCenterExportMetadataService exporter with Injvm protocol
        Assertions.assertEquals(exporterListener.getExportedExporters().size(), 2);
        // Obtain SingleRegistryCenterExportMetadataService exporter with Injvm protocol
        Exporter<?> injvmExporter = (Exporter<?>) exporterListener.getExportedExporters()
            .stream()
            .filter(
                exporter -> PROTOCOL_NAME.equalsIgnoreCase(exporter.getInvoker().getUrl().getProtocol())
            )
            .findFirst()
            .get();
        // Obtain Metadata Service exporter with dubbo protocol
        Exporter<?> metadataExporter = (Exporter<?>) exporterListener.getExportedExporters()
            .stream()
            .filter(
                exporter -> !PROTOCOL_NAME.equalsIgnoreCase(exporter.getInvoker().getUrl().getProtocol())
            )
            .filter(
                exporter -> exporter.getInvoker().getInterface().equals(MetadataService.class)
            )
            .findFirst()
            .get();
        // Make sure injvmExporter is not null
        Assertions.assertNotNull(injvmExporter);
        // Make sure metadataExporter is not null
        Assertions.assertNotNull(metadataExporter);
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
        registryCenter.shutdown();
        registryCenter = null;
    }
}
