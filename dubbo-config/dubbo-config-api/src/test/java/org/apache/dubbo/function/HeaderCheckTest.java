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
package org.apache.dubbo.function;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.*;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.integration.single.SingleRegistryCenterExportedServiceListener;
import org.apache.dubbo.integration.single.SingleRegistryCenterIntegrationService;
import org.apache.dubbo.integration.single.SingleRegistryCenterIntegrationServiceImpl;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.client.ServiceDiscoveryRegistry;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;
import org.apache.dubbo.registrycenter.RegistryCenter;
import org.apache.dubbo.registrycenter.ZookeeperSingleRegistryCenter;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import static org.apache.dubbo.rpc.Constants.SCOPE_REMOTE;
import static org.junit.Assert.assertTrue;

/**
 * This class will test header length limit function for triple protocol
 */
public class HeaderCheckTest {
    private static final Logger logger = LoggerFactory.getLogger(HeaderCheckTest.class);
    /**
     * Define the provider application name.
     */
    private static String PROVIDER_APPLICATION_NAME = "single-registry-center-provider-for-dubbo-protocol";
    /**
     * Define the protocol's name.
     */
    private static String PROTOCOL_NAME = CommonConstants.TRIPLE;
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

    /**
     * Define a registry center.
     */
    private RegistryCenter registryCenter;

    @BeforeEach
    public void setUp() throws Exception {
        logger.info(getClass().getSimpleName() + " testcase is beginning...");
        DubboBootstrap.reset();
        registryCenter = new ZookeeperSingleRegistryCenter(NetUtils.getAvailablePort());
        registryCenter.startup();
        // initialize ServiceConfig
        serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(SingleRegistryCenterIntegrationService.class);
        serviceConfig.setRef(new SingleRegistryCenterIntegrationServiceImpl());
        serviceConfig.setAsync(false);
        serviceConfig.setScope("remote");

        DubboBootstrap.getInstance()
            .application(new ApplicationConfig(PROVIDER_APPLICATION_NAME))
            .protocol(new ProtocolConfig(PROTOCOL_NAME, 51008))
            .service(serviceConfig);
        RegistryCenter.Instance instance = registryCenter.getRegistryCenterInstance().get(0);
        registryConfig = new RegistryConfig(String.format("%s://%s:%s",
            instance.getType(),
            instance.getHostname(),
            instance.getPort()));
        DubboBootstrap.getInstance().registry(registryConfig);
    }

    /**
     * setting attachment to trigger the exception of checking header length  after removing tripleutil check
     * pass the error to upper layer
     */
    @Test()
    public void integrate() {
        try {
            // export provider
            DubboBootstrap.getInstance().start();
            singleRegistryCenterExportedServiceListener = (SingleRegistryCenterExportedServiceListener) ExtensionLoader.getExtensionLoader(ServiceListener.class).getExtension("exported");
            // initialize consumer
            this.initConsumer();
            singleRegistryCenterIntegrationService = referenceConfig.get();
            StringBuilder sb = new StringBuilder("a");
            for (int j = 0; j < 15; j++) {
                sb.append(sb);
            }
            sb.setLength(1000);
            RpcContext.getClientAttachment().setObjectAttachment("large-size-meta", sb.toString());
            singleRegistryCenterIntegrationService.hello("hello");
        } catch (Exception e) {
            assertTrue(e instanceof RpcException);
        }
    }

    /**
     * Returns {@link ServiceDiscoveryRegistry} instance.
     * <p>
     */
    private ServiceDiscoveryRegistry getServiceDiscoveryRegistry() {
        ServiceDiscoveryRegistry serviceDiscoveryRegistry = null;
        try {
            // get AbstractRegistryFactory.REGISTRIES
            Field field = AbstractRegistryFactory.class.getDeclaredField("REGISTRIES");
            field.setAccessible(true);
            Map<String, Registry> REGISTRIES = (Map<String, Registry>) field.get(AbstractRegistryFactory.class);
            for (Registry registry : REGISTRIES.values()) {
                if (registry instanceof ServiceDiscoveryRegistry) {
                    serviceDiscoveryRegistry = (ServiceDiscoveryRegistry) registry;
                    break;
                }
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            // ignore
        }
        return serviceDiscoveryRegistry;
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
        // destroy registry center
        registryCenter.shutdown();
        registryCenter = null;
    }

}
