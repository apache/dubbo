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
package org.apache.dubbo.config.integration.multiple.servicediscoveryregistry;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.integration.IntegrationTest;
import org.apache.dubbo.config.metadata.MetadataServiceDelegation;
import org.apache.dubbo.registry.RegistryServiceListener;
import org.apache.dubbo.test.check.registrycenter.config.ZookeeperConfig;
import org.apache.dubbo.test.check.registrycenter.config.ZookeeperRegistryCenterConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.config.integration.Constants.MULTIPLE_CONFIG_CENTER_SERVICE_DISCOVERY_REGISTRY;

/**
 * The testcases are only for checking the process of exporting provider using service-discovery-registry protocol.
 */
public class MultipleRegistryCenterServiceDiscoveryRegistryIntegrationTest implements IntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(MultipleRegistryCenterServiceDiscoveryRegistryIntegrationTest.class);

    /**
     * Define the provider application name.
     */
    public static String PROVIDER_APPLICATION_NAME = "multiple-registry-center-provider-for-service-discovery-registry-protocol";

    /**
     * Define the protocol's name.
     */
    private static String PROTOCOL_NAME = CommonConstants.DUBBO;
    /**
     * Define the protocol's port.
     */
    private static int PROTOCOL_PORT = 20880;

    /**
     * Define the {@link ServiceConfig} instance.
     */
    private ServiceConfig<MultipleRegistryCenterServiceDiscoveryRegistryService> serviceConfig;

    /**
     * Define a {@link RegistryServiceListener} instance.
     */
    private MultipleRegistryCenterServiceDiscoveryRegistryRegistryServiceListener registryServiceListener;

    /**
     * The localhost.
     */
    private static String HOST = "127.0.0.1";

    /**
     * The port of register center.
     */
    private Set<Integer> ports = new HashSet<>(2);

    @BeforeEach
    public void setUp() throws Exception {
        logger.info(getClass().getSimpleName() + " testcase is beginning...");
        DubboBootstrap.reset();
        // initialize service config
        serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(MultipleRegistryCenterServiceDiscoveryRegistryService.class);
        serviceConfig.setRef(new MultipleRegistryCenterServiceDiscoveryRegistryServiceImpl());
        serviceConfig.setAsync(false);

        RegistryConfig registryConfig1 = new RegistryConfig(ZookeeperRegistryCenterConfig.getConnectionAddress1());
        Map<String, String> parameters1 = new HashMap<>();
        parameters1.put("registry.listeners", MULTIPLE_CONFIG_CENTER_SERVICE_DISCOVERY_REGISTRY);
        registryConfig1.updateParameters(parameters1);
        DubboBootstrap.getInstance().registry(registryConfig1);
        ports.add(ZookeeperConfig.DEFAULT_CLIENT_PORT_1);

        RegistryConfig registryConfig2 = new RegistryConfig(ZookeeperRegistryCenterConfig.getConnectionAddress2());
        Map<String, String> parameters2 = new HashMap<>();
        parameters2.put("registry.listeners", MULTIPLE_CONFIG_CENTER_SERVICE_DISCOVERY_REGISTRY);
        registryConfig2.updateParameters(parameters2);
        DubboBootstrap.getInstance().registry(registryConfig2);
        ports.add(ZookeeperConfig.DEFAULT_CLIENT_PORT_2);

        DubboBootstrap.getInstance()
            .application(new ApplicationConfig(PROVIDER_APPLICATION_NAME))
            .protocol(new ProtocolConfig(PROTOCOL_NAME, PROTOCOL_PORT))
            .service(serviceConfig);
        // ---------------initialize--------------- //
        registryServiceListener = (MultipleRegistryCenterServiceDiscoveryRegistryRegistryServiceListener) ExtensionLoader
            .getExtensionLoader(RegistryServiceListener.class).getExtension(MULTIPLE_CONFIG_CENTER_SERVICE_DISCOVERY_REGISTRY);
        // RegistryServiceListener is not null
        Assertions.assertNotNull(registryServiceListener);
        registryServiceListener.getStorage().clear();

    }

    /**
     * Define a {@link RegistryServiceListener} for helping check.<p>
     * There are some checkpoints need to verify as follow:
     * <ul>
     *     <li>ServiceConfig is exported or not</li>
     *     <li>ServiceDiscoveryRegistryStorage is empty or not</li>
     * </ul>
     */
    private void beforeExport() {
        // ---------------checkpoints--------------- //
        // ServiceConfig isn't exported
        Assertions.assertFalse(serviceConfig.isExported());

        // ServiceDiscoveryRegistryStorage is empty
        Assertions.assertEquals(registryServiceListener.getStorage().size(), 0);
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
        ReferenceConfig<MultipleRegistryCenterServiceDiscoveryRegistryService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(MultipleRegistryCenterServiceDiscoveryRegistryService.class);
        referenceConfig.get().hello("Dubbo in multiple registry center");
        afterInvoke();
    }

    /**
     * There are some checkpoints need to check after exported as follow:
     * <ul>
     *     <li>ServiceDiscoveryRegistry is right or not</li>
     *     <li>All register center has been registered and subscribed</li>
     * </ul>
     */
    private void afterExport() {
        // ServiceDiscoveryRegistry is not null
        Assertions.assertEquals(registryServiceListener.getStorage().size(), 2);
        // All register center has been registered and subscribed
        for (int port : ports) {
            Assertions.assertTrue(registryServiceListener.getStorage().contains(HOST, port));
            ServiceDiscoveryRegistryInfoWrapper serviceDiscoveryRegistryInfoWrapper = registryServiceListener.getStorage().get(HOST, port);
            // check if it's registered
            Assertions.assertTrue(serviceDiscoveryRegistryInfoWrapper.isRegistered());
            // check if it's subscribed
            Assertions.assertFalse(serviceDiscoveryRegistryInfoWrapper.isSubscribed());
            MetadataServiceDelegation metadataService = DubboBootstrap.getInstance().getApplicationModel().getBeanFactory().getBean(MetadataServiceDelegation.class);
            // check if the count of exported urls is right or not
            Assertions.assertEquals(metadataService.getExportedURLs().size(), 1);
            // check the exported url is right or not.
            Assertions.assertTrue(metadataService.getExportedURLs()
                .first()
                .contains(MultipleRegistryCenterServiceDiscoveryRegistryService.class.getName()));
            // check the count of metadatainfo is right or not.
            Assertions.assertEquals(2, metadataService.getMetadataInfos().size());
        }
    }

    /**
     * There are some checkpoints need to check after invoked as follow:
     */
    private void afterInvoke() {

    }

    @AfterEach
    public void tearDown() throws IOException {
        DubboBootstrap.reset();
        PROVIDER_APPLICATION_NAME = null;
        serviceConfig = null;
        // TODO: we need to check whether this scenario is normal
        // TODO: the Exporter and ServiceDiscoveryRegistry are same in multiple registry center
        /*
        for (int port: ports) {
            Assertions.assertTrue(registryServiceListener.getStorage().contains(HOST, port));
            ServiceDiscoveryRegistryInfoWrapper serviceDiscoveryRegistryInfoWrapper = registryServiceListener.getStorage().get(HOST, port);
            // check if it's registered
            Assertions.assertFalse(serviceDiscoveryRegistryInfoWrapper.isRegistered());
            // check if it's subscribed
            Assertions.assertFalse(serviceDiscoveryRegistryInfoWrapper.isSubscribed());
        }
        */
        registryServiceListener.getStorage().clear();
        registryServiceListener = null;
        logger.info(getClass().getSimpleName() + " testcase is ending...");
    }
}
