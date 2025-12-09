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

import org.apache.dubbo.cache.support.expiring.ExpiringMap;
import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.utils.IOUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.mcp.McpConstant;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.HttpResult;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.message.ServerSentEvent;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION;

public class DubboMcpSseTransportProvider implements McpServerTransportProvider {

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(DubboMcpSseTransportProvider.class);

    /**
     * Event type for JSON-RPC messages sent through the SSE connection.
     */
    public static final String MESSAGE_EVENT_TYPE = "message";

    /**
     * Event type for sending the message endpoint URI to clients.
     */
    public static final String ENDPOINT_EVENT_TYPE = "endpoint";

    private McpServerSession.Factory sessionFactory;

    private final ObjectMapper objectMapper;

    /**
     * session cache, default expire time is 60 seconds
     */
    private final ExpiringMap<String, McpServerSession> sessions;

    public DubboMcpSseTransportProvider(ObjectMapper objectMapper, Integer expireSeconds) {
        if (expireSeconds != null) {
            if (expireSeconds < 60) {
                expireSeconds = 60;
            }
        } else {
            expireSeconds = 60;
        }
        sessions = new ExpiringMap<>(expireSeconds, 30);
        this.objectMapper = objectMapper;
        sessions.getExpireThread().startExpiryIfNotStarted();
    }

    public DubboMcpSseTransportProvider(ObjectMapper objectMapper) {
        this(objectMapper, 60);
    }

    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        if (sessions.isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(sessions.values())
                .flatMap(session -> session.sendNotification(method, params)
                        .doOnError(e -> logger.error(
                                COMMON_UNEXPECTED_EXCEPTION,
                                "",
                                "",
                                String.format(
                                        "Failed to send message to session %s: %s", session.getId(), e.getMessage()),
                                e))
                        .onErrorComplete())
                .then();
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Flux.fromIterable(sessions.values())
                .flatMap(McpServerSession::closeGracefully)
                .then();
    }

    public void handleRequest(StreamObserver<ServerSentEvent<String>> responseObserver) {
        // Handle the request and return the response
        HttpRequest request = RpcContext.getServiceContext().getRequest(HttpRequest.class);
        if (HttpMethods.isGet(request.method())) {
            handleSseConnection(responseObserver);
        } else if (HttpMethods.isPost(request.method())) {
            handleMessage();
        }
    }

    public void handleMessage() {
        HttpRequest request = RpcContext.getServiceContext().getRequest(HttpRequest.class);
        String sessionId = request.parameter("sessionId");
        HttpResponse response = RpcContext.getServiceContext().getResponse(HttpResponse.class);
        if (StringUtils.isBlank(sessionId)) {
            throw HttpResult.of(HttpStatus.BAD_REQUEST, new McpError("Session ID missing in message endpoint"))
                    .toPayload();
        }

        McpServerSession session = sessions.get(sessionId);
        if (session == null) {
            throw HttpResult.of(HttpStatus.NOT_FOUND, "Unknown sessionId: " + sessionId)
                    .toPayload();
        }
        refreshSessionExpire(session);
        try {
            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(
                    objectMapper, IOUtils.read(request.inputStream(), String.valueOf(StandardCharsets.UTF_8)));
            session.handle(message).block();
            response.setStatus(HttpStatus.OK.getCode());
        } catch (IOException e) {
            throw HttpResult.of(HttpStatus.INTERNAL_SERVER_ERROR, new McpError("Invalid message format"))
                    .toPayload();
        }
    }

    private void handleSseConnection(StreamObserver<ServerSentEvent<String>> responseObserver) {
        // Handle the SSE connection
        // This is where you would set up the SSE stream and send events to the client
        DubboMcpSessionTransport dubboMcpSessionTransport =
                new DubboMcpSessionTransport(responseObserver, objectMapper);
        McpServerSession mcpServerSession = sessionFactory.create(dubboMcpSessionTransport);
        sessions.put(mcpServerSession.getId(), mcpServerSession);
        Configuration conf = ConfigurationUtils.getGlobalConfiguration(ApplicationModel.defaultModel());
        String messagePath = conf.getString(McpConstant.SETTINGS_MCP_PATHS_MESSAGE, "/mcp/message");
        sendEvent(responseObserver, ENDPOINT_EVENT_TYPE, messagePath + "?sessionId=" + mcpServerSession.getId());
    }

    private void refreshSessionExpire(McpServerSession session) {
        sessions.put(session.getId(), session);
    }

    private void sendEvent(StreamObserver<ServerSentEvent<String>> responseObserver, String eventType, String data) {
        responseObserver.onNext(
                ServerSentEvent.<String>builder().event(eventType).data(data).build());
    }

    private static class DubboMcpSessionTransport implements McpServerTransport {

        private final ObjectMapper JSON;

        private final StreamObserver<ServerSentEvent<String>> responseObserver;

        public DubboMcpSessionTransport(
                StreamObserver<ServerSentEvent<String>> responseObserver, ObjectMapper objectMapper) {
            this.responseObserver = responseObserver;
            this.JSON = objectMapper;
        }

        @Override
        public void close() {
            responseObserver.onCompleted();
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(responseObserver::onCompleted);
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            return Mono.fromRunnable(() -> {
                try {
                    String jsonText = JSON.writeValueAsString(message);
                    responseObserver.onNext(ServerSentEvent.<String>builder()
                            .event(MESSAGE_EVENT_TYPE)
                            .data(jsonText)
                            .build());
                } catch (Exception e) {
                    responseObserver.onError(e);
                }
            });
        }

        @Override
        public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
            return JSON.convertValue(data, typeRef);
        }
    }
}
