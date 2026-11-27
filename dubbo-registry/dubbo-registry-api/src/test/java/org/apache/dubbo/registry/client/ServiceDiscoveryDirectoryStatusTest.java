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
package org.apache.dubbo.registry.client;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceDiscoveryDirectoryStatusTest {
    @Test
    void nativeBinaryEmptyAddressesAreUnavailableWithEitherRegistryUrlForm() {
        assertEmptyStatus("xds", "dubbo", RpcException.NETWORK_EXCEPTION);
        assertEmptyStatus("registry", "dubbo", RpcException.NETWORK_EXCEPTION);
    }

    @Test
    void ordinaryBinaryDiscoveryRetainsItsExistingEmptyAddressStatus() {
        assertEmptyStatus("zookeeper", "dubbo", RpcException.FORBIDDEN_EXCEPTION);
    }

    @Test
    void nativeTripleStillUsesItsClusterLevelEmptyAddressTranslation() {
        assertEmptyStatus("xds", "tri", RpcException.FORBIDDEN_EXCEPTION);
    }

    private static void assertEmptyStatus(String registryProtocol, String protocol, int expected) {
        FrameworkModel framework = new FrameworkModel();
        ApplicationModel application = framework.newApplication();
        application.getApplicationConfigManager().setApplication(new ApplicationConfig("directory-status-test"));
        EmptyDirectory directory = null;
        try {
            URL reference = URL.valueOf(protocol + "://localhost/" + Runnable.class.getName())
                    .addParameter("interface", Runnable.class.getName())
                    .addParameter("protocol", protocol);
            URL registry = URL.valueOf(registryProtocol + "://localhost/" + Runnable.class.getName())
                    .addParameterAndEncoded("refer", reference.toParameterString())
                    .setScopeModel(application.getInternalModule());
            if ("registry".equals(registryProtocol)) registry = registry.addParameter("registry", "xds");
            directory = new EmptyDirectory(registry);
            URL consumer = reference.setScopeModel(application.getInternalModule());
            directory.buildRouterChain(consumer);
            Registry registryMock = mock(Registry.class);
            when(registryMock.getUrl()).thenReturn(registry);
            directory.setRegistry(registryMock);
            directory.subscribe(consumer);
            EmptyDirectory current = directory;
            RpcException error =
                    assertThrows(RpcException.class, () -> current.doList(null, BitList.emptyList(), null));
            assertEquals(expected, error.getCode());
        } finally {
            if (directory != null) directory.destroy();
            application.destroy();
            framework.destroy();
        }
    }

    private static final class EmptyDirectory extends ServiceDiscoveryRegistryDirectory<Runnable> {
        EmptyDirectory(URL url) {
            super(Runnable.class, url);
            forbidden = true;
        }
    }
}
