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
package org.apache.dubbo.integration.single.exportprovider;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.ServiceListener;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.integration.IntegrationTest;
import org.apache.dubbo.registry.integration.RegistryProtocolListener;
import org.apache.dubbo.registrycenter.DefaultSingleRegistryCenter;
import org.apache.dubbo.registrycenter.SingleRegistryCenter;
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
 * The testcases are only for checking the core process of exporting provider.
 */
public class SingleRegistryCenterExportProviderIntegrationTest implements IntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(SingleRegistryCenterExportProviderIntegrationTest.class);

    /**
     * Define the provider application name.
     */
    private static String PROVIDER_APPLICATION_NAME = "single-registry-center-for-export-provider";

    /**
     * The name for getting the specified instance, which is loaded using SPI.
     */
    private static String SPI_NAME = "singleConfigCenterExportProvider";

    /**
     * Define the protocol's name.
     */
    private static String PROTOCOL_NAME = CommonConstants.DUBBO;
    /**
     * Define the protocol's port.
     */
    private static int PROTOCOL_PORT = 20800;

    /**
     * Define the {@link ServiceConfig} instance.
     */
    private ServiceConfig<SingleRegistryCenterExportProviderService> serviceConfig;

    /**
     * Define a registry center.
     */
    private SingleRegistryCenter registryCenter;

    /**
     * Define a {@link RegistryProtocolListener} instance.
     */
    private SingleRegistryCenterExportProviderRegistryProtocolListener registryProtocolListener;

    /**
     * Define a {@link ExporterListener} instance.
     */
    private SingleRegistryCenterExportProviderExporterListener exporterListener;

    /**
     * Define a {@link Filter} instance.
     */
    private SingleRegistryCenterExportProviderFilter filter;

    /**
     * Define a {@link ServiceListener} instance.
     */
    private SingleRegistryCenterExportProviderServiceListener serviceListener;

    @BeforeEach
    public void setUp() throws Exception {
        logger.info(getClass().getSimpleName() + " testcase is beginning...");
        DubboBootstrap.reset();
        registryCenter = new DefaultSingleRegistryCenter();
        registryCenter.startup();
        // initialize service config
        serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(SingleRegistryCenterExportProviderService.class);
        serviceConfig.setRef(new SingleRegistryCenterExportProviderServiceImpl());
        serviceConfig.setAsync(false);

        // initailize bootstrap
        DubboBootstrap.getInstance()
            .application(new ApplicationConfig(PROVIDER_APPLICATION_NAME))
            .registry(registryCenter.getRegistryConfig())
            .protocol(new ProtocolConfig(PROTOCOL_NAME, PROTOCOL_PORT))
            .service(serviceConfig);
    }

    /**
     * There are some checkpoints need to verify as follow:
     * <ul>
     *     <li>ServiceConfig is exported or not</li>
     *     <li>SingleRegistryCenterExportProviderRegistryProtocolListener is null or not</li>
     *     <li>There is nothing in ServiceListener or not</li>
     *     <li>There is nothing in ExporterListener or not</li>
     * </ul>
     */
    private void beforeExport() {
        registryProtocolListener = (SingleRegistryCenterExportProviderRegistryProtocolListener) ExtensionLoader
            .getExtensionLoader(RegistryProtocolListener.class)
            .getExtension(SPI_NAME);
        exporterListener = (SingleRegistryCenterExportProviderExporterListener) ExtensionLoader
            .getExtensionLoader(ExporterListener.class)
            .getExtension(SPI_NAME);
        filter = (SingleRegistryCenterExportProviderFilter) ExtensionLoader
            .getExtensionLoader(Filter.class)
            .getExtension(SPI_NAME);
        serviceListener = (SingleRegistryCenterExportProviderServiceListener) ExtensionLoader
            .getExtensionLoader(ServiceListener.class)
            .getExtension(SPI_NAME);
        // ---------------checkpoints--------------- //
        // ServiceConfig isn't exported
        Assertions.assertFalse(serviceConfig.isExported());
        // registryProtocolListener is just initialized by SPI
        // so, all of fields are the default value.
        Assertions.assertNotNull(registryProtocolListener);
        Assertions.assertFalse(registryProtocolListener.isExported());
        // There is nothing in ServiceListener
        Assertions.assertTrue(serviceListener.getExportedServices().isEmpty());
        // There is nothing in ExporterListener
        Assertions.assertTrue(exporterListener.getExportedExporters().isEmpty());
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
        ReferenceConfig<SingleRegistryCenterExportProviderService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(SingleRegistryCenterExportProviderService.class);
        referenceConfig.setBootstrap(DubboBootstrap.getInstance());
        referenceConfig.setScope(SCOPE_LOCAL);
        referenceConfig.get().hello(PROVIDER_APPLICATION_NAME);
        afterInvoke();
    }

    /**
     * There are some checkpoints need to check after exported as follow:
     * <ul>
     *     <li>the exporter is exported or not</li>
     *     <li>The exported exporter are three</li>
     *     <li>The exported service is SingleRegistryCenterExportProviderService or not</li>
     *     <li>The SingleRegistryCenterExportProviderService is exported or not</li>
     *     <li>The exported exporter contains SingleRegistryCenterExportProviderFilter or not</li>
     * </ul>
     */
    private void afterExport() {
        // The exporter is exported
        Assertions.assertTrue(registryProtocolListener.isExported());
        // The exported service is only one
        Assertions.assertEquals(serviceListener.getExportedServices().size(), 1);
        // The exported service is SingleRegistryCenterExportProviderService
        Assertions.assertEquals(serviceListener.getExportedServices().get(0).getInterfaceClass(),
            SingleRegistryCenterExportProviderService.class);
        // The SingleRegistryCenterExportProviderService is exported
        Assertions.assertTrue(serviceListener.getExportedServices().get(0).isExported());
        // The exported exporter are three
        // 1. InjvmExporter
        // 2. DubboExporter with service-discovery-registry protocol
        // 3. DubboExporter with registry protocol
        Assertions.assertEquals(exporterListener.getExportedExporters().size(), 3);
        // The exported exporter contains SingleRegistryCenterExportProviderFilter
        Assertions.assertTrue(exporterListener.getFilters().contains(filter));
    }

    /**
     * There are some checkpoints need to check after invoked as follow:
     * <ul>
     *     <li>The SingleRegistryCenterExportProviderFilter has called or not</li>
     *     <li>The SingleRegistryCenterExportProviderFilter exists error after invoked</li>
     *     <li>The SingleRegistryCenterExportProviderFilter's response is right or not</li>
     * </ul>
     */
    private void afterInvoke() {
        // The SingleRegistryCenterInjvmFilter has called
        Assertions.assertTrue(filter.hasCalled());
        // The SingleRegistryCenterInjvmFilter doesn't exist error
        Assertions.assertFalse(filter.hasError());
        // Check the SingleRegistryCenterInjvmFilter's response
        Assertions.assertEquals("Hello " + PROVIDER_APPLICATION_NAME, filter.getResponse());
    }

    @AfterEach
    public void tearDown() throws IOException {
        DubboBootstrap.reset();
        PROVIDER_APPLICATION_NAME = null;
        serviceConfig = null;
        // The exported service has been unexported
        Assertions.assertTrue(serviceListener.getExportedServices().isEmpty());
        logger.info(getClass().getSimpleName() + " testcase is ending...");
        registryCenter.shutdown();
        registryProtocolListener = null;
        registryCenter = null;
    }
}
