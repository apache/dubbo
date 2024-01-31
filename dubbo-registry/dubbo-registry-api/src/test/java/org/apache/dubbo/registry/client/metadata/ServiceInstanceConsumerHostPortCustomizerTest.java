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
package org.apache.dubbo.registry.client.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class ServiceInstanceConsumerHostPortCustomizerTest {
    private static ServiceInstanceConsumerHostPortCustomizer serviceInstanceConsumerHostPortCustomizer;

    private static ApplicationModel applicationModel;

    @BeforeAll
    public static void setUp() {
        applicationModel = spy(ApplicationModel.defaultModel());
        serviceInstanceConsumerHostPortCustomizer = new ServiceInstanceConsumerHostPortCustomizer();
    }

    @AfterAll
    public static void clearUp() {
        ApplicationModel.reset();
    }

    @Test
    void customizePreferredProtocol() {
        ApplicationConfig applicationConfig = new ApplicationConfig("demo");
        applicationConfig.setProtocol(DUBBO);
        // mock currentConfig
        doReturn(applicationConfig).when(applicationModel).getCurrentConfig();

        DefaultServiceInstance serviceInstance = new DefaultServiceInstance("with-preferredProtocol", applicationModel);
        // mock frameworkModel
        FrameworkModel frameworkModel = mock(FrameworkModel.class);
        when(applicationModel.getFrameworkModel()).thenReturn(frameworkModel);
        // mock extensionLoader
        ExtensionLoader<Protocol> extensionLoader = mock(ExtensionLoader.class);
        when(frameworkModel.getExtensionLoader(Protocol.class)).thenReturn(extensionLoader);
        // mock protocol
        Protocol protocol = mock(Protocol.class);
        when(extensionLoader.getExtension(applicationModel.getCurrentConfig().getProtocol(), false))
                .thenReturn(protocol);
        // mock serverList
        when(protocol.getServers()).thenReturn(getServerList());
        // metadataInfo without service
        MetadataInfo metadataInfo = new MetadataInfo();
        serviceInstance.setServiceMetadata(metadataInfo);
        serviceInstanceConsumerHostPortCustomizer.customize(serviceInstance, applicationModel);
        Assertions.assertEquals("127.1.1.1", serviceInstance.getHost());
        Assertions.assertEquals(20880, serviceInstance.getPort());
    }

    public List<ProtocolServer> getServerList() {
        ProtocolServer protocolServer = new ProtocolServer() {
            @Override
            public String getAddress() {
                return "127.1.1.1:20880";
            }

            @Override
            public void setAddress(String address) {}

            @Override
            public void close() {}

            @Override
            public URL getUrl() {
                return URL.valueOf("dubbo://127.1.1.1:20880/org.apache.dubbo.metadata.MetadataService");
            }

            @Override
            public Map<String, Object> getAttributes() {
                return null;
            }
        };
        List<ProtocolServer> protocolServerList = new ArrayList<>();
        protocolServerList.add(protocolServer);
        return protocolServerList;
    }
}
