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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.IOUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.mcp.McpConstant;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.HttpResult;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.HttpUtils;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.remoting.http12.message.ServerSentEvent;
import org.apache.dubbo.rpc.RpcContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStreamableServerSession;
import io.modelcontextprotocol.spec.McpStreamableServerSession.Factory;
import io.modelcontextprotocol.spec.McpStreamableServerTransport;
import io.modelcontextprotocol.spec.McpStreamableServerTransportProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION;

/**
 * Implementation of {@link McpStreamableServerTransportProvider} for the Dubbo MCP transport.
 * This class provides methods to manage streamable server sessions and notify clients.
 */
public class DubboMcpStreamableTransportProvider implements McpStreamableServerTransportProvider {

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(DubboMcpStreamableTransportProvider.class);

    private Factory sessionFactory;

    private final ObjectMapper objectMapper;

    public static final String SESSION_ID_HEADER = "mcp-session-id";

    private static final String LAST_EVENT_ID_HEADER = "Last-Event-ID";

    /**
     * TODO: This design is suboptimal. A mechanism should be implemented to remove the session object upon connection closure or timeout.
     */
    private final ExpiringMap<String, McpStreamableServerSession> sessions;

    public DubboMcpStreamableTransportProvider(ObjectMapper objectMapper) {
        this(objectMapper, McpConstant.DEFAULT_SESSION_TIMEOUT);
    }

    public DubboMcpStreamableTransportProvider(ObjectMapper objectMapper, Integer expireSeconds) {
        // Minimum expiration time is 60 seconds
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

    @Override
    public void setSessionFactory(Factory sessionFactory) {
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
                .flatMap(McpStreamableServerSession::closeGracefully)
                .then();
    }

    public void handleRequest(StreamObserver<ServerSentEvent<byte[]>> responseObserver) {
        HttpRequest request = RpcContext.getServiceContext().getRequest(HttpRequest.class);
        HttpResponse response = RpcContext.getServiceContext().getResponse(HttpResponse.class);

        if (HttpMethods.isGet(request.method())) {
            handleGet(responseObserver);
        } else if (HttpMethods.isPost(request.method())) {
            handlePost(responseObserver);
        } else if (HttpMethods.DELETE.name().equals(request.method())) {
            handleDelete(responseObserver);
        } else {
            // unSupport method
            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.getCode());
            response.setBody(new McpError("Method not allowed: " + request.method()).getJsonRpcError());
            if (responseObserver != null) {
                responseObserver.onError(HttpResult.builder()
                        .status(HttpStatus.METHOD_NOT_ALLOWED.getCode())
                        .body(new McpError("Method not allowed: " + request.method()).getJsonRpcError())
                        .build()
                        .toPayload());
                responseObserver.onCompleted();
            }
        }
    }

