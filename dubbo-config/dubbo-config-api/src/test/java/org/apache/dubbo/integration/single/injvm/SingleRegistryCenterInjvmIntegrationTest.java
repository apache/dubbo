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
package org.apache.dubbo.integration.single.injvm;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.ServiceListener;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.integration.IntegrationTest;
import org.apache.dubbo.integration.single.SingleZooKeeperServer;
import org.apache.dubbo.integration.single.listener.InjvmExporterListener;
import org.apache.dubbo.integration.single.listener.InjvmServiceListener;
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
    private InjvmServiceListener serviceListener;

    /**
     * The listener to record exported exporters.
     */
    private InjvmExporterListener exporterListener;

    /**
     * The filter for checking filter chain.
     */
    private InjvmFilter filter;

    @BeforeEach
    public void setUp() throws Exception {
        logger.info(getClass().getSimpleName() + " testcase is beginning...");
        DubboBootstrap.reset();
        //start zookeeper only once
        logger.info(SingleZooKeeperServer.getZookeeperServerName() + " is beginning to start...");
        SingleZooKeeperServer.start();
        logger.info(SingleZooKeeperServer.getZookeeperServerName() + " has started.");

        // initialize service config
        serviceConfig = new ServiceConfig<SingleRegistryCenterInjvmService>();
        serviceConfig.setInterface(SingleRegistryCenterInjvmService.class);
        serviceConfig.setRef(new SingleRegistryCenterInjvmServiceImpl());
        serviceConfig.setAsync(false);
        serviceConfig.setScope(SCOPE_LOCAL);

        // initailize bootstrap
        DubboBootstrap.getInstance()
            .application(new ApplicationConfig(PROVIDER_APPLICATION_NAME))
            .registry(new RegistryConfig("N/A"))
            .protocol(new ProtocolConfig("injvm"))
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
        serviceListener = (InjvmServiceListener) ExtensionLoader.getExtensionLoader(ServiceListener.class).getExtension(SPI_NAME);
        exporterListener = (InjvmExporterListener) ExtensionLoader.getExtensionLoader(ExporterListener.class).getExtension(SPI_NAME);
        filter = (InjvmFilter) ExtensionLoader.getExtensionLoader(Filter.class).getExtension(SPI_NAME);

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
        referenceConfig.setBootstrap(DubboBootstrap.getInstance());
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
     *     <li>The exported exporter contains InjvmFilter or not</li>
     * </ul>
     */
    private void afterExport() {
        // The exported service is only one
        Assertions.assertEquals(serviceListener.getExportedServices().size(),1);
        // The exported service is SingleRegistryCenterInjvmService
        Assertions.assertEquals(serviceListener.getExportedServices().get(0).getInterfaceClass(),
            SingleRegistryCenterInjvmService.class);
        // The SingleRegistryCenterInjvmService is exported
        Assertions.assertTrue(serviceListener.getExportedServices().get(0).isExported());
        // The exported exporter is only one
        Assertions.assertEquals(exporterListener.getExportedExporters().size(),1);
        // The exported exporter contains InjvmFilter
        Assertions.assertTrue(exporterListener.getFilters().contains(filter));
    }

    /**
     * There are some checkpoints need to check after invoked as follow:
     * <ul>
     *     <li>The InjvmFilter has called or not</li>
     *     <li>The InjvmFilter exists error after invoked</li>
     *     <li>The InjvmFilter's response is right or not</li>
     * </ul>
     */
    private void afterInvoke(){
        // The InjvmFilter has called
        Assertions.assertTrue(filter.hasCalled());
        // The InjvmFilter doesn't exist error
        Assertions.assertFalse(filter.hasError());
        // Check the InjvmFilter's response
        Assertions.assertEquals("Hello Dubbo",filter.getResponse());
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
        // destroy zookeeper only once
        logger.info(SingleZooKeeperServer.getZookeeperServerName() + " is beginning to shutdown...");
        SingleZooKeeperServer.shutdown();
        logger.info(SingleZooKeeperServer.getZookeeperServerName() + " has shutdown.");
    }
}
