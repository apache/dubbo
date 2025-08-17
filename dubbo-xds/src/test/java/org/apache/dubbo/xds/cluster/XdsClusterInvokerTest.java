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
package org.apache.dubbo.xds.cluster;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.xds.resource.route.RetryPolicy;
import org.apache.dubbo.xds.resource.route.RouteAction;

import com.google.protobuf.Duration;
import io.grpc.Status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for XdsClusterInvoker
 */
class XdsClusterInvokerTest {

    @Mock
    private Directory<Object> directory;

    @Mock
    private Invoker<Object> invoker1;

    @Mock
    private Invoker<Object> invoker2;

    @Mock
    private LoadBalance loadBalance;

    @Mock
    private Result result;

    @Mock
    private RouteAction routeAction;

    @Mock
    private RetryPolicy retryPolicy;

    private XdsClusterInvoker<Object> xdsClusterInvoker;
    private URL url;
    private RpcInvocation invocation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        url = URL.valueOf("dubbo://127.0.0.1:20880/com.example.Service");

        // Set up directory mocks BEFORE creating XdsClusterInvoker
        when(directory.getUrl()).thenReturn(url);
        when(directory.getConsumerUrl()).thenReturn(url);
        when(directory.getInterface()).thenReturn(Object.class);

        // Now create XdsClusterInvoker after directory is properly mocked
        xdsClusterInvoker = new XdsClusterInvoker<>(directory);

        invocation = new RpcInvocation();
        invocation.setMethodName("testMethod");
        invocation.setParameterTypes(new Class[]{String.class});
        invocation.setArguments(new Object[]{"test"});

        // Set up invoker mocks
        when(invoker1.getUrl()).thenReturn(url);
        when(invoker2.getUrl()).thenReturn(url);
        when(invoker1.getInterface()).thenReturn(Object.class);
        when(invoker2.getInterface()).thenReturn(Object.class);
        when(invoker1.isAvailable()).thenReturn(true);
        when(invoker2.isAvailable()).thenReturn(true);

