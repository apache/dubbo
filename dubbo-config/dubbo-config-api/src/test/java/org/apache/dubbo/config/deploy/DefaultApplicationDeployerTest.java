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
package org.apache.dubbo.config.deploy;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;
import org.apache.dubbo.metrics.utils.MetricsSupportUtil;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_PROMETHEUS;

class DefaultApplicationDeployerTest {

    @BeforeEach
    void tearDown() {
        DubboBootstrap.reset();
    }

    @Test
    void isSupportPrometheus() {
        boolean supportPrometheus = MetricsSupportUtil.isSupportPrometheus();
        Assert.assertTrue(supportPrometheus, "MetricsSupportUtil.isSupportPrometheus() should return true");
    }

    @Test
    void isImportPrometheus() {
        MetricsConfig metricsConfig = new MetricsConfig();
        metricsConfig.setProtocol("prometheus");
        boolean importPrometheus =
                PROTOCOL_PROMETHEUS.equals(metricsConfig.getProtocol()) && !MetricsSupportUtil.isSupportPrometheus();
        Assert.assertTrue(!importPrometheus, " should return false");
    }

    @Test
    void testRemoteMetadataServiceExporterCheckMetadataType() {

        Assertions.assertThrowsExactly(
                IllegalStateException.class,
                () -> {
                    int availablePort = NetUtils.getAvailablePort();

                    ApplicationConfig applicationConfig = new ApplicationConfig("test");
                    applicationConfig.setMetadataServicePort(availablePort);
                    applicationConfig.setMetadataType(REMOTE_METADATA_STORAGE_TYPE);
                    DubboBootstrap.getInstance().application(applicationConfig).initialize();
                },
                "No MetadataConfig found, Metadata Center address is required when 'metadata=remote' is enabled");
    }

    @Test
    void testStartStatus() {
        ServiceConfig<DemoService> service = new ServiceConfig<>();
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());

        RegistryConfig registryConfig = new RegistryConfig("zookeeper://127.0.0.1:2181");
        registryConfig.setUseAsConfigCenter(false);
        registryConfig.setUseAsMetadataCenter(false);
        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        ApplicationModel applicationModel = bootstrap
                .application(new ApplicationConfig("test"))
                .registry(registryConfig)
                .protocol(new ProtocolConfig(CommonConstants.DUBBO_PROTOCOL, -1))
                .getApplicationModel();
        DefaultApplicationDeployer applicationDeployer = new DefaultApplicationDeployer(applicationModel);
        applicationDeployer.start();

        Assertions.assertTrue(bootstrap.isInitialized());
        Assertions.assertTrue(bootstrap.isCompletion());
        Assertions.assertFalse(bootstrap.isStopped());
    }
}
