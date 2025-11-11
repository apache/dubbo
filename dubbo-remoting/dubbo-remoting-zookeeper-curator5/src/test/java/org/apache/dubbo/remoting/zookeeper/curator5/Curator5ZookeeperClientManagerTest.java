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
package org.apache.dubbo.remoting.zookeeper.curator5;

import org.apache.dubbo.common.URL;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.listen.StandardListenerManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.apache.dubbo.common.constants.CommonConstants.CHECK_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class Curator5ZookeeperClientManagerTest {
    private ZookeeperClient zookeeperClient;
    private static URL zookeeperUrl;
    private static MockedStatic<CuratorFrameworkFactory> curatorFrameworkFactoryMockedStatic;
    private static CuratorFramework mockCuratorFramework;

    @BeforeAll
    public static void beforeAll() {
        String zookeeperConnectionAddress1 = "zookeeper://127.0.0.1:2181";
        zookeeperUrl = URL.valueOf(zookeeperConnectionAddress1 + "/service");

        CuratorFrameworkFactory.Builder realBuilder = CuratorFrameworkFactory.builder();
        CuratorFrameworkFactory.Builder spyBuilder = spy(realBuilder);

        curatorFrameworkFactoryMockedStatic = mockStatic(CuratorFrameworkFactory.class);
        curatorFrameworkFactoryMockedStatic
                .when(CuratorFrameworkFactory::builder)
                .thenReturn(spyBuilder);
        mockCuratorFramework = mock(CuratorFramework.class);
        doReturn(mockCuratorFramework).when(spyBuilder).build();
    }

    @BeforeEach
    public void setUp() throws InterruptedException {
        when(mockCuratorFramework.blockUntilConnected(anyInt(), any())).thenReturn(true);
        when(mockCuratorFramework.getConnectionStateListenable()).thenReturn(StandardListenerManager.standard());
        zookeeperClient = new ZookeeperClientManager().connect(zookeeperUrl);
    }

    @Test
    void testZookeeperClient() {
        assertThat(zookeeperClient, not(nullValue()));
        zookeeperClient.close();
    }

    @Test
    void testRegistryCheckConnectDefault() throws InterruptedException {
        when(mockCuratorFramework.blockUntilConnected(anyInt(), any())).thenReturn(false);

        ZookeeperClientManager zookeeperClientManager = new ZookeeperClientManager();
        Assertions.assertThrowsExactly(IllegalStateException.class, () -> {
            zookeeperClientManager.connect(zookeeperUrl);
        });
    }

    @Test
    void testRegistryNotCheckConnect() throws InterruptedException {
        when(mockCuratorFramework.blockUntilConnected(anyInt(), any())).thenReturn(false);

        URL url = zookeeperUrl.addParameter(CHECK_KEY, false);
        ZookeeperClientManager zookeeperClientManager = new ZookeeperClientManager();
        Assertions.assertDoesNotThrow(() -> {
            zookeeperClientManager.connect(url);
        });
    }

    @AfterAll
    public static void afterAll() {
        curatorFrameworkFactoryMockedStatic.close();
    }
}