        // Set up directory.list() mock for isAvailable() method
        when(directory.list(any())).thenReturn(Arrays.asList(invoker1, invoker2));
    }

    @Test
    void testSuccessfulInvocationWithoutRetryPolicy() {
        // Arrange
        List<Invoker<Object>> invokers = Arrays.asList(invoker1);
        when(directory.list(any(Invocation.class))).thenReturn(invokers);
        when(invoker1.invoke(any(Invocation.class))).thenReturn(result);
        when(result.hasException()).thenReturn(false);

        // Act
        Result actualResult = xdsClusterInvoker.invoke(invocation);

        // Assert
        assertNotNull(actualResult);
        assertEquals(result, actualResult);
        verify(invoker1, times(1)).invoke(invocation);
    }

    @Test
    void testSuccessfulInvocationWithRetryPolicy() {
        // Arrange
        List<Invoker<Object>> invokers = Arrays.asList(invoker1);
        when(directory.list(any(Invocation.class))).thenReturn(invokers);
        when(invoker1.invoke(any(Invocation.class))).thenReturn(result);
        when(result.hasException()).thenReturn(false);

        // Set up retry policy in invocation
        when(routeAction.getRetryPolicy()).thenReturn(retryPolicy);
        when(retryPolicy.getMaxAttempts()).thenReturn(3);
        when(retryPolicy.getInitialBackoff()).thenReturn(Duration.newBuilder().setNanos(25_000_000).build());
        when(retryPolicy.getMaxBackoff()).thenReturn(Duration.newBuilder().setNanos(250_000_000).build());
        when(retryPolicy.getRetryableStatusCodes()).thenReturn(Collections.emptyList());
        invocation.put("xds.route.action", routeAction);

        // Act
        Result actualResult = xdsClusterInvoker.invoke(invocation);

        // Assert
        assertNotNull(actualResult);
        assertEquals(result, actualResult);
        verify(invoker1, times(1)).invoke(invocation);
    }

    @Test
    void testRetryOnFailure() {
        // Arrange
        List<Invoker<Object>> invokers = Arrays.asList(invoker1, invoker2);
        when(directory.list(any(Invocation.class))).thenReturn(invokers);

        // Set up retry policy with proper Duration objects
        when(routeAction.getRetryPolicy()).thenReturn(retryPolicy);
        when(retryPolicy.getMaxAttempts()).thenReturn(3);
        when(retryPolicy.getInitialBackoff()).thenReturn(Duration.newBuilder().setSeconds(0).setNanos(25_000_000).build());
        when(retryPolicy.getMaxBackoff()).thenReturn(Duration.newBuilder().setSeconds(0).setNanos(250_000_000).build());
        when(retryPolicy.getRetryableStatusCodes()).thenReturn(Collections.emptyList());
        invocation.put("xds.route.action", routeAction);

        // First invocation fails, second succeeds
        RpcException exception = new RpcException(RpcException.NETWORK_EXCEPTION, "Network error");
        when(invoker1.invoke(any(Invocation.class))).thenThrow(exception);
        when(invoker2.invoke(any(Invocation.class))).thenReturn(result);
        when(result.hasException()).thenReturn(false);

        // Act
        Result actualResult = xdsClusterInvoker.invoke(invocation);

        // Assert
        assertNotNull(actualResult);
        assertEquals(result, actualResult);
        verify(invoker1, times(1)).invoke(invocation);
        verify(invoker2, times(1)).invoke(invocation);
    }

    @Test
    void testRetryExhaustion() {
        // Arrange
        List<Invoker<Object>> invokers = Arrays.asList(invoker1, invoker2);
        when(directory.list(any(Invocation.class))).thenReturn(invokers);

        // Set up retry policy with proper Duration objects
        when(routeAction.getRetryPolicy()).thenReturn(retryPolicy);
        when(retryPolicy.getMaxAttempts()).thenReturn(2);
        when(retryPolicy.getInitialBackoff()).thenReturn(Duration.newBuilder().setSeconds(0).setNanos(1_000_000).build()); // 1ms for faster test
        when(retryPolicy.getMaxBackoff()).thenReturn(Duration.newBuilder().setSeconds(0).setNanos(10_000_000).build());
        when(retryPolicy.getRetryableStatusCodes()).thenReturn(Collections.emptyList());
        invocation.put("xds.route.action", routeAction);

        // All attempts fail
        RpcException exception = new RpcException(RpcException.NETWORK_EXCEPTION, "Network error");
        when(invoker1.invoke(any(Invocation.class))).thenThrow(exception);
        when(invoker2.invoke(any(Invocation.class))).thenThrow(exception);

        // Act & Assert
        assertThrows(RpcException.class, () -> xdsClusterInvoker.invoke(invocation));

        // Should attempt maxAttempts times
        verify(invoker1, times(1)).invoke(invocation);
        verify(invoker2, times(1)).invoke(invocation);
    }

    @Test
    void testNoAvailableInvokers() {
        // Arrange
        when(directory.list(any(Invocation.class))).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(RpcException.class, () -> xdsClusterInvoker.invoke(invocation));
    }

    @Test
    void testNonRetryableException() {
        // Arrange
        List<Invoker<Object>> invokers = Arrays.asList(invoker1);
        when(directory.list(any(Invocation.class))).thenReturn(invokers);

        // Set up retry policy with specific retryable status codes and proper Duration objects
        when(routeAction.getRetryPolicy()).thenReturn(retryPolicy);
        when(retryPolicy.getMaxAttempts()).thenReturn(3);
        when(retryPolicy.getInitialBackoff()).thenReturn(Duration.newBuilder().setSeconds(0).setNanos(25_000_000).build());
        when(retryPolicy.getMaxBackoff()).thenReturn(Duration.newBuilder().setSeconds(0).setNanos(250_000_000).build());
        when(retryPolicy.getRetryableStatusCodes()).thenReturn(Arrays.asList(Status.Code.UNAVAILABLE));
        invocation.put("xds.route.action", routeAction);

        // Throw non-retryable exception (not in retryable status codes)
        RpcException exception = new RpcException(RpcException.SERIALIZATION_EXCEPTION, "Serialization error");
        when(invoker1.invoke(any(Invocation.class))).thenThrow(exception);

        // Act & Assert
        assertThrows(RpcException.class, () -> xdsClusterInvoker.invoke(invocation));

        // Should only attempt once for non-retryable exceptions
        verify(invoker1, times(1)).invoke(invocation);
    }

    @Test
    void testIsNotAvailable() {
        // Arrange - return empty list to make isAvailable() return false
        when(directory.list(any())).thenReturn(Collections.emptyList());

        // Act
        boolean available = xdsClusterInvoker.isAvailable();

        // Assert
        assertFalse(available);
        verify(directory, times(1)).list(any());
    }

    @Test
    void testGetInterface() {
        // Act
        Class<Object> interfaceClass = xdsClusterInvoker.getInterface();

        // Assert
        assertEquals(Object.class, interfaceClass);
    }

    @Test
    void testGetUrl() {
        // Act
        URL actualUrl = xdsClusterInvoker.getUrl();

        // Assert
        assertEquals(url, actualUrl);
    }

    @Test
    void testIsAvailable() {
        // Arrange - directory.list() already returns non-empty list from setUp()
        // Act
        boolean available = xdsClusterInvoker.isAvailable();

        // Assert
        assertTrue(available);
        verify(directory, atLeastOnce()).list(any());
    }

    @Test
    void testDestroy() {
        // Act
        xdsClusterInvoker.destroy();

        // Assert
        verify(directory, times(1)).destroy();
    }
}
