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
package org.apache.dubbo.mcp.transport;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.message.ServerSentEvent;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcServiceContext;

import java.io.ByteArrayInputStream;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStreamableServerSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DubboMcpStreamableTransportProviderTest {

    @Mock
    private StreamObserver<ServerSentEvent<byte[]>> responseObserver;

    @Mock
    private HttpRequest httpRequest;

    @Mock
    private HttpResponse httpResponse;

    @Mock
    private McpStreamableServerSession.Factory sessionFactory;

    @Mock
    private RpcServiceContext rpcServiceContext;

    @Mock
    private McpStreamableServerSession mockSession;

    private MockedStatic<RpcContext> rpcContextMockedStatic;

    private DubboMcpStreamableTransportProvider transportProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        rpcContextMockedStatic = mockStatic(RpcContext.class);
        rpcContextMockedStatic.when(RpcContext::getServiceContext).thenReturn(rpcServiceContext);
        when(rpcServiceContext.getRequest(HttpRequest.class)).thenReturn(httpRequest);
        when(rpcServiceContext.getResponse(HttpResponse.class)).thenReturn(httpResponse);
        transportProvider = new DubboMcpStreamableTransportProvider(objectMapper);
        transportProvider.setSessionFactory(sessionFactory);
    }

    @Test
    void handleRequestHandlesGetRequest() {
        when(httpRequest.method()).thenReturn(HttpMethods.GET.name());
        when(httpRequest.accept()).thenReturn("text/event-stream");
        when(httpRequest.header(DubboMcpStreamableTransportProvider.SESSION_ID_HEADER))
                .thenReturn("test-session-id");

        transportProvider.handleRequest(responseObserver);

        verify(httpRequest, times(1)).method();
        // For GET requests, we don't verify onNext because the implementation calls
        // session.sendNotification().subscribe() directly
    }

    @Test
    void handleRequestHandlesPostRequest() {
        when(httpRequest.method()).thenReturn(HttpMethods.POST.name());
        when(httpRequest.accept()).thenReturn("application/json");
        when(httpRequest.header(DubboMcpStreamableTransportProvider.SESSION_ID_HEADER))
                .thenReturn("test-session-id");
        when(httpRequest.inputStream())
                .thenReturn(new ByteArrayInputStream(
                        "{\"jsonrpc\":\"2.0\",\"method\":\"initialize\",\"id\":\"1\"}".getBytes()));

        transportProvider.handleRequest(responseObserver);

        verify(httpRequest, times(2)).method();
        verify(httpResponse).setStatus(anyInt());
    }

    @Test
    void handleRequestHandlesDeleteRequest() {
        when(httpRequest.method()).thenReturn(HttpMethods.DELETE.name());
        when(httpRequest.header(DubboMcpStreamableTransportProvider.SESSION_ID_HEADER))
                .thenReturn("test-session-id");

        transportProvider.handleRequest(responseObserver);

        verify(httpRequest, times(3)).method();
        verify(httpResponse).setStatus(HttpStatus.NOT_FOUND.getCode());
        verify(responseObserver).onCompleted();
    }

    @Test
    void handleRequestIgnoresUnsupportedMethods() {
        when(httpRequest.method()).thenReturn(HttpMethods.PUT.name());

        transportProvider.handleRequest(responseObserver);

        verify(httpRequest, times(5)).method();
        verify(httpResponse).setStatus(HttpStatus.METHOD_NOT_ALLOWED.getCode());
        verify(responseObserver).onError(any());
        verify(responseObserver).onCompleted();
    }

    @Test
    void handleGetReturnsBadRequestWhenSessionIdIsMissing() {
        when(httpRequest.method()).thenReturn(HttpMethods.GET.name());
        when(httpRequest.accept()).thenReturn("text/event-stream");
        when(httpRequest.header(DubboMcpStreamableTransportProvider.SESSION_ID_HEADER))
                .thenReturn(null);

        transportProvider.handleRequest(responseObserver);

        verify(httpRequest, times(1)).method();
        verify(httpResponse).setStatus(HttpStatus.BAD_REQUEST.getCode());
        verify(responseObserver).onError(any());
        verify(responseObserver).onCompleted();
    }

    @Test
    void handleGetReturnsNotFoundForUnknownSessionId() {
        when(httpRequest.method()).thenReturn(HttpMethods.GET.name());
        when(httpRequest.accept()).thenReturn("text/event-stream");
        when(httpRequest.header(DubboMcpStreamableTransportProvider.SESSION_ID_HEADER))
                .thenReturn("unknownSessionId");

        transportProvider.handleRequest(responseObserver);

        verify(httpRequest, times(1)).method();
        verify(httpResponse).setStatus(HttpStatus.NOT_FOUND.getCode());
        verify(responseObserver).onError(any());
        verify(responseObserver).onCompleted();
    }

    @Test
    void handleGetWithReplayRequestCallsSessionReplay() throws Exception {
        // Create a transport provider subclass for testing to access private methods and fields
        DubboMcpStreamableTransportProvider transportProviderUnderTest =
                new DubboMcpStreamableTransportProvider(objectMapper);
        transportProviderUnderTest.setSessionFactory(sessionFactory);

        // Use reflection to put mockSession into the sessions map
        java.lang.reflect.Field sessionsField = DubboMcpStreamableTransportProvider.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        Object sessionsMap = sessionsField.get(transportProviderUnderTest);

        // Use reflection to call the put method
        java.lang.reflect.Method putMethod = sessionsMap.getClass().getMethod("put", Object.class, Object.class);
        putMethod.invoke(sessionsMap, "test-session-id", mockSession);

        // Set up mock behavior
        when(httpRequest.method()).thenReturn(HttpMethods.GET.name());
        when(httpRequest.accept()).thenReturn("text/event-stream");
        when(httpRequest.header(DubboMcpStreamableTransportProvider.SESSION_ID_HEADER))
                .thenReturn("test-session-id");
        when(httpRequest.header("Last-Event-ID")).thenReturn("12345");

        // Mock the replay method to return a Flux that emits a test message
        McpSchema.JSONRPCNotification testNotification =
                new McpSchema.JSONRPCNotification("2.0", "test_method", Collections.singletonMap("key", "value"));

        when(mockSession.replay("12345")).thenReturn(reactor.core.publisher.Flux.just(testNotification));

        transportProviderUnderTest.handleRequest(responseObserver);

        // Verify that the replay method is called
        verify(mockSession).replay("12345");
        // Verify that the message is sent
        verify(responseObserver).onNext(any());
        verify(responseObserver).onCompleted();
    }

    @AfterEach
    public void tearDown() {
        if (rpcContextMockedStatic != null) {
            rpcContextMockedStatic.close();
        }
    }
}
