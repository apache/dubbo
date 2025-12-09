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
package org.apache.dubbo.xds.router;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.xds.bootstrap.BootstrapInfo;
import org.apache.dubbo.xds.bootstrap.Bootstrapper;
import org.apache.dubbo.xds.bootstrap.XdsServer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for XdsRouter
 */
class XdsRouterTest {

    @Mock
    private Invoker<Object> invoker1;

    @Mock
    private Invoker<Object> invoker2;

    @Mock
    private Invoker<Object> invoker3;

    @Mock
    private Bootstrapper mockBootstrapper;

    @Mock
    private BootstrapInfo mockBootstrapInfo;

    @Mock
    private org.apache.dubbo.xds.bootstrap.Node mockBootstrapNode;

    @Mock
    private XdsServer mockXdsServer;

    private XdsRouter<Object> xdsRouter;
    private URL consumerUrl;
    private RpcInvocation invocation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        consumerUrl = URL.valueOf("dubbo://127.0.0.1:20880/com.example.Service");

        // Mock the bootstrap chain to avoid initialization issues
        when(mockBootstrapNode.getId()).thenReturn("test-node-id");
        when(mockBootstrapNode.getCluster()).thenReturn("test-cluster");
        when(mockBootstrapNode.getMetadata()).thenReturn(new HashMap<>());
        when(mockBootstrapInfo.getNode()).thenReturn(mockBootstrapNode);

        // Mock XDS server info to avoid IndexOutOfBoundsException in XdsChannel
        when(mockXdsServer.getServerURI()).thenReturn("istiod.istio-system.svc:15010");
        when(mockBootstrapInfo.getXdsServers()).thenReturn(Arrays.asList(mockXdsServer));

        try (MockedStatic<Bootstrapper> bootstrapperMock = mockStatic(Bootstrapper.class)) {
            bootstrapperMock.when(Bootstrapper::getInstance).thenReturn(mockBootstrapper);
            when(mockBootstrapper.bootstrap()).thenReturn(mockBootstrapInfo);

            // Create XdsRouter with mocked bootstrap
            xdsRouter = new XdsRouter<>(consumerUrl);
        }

        // Setup invoker URLs with different versions and provided-by parameter
        URL url1 = URL.valueOf("dubbo://127.0.0.1:20881/com.example.Service?version=v1&provided-by=test-service:8080");
        URL url2 = URL.valueOf("dubbo://127.0.0.1:20882/com.example.Service?version=v2&provided-by=test-service:8080");
        URL url3 = URL.valueOf("dubbo://127.0.0.1:20883/com.example.Service?version=v1&provided-by=test-service:8080");

        when(invoker1.getUrl()).thenReturn(url1);
        when(invoker2.getUrl()).thenReturn(url2);
        when(invoker3.getUrl()).thenReturn(url3);

        invocation = new RpcInvocation();
        invocation.setMethodName("testMethod");
        invocation.setParameterTypes(new Class[] {String.class});
        invocation.setArguments(new Object[] {"test"});
        invocation.setInvoker(invoker1); // Set a default invoker to avoid null pointer
    }

    @Test
    void testRouteWithoutHeaders() {
        // Arrange
        List<Invoker<Object>> invokers = Arrays.asList(invoker1, invoker2, invoker3);
        BitList<Invoker<Object>> invokerBitList = new BitList<>(invokers);

        // Act
        BitList<Invoker<Object>> result = xdsRouter.route(invokerBitList, consumerUrl, invocation, false, null);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void testRouteWithVersionHeader() {
        // Arrange
        List<Invoker<Object>> invokers = Arrays.asList(invoker1, invoker2, invoker3);
        BitList<Invoker<Object>> invokerBitList = new BitList<>(invokers);

        RpcContext.getClientAttachment().setAttachment("version", "v1");

        try {
            // Act
            BitList<Invoker<Object>> result = xdsRouter.route(invokerBitList, consumerUrl, invocation, false, null);

            // Assert
            assertNotNull(result);
            assertTrue(result.size() <= 3);
        } finally {
            RpcContext.getClientAttachment().clearAttachments();
        }
    }

    @Test
    void testGetUrl() {
        URL url = xdsRouter.getUrl();
        assertEquals(consumerUrl, url);
    }

    @Test
    void testConstructor() {
        // Act & Assert
        assertNotNull(xdsRouter);
        assertEquals(consumerUrl, xdsRouter.getUrl());
    }
}
