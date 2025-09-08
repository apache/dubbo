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
package org.apache.dubbo.config.integration.single.exportprovider;

import org.apache.dubbo.common.config.configcenter.ConfigItem;
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
import org.apache.dubbo.metadata.ServiceNameMapping;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.registry.integration.RegistryProtocolListener;
import org.apache.dubbo.rpc.ExporterListener;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.test.check.registrycenter.config.ZookeeperRegistryCenterConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.dubbo.common.constants.CommonConstants.REGISTRY_PROTOCOL_LISTENER_KEY;
import static org.apache.dubbo.config.integration.Constants.SINGLE_CONFIG_CENTER_EXPORT_PROVIDER;
import static org.apache.dubbo.rpc.Constants.SCOPE_LOCAL;

/**
 * The testcases are only for checking the core process of exporting provider.
 */
class SingleRegistryCenterExportProviderIntegrationTest implements IntegrationTest {

    private static final Logger logger =
            LoggerFactory.getLogger(SingleRegistryCenterExportProviderIntegrationTest.class);

    /**
     * Define the provider application name.
     */
    private static String PROVIDER_APPLICATION_NAME = "single-registry-center-for-export-provider";

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
    private ServiceConfig<SingleRegistryCenterExportProviderService> serviceConfig;

    /**
     * Define a {@link RegistryProtocolListener} instance.
     */
    private SingleRegistryCenterExportProviderRegistryProtocolListener registryProtocolListener;

    /**
     * Define a {@link ExporterListener} instance.
     */
    private SingleRegistryCenterExportProviderExporterListener exporterListener;

    /**
     * Define a {@link Filter} instance.
     */
    private SingleRegistryCenterExportProviderFilter filter;

    /**
     * Define a {@link ServiceListener} instance.
     */
    private SingleRegistryCenterExportProviderServiceListener serviceListener;

    @BeforeEach
    public void setUp() throws Exception {
        logger.info(getClass().getSimpleName() + " testcase is beginning...");
        DubboBootstrap.reset();
        // initialize service config
        serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(SingleRegistryCenterExportProviderService.class);
        serviceConfig.setRef(new SingleRegistryCenterExportProviderServiceImpl());
        serviceConfig.setAsync(false);

        // initialize bootstrap
        DubboBootstrap.getInstance()
                .application(new ApplicationConfig(PROVIDER_APPLICATION_NAME))
                .protocol(new ProtocolConfig(PROTOCOL_NAME, PROTOCOL_PORT))
                .service(serviceConfig);

        RegistryConfig registryConfig = new RegistryConfig(ZookeeperRegistryCenterConfig.getConnectionAddress());
        Map<String, String> parameters = new HashMap<>();
        parameters.put(REGISTRY_PROTOCOL_LISTENER_KEY, "singleConfigCenterExportProvider");
        registryConfig.updateParameters(parameters);
        DubboBootstrap.getInstance().registry(registryConfig);
    }

    /**
     * There are some checkpoints need to verify as follow:
     * <ul>
     *     <li>ServiceConfig is exported or not</li>
     *     <li>SingleRegistryCenterExportProviderRegistryProtocolListener is null or not</li>
     *     <li>There is nothing in ServiceListener or not</li>
     *     <li>There is nothing in ExporterListener or not</li>
     * </ul>
     */
    private void beforeExport() {
        registryProtocolListener = (SingleRegistryCenterExportProviderRegistryProtocolListener)
                ExtensionLoader.getExtensionLoader(RegistryProtocolListener.class)
                        .getExtension(SINGLE_CONFIG_CENTER_EXPORT_PROVIDER);
        exporterListener = (SingleRegistryCenterExportProviderExporterListener)
                ExtensionLoader.getExtensionLoader(ExporterListener.class)
                        .getExtension(SINGLE_CONFIG_CENTER_EXPORT_PROVIDER);
        filter = (SingleRegistryCenterExportProviderFilter)
                ExtensionLoader.getExtensionLoader(Filter.class).getExtension(SINGLE_CONFIG_CENTER_EXPORT_PROVIDER);
        serviceListener = (SingleRegistryCenterExportProviderServiceListener)
                ExtensionLoader.getExtensionLoader(ServiceListener.class)
                        .getExtension(SINGLE_CONFIG_CENTER_EXPORT_PROVIDER);
        // ---------------checkpoints--------------- //
        // ServiceConfig isn't exported
        Assertions.assertFalse(serviceConfig.isExported());
        // registryProtocolListener is just initialized by SPI
        // so, all of fields are the default value.
        Assertions.assertNotNull(registryProtocolListener);
        Assertions.assertFalse(registryProtocolListener.isExported());
        // There is nothing in ServiceListener
        Assertions.assertTrue(serviceListener.getExportedServices().isEmpty());
        // There is nothing in ExporterListener
        Assertions.assertTrue(exporterListener.getExportedExporters().isEmpty());
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
        ReferenceConfig<SingleRegistryCenterExportProviderService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(SingleRegistryCenterExportProviderService.class);
        referenceConfig.setScope(SCOPE_LOCAL);
        referenceConfig.get().hello(PROVIDER_APPLICATION_NAME);
        afterInvoke();
    }

