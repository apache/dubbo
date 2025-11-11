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
import org.apache.dubbo.remoting.http12.exception.HttpResultPayloadException;
import org.apache.dubbo.remoting.http12.message.ServerSentEvent;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcServiceContext;

import java.io.ByteArrayInputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DubboMcpSseTransportProviderTest {

    @Mock
    private StreamObserver<ServerSentEvent<String>> responseObserver;

    @Mock
    private HttpRequest httpRequest;

    @Mock
    private HttpResponse httpResponse;

    @Mock
    private McpServerSession.Factory sessionFactory;

    @Mock
    private RpcServiceContext rpcServiceContext;

    @Mock
    private McpServerSession mockSession;

    private MockedStatic<RpcContext> rpcContextMockedStatic;

    @InjectMocks
    private DubboMcpSseTransportProvider transportProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        rpcContextMockedStatic = mockStatic(RpcContext.class);
        rpcContextMockedStatic.when(RpcContext::getServiceContext).thenReturn(rpcServiceContext);
        when(rpcServiceContext.getRequest(HttpRequest.class)).thenReturn(httpRequest);
        when(rpcServiceContext.getResponse(HttpResponse.class)).thenReturn(httpResponse);
        transportProvider = new DubboMcpSseTransportProvider(objectMapper);
        transportProvider.setSessionFactory(sessionFactory);
    }

    @Test
    void handleRequestHandlesGetRequest() {
        when(httpRequest.method()).thenReturn(HttpMethods.GET.name());
        when(sessionFactory.create(any())).thenReturn(mockSession);
        when(mockSession.getId()).thenReturn("1");

        transportProvider.handleRequest(responseObserver);

        verify(httpRequest, times(1)).method();
        ArgumentCaptor<ServerSentEvent> captor = ArgumentCaptor.forClass(ServerSentEvent.class);
        verify(responseObserver, times(1)).onNext(captor.capture());
        ServerSentEvent evt = captor.getValue();
        Assertions.assertEquals("endpoint", evt.getEvent());
        Assertions.assertTrue(((String) evt.getData()).startsWith("/mcp/message?sessionId="));
    }

    @Test
    void handleRequestHandlesPostRequest() {
        when(httpRequest.method()).thenReturn(HttpMethods.GET.name());
        when(sessionFactory.create(any())).thenReturn(mockSession);
        when(mockSession.getId()).thenReturn("1");
        when(httpRequest.parameter("sessionId")).thenReturn("1");
        when(httpRequest.inputStream())
                .thenReturn(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"method\":\"test\"}".getBytes()));
        when(mockSession.handle(any(McpSchema.JSONRPCMessage.class))).thenReturn(mock());
        transportProvider.handleRequest(responseObserver);
        when(httpRequest.method()).thenReturn(HttpMethods.POST.name());
        transportProvider.handleRequest(responseObserver);
        verify(httpRequest, times(3)).method();
        verify(httpResponse).setStatus(HttpStatus.OK.getCode());
    }

    @Test
    void handleRequestIgnoresUnsupportedMethods() {
        when(httpRequest.method()).thenReturn(HttpMethods.PUT.name());

        transportProvider.handleRequest(responseObserver);

        verify(httpRequest, times(2)).method();
        verifyNoInteractions(responseObserver);
        verifyNoInteractions(httpResponse);
    }

    @Test
    void handleMessageReturnsBadRequestWhenSessionIdIsMissing() {
        when(httpRequest.parameter("sessionId")).thenReturn(null);

        try {
            transportProvider.handleMessage();
        } catch (Exception e) {
            Assertions.assertInstanceOf(HttpResultPayloadException.class, e);
            HttpResultPayloadException httpResultPayloadException = (HttpResultPayloadException) e;
            Assertions.assertEquals(
                    HttpStatus.BAD_REQUEST.getCode(),
                    httpResultPayloadException.getResult().getStatus());
        }
    }

    @Test
    void handleMessageReturnsNotFoundForUnknownSessionId() {
        when(httpRequest.parameter("sessionId")).thenReturn("unknownSessionId");

        try {
            transportProvider.handleMessage();
        } catch (Exception e) {
            Assertions.assertInstanceOf(HttpResultPayloadException.class, e);
            HttpResultPayloadException httpResultPayloadException = (HttpResultPayloadException) e;
            Assertions.assertEquals(
                    HttpStatus.NOT_FOUND.getCode(),
                    httpResultPayloadException.getResult().getStatus());
        }
    }

    @AfterEach
    public void tearDown() {
        if (rpcContextMockedStatic != null) {
            rpcContextMockedStatic.close();
        }
    }
}
