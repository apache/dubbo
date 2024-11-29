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
package org.apache.dubbo.config.metadata;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

public class ExporterDeployListenerTest {

    @BeforeEach
    void tearDown() {
        DubboBootstrap.reset();
    }

    @Test
    void testRemoteMetadataServiceExporter() {
        ServiceConfig<DemoService> service = new ServiceConfig<>();
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());

        int availablePort = NetUtils.getAvailablePort();

        ApplicationConfig applicationConfig = new ApplicationConfig("bootstrap-test");
        applicationConfig.setMetadataServicePort(availablePort);
        applicationConfig.setMetadataType(REMOTE_METADATA_STORAGE_TYPE);

        RegistryConfig registryConfig = new RegistryConfig("zookeeper://127.0.0.1:2181");
        registryConfig.setUseAsMetadataCenter(false);
        registryConfig.setUseAsConfigCenter(false);

        ExporterDeployListener exporterDeployListener = new ExporterDeployListener();
        ApplicationModel applicationModel = DubboBootstrap.getInstance()
                .application(applicationConfig)
                .registry(registryConfig)
                .protocol(new ProtocolConfig(CommonConstants.DUBBO_PROTOCOL, -1))
                .service(service)
                .metadataReport(new MetadataReportConfig("zookeeper://127.0.0.1:2181"))
                .getApplicationModel();
        exporterDeployListener.onModuleStarted(applicationModel);
        Assertions.assertFalse(
                exporterDeployListener.getMetadataServiceExporter().isExported());
        DubboProtocol protocol = DubboProtocol.getDubboProtocol(applicationModel);
        Map<String, Exporter<?>> exporters = protocol.getExporterMap();
        Assertions.assertEquals(0, exporters.size());
    }

    @Test
    void testLocalMetadataServiceExporter() {
        ServiceConfig<DemoService> service = new ServiceConfig<>();
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());

        int availablePort = NetUtils.getAvailablePort();

        ApplicationConfig applicationConfig = new ApplicationConfig("test");
        applicationConfig.setMetadataServicePort(availablePort);
        RegistryConfig registryConfig = new RegistryConfig("zookeeper://127.0.0.1:2181");
        registryConfig.setUseAsMetadataCenter(false);
        registryConfig.setUseAsConfigCenter(false);

        ExporterDeployListener exporterDeployListener = new ExporterDeployListener();
        ApplicationModel applicationModel = DubboBootstrap.getInstance()
                .application(applicationConfig)
                .registry(registryConfig)
                .protocol(new ProtocolConfig(CommonConstants.DUBBO_PROTOCOL, -1))
                .service(service)
                .getApplicationModel();
        exporterDeployListener.onModuleStarted(applicationModel);

        Assertions.assertTrue(
                exporterDeployListener.getMetadataServiceExporter().isExported());

        DubboProtocol protocol = DubboProtocol.getDubboProtocol(applicationModel);
        Map<String, Exporter<?>> exporters = protocol.getExporterMap();
        Assertions.assertEquals(1, exporters.size());

        ServiceConfig<MetadataService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setRegistry(new RegistryConfig("N/A"));
        serviceConfig.setInterface(MetadataService.class);
        serviceConfig.setGroup(
                ApplicationModel.defaultModel().getCurrentConfig().getName());
        serviceConfig.setVersion(MetadataService.VERSION);
        assertThat(exporters, hasEntry(is(serviceConfig.getUniqueServiceName() + ":" + availablePort), anything()));
    }
}
