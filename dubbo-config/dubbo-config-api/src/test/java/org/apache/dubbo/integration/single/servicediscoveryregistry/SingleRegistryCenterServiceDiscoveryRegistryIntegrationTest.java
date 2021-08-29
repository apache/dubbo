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
package org.apache.dubbo.integration.single.serviceDiscoveryRegistry;

import com.alibaba.nacos.common.utils.MapUtil;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.*;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.integration.IntegrationTest;
import org.apache.dubbo.integration.single.injvm.SingleRegistryCenterInjvmService;
import org.apache.dubbo.integration.single.injvm.SingleRegistryCenterInjvmServiceImpl;
import org.apache.dubbo.integration.ServiceDiscoveryRegistryListener;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.RegistryServiceListener;
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
public class SingleRegistryCenterServiceDiscoveryRegistryIntegrationTest implements IntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(SingleRegistryCenterServiceDiscoveryRegistryIntegrationTest.class);

    /**
     * Define the provider application name.
     */
    private static String PROVIDER_APPLICATION_NAME = "single-registry-center-provider-for-injvm-protocol";

    /**
     * The name for getting the specified instance, which is loaded using SPI.
     */
    private static String SPI_NAME = "singleConfigCenterServiceDiscoveryRegistry";
    /**
     * Define the {@link ServiceConfig} instance.
     */
    private ServiceConfig<SingleRegistryCenterInjvmService> serviceConfig;

    /**
     * The listener to mark is Called
     */
    private ServiceDiscoveryRegistryListener registryServiceListener;

    /**
     * the service to write meta info locally
     */
    private WritableMetadataService writableMetadataService;

    @BeforeEach
    public void setUp() throws Exception {
        logger.info(getClass().getSimpleName() + " testcase is beginning...");
        DubboBootstrap.reset();

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
            .protocol(new ProtocolConfig("tri"))
            .service(serviceConfig);

        writableMetadataService = WritableMetadataService.getDefaultExtension();
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
    private void beforeServiceDiscoveryRegistry() {
        // ---------------initialize--------------- //
        registryServiceListener = (ServiceDiscoveryRegistryListener) ExtensionLoader.getExtensionLoader(RegistryServiceListener.class).getExtension(SPI_NAME);

        // ---------------checkpoints--------------- //
        // registryServiceListener onRegister is not called
        Assertions.assertFalse(registryServiceListener.isRegisterHasCalled());
        // registryServiceListener onSubscribe is not called
        Assertions.assertFalse(registryServiceListener.isSubscribeHasCalled());
        // exportedServiceURLs of InMemoryWritableMetadataService is empty
        Assertions.assertTrue(CollectionUtils.isEmpty(writableMetadataService.getExportedURLs()));
        // metadataInfos of InMemoryWritableMetadataService is empty
        Assertions.assertTrue(MapUtil.isEmpty(writableMetadataService.getMetadataInfos()));
        // serviceToAppsMapping of InMemoryWritableMetadataService is empty
        Assertions.assertTrue(MapUtil.isEmpty(writableMetadataService.getCachedMapping()));
    }

    /**
     * {@inheritDoc}
     */
    @Test
    @Override
    public void integrate() {
        beforeServiceDiscoveryRegistry();
        DubboBootstrap.getInstance().start();
        afterServiceDiscoveryRegistry();
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
    private void afterServiceDiscoveryRegistry() {
        // ---------------initialize--------------- //
        registryServiceListener = (ServiceDiscoveryRegistryListener) ExtensionLoader.getExtensionLoader(RegistryServiceListener.class).getExtension(SPI_NAME);

        // ---------------checkpoints--------------- //
        // registryServiceListener onRegister is called
        Assertions.assertTrue(registryServiceListener.isRegisterHasCalled());
        // registryServiceListener onSubscribe is called
        Assertions.assertTrue(registryServiceListener.isSubscribeHasCalled());
        // exportedServiceURLs of InMemoryWritableMetadataService is empty
        Assertions.assertTrue(CollectionUtils.isEmpty(writableMetadataService.getExportedURLs()));
        // metadataInfos of InMemoryWritableMetadataService is empty
        Assertions.assertTrue(MapUtil.isEmpty(writableMetadataService.getMetadataInfos()));
        // serviceToAppsMapping of InMemoryWritableMetadataService is empty
        Assertions.assertTrue(MapUtil.isEmpty(writableMetadataService.getCachedMapping()));
    }

    @AfterEach
    public void tearDown() throws IOException {
        DubboBootstrap.reset();
        PROVIDER_APPLICATION_NAME = null;
        serviceConfig = null;
        Assertions.assertTrue(registryServiceListener.isUnRegisterHasCalled());
        Assertions.assertTrue(registryServiceListener.isUnSubscribeHasCalled());
        logger.info(getClass().getSimpleName() + " testcase is ending...");
    }
}
