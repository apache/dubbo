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
package org.apache.dubbo.integration.single.servicediscoveryregistry;

import com.alibaba.nacos.common.utils.MapUtil;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.integration.IntegrationTest;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.RegistryServiceListener;
import org.apache.dubbo.registrycenter.RegistryCenter;
import org.apache.dubbo.registrycenter.ZookeeperSingleRegistryCenter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.SortedSet;

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
    private static String SPI_NAME = "singleRegistryCenterServiceDiscoveryRegistry";
    /**
     * Define the {@link ServiceConfig} instance.
     */
    private ServiceConfig<SingleRegistryCenterServiceDiscoveryRegistryService> serviceConfig;

    /**
     * The listener to mark is Called
     */
    private SingleRegistryCenterServiceDiscoveryRegistryListener registryServiceListener;

    /**
     * the service to write meta info locally
     */
    private WritableMetadataService writableMetadataService;

    /**
     * Default a registry center.
     */
    private RegistryCenter registryCenter;

    @BeforeEach
    public void setUp() throws Exception {
        logger.info(getClass().getSimpleName() + " testcase is beginning...");
        DubboBootstrap.reset();

        //start all zookeeper services only once
        registryCenter = new ZookeeperSingleRegistryCenter();
        registryCenter.startup();

        // initialize service config
        serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(SingleRegistryCenterServiceDiscoveryRegistryService.class);
        serviceConfig.setRef(new SingleRegistryCenterServiceDiscoveryRegistryServiceImpl());
        serviceConfig.setAsync(false);

        // initailize bootstrap
        for (RegistryCenter.Instance instance : registryCenter.getRegistryCenterInstance()) {
            DubboBootstrap.getInstance().registry(new RegistryConfig(String.format("%s://%s:%s",
                instance.getType(),
                instance.getHostname(),
                instance.getPort())));
        }

        // initailize bootstrap
        DubboBootstrap.getInstance()
            .application(new ApplicationConfig(PROVIDER_APPLICATION_NAME))
            .protocol(new ProtocolConfig(PROTOCOL_NAME, PROTOCOL_PORT))
            .service(serviceConfig);

        writableMetadataService = WritableMetadataService.getDefaultExtension();

        registryServiceListener = (SingleRegistryCenterServiceDiscoveryRegistryListener) ExtensionLoader.getExtensionLoader(RegistryServiceListener.class).getExtension(SPI_NAME);
        // RegistryServiceListener is not null
        Assertions.assertNotNull(registryServiceListener);
    }

    private void beforeServiceDiscoveryRegistry() {
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
     * There are some checkpoints need to check after start as follow:
     * <ul>
     *     <li>ServiceDiscoveryRegistry is called through checking the listener is called</li>
     *     <li>the metadata info is correct through checking the store of writableMetadataService</li>
     * </ul>
     */
    private void afterServiceDiscoveryRegistry() {
        // ---------------checkpoints--------------- //
        // registryServiceListener onRegister is called
        Assertions.assertTrue(registryServiceListener.isRegisterHasCalled());
        // registryServiceListener onSubscribe is called
        Assertions.assertTrue(registryServiceListener.isSubscribeHasCalled());
        // exportedServiceURLs of InMemoryWritableMetadataService has stored correct info
        Assertions.assertTrue(isCorrectForExportedURLs(writableMetadataService.getExportedURLs()));
        // metadataInfos of InMemoryWritableMetadataService has stored correct info
        Assertions.assertTrue(isCorrectForMetadataInfos(writableMetadataService.getMetadataInfos()));
        // serviceToAppsMapping of InMemoryWritableMetadataService is empty
        Assertions.assertTrue(MapUtil.isEmpty(writableMetadataService.getCachedMapping()));
    }

    /**
     * check the important info such as application name, exported service is correct for exportedURLs
     * @param exportedURLs
     * @return
     */
    private Boolean isCorrectForExportedURLs(SortedSet<String> exportedURLs) {
        if(CollectionUtils.isEmpty(exportedURLs)) {
            return false;
        }
        if(!exportedURLs.toString().contains(PROVIDER_APPLICATION_NAME) || !exportedURLs.toString().contains("SingleRegistryCenterServiceDiscoveryRegistryService")) {
            return false;
        }
        return true;
    }

    /**
     * check the important info such as application name, exported service is correct for metadataInfos
     * @param metadataInfos
     * @return
     */
    private Boolean isCorrectForMetadataInfos(Map<String, MetadataInfo> metadataInfos) {
        if(MapUtil.isEmpty(metadataInfos)) {
            return false;
        }
        if(!metadataInfos.toString().contains(PROVIDER_APPLICATION_NAME) || !metadataInfos.toString().contains("SingleRegistryCenterServiceDiscoveryRegistryService")) {
            return false;
        }
        return true;
    }

    @AfterEach
    public void tearDown() throws IOException {
        DubboBootstrap.reset();
        PROVIDER_APPLICATION_NAME = null;
        serviceConfig = null;
//        Assertions.assertTrue(registryServiceListener.isUnRegisterHasCalled());
//        Assertions.assertTrue(registryServiceListener.isUnSubscribeHasCalled());
        registryServiceListener = null;
        logger.info(getClass().getSimpleName() + " testcase is ending...");
        // destroy registry center
        registryCenter.shutdown();
        registryCenter = null;
        writableMetadataService = null;
    }
}
