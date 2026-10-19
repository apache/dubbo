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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.MetadataServiceV2;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceDiscoveryRegistry;
import org.apache.dubbo.registry.client.metadata.MetadataServiceDelegation;
import org.apache.dubbo.registry.client.metadata.MetadataServiceDelegationV2;
import org.apache.dubbo.registry.client.metadata.MetadataUtils;
import org.apache.dubbo.registry.client.metadata.StandardMetadataServiceURLBuilder;
import org.apache.dubbo.registry.support.RegistryManager;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.METADATA_SERVICE_VERSION_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetadataServiceProtocolTest {

    static Stream<Arguments> metadataProtocols() {
        return Stream.of(
                Arguments.of(Arrays.asList("dubbo", "tri"), false, null, false, "tri", true),
                Arguments.of(Arrays.asList("tri", "dubbo"), false, null, false, "tri", true),
                Arguments.of(Collections.singletonList("tri"), false, null, false, "tri", true),
                Arguments.of(Collections.singletonList("tri"), true, null, false, "tri", true),
                Arguments.of(Collections.singletonList("dubbo"), false, null, false, "dubbo", false),
                Arguments.of(Arrays.asList("dubbo", "tri"), false, "dubbo", false, "dubbo", false),
                Arguments.of(Arrays.asList("dubbo", "tri"), false, "dubbo", true, "dubbo", false));
    }

    @ParameterizedTest
    @MethodSource("metadataProtocols")
    void advertisedMetadataSupportsConsumers(
            List<String> protocols,
            boolean onlyV2,
            String metadataProtocol,
            boolean protocolParameter,
            String expectedProtocol,
            boolean supportsV2) {
        FrameworkModel frameworkModel = new FrameworkModel();
        try {
            DubboBootstrap provider = DubboBootstrap.newInstance(frameworkModel);
            ApplicationConfig config = new ApplicationConfig("mixed-protocol-metadata-provider");
            config.setQosEnable(false);
            config.setOnlyUseMetadataV2(onlyV2);
            if (protocolParameter) {
                config.setParameters(Collections.singletonMap("metadata-service-protocol", metadataProtocol));
            } else {
                config.setMetadataServiceProtocol(metadataProtocol);
            }
            config.setMetadataType("local");

            ServiceConfig<DemoService> service = new ServiceConfig<>();
            service.setInterface(DemoService.class);
            service.setRef(new DemoServiceImpl());
            List<ProtocolConfig> protocolConfigs = new ArrayList<>();
            for (String protocol : protocols) {
                ProtocolConfig protocolConfig = new ProtocolConfig(protocol, -1);
                protocolConfig.setId(protocol);
                protocolConfigs.add(protocolConfig);
            }
            provider.application(config)
                    .registry(new RegistryConfig("N/A"))
                    .protocols(protocolConfigs)
                    .service(service)
                    .start();

            ApplicationModel providerModel = provider.getApplicationModel();
            MetadataInfo expected = new MetadataInfo(config.getName(), "metadata-revision", Collections.emptyMap());
            ServiceDiscovery discovery = mock(ServiceDiscovery.class);
            when(discovery.getLocalMetadata(expected.getRevision())).thenReturn(expected);
            ServiceDiscoveryRegistry registry = mock(ServiceDiscoveryRegistry.class);
            when(registry.getServiceDiscovery()).thenReturn(discovery);
            RegistryManager.getInstance(providerModel).putRegistry("metadata-test", registry);

            DubboBootstrap consumer = DubboBootstrap.newInstance(frameworkModel);
            ApplicationConfig consumerConfig = new ApplicationConfig("metadata-consumer");
            consumerConfig.setQosEnable(false);
            consumer.application(consumerConfig).initialize();
            ApplicationModel consumerModel = consumer.getApplicationModel();
            DefaultServiceInstance instance =
                    new DefaultServiceInstance(config.getName(), "127.0.0.1", 20880, consumerModel);
            new MetadataServiceURLParamsMetadataCustomizer().customize(instance, providerModel);

            // Legacy consumers use the advertised URL without negotiating MetadataServiceV2.
            URL legacyUrl = new StandardMetadataServiceURLBuilder(consumerModel)
                    .build(instance)
                    .get(0);
            if (!onlyV2) {
                legacyUrl = legacyUrl.setServiceModel(
                        consumerModel.getInternalModule().registerInternalConsumer(MetadataService.class, legacyUrl));
                Invoker<MetadataService> invoker = consumerModel
                        .getExtensionLoader(Protocol.class)
                        .getExtension(legacyUrl.getProtocol())
                        .refer(MetadataService.class, legacyUrl);
                try {
                    MetadataService legacyClient = consumerModel
                            .getExtensionLoader(ProxyFactory.class)
                            .getAdaptiveExtension()
                            .getProxy(invoker);
                    assertEquals(
                            expected.getRevision(),
                            legacyClient.getMetadataInfo(expected.getRevision()).getRevision());
                } finally {
                    invoker.destroy();
                }
            }

            MetadataUtils.RemoteMetadataService currentClient = MetadataUtils.referMetadataService(instance);
            try {
                if (supportsV2) {
                    assertInstanceOf(MetadataServiceV2.class, currentClient.getInternalProxy());
                } else {
                    assertInstanceOf(MetadataService.class, currentClient.getInternalProxy());
                }
                assertEquals(
                        expected.getRevision(),
                        currentClient.getRemoteMetadata(expected.getRevision()).getRevision());
            } finally {
                MetadataUtils.destroyProxy(currentClient);
            }

            assertEquals(expectedProtocol, legacyUrl.getProtocol());
            assertEquals(
                    onlyV2 ? MetadataServiceDelegationV2.VERSION : MetadataServiceDelegation.VERSION,
                    legacyUrl.getVersion());
            assertEquals(
                    supportsV2 ? MetadataServiceDelegationV2.VERSION : MetadataServiceDelegation.VERSION,
                    instance.getMetadata().get(METADATA_SERVICE_VERSION_NAME));
        } finally {
            frameworkModel.destroy();
        }
    }
}
