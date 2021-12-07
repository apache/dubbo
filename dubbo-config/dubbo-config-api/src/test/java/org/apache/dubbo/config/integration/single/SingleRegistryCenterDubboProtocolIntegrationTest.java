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
package org.apache.dubbo.config.integration.single;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.ServiceListener;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.integration.IntegrationTest;
import org.apache.dubbo.config.metadata.MetadataServiceDelegation;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.ListenerRegistryWrapper;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.client.ServiceDiscoveryRegistry;
import org.apache.dubbo.registry.client.ServiceDiscoveryRegistryDirectory;
import org.apache.dubbo.registry.client.migration.MigrationInvoker;
import org.apache.dubbo.registry.support.RegistryManager;
import org.apache.dubbo.registry.zookeeper.ZookeeperServiceDiscovery;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.test.check.registrycenter.config.ZookeeperRegistryCenterConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import static org.apache.dubbo.common.constants.RegistryConstants.CONSUMERS_CATEGORY;
import static org.apache.dubbo.rpc.Constants.SCOPE_REMOTE;


/**
 * This abstraction class will implement some methods as base for single registry center.
 */
public class SingleRegistryCenterDubboProtocolIntegrationTest implements IntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(SingleRegistryCenterDubboProtocolIntegrationTest.class);
    /**
     * Define the provider application name.
     */
    private static String PROVIDER_APPLICATION_NAME = "single-registry-center-provider-for-dubbo-protocol";
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
    private ServiceConfig<SingleRegistryCenterIntegrationServiceImpl> serviceConfig;

    /**
     * Define the {@link ReferenceConfig} instance.
     */
    private ReferenceConfig<SingleRegistryCenterIntegrationService> referenceConfig;

    /**
     * Define the {@link RegistryConfig} instance.
     */
    private RegistryConfig registryConfig;

    /**
     * The service instance of {@link SingleRegistryCenterIntegrationService}
     */
    private SingleRegistryCenterIntegrationService singleRegistryCenterIntegrationService;

    /**
     * Define the {@link SingleRegistryCenterExportedServiceListener} instance to obtain the exported services.
     */
    private SingleRegistryCenterExportedServiceListener singleRegistryCenterExportedServiceListener;

    @BeforeEach
    public void setUp() throws Exception {
        logger.info(getClass().getSimpleName() + " testcase is beginning...");
        DubboBootstrap.reset();
        // initialize ServiceConfig
        serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(SingleRegistryCenterIntegrationService.class);
        serviceConfig.setRef(new SingleRegistryCenterIntegrationServiceImpl());
        serviceConfig.setAsync(false);

        DubboBootstrap.getInstance()
            .application(new ApplicationConfig(PROVIDER_APPLICATION_NAME))
            .protocol(new ProtocolConfig(PROTOCOL_NAME, PROTOCOL_PORT))
            .service(serviceConfig);
        registryConfig = new RegistryConfig(ZookeeperRegistryCenterConfig.getConnectionAddress());
        DubboBootstrap.getInstance().registry(registryConfig);
    }

    @Test
    @Override
    public void integrate() {
        this.beforeExport();
        // export provider
        DubboBootstrap.getInstance().start();
        this.afterExport();

        // initialize consumer
        this.initConsumer();
        this.beforeRefer();
        singleRegistryCenterIntegrationService = referenceConfig.get();
        this.afterRefer();
    }

    /**
     * There are some checkpoints needed to check as follow :
     * <ul>
     *     <li>ServiceConfig is exported or not</li>
     *     <li>ServiceConfig's exportedUrl has values or not</li>
     *     <li>DubboBootstrap is initialized or not</li>
     *     <li>DubboBootstrap is started or not</li>
     *     <li>DubboBootstrap is shutdown or not</li>
     *     <li>The ServiceListener is loaded by SPI or not</li>
     * </ul>
     */
    private void beforeExport() {
        // ServiceConfig is exported or not
        Assertions.assertFalse(serviceConfig.isExported());
        // ServiceConfig's exportedUrl has values or not
        Assertions.assertEquals(serviceConfig.getExportedUrls().size(), 0);
        // DubboBootstrap is pending or not
        Assertions.assertTrue(DubboBootstrap.getInstance().isPending());
        // DubboBootstrap is initialized or not
        Assertions.assertFalse(DubboBootstrap.getInstance().isInitialized());
        // DubboBootstrap is started or not
        Assertions.assertFalse(DubboBootstrap.getInstance().isStarted());
        // DubboBootstrap is stopped or not
        Assertions.assertFalse(DubboBootstrap.getInstance().isStopped());
        // The ServiceListener is loaded by SPI or not
        Assertions.assertNull(singleRegistryCenterExportedServiceListener);
    }

    /**
     * There are some checkpoints needed to check as follow :
     * <ul>
     *     <li>DubboBootstrap is initialized or not</li>
     *     <li>DubboBootstrap is started or not</li>
     *     <li>DubboBootstrap is shutdown or not</li>
     *     <li>Service has been exported or not</li>
     *     <li>There is exported urls or not</li>
     *     <li>Protocol name is right or not</li>
     *     <li>Protocol port is right or not</li>
     *     <li>ServiceDiscoveryRegistry's protocol is right or not</li>
     *     <li>Registered service in registry center is right or not</li>
     *     <li>MetadataInfo has reported or not</li>
     *     <li>MetadataInfo has reported or not has service or not</li>
     *     <li>MetadataInfo's application name is right or not</li>
     *     <li>MetadataInfo's service exists or not</li>
     *     <li>The name of MetadataInfo's service is right or not</li>
     *     <li>The group of MetadataInfo's service is right or not</li>
     *     <li>The version of MetadataInfo's service is right or not</li>
     *     <li>The protocol of MetadataInfo's service is right or not</li>
     *     <li>The serviceKey of MetadataInfo's service is right or not</li>
     *     <li>The matchKey of MetadataInfo's service is right or not</li>
     *     <li>The exported service are right or not</li>
     * </ul>
     */
    private void afterExport() {
        // DubboBootstrap is initialized or not
        Assertions.assertTrue(DubboBootstrap.getInstance().isInitialized());
        // DubboBootstrap is pending or not
        Assertions.assertFalse(DubboBootstrap.getInstance().isPending());
        // DubboBootstrap is started or not
        Assertions.assertTrue(DubboBootstrap.getInstance().isStarted());
        // DubboBootstrap is shutdown or not
        Assertions.assertFalse(DubboBootstrap.getInstance().isStopped());
        // Service has been exported or not
        Assertions.assertTrue(this.serviceConfig.isExported());
        // There is exported urls or not
        Assertions.assertEquals(this.serviceConfig.getExportedUrls().size(), 1);
        URL exportedUrl = this.serviceConfig.getExportedUrls().get(0);
        // Protocol name is right or not
        Assertions.assertEquals(exportedUrl.getProtocol(), PROTOCOL_NAME);
        // Protocol port is right or not
        Assertions.assertEquals(exportedUrl.getPort(), PROTOCOL_PORT);
        // Application name is right or not
        Assertions.assertEquals(exportedUrl.getApplication(), PROVIDER_APPLICATION_NAME);

        // obtain ServiceDiscoveryRegistry instance
        ServiceDiscoveryRegistry serviceDiscoveryRegistry = this.getServiceDiscoveryRegistry();
        // ServiceDiscoveryRegistry instance cannot be null
        Assertions.assertNotNull(serviceDiscoveryRegistry);
        // ServiceDiscoveryRegistry's protocol is right or not
        Assertions.assertTrue(serviceDiscoveryRegistry.getServiceDiscovery() instanceof ZookeeperServiceDiscovery);
        // Convert to ZookeeperServiceDiscovery instance
        ZookeeperServiceDiscovery zookeeperServiceDiscovery = (ZookeeperServiceDiscovery) serviceDiscoveryRegistry.getServiceDiscovery();
        // Gets registered service by ZookeeperServiceDiscovery
        Set<String> services = zookeeperServiceDiscovery.getServices();
        // check service exists
        Assertions.assertTrue(!services.isEmpty());
        // Registered service in registry center is right or not
        Assertions.assertTrue(services.contains(PROVIDER_APPLICATION_NAME));

        // obtain InMemoryWritableMetadataService instance
        MetadataServiceDelegation inMemoryWritableMetadataService = (MetadataServiceDelegation) serviceConfig.getScopeModel().getBeanFactory().getBean(MetadataService.class);
        // Exported url is right or not in InMemoryWritableMetadataService
        Assertions.assertEquals(inMemoryWritableMetadataService.getExportedURLs().size(), 1);
        // MetadataInfo exists or not in InMemoryWritableMetadataService
        Assertions.assertFalse(inMemoryWritableMetadataService.getMetadataInfos().isEmpty());
        // MetadataInfo has reported or not has service or not
        Assertions.assertFalse(inMemoryWritableMetadataService.getMetadataInfos().get(0).getServices().isEmpty());
        // MetadataInfo has reported or not has service or not
        Assertions.assertEquals(inMemoryWritableMetadataService.getMetadataInfos().get(0).getServices().size(), 1);
        // obtain the service's key
        String key = SingleRegistryCenterIntegrationService.class.getName() + ":" + PROTOCOL_NAME;
        MetadataInfo.ServiceInfo serviceInfo = inMemoryWritableMetadataService.getMetadataInfos().get(0).getServices().get(key);
        // MetadataInfo's service exists or not
        Assertions.assertNotNull(serviceInfo);
        // The name of MetadataInfo's service is right or not
        Assertions.assertEquals(serviceInfo.getName(), SingleRegistryCenterIntegrationService.class.getName());
        // The group of MetadataInfo's service is right or not
        Assertions.assertNull(serviceInfo.getGroup());
        // The version of MetadataInfo's service is right or not
        Assertions.assertNull(serviceInfo.getVersion());
        // The protocol of MetadataInfo's service is right or not
        Assertions.assertEquals(serviceInfo.getProtocol(), PROTOCOL_NAME);
        // The serviceKey of MetadataInfo's service is right or not
        Assertions.assertEquals(serviceInfo.getServiceKey(), SingleRegistryCenterIntegrationService.class.getName());
        // The matchKey of MetadataInfo's service is right or not
        Assertions.assertEquals(serviceInfo.getMatchKey(), key);
        // The exported services are right or not
        // 1. The exported service must contain SingleRegistryCenterIntegrationService
        // 2. The exported service's interface must be SingleRegistryCenterIntegrationService.class
        // 3. All exported services must be exported
        singleRegistryCenterExportedServiceListener = (SingleRegistryCenterExportedServiceListener) ExtensionLoader.getExtensionLoader(ServiceListener.class).getExtension("exported");
        Assertions.assertNotNull(singleRegistryCenterExportedServiceListener);
        Assertions.assertEquals(singleRegistryCenterExportedServiceListener.getExportedServices().size(), 1);
        Assertions.assertEquals(SingleRegistryCenterIntegrationService.class,
            singleRegistryCenterExportedServiceListener.getExportedServices().get(0).getInterfaceClass());
        ServiceConfig singleRegistryCenterServiceConfig = singleRegistryCenterExportedServiceListener.getExportedServices().get(0);
        Assertions.assertNotNull(singleRegistryCenterServiceConfig);
        Assertions.assertTrue(singleRegistryCenterServiceConfig.isExported());
    }

    /**
     * Returns {@link ServiceDiscoveryRegistry} instance.
     * <p>
     * FIXME It's not a good way to obtain {@link ServiceDiscoveryRegistry} using Reflection.
     */
    private ServiceDiscoveryRegistry getServiceDiscoveryRegistry() {
        Collection<Registry> registries = RegistryManager.getInstance(ApplicationModel.defaultModel()).getRegistries();
        for (Registry registry : registries) {
            if(registry instanceof ServiceDiscoveryRegistry) {
                return (ServiceDiscoveryRegistry) registry;
            }
        }
        return null;
    }

    /**
     * Initialize the consumer.
     */
    private void initConsumer() {
        referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(SingleRegistryCenterIntegrationService.class);
        DubboBootstrap.getInstance().reference(referenceConfig);
        referenceConfig.setRegistry(registryConfig);
        referenceConfig.setScope(SCOPE_REMOTE);
        referenceConfig.setGeneric("false");
        referenceConfig.setProtocol(PROTOCOL_NAME);
    }

    /**
     * There are some checkpoints needed to check before referring as follow :
     * <ul>
     *     <li>ReferenceConfig has integrated into DubboBootstrap or not</li>
     * </ul>
     */
    private void beforeRefer() {
        // ReferenceConfig has integrated into DubboBootstrap or not
        Assertions.assertEquals(referenceConfig.getScopeModel(), DubboBootstrap.getInstance().getApplicationModel().getDefaultModule());
    }

    /**
     * There are some checkpoints needed to check after referred as follow :
     * <ul>
     *     <li>SingleRegistryCenterIntegrationService instance can't be null</li>
     *     <li>RPC works well or not</li>
     *     <li>Invoker is right or not</li>
     *     <li>Directory is null or not</li>
     *     <li>Registered interface is right or not</li>
     *     <li>Directory is available or not</li>
     *     <li>Directory is destroyed or not</li>
     *     <li>Directory has received notification or not</li>
     *     <li>ServiceDiscoveryRegistryDirectory should register or not</li>
     *     <li>ServiceDiscoveryRegistryDirectory's registered consumer url is right or not</li>
     *     <li>ServiceDiscoveryRegistryDirectory's registry is right or not</li>
     *     <li>Directory's invokers are right or not</li>
     * </ul>
     */
    private void afterRefer() {
        // SingleRegistryCenterIntegrationService instance can't be null
        Assertions.assertNotNull(singleRegistryCenterIntegrationService);
        // Invoker is right or not
        Assertions.assertNotNull(referenceConfig.getInvoker());
        Assertions.assertTrue(referenceConfig.getInvoker() instanceof MigrationInvoker);
        // RPC works well or not
        Assertions.assertEquals("Hello Reference",
            singleRegistryCenterIntegrationService.hello("Reference"));
        // get ServiceDiscoveryRegistryDirectory instance
        Directory directory = ((MigrationInvoker) referenceConfig.getInvoker()).getDirectory();
        // Directory is null or not
        Assertions.assertNotNull(directory);
        // Check Directory's type
        Assertions.assertTrue(directory instanceof ServiceDiscoveryRegistryDirectory);
        // Registered interface is right or not
        Assertions.assertEquals(directory.getInterface(), SingleRegistryCenterIntegrationService.class);
        // Directory is available or not
        Assertions.assertTrue(directory.isAvailable());
        // Directory is destroyed or not
        Assertions.assertFalse(directory.isDestroyed());
        // Directory has received notification or not
        Assertions.assertTrue(directory.isNotificationReceived());
        ServiceDiscoveryRegistryDirectory serviceDiscoveryRegistryDirectory = (ServiceDiscoveryRegistryDirectory) directory;
        // ServiceDiscoveryRegistryDirectory should register or not
        Assertions.assertTrue(serviceDiscoveryRegistryDirectory.isShouldRegister());
        // ServiceDiscoveryRegistryDirectory's registered consumer url is right or not
        Assertions.assertEquals(serviceDiscoveryRegistryDirectory.getRegisteredConsumerUrl().getCategory(), CONSUMERS_CATEGORY);
        // ServiceDiscoveryRegistryDirectory's registry is right or not
        Assertions.assertTrue(serviceDiscoveryRegistryDirectory.getRegistry() instanceof ListenerRegistryWrapper);
        // Directory's invokers are right or not
        Assertions.assertEquals(serviceDiscoveryRegistryDirectory.getAllInvokers().size(), 1);
        Assertions.assertEquals(serviceDiscoveryRegistryDirectory.getInvokers(), serviceDiscoveryRegistryDirectory.getAllInvokers());
    }

    @AfterEach
    public void tearDown() throws IOException {
        DubboBootstrap.reset();
        PROVIDER_APPLICATION_NAME = null;
        PROTOCOL_NAME = null;
        PROTOCOL_PORT = 0;
        serviceConfig = null;
        referenceConfig = null;
        // The exported service has been unexported
        Assertions.assertTrue(singleRegistryCenterExportedServiceListener.getExportedServices().isEmpty());
        singleRegistryCenterExportedServiceListener = null;
        logger.info(getClass().getSimpleName() + " testcase is ending...");
    }
}