    private void handleGet(StreamObserver<ServerSentEvent<byte[]>> responseObserver) {
        HttpRequest request = RpcContext.getServiceContext().getRequest(HttpRequest.class);
        HttpResponse response = RpcContext.getServiceContext().getResponse(HttpResponse.class);

        List<String> badRequestErrors = new ArrayList<>();

        // check Accept header
        List<String> accepts = HttpUtils.parseAccept(request.accept());
        if (CollectionUtils.isEmpty(accepts)
                || (!accepts.contains(MediaType.TEXT_EVENT_STREAM.getName())
                        && !accepts.contains(MediaType.APPLICATION_JSON.getName()))) {
            badRequestErrors.add("text/event-stream or application/json required in Accept header");
        }

        // check sessionId
        String sessionId = request.header(SESSION_ID_HEADER);
        if (StringUtils.isBlank(sessionId)) {
            badRequestErrors.add("Session ID required in mcp-session-id header");
        }

        if (!badRequestErrors.isEmpty()) {
            String combinedMessage = String.join("; ", badRequestErrors);
            response.setStatus(HttpStatus.BAD_REQUEST.getCode());
            response.setBody(new McpError(combinedMessage).getJsonRpcError());
            if (responseObserver != null) {
                responseObserver.onError(HttpResult.builder()
                        .status(HttpStatus.BAD_REQUEST.getCode())
                        .body(new McpError(combinedMessage).getJsonRpcError())
                        .build()
                        .toPayload());
                responseObserver.onCompleted();
            }
            return;
        }

        // Find existing session
        McpStreamableServerSession session = sessions.get(sessionId);
        if (session == null) {
            response.setStatus(HttpStatus.NOT_FOUND.getCode());
            response.setBody(new McpError("Session not found").getJsonRpcError());
            if (responseObserver != null) {
                responseObserver.onError(HttpResult.builder()
                        .status(HttpStatus.NOT_FOUND.getCode())
                        .body(new McpError("Session not found").getJsonRpcError())
                        .build()
                        .toPayload());
                responseObserver.onCompleted();
            }
            return;
        }

        // Check if this is a replay request
        String lastEventId = request.header(LAST_EVENT_ID_HEADER);
        if (StringUtils.isNotBlank(lastEventId)) {
            // Handle replay request by calling session.replay()
            try {
                session.replay(lastEventId)
                        .subscribe(
                                message -> {
                                    if (responseObserver != null) {
                                        try {
                                            String jsonData = objectMapper.writeValueAsString(message);
                                            responseObserver.onNext(ServerSentEvent.<byte[]>builder()
                                                    .event("message")
                                                    .data(jsonData.getBytes(StandardCharsets.UTF_8))
                                                    .build());
                                        } catch (Exception e) {
                                            logger.error(
                                                    COMMON_UNEXPECTED_EXCEPTION,
                                                    "",
                                                    "",
                                                    String.format(
                                                            "Failed to serialize replay message for session %s: %s",
                                                            sessionId, e.getMessage()),
                                                    e);
                                        }
                                    }
                                },
                                error -> {
                                    logger.error(
                                            COMMON_UNEXPECTED_EXCEPTION,
                                            "",
                                            "",
                                            String.format(
                                                    "Failed to replay messages for session %s with lastEventId %s: %s",
                                                    sessionId, lastEventId, error.getMessage()),
                                            error);
                                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
                                    response.setBody(new McpError("Failed to replay messages: " + error.getMessage())
                                            .getJsonRpcError());
                                    if (responseObserver != null) {
                                        responseObserver.onError(HttpResult.builder()
                                                .status(HttpStatus.INTERNAL_SERVER_ERROR.getCode())
                                                .body(new McpError("Failed to replay messages: " + error.getMessage())
                                                        .getJsonRpcError())
                                                .build()
                                                .toPayload());
                                        responseObserver.onCompleted();
                                    }
                                },
                                () -> {
                                    if (responseObserver != null) {
                                        responseObserver.onCompleted();
                                    }
                                });
            } catch (Exception e) {
                logger.error(
                        COMMON_UNEXPECTED_EXCEPTION,
                        "",
                        "",
                        String.format(
                                "Failed to handle replay for session %s with lastEventId %s: %s",
                                sessionId, lastEventId, e.getMessage()),
                        e);
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
                response.setBody(new McpError("Failed to handle replay: " + e.getMessage()).getJsonRpcError());
                if (responseObserver != null) {
                    responseObserver.onError(HttpResult.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.getCode())
                            .body(new McpError("Failed to handle replay: " + e.getMessage()).getJsonRpcError())
                            .build()
                            .toPayload());
                    responseObserver.onCompleted();
                }
            }
        } else {
            // Send initial notification for new connection
            session.sendNotification("tools").subscribe();
        }
    }

    private void handlePost(StreamObserver<ServerSentEvent<byte[]>> responseObserver) {
        HttpRequest request = RpcContext.getServiceContext().getRequest(HttpRequest.class);
        HttpResponse response = RpcContext.getServiceContext().getResponse(HttpResponse.class);

        List<String> badRequestErrors = new ArrayList<>();
        McpStreamableServerSession session = null;

        try {
            // Check Accept header
            List<String> accepts = HttpUtils.parseAccept(request.accept());
            if (CollectionUtils.isEmpty(accepts)
                    || (!accepts.contains(MediaType.TEXT_EVENT_STREAM.getName())
                            && !accepts.contains(MediaType.APPLICATION_JSON.getName()))) {
                badRequestErrors.add("text/event-stream or application/json required in Accept header");
            }

            // Read and deserialize JSON-RPC message from request body
            String requestBody = IOUtils.read(request.inputStream(), StandardCharsets.UTF_8.name());
            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, requestBody);

            // Check if it's an initialization request
            if (message instanceof McpSchema.JSONRPCRequest
                    && McpSchema.METHOD_INITIALIZE.equals(((McpSchema.JSONRPCRequest) message).method())) {
                // New initialization request
                if (!badRequestErrors.isEmpty()) {
                    String combinedMessage = String.join("; ", badRequestErrors);
                    response.setStatus(HttpStatus.BAD_REQUEST.getCode());
                    response.setBody(new McpError(combinedMessage).getJsonRpcError());
                    if (responseObserver != null) {
                        responseObserver.onError(HttpResult.builder()
                                .status(HttpStatus.BAD_REQUEST.getCode())
                                .body(new McpError(combinedMessage).getJsonRpcError())
                                .build()
                                .toPayload());
                        responseObserver.onCompleted();
                    }
                    return;
                }

                // Create new session
                McpSchema.InitializeRequest initializeRequest = objectMapper.convertValue(
                        ((McpSchema.JSONRPCRequest) message).params(), new TypeReference<>() {});

                McpStreamableServerSession.McpStreamableServerSessionInit init =
                        sessionFactory.startSession(initializeRequest);
                session = init.session();
                sessions.put(session.getId(), session);

                try {
                    McpSchema.InitializeResult initResult = init.initResult().block();

                    response.setHeader("Content-Type", MediaType.APPLICATION_JSON.getName());
                    response.setHeader(SESSION_ID_HEADER, session.getId());
                    response.setStatus(HttpStatus.OK.getCode());

                    String jsonResponse = objectMapper.writeValueAsString(new McpSchema.JSONRPCResponse(
                            McpSchema.JSONRPC_VERSION, ((McpSchema.JSONRPCRequest) message).id(), initResult, null));

                    if (responseObserver != null) {
                        responseObserver.onNext(ServerSentEvent.<byte[]>builder()
                                .event("message")
                                .data(jsonResponse.getBytes(StandardCharsets.UTF_8))
                                .build());
                        responseObserver.onCompleted();
                    }
                    return;
                } catch (Exception e) {
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
                    response.setBody(new McpError("Failed to initialize session: " + e.getMessage()).getJsonRpcError());
                    if (responseObserver != null) {
                        responseObserver.onError(HttpResult.builder()
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.getCode())
                                .body(new McpError("Failed to initialize session: " + e.getMessage()).getJsonRpcError())
                                .build()
                                .toPayload());
                        responseObserver.onCompleted();
                    }
                    return;
                }
            }

            // Non-initialization request requires sessionId
            String sessionId = request.header(SESSION_ID_HEADER);
            if (StringUtils.isBlank(sessionId)) {
                badRequestErrors.add("Session ID required in mcp-session-id header");
            }

            if (!badRequestErrors.isEmpty()) {
                String combinedMessage = String.join("; ", badRequestErrors);
                response.setStatus(HttpStatus.BAD_REQUEST.getCode());
                response.setBody(new McpError(combinedMessage).getJsonRpcError());
                if (responseObserver != null) {
                    responseObserver.onError(HttpResult.builder()
                            .status(HttpStatus.BAD_REQUEST.getCode())
                            .body(new McpError(combinedMessage).getJsonRpcError())
                            .build()
                            .toPayload());
                    responseObserver.onCompleted();
                }
                return;
            }

            // Find existing session
            session = sessions.get(sessionId);
            if (session == null) {
                response.setStatus(HttpStatus.NOT_FOUND.getCode());
                response.setBody(new McpError("Unknown sessionId: " + sessionId).getJsonRpcError());
                if (responseObserver != null) {
                    responseObserver.onError(HttpResult.builder()
                            .status(HttpStatus.NOT_FOUND.getCode())
                            .body(new McpError("Unknown sessionId: " + sessionId).getJsonRpcError())
                            .build()
                            .toPayload());
                    responseObserver.onCompleted();
                }
                return;
            }

            // Refresh session expiration time
            refreshSessionExpire(session);

            if (message instanceof McpSchema.JSONRPCResponse) {
                session.accept((McpSchema.JSONRPCResponse) message).block();
                response.setStatus(HttpStatus.ACCEPTED.getCode());
                if (responseObserver != null) {
                    responseObserver.onNext(ServerSentEvent.<byte[]>builder()
                            .event("response")
                            .data("{\"status\":\"accepted\"}".getBytes(StandardCharsets.UTF_8))
                            .build());
                    responseObserver.onCompleted();
                }
            } else if (message instanceof McpSchema.JSONRPCNotification) {
                session.accept((McpSchema.JSONRPCNotification) message).block();
                response.setStatus(HttpStatus.ACCEPTED.getCode());
                if (responseObserver != null) {
                    responseObserver.onNext(ServerSentEvent.<byte[]>builder()
                            .event("response")
                            .data("{\"status\":\"accepted\"}".getBytes(StandardCharsets.UTF_8))
                            .build());
                    responseObserver.onCompleted();
                }
            } else if (message instanceof McpSchema.JSONRPCRequest) {
                // For streaming responses, we need to return SSE
                response.setHeader("Content-Type", MediaType.TEXT_EVENT_STREAM.getName());
                response.setHeader("Cache-Control", "no-cache");
                response.setHeader("Connection", "keep-alive");
                response.setHeader("Access-Control-Allow-Origin", "*");

                // Handle request stream
                DubboMcpSessionTransport sessionTransport =
                        new DubboMcpSessionTransport(responseObserver, objectMapper);
                session.responseStream((McpSchema.JSONRPCRequest) message, sessionTransport)
                        .block();
            } else {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
                response.setBody(new McpError("Unknown message type").getJsonRpcError());
                if (responseObserver != null) {
                    responseObserver.onError(HttpResult.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.getCode())
                            .body(new McpError("Unknown message type").getJsonRpcError())
                            .build()
                            .toPayload());
                    responseObserver.onCompleted();
                }
            }

        } catch (IOException e) {
            response.setStatus(HttpStatus.BAD_REQUEST.getCode());
            response.setBody(new McpError("Invalid message format: " + e.getMessage()).getJsonRpcError());
            if (responseObserver != null) {
                responseObserver.onError(HttpResult.builder()
                        .status(HttpStatus.BAD_REQUEST.getCode())
                        .body(new McpError("Invalid message format: " + e.getMessage()).getJsonRpcError())
                        .build()
                        .toPayload());
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
            response.setBody(new McpError("Internal server error: " + e.getMessage()).getJsonRpcError());
            if (responseObserver != null) {
                responseObserver.onError(HttpResult.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.getCode())
                        .body(new McpError("Internal server error: " + e.getMessage()).getJsonRpcError())
                        .build()
                        .toPayload());
                responseObserver.onCompleted();
            }
        }
    }

    private void handleDelete(StreamObserver<ServerSentEvent<byte[]>> responseObserver) {
        HttpRequest request = RpcContext.getServiceContext().getRequest(HttpRequest.class);
        HttpResponse response = RpcContext.getServiceContext().getResponse(HttpResponse.class);

        String sessionId = request.header(SESSION_ID_HEADER);
        if (StringUtils.isBlank(sessionId)) {
            response.setStatus(HttpStatus.BAD_REQUEST.getCode());
            response.setBody(new McpError("Session ID required in mcp-session-id header").getJsonRpcError());
            if (responseObserver != null) {
                responseObserver.onError(HttpResult.builder()
                        .status(HttpStatus.BAD_REQUEST.getCode())
                        .body(new McpError("Session ID required in mcp-session-id header").getJsonRpcError())
                        .build()
                        .toPayload());
                responseObserver.onCompleted();
            }
            return;
        }

        McpStreamableServerSession session = sessions.get(sessionId);
        if (session == null) {
            response.setStatus(HttpStatus.NOT_FOUND.getCode());
            if (responseObserver != null) {
                responseObserver.onCompleted();
            }
            return;
        }

        try {
            session.delete().block();
            sessions.remove(sessionId);
            response.setStatus(HttpStatus.OK.getCode());
            if (responseObserver != null) {
                responseObserver.onNext(ServerSentEvent.<byte[]>builder()
                        .event("response")
                        .data("{\"status\":\"deleted\"}".getBytes(StandardCharsets.UTF_8))
                        .build());
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
            response.setBody(new McpError(e.getMessage()).getJsonRpcError());
            if (responseObserver != null) {
                responseObserver.onError(HttpResult.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.getCode())
                        .body(new McpError(e.getMessage()).getJsonRpcError())
                        .build()
                        .toPayload());
                responseObserver.onCompleted();
            }
        }
    }

    private void refreshSessionExpire(McpStreamableServerSession session) {
        sessions.put(session.getId(), session);
    }

    private static class DubboMcpSessionTransport implements McpStreamableServerTransport {

        private final ObjectMapper JSON;

        private final StreamObserver<ServerSentEvent<byte[]>> responseObserver;

        public DubboMcpSessionTransport(
                StreamObserver<ServerSentEvent<byte[]>> responseObserver, ObjectMapper objectMapper) {
            this.responseObserver = responseObserver;
            this.JSON = objectMapper;
        }

        @Override
        public void close() {
            if (responseObserver != null) {
                responseObserver.onCompleted();
            }
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(this::close);
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            return Mono.fromRunnable(() -> {
                try {
                    if (responseObserver != null) {
                        String jsonText = JSON.writeValueAsString(message);
                        responseObserver.onNext(ServerSentEvent.<byte[]>builder()
                                .event("message")
                                .data(jsonText.getBytes(StandardCharsets.UTF_8))
                                .build());
                    }
                } catch (Exception e) {
                    responseObserver.onError(e);
                }
            });
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message, String messageId) {
            return Mono.fromRunnable(() -> {
                try {
                    if (responseObserver != null) {
                        String jsonText = JSON.writeValueAsString(message);
                        ServerSentEvent<byte[]> event = ServerSentEvent.<byte[]>builder()
                                .event("message")
                                .data(jsonText.getBytes(StandardCharsets.UTF_8))
                                .id(messageId)
                                .build();
                        responseObserver.onNext(event);
                    }
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
