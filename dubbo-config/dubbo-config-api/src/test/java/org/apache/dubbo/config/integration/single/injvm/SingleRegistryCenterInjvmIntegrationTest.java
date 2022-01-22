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
package org.apache.dubbo.config.integration.single.injvm;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.ServiceListener;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.integration.IntegrationTest;
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

import static org.apache.dubbo.rpc.Constants.SCOPE_LOCAL;

/**
 * The testcases are only for checking the process of exporting provider using injvm protocol.
 */
public class SingleRegistryCenterInjvmIntegrationTest implements IntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(SingleRegistryCenterInjvmIntegrationTest.class);

    /**
     * Define the provider application name.
     */
    private static String PROVIDER_APPLICATION_NAME = "single-registry-center-provider-for-injvm-protocol";

    /**
     * The name for getting the specified instance, which is loaded using SPI.
     */
    private static String SPI_NAME = "singleConfigCenterInjvm";
    /**
     * Define the {@link ServiceConfig} instance.
     */
    private ServiceConfig<SingleRegistryCenterInjvmService> serviceConfig;

    /**
     * The listener to record exported services
     */
    private SingleRegistryCenterInjvmServiceListener serviceListener;

    /**
     * The listener to record exported exporters.
     */
    private SingleRegistryCenterInjvmExporterListener exporterListener;

    /**
     * The filter for checking filter chain.
     */
    private SingleRegistryCenterInjvmFilter filter;

    @BeforeEach
    public void setUp() throws Exception {
        logger.info(getClass().getSimpleName() + " testcase is beginning...");
        DubboBootstrap.reset();
        // initialize service config
        serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(SingleRegistryCenterInjvmService.class);
        serviceConfig.setRef(new SingleRegistryCenterInjvmServiceImpl());
        serviceConfig.setAsync(false);
        serviceConfig.setScope(SCOPE_LOCAL);

        // initailize bootstrap
        DubboBootstrap.getInstance()
            .application(new ApplicationConfig(PROVIDER_APPLICATION_NAME))
            .protocol(new ProtocolConfig("injvm"))
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
        serviceListener = (SingleRegistryCenterInjvmServiceListener) ExtensionLoader.getExtensionLoader(ServiceListener.class).getExtension(SPI_NAME);
        exporterListener = (SingleRegistryCenterInjvmExporterListener) ExtensionLoader.getExtensionLoader(ExporterListener.class).getExtension(SPI_NAME);
        filter = (SingleRegistryCenterInjvmFilter) ExtensionLoader.getExtensionLoader(Filter.class).getExtension(SPI_NAME);

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
        ReferenceConfig<SingleRegistryCenterInjvmService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(SingleRegistryCenterInjvmService.class);
        referenceConfig.setScope(SCOPE_LOCAL);
        referenceConfig.get().hello("Dubbo");
        afterInvoke();
    }

    /**
     * There are some checkpoints need to check after exported as follow:
     * <ul>
     *     <li>The exported service is only one or not</li>
     *     <li>The exported service is SingleRegistryCenterInjvmService or not</li>
     *     <li>The SingleRegistryCenterInjvmService is exported or not</li>
     *     <li>The exported exporter is only one or not</li>
     *     <li>The exported exporter contains SingleRegistryCenterInjvmFilter or not</li>
     * </ul>
     */
    private void afterExport() {
        // The exported service is only one
        Assertions.assertEquals(serviceListener.getExportedServices().size(), 1);
        // The exported service is SingleRegistryCenterInjvmService
        Assertions.assertEquals(serviceListener.getExportedServices().get(0).getInterfaceClass(),
            SingleRegistryCenterInjvmService.class);
        // The SingleRegistryCenterInjvmService is exported
        Assertions.assertTrue(serviceListener.getExportedServices().get(0).isExported());
        // The exported exporter is only one
        Assertions.assertEquals(exporterListener.getExportedExporters().size(), 1);
        // The exported exporter contains SingleRegistryCenterInjvmFilter
        Assertions.assertTrue(exporterListener.getFilters().contains(filter));
    }

    /**
     * There are some checkpoints need to check after invoked as follow:
     * <ul>
     *     <li>The SingleRegistryCenterInjvmFilter has called or not</li>
     *     <li>The SingleRegistryCenterInjvmFilter exists error after invoked</li>
     *     <li>The SingleRegistryCenterInjvmFilter's response is right or not</li>
     * </ul>
     */
    private void afterInvoke() {
        // The SingleRegistryCenterInjvmFilter has called
        Assertions.assertTrue(filter.hasCalled());
        // The SingleRegistryCenterInjvmFilter doesn't exist error
        Assertions.assertFalse(filter.hasError());
        // Check the SingleRegistryCenterInjvmFilter's response
        Assertions.assertEquals("Hello Dubbo", filter.getResponse());
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
