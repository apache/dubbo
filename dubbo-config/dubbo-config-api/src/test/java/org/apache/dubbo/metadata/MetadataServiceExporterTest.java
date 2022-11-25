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
//package org.apache.dubbo.metadata;
//
//import org.apache.dubbo.common.URL;
//import org.apache.dubbo.common.deploy.ApplicationDeployListener;
//import org.apache.dubbo.common.utils.NetUtils;
//import org.apache.dubbo.config.ApplicationConfig;
//import org.apache.dubbo.config.ProtocolConfig;
//import org.apache.dubbo.config.RegistryConfig;
//import org.apache.dubbo.config.ServiceConfig;
//import org.apache.dubbo.config.api.DemoService;
//import org.apache.dubbo.config.bootstrap.DubboBootstrap;
//import org.apache.dubbo.config.metadata.ConfigurableMetadataServiceExporter;
//import org.apache.dubbo.config.metadata.ExporterDeployListener;
//import org.apache.dubbo.registry.client.metadata.MetadataServiceDelegation;
//import org.apache.dubbo.config.provider.impl.DemoServiceImpl;
//import org.apache.dubbo.rpc.model.ApplicationModel;
//import org.apache.dubbo.rpc.model.FrameworkModel;
//import org.apache.dubbo.test.check.registrycenter.RegistryCenter;
//
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//
//import java.util.List;
//
//import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
//import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertNotEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//public class MetadataServiceExporterTest {
//
//    @BeforeEach
//    public void init() {
//        DubboBootstrap.reset();
//
//        ApplicationConfig applicationConfig = new ApplicationConfig("Test");
//        applicationConfig.setRegisterConsumer(true);
//        ApplicationModel.defaultModel().getApplicationConfigManager().setApplication(applicationConfig);
//        ApplicationModel.defaultModel().getApplicationConfigManager().addRegistry(new RegistryConfig("multicast://224.5.6.7:1234"));
//        ApplicationModel.defaultModel().getApplicationConfigManager().addProtocol(new ProtocolConfig("injvm"));
//    }
//
//    @Test
//    public void test() {
//        MetadataServiceDelegation metadataService = Mockito.mock(MetadataServiceDelegation.class);
//        ConfigurableMetadataServiceExporter exporter = new ConfigurableMetadataServiceExporter(ApplicationModel.defaultModel(), metadataService);
//        exporter.setMetadataService(metadataService);
//
//        exporter.export();
//        assertTrue(exporter.isExported());
//        exporter.unexport();
//
//    }
//
//    @Test
//    public void test2() throws Exception {
//
//        ApplicationModel applicationModel = ApplicationModel.defaultModel();
//
//        applicationModel.getDeployer().start().get();
//        ExporterDeployListener listener = getListener(applicationModel);
//        ConfigurableMetadataServiceExporter exporter = listener.getMetadataServiceExporter();
//
//        assertTrue(exporter.isExported());
//
//        applicationModel.getDeployer().stop();
//        assertFalse(exporter.isExported());
//    }
//
//    /**
//     * test reuse of port started by normal service
//     */
//    @Test
//    public void testPortReuse() throws Exception {
//        DubboBootstrap providerBootstrap = DubboBootstrap.newInstance();
//        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
//        serviceConfig.setInterface(DemoService.class);
//        serviceConfig.setRef(new DemoServiceImpl());
//
//        ApplicationConfig applicationConfig = new ApplicationConfig("exporter-test");
//        applicationConfig.setMetadataType(DEFAULT_METADATA_STORAGE_TYPE);
//
//        providerBootstrap
//            .application(applicationConfig)
//            .registry(registryConfig)
//            .protocol(new ProtocolConfig("dubbo", 2002))
//            .service(serviceConfig);
//
//        // will start exporter
//        providerBootstrap.start();
//        ExporterDeployListener listener = getListener(providerBootstrap.getApplicationModel());
//        ConfigurableMetadataServiceExporter exporter = listener.getMetadataServiceExporter();
//
//        try {
//            assertTrue(exporter.isExported());
//            List<URL> urls = exporter.getExportedURLs();
//            assertNotNull(urls);
//            assertEquals(2002, urls.get(0).getPort());
//            assertEquals(DUBBO_PROTOCOL, urls.get(0).getProtocol());
//        } finally {
//            providerBootstrap.stop();
//        }
//        assertFalse(exporter.isExported());
//    }
//
//    /**
//     * test user specified port and protocol
//     * @throws Exception
//     */
//    @Test
//    public void testSpecifiedPortAndProtocol() throws Exception {
//        DubboBootstrap providerBootstrap = DubboBootstrap.newInstance();
//        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
//        serviceConfig.setInterface(DemoService.class);
//        serviceConfig.setRef(new DemoServiceImpl());
//
//        ApplicationConfig applicationConfig = new ApplicationConfig("exporter-test");
//        applicationConfig.setMetadataType(DEFAULT_METADATA_STORAGE_TYPE);
//        applicationConfig.setMetadataServiceProtocol("tri");
//        applicationConfig.setMetadataServicePort(8089);
//
//        providerBootstrap
//            .application(applicationConfig)
//            .registry(registryConfig)
//            .protocol(new ProtocolConfig("dubbo", 2002))
//            .service(serviceConfig);
//
//        // will start exporter.export()
//        providerBootstrap.start();
//        ExporterDeployListener listener = getListener(providerBootstrap.getApplicationModel());
//        ConfigurableMetadataServiceExporter exporter = listener.getMetadataServiceExporter();
//
//        try {
//            assertTrue(exporter.isExported());
//            List<URL> urls = exporter.getExportedURLs();
//            assertNotNull(urls);
//            assertEquals(8089, urls.get(0).getPort());
//            assertEquals("tri", urls.get(0).getProtocol());
//        } finally {
//            providerBootstrap.stop();
//        }
//        assertFalse(exporter.isExported());
//    }
//
//    @Test
//    public void testMetadataStartsBeforeNormalService() throws Exception {
//        DubboBootstrap providerBootstrap = DubboBootstrap.newInstance();
//        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
//        serviceConfig.setInterface(DemoService.class);
//        serviceConfig.setRef(new DemoServiceImpl());
//        serviceConfig.setDelay(1000);
//
//        ApplicationConfig applicationConfig = new ApplicationConfig("exporter-test");
//        applicationConfig.setMetadataType(DEFAULT_METADATA_STORAGE_TYPE);
////        applicationConfig.setMetadataServiceProtocol("triple");
////        applicationConfig.setMetadataServicePort(8089);
//
//        providerBootstrap
//            .application(applicationConfig)
//            .registry(registryConfig)
//            .protocol(new ProtocolConfig("dubbo", 2002))
//            .service(serviceConfig);
//
//        // will start exporter.export()
//        providerBootstrap.start();
//        ExporterDeployListener listener = getListener(providerBootstrap.getApplicationModel());
//        ConfigurableMetadataServiceExporter exporter = listener.getMetadataServiceExporter();
//        try {
//            assertTrue(exporter.isExported());
//            List<URL> urls = exporter.getExportedURLs();
//            assertNotNull(urls);
//            assertNotEquals(2002, urls.get(0).getPort());
//            assertEquals("dubbo", urls.get(0).getProtocol());
//        } finally {
//            providerBootstrap.stop();
//        }
//        assertFalse(exporter.isExported());
//    }
////
////    /**
////     * test multiple protocols
////     * @throws Exception
////     */
////    @Test
////    public void testMultiProtocols() throws Exception {
////        DubboBootstrap providerBootstrap = DubboBootstrap.newInstance();
////        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
////        serviceConfig.setInterface(DemoService.class);
////        serviceConfig.setRef(new DemoServiceImpl());
////
////        providerBootstrap
////            .application("provider-app")
////            .registry(registryConfig)
////            .protocol(new ProtocolConfig("dubbo", 2002))
////            .service(serviceConfig);
////
////        ConfigurableMetadataServiceExporter exporter = (ConfigurableMetadataServiceExporter) applicationModel.getExtensionLoader(MetadataServiceExporter.class).getDefaultExtension();
////        MetadataService metadataService = Mockito.mock(MetadataService.class);
////        exporter.setMetadataService(metadataService);
////
////        try {
////            providerBootstrap.start();
////            assertTrue(exporter.isExported());
////            assertTrue(exporter.supports(DEFAULT_METADATA_STORAGE_TYPE));
////            assertTrue(exporter.supports(REMOTE_METADATA_STORAGE_TYPE));
////            assertTrue(exporter.supports(COMPOSITE_METADATA_STORAGE_TYPE));
////        } finally {
////            providerBootstrap.stop();
////        }
////        assertFalse(exporter.isExported());
////    }
//
//    private static ZookeeperSingleRegistryCenter registryCenter;
//    private static RegistryConfig registryConfig;
//
//    @BeforeAll
//    public static void beforeAll() {
//        FrameworkModel.destroyAll();
//        registryCenter = new ZookeeperSingleRegistryCenter(NetUtils.getAvailablePort());
//        registryCenter.startup();
//        RegistryCenter.Instance instance = registryCenter.getRegistryCenterInstance().get(0);
//        registryConfig = new RegistryConfig(String.format("%s://%s:%s",
//            instance.getType(),
//            instance.getHostname(),
//            instance.getPort()));
//
//        // pre-check threads
//        //precheckUnclosedThreads();
//    }
//
//    private ExporterDeployListener getListener(ApplicationModel model) {
//        return (ExporterDeployListener)model.getExtensionLoader(ApplicationDeployListener.class).getExtension("exporter");
//    }
//
//}