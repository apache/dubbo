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
package org.apache.dubbo.integration.servicediscoveryregistry;

import org.apache.dubbo.ZooKeeperServerTesting;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.integration.AbstractIntegrationTest;
import org.apache.dubbo.integration.IntegrationService;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.client.ServiceDiscoveryRegistry;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;
import org.apache.dubbo.registry.zookeeper.ZookeeperServiceDiscovery;
import org.junit.jupiter.api.Assertions;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

/**
 * The purpose of is to check if there exists problem
 * between {@link org.apache.dubbo.registry.integration.RegistryProtocol} and
 * {@link org.apache.dubbo.registry.client.ServiceDiscoveryRegistry}
 * using zookeeper as registry center.
 */
public abstract class AbstractServiceDiscoveryRegistryTest extends AbstractIntegrationTest<ServiceDiscoveryRegistryIntegrationService> {

    private ServiceConfig<ServiceDiscoveryRegistryIntegrationService> serviceConfig;

    @Override
    public void initialize() {
        // initialize ServiceConfig
        this.serviceConfig = new ServiceConfig<>();
        this.serviceConfig.setInterface(IntegrationService.class);
        this.serviceConfig.setRef(new ServiceDiscoveryRegistryIntegrationService());
        this.serviceConfig.setAsync(false);

        DubboBootstrap.getInstance()
            .application(new ApplicationConfig(this.getApplicationName()))
            .registry(new RegistryConfig("zookeeper://127.0.0.1:" + ZooKeeperServerTesting.getPort()))
            .protocol(new ProtocolConfig(this.getProtocolName(), this.getProtocolPort()))
            .service(this.serviceConfig);
    }

    @Override
    public void process() {
        this.beforeExport();
        // export provider
        DubboBootstrap.getInstance().start();
        this.afterExport();
    }

    /**
     * before export
     */
    private void beforeExport() {
        // assert the service hasn't been exported
        Assertions.assertFalse(this.serviceConfig.isExported());
        // assert there is no exported url
        Assertions.assertEquals(this.serviceConfig.getExportedUrls().size(), 0);
    }

    /**
     * after export
     */
    private void afterExport() {
        // obtain ServiceDiscoveryRegistry instance
        ServiceDiscoveryRegistry serviceDiscoveryRegistry = this.getServiceDiscoveryRegistry();
        serviceDiscoveryRegistry.getServiceDiscovery();
        // check service discovery protocol
        Assertions.assertTrue(serviceDiscoveryRegistry.getServiceDiscovery() instanceof ZookeeperServiceDiscovery);
        // convert to ZookeeperServiceDiscovery instance
        ZookeeperServiceDiscovery zookeeperServiceDiscovery =(ZookeeperServiceDiscovery) serviceDiscoveryRegistry.getServiceDiscovery();
        // zookeeperServiceDiscovery can't destroy
        Assertions.assertFalse(zookeeperServiceDiscovery.isDestroy());
        // Gets registered service by ZookeeperServiceDiscovery
        Set<String> services = zookeeperServiceDiscovery.getServices();
        // check service exists
        Assertions.assertTrue(!services.isEmpty());
        Assertions.assertTrue(services.contains(this.getApplicationName()));
    }

    /**
     * Returns {@link ServiceDiscoveryRegistry} instance.
     * <p>
     * FIXME It's not a good way to obtain {@link ServiceDiscoveryRegistry} using Reflection.
     */
    private ServiceDiscoveryRegistry getServiceDiscoveryRegistry(){
        ServiceDiscoveryRegistry serviceDiscoveryRegistry = null;
        try {
            // get AbstractRegistryFactory.REGISTRIES
            Field field = AbstractRegistryFactory.class.getDeclaredField("REGISTRIES");
            field.setAccessible(true);
            Map<String, Registry> REGISTRIES = (Map<String, Registry>)field.get(AbstractRegistryFactory.class);
            for(Registry registry:REGISTRIES.values()){
                if(registry instanceof ServiceDiscoveryRegistry){
                    serviceDiscoveryRegistry = (ServiceDiscoveryRegistry)registry;
                    break;
                }
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            // ignore
        }
        return serviceDiscoveryRegistry;
    }

    @Override
    public void destroy() {
        this.serviceConfig = null;
    }

    /**
     * Returns the protocol's name.
     */
    protected abstract String getProtocolName();

    /**
     * Returns the application name.
     */
    protected abstract String getApplicationName();

    /**
     * Returns the protocol's port.
     */
    protected abstract int getProtocolPort();
}
