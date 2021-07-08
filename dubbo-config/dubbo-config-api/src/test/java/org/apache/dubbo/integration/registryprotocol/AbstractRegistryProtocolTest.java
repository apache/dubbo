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
package org.apache.dubbo.integration.registryprotocol;

import org.apache.dubbo.ZooKeeperServerTesting;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.integration.AbstractIntegrationTest;
import org.apache.dubbo.integration.IntegrationService;
import org.junit.jupiter.api.Assertions;

/**
 * <pre>The purpose of this testcase is to check if there exists problems between {@link org.apache.dubbo.config.ServiceConfig} and {@link org.apache.dubbo.registry.integration.RegistryProtocol} using {@code zookeeper} protocol.</pre>
 */
public abstract class AbstractRegistryProtocolTest extends AbstractIntegrationTest<RegistryProtocolIntegrationService> {

    private ServiceConfig<RegistryProtocolIntegrationService> serviceConfig;

    @Override
    public void initialize() {
        // initialize ServiceConfig
        this.serviceConfig = new ServiceConfig<>();
        this.serviceConfig.setInterface(IntegrationService.class);
        this.serviceConfig.setRef(new RegistryProtocolIntegrationService());
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
        // check the service has been exported
        Assertions.assertTrue(this.serviceConfig.isExported());
        // check there is exported url
        Assertions.assertEquals(this.serviceConfig.getExportedUrls().size(), 1);
        URL exportedUrl = this.serviceConfig.getExportedUrls().get(0);
        // check the protocol
        Assertions.assertEquals(exportedUrl.getProtocol(), this.getProtocolName());
        // check the application name
        Assertions.assertEquals(exportedUrl.getApplication(), this.getApplicationName());
        // check the service port
        Assertions.assertEquals(exportedUrl.getPort(), this.getProtocolPort());
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
