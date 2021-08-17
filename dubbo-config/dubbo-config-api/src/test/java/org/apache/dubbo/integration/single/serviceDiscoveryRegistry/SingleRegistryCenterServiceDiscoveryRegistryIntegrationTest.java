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
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.*;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.integration.IntegrationTest;
import org.apache.dubbo.integration.single.injvm.SingleRegistryCenterInjvmService;
import org.apache.dubbo.integration.single.injvm.SingleRegistryCenterInjvmServiceImpl;
import org.apache.dubbo.integration.ServiceDiscoveryRegistryListener;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.RegistryServiceListener;
import org.apache.dubbo.registrycenter.DefaultMultipleRegistryCenter;
import org.apache.dubbo.registrycenter.DefaultSingleRegistryCenter;
import org.apache.dubbo.registrycenter.MultipleRegistryCenter;
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
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import static org.apache.dubbo.rpc.Constants.SCOPE_LOCAL;

/**
 * The testcases are only for checking the process of saving metainfos for service-discovery-registry protocol.
 */
public class SingleRegistryCenterServiceDiscoveryRegistryIntegrationTest implements IntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(SingleRegistryCenterServiceDiscoveryRegistryIntegrationTest.class);

    /**
     * Define the provider application name.
     */
    private static String PROVIDER_APPLICATION_NAME = "single-registry-center-provider-for-service-discovery-registry-protocol";

    /**
     * Define the protocol's name.
     */
    private static String PROTOCOL_NAME = CommonConstants.DUBBO;
    /**
     * Define the protocol's port.
     */
    private static int PROTOCOL_PORT = 20880;

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

    /**
     * Default a registry center.
     */
    private SingleRegistryCenter registryCenter;

    @BeforeEach
    public void setUp() throws Exception {
        logger.info(getClass().getSimpleName() + " testcase is beginning...");
        DubboBootstrap.reset();

        //start all zookeeper services only once
        registryCenter = new DefaultSingleRegistryCenter();
        registryCenter.startup();

        // initialize service config
        serviceConfig = new ServiceConfig<SingleRegistryCenterInjvmService>();
        serviceConfig.setInterface(SingleRegistryCenterInjvmService.class);
        serviceConfig.setRef(new SingleRegistryCenterInjvmServiceImpl());
        serviceConfig.setAsync(false);
//        serviceConfig.setScope(SCOPE_LOCAL);

        // initailize bootstrap
        DubboBootstrap.getInstance().registry(registryCenter.getRegistryConfig());

        // initailize bootstrap
        DubboBootstrap.getInstance()
            .application(new ApplicationConfig(PROVIDER_APPLICATION_NAME))
            .protocol(new ProtocolConfig(PROTOCOL_NAME, PROTOCOL_PORT))
            .service(serviceConfig);

        writableMetadataService = WritableMetadataService.getDefaultExtension();
    }

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

    private void afterServiceDiscoveryRegistry() {
        // ---------------initialize--------------- //
        registryServiceListener = (ServiceDiscoveryRegistryListener) ExtensionLoader.getExtensionLoader(RegistryServiceListener.class).getExtension(SPI_NAME);

        // ---------------checkpoints--------------- //
        // registryServiceListener onRegister is called
        Assertions.assertTrue(registryServiceListener.isRegisterHasCalled());
        // registryServiceListener onSubscribe is called
        Assertions.assertTrue(registryServiceListener.isSubscribeHasCalled());
        // exportedServiceURLs of InMemoryWritableMetadataService is empty
        Assertions.assertTrue(isCorrectForExportedURLs(writableMetadataService.getExportedURLs()));
        // metadataInfos of InMemoryWritableMetadataService is empty
        Assertions.assertTrue(isCorrectForMetadataInfos(writableMetadataService.getMetadataInfos()));
        // serviceToAppsMapping of InMemoryWritableMetadataService is empty
        Assertions.assertTrue(isCorrectForServiceToAppsMapping(writableMetadataService.getCachedMapping()));
    }

    private Boolean isCorrectForExportedURLs(SortedSet<String> exportedURLs) {
        if(CollectionUtils.isEmpty(exportedURLs)) {
            return false;
        }
        if(!exportedURLs.toString().contains(PROVIDER_APPLICATION_NAME) || !exportedURLs.toString().contains("SingleRegistryCenterInjvmService")) {
            return false;
        }
        return true;
    }

    private Boolean isCorrectForMetadataInfos(Map<String, MetadataInfo> metadataInfos) {
        if(MapUtil.isEmpty(metadataInfos)) {
            return false;
        }
        if(!metadataInfos.toString().contains(PROVIDER_APPLICATION_NAME) || !metadataInfos.toString().contains("SingleRegistryCenterInjvmService")) {
            return false;
        }
        return true;
    }

    private Boolean isCorrectForServiceToAppsMapping(Map<String, Set<String>> serviceToAppsMapping) {
        return true;
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