    /**
     * There are some checkpoints need to check after exported as follow:
     * <ul>
     *     <li>the exporter is exported or not</li>
     *     <li>The exported exporter are three</li>
     *     <li>The exported service is SingleRegistryCenterExportProviderService or not</li>
     *     <li>The SingleRegistryCenterExportProviderService is exported or not</li>
     *     <li>The exported exporter contains SingleRegistryCenterExportProviderFilter or not</li>
     *     <li>The metadata mapping info is right or not</li>
     * </ul>
     */
    private void afterExport() {
        // The exporter is exported
        Assertions.assertTrue(registryProtocolListener.isExported());
        // The exported service is only one
        Assertions.assertEquals(serviceListener.getExportedServices().size(), 1);
        // The exported service is SingleRegistryCenterExportProviderService
        Assertions.assertEquals(
                serviceListener.getExportedServices().get(0).getInterfaceClass(),
                SingleRegistryCenterExportProviderService.class);
        // The SingleRegistryCenterExportProviderService is exported
        Assertions.assertTrue(serviceListener.getExportedServices().get(0).isExported());
        // The exported exporter are three
        // 1. InjvmExporter
        // 2. DubboExporter with service-discovery-registry protocol
        // 3. DubboExporter with registry protocol
        Assertions.assertEquals(exporterListener.getExportedExporters().size(), 4);
        // The exported exporter contains SingleRegistryCenterExportProviderFilter
        Assertions.assertTrue(exporterListener.getFilters().contains(filter));
        // The consumer can be notified and get provider's metadata through metadata mapping info.
        // So, the metadata mapping is necessary to check after exported service (or provider)
        // The best way to verify this issue is to check if the exported service (or provider)
        // has been registered in the path of /dubbo/mapping/****
        // What are the parameters?
        // registryKey: the registryKey is the default cluster, CommonConstants.DEFAULT_KEY
        // key: The exported interface's name
        // group: the group is "mapping", ServiceNameMapping.DEFAULT_MAPPING_GROUP
        ConfigItem configItem = ApplicationModel.defaultModel()
                .getBeanFactory()
                .getBean(MetadataReportInstance.class)
                .getMetadataReport(CommonConstants.DEFAULT_KEY)
                .getConfigItem(serviceConfig.getInterface(), ServiceNameMapping.DEFAULT_MAPPING_GROUP);
        // Check if the exported service (provider) is registered
        Assertions.assertNotNull(configItem);
        // Check if registered service (provider)'s name is right
        Assertions.assertEquals(PROVIDER_APPLICATION_NAME, configItem.getContent());
        // Check if registered service (provider)'s version exists
        Assertions.assertNotNull(configItem.getTicket());
    }

    /**
     * There are some checkpoints need to check after invoked as follow:
     * <ul>
     *     <li>The SingleRegistryCenterExportProviderFilter has called or not</li>
     *     <li>The SingleRegistryCenterExportProviderFilter exists error after invoked</li>
     *     <li>The SingleRegistryCenterExportProviderFilter's response is right or not</li>
     * </ul>
     */
    private void afterInvoke() {
        // The SingleRegistryCenterExportProviderFilter has called
        Assertions.assertTrue(filter.hasCalled());
        // The SingleRegistryCenterExportProviderFilter doesn't exist error
        Assertions.assertFalse(filter.hasError());
        // Check the SingleRegistryCenterExportProviderFilter's response
        Assertions.assertEquals("Hello " + PROVIDER_APPLICATION_NAME, filter.getResponse());
    }

    @AfterEach
    public void tearDown() throws IOException {
        DubboBootstrap.reset();
        serviceConfig = null;
        // The exported service has been unexported
        Assertions.assertTrue(serviceListener.getExportedServices().isEmpty());
        logger.info(getClass().getSimpleName() + " testcase is ending...");
        registryProtocolListener = null;
    }
}
