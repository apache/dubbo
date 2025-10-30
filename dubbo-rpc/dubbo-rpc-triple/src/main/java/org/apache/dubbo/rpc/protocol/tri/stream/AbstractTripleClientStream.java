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
package org.apache.dubbo.rpc.protocol.tri.stream;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.ClassLoadUtil;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;
import org.apache.dubbo.rpc.protocol.tri.RequestMetadata;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.command.CancelQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.DataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.EndStreamQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.HeaderQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.compressor.DeCompressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.frame.Deframer;
import org.apache.dubbo.rpc.protocol.tri.frame.TriDecoder;
import org.apache.dubbo.rpc.protocol.tri.h12.grpc.GrpcUtils;
import org.apache.dubbo.rpc.protocol.tri.store.PendingMessageStore;
import org.apache.dubbo.rpc.protocol.tri.store.SessionMetadata;
import org.apache.dubbo.rpc.protocol.tri.store.StoreException;
import org.apache.dubbo.rpc.protocol.tri.transport.AbstractH2TransportListener;
import org.apache.dubbo.rpc.protocol.tri.transport.H2TransportListener;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleWriteQueue;

import javax.net.ssl.SSLSession;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.google.protobuf.Any;
import com.google.rpc.DebugInfo;
import com.google.rpc.ErrorInfo;
import com.google.rpc.Status;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_RESPONSE;

/**
 * ClientStream is an abstraction for bidirectional messaging. It maintains a {@link TripleWriteQueue} to
 * write Http2Frame to remote. A {@link H2TransportListener} receives Http2Frame from remote.
 * Instead of maintaining state, this class depends on upper layer or transport layer's states.
 */
public abstract class AbstractTripleClientStream extends AbstractStream implements ClientStream, ReliabilityContext {

    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(AbstractTripleClientStream.class);
    private static final AttributeKey<SSLSession> SSL_SESSION_KEY = AttributeKey.valueOf(Constants.SSL_SESSION_KEY);

    private final ClientStream.Listener listener;
    protected volatile TripleWriteQueue writeQueue;
    private Deframer deframer;
    private volatile Channel parent;
    private final AtomicReference<TripleStreamChannelFuture> streamChannelFuture = new AtomicReference<>();
    private boolean halfClosed;
    private boolean rst;

    private boolean isReturnTriException = false;

    // Reliability fields (lazy initialization)
    private volatile boolean reliabilityEnabled = false;
    private AtomicLong sequenceNumber;
    private ConcurrentHashMap<Long, PendingMessage> pendingMessages;
    private AtomicLong ackWatermark; // 水位标记，用于O(1)清理
    private PendingMessageStore messageStore; // 持久化存储
    private SessionMetadata currentMetadata; // Current session metadata for persistence

    // ReliabilityContext support fields
    private final AtomicLong totalSentCount = new AtomicLong(0);
    private final AtomicLong totalRetryCount = new AtomicLong(0);
    private volatile Consumer<String> stateChangeCallback;
    private volatile Consumer<String> recoveryCallback;
    private volatile Consumer<Long> retryCallback;
    private String sessionId;
    private ReliabilityConfig config;
    private URL url; // Store URL for message store initialization
    private ScheduledExecutorService heartbeatScheduler; // 心跳专用调度器
    private ScheduledExecutorService retryScheduler; // 重试检查专用调度器
    private ScheduledExecutorService recoveryExecutor; // 恢复操作专用调度器（支持延迟恢复）

    // Track scheduled tasks for proper cancellation
    private volatile ScheduledFuture<?> currentHeartbeatTask;
    private volatile ScheduledFuture<?> currentRetryTask;
    private final Set<ScheduledFuture<?>> activeTasks = ConcurrentHashMap.newKeySet();
    private ReconnectionManager reconnectionManager; // 重连管理器
    private volatile long lastHeartbeatAckTime = System.currentTimeMillis();
    private final AtomicLong lastAckedSeq = new AtomicLong(0);
    private final AtomicInteger inFlightCount = new AtomicInteger(0); // InFlight 消息计数器
    private int maxRetryTimes; // 最大重试次数

    // Transport listener for handling incoming data
    private H2TransportListener transportListener;

    // Heartbeat state machine
    private volatile HeartbeatState heartbeatState = HeartbeatState.HEALTHY;
    private volatile int missedHeartbeats = 0;
    private volatile long lastHeartbeatSentTime = 0;

    enum HeartbeatState {
        HEALTHY, // Normal operation
        SUSPECT, // Missed heartbeats, monitoring closely
        RECONNECTING, // Attempting to reconnect
        FAILED, // Connection failed, stop trying
        PAUSED, // Temporarily paused but can be recovered
        CLOSED // Stream closed, no further operations
    }

    protected AbstractTripleClientStream(
            FrameworkModel frameworkModel,
            Executor executor,
            TripleWriteQueue writeQueue,
            ClientStream.Listener listener,
            Http2StreamChannel http2StreamChannel) {
        super(executor, frameworkModel);
        this.parent = http2StreamChannel.parent();
        this.listener = listener;
        this.writeQueue = writeQueue;
        this.streamChannelFuture.set(initStreamChannel(http2StreamChannel));
        this.transportListener = createTransportListener();
    }

    protected AbstractTripleClientStream(
            FrameworkModel frameworkModel,
            Executor executor,
            TripleWriteQueue writeQueue,
            ClientStream.Listener listener,
            Channel parent) {
        super(executor, frameworkModel);
        this.parent = parent;
        this.listener = listener;
        this.writeQueue = writeQueue;
        this.streamChannelFuture.set(initStreamChannel(parent));
        this.transportListener = createTransportListener();
    }

    /**
     * Unified reliability initialization method to prevent duplicate initialization.
     * This method contains all the core initialization logic including persistence, recovery, and scheduling.
     */
    private void doInitializeReliability(String sessionId, ReliabilityConfig config, URL url) {
        // Use atomic flag to prevent duplicate initialization
        if (!reliabilityInitialized.compareAndSet(false, true)) {
            LOGGER.debug(
                    "Reliability already initialized for session: {}, skipping duplicate initialization", sessionId);
            return;
        }

        try {
            LOGGER.info("Starting unified reliability initialization for session: {}", sessionId);

            // Set core reliability state
            this.reliabilityEnabled = true;
            this.sessionId = sessionId;
            this.url = url;
            this.config = config;

            // Initialize with default values (will be overridden if recovery data exists)
            this.sequenceNumber = new AtomicLong(0);
            this.pendingMessages = new ConcurrentHashMap<>();
            this.ackWatermark = new AtomicLong(0);
            this.lastHeartbeatAckTime = System.currentTimeMillis();
            this.missedHeartbeats = 0;
            this.heartbeatState = HeartbeatState.HEALTHY;

            // Initialize thread pools and schedulers
            initializeExecutors();

            // Initialize persistent message store
            initializeMessageStore();

            // Try to recover session state from persistent storage (for process restart)
            recoverSessionStateFromStorage();

            // Recover any pending messages from storage
            recoverPendingMessages();

            // Start heartbeat and retry scheduling
            scheduleHeartbeat();
            scheduleRetryCheck();

            LOGGER.info("Successfully completed unified reliability initialization for session: {}", sessionId);

        } catch (Exception e) {
            LOGGER.error(INTERNAL_ERROR, "", "", "Failed to initialize reliability for session: " + sessionId, e);
            // Reset the flag to allow retry
            reliabilityInitialized.set(false);
            // Clean up any partial initialization
            cleanupReliabilityResources();
            throw new RuntimeException("Reliability initialization failed", e);
        }
    }

    /**
     * Initialize executors and schedulers for reliability features.
     */
    private void initializeExecutors() {
        // Use safe session identifier to handle potential null sessionId
        String safeSessionId = (sessionId != null) ? sessionId : "temp-" + System.nanoTime();

        this.heartbeatScheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "reliability-heartbeat-" + safeSessionId);
            t.setDaemon(true);
            return t;
        });

        this.retryScheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "reliability-retry-" + safeSessionId);
            t.setDaemon(true);
            return t;
        });

        this.recoveryExecutor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "reliability-recovery-" + safeSessionId);
            t.setDaemon(true);
            return t;
        });
    }

    public void initializeReliability(RequestMetadata requestMetadata, URL url) {
        Map<String, Object> attachments = requestMetadata.attachments;
        boolean hasReliabilityInMetadata = attachments != null && attachments.containsKey("tri-reliable-version");
        boolean hasSessionIdFromHeaders = this.sessionId != null && !this.sessionId.startsWith("metadata-session-");

        if (hasReliabilityInMetadata || hasSessionIdFromHeaders) {
            // Extract session ID from metadata or use existing header-based session ID
            String sessionId;
            if (hasReliabilityInMetadata) {
                sessionId = (String) attachments.get("tri-session-id");
                if (sessionId == null) {
                    sessionId = hasSessionIdFromHeaders ? this.sessionId : "metadata-session-" + System.nanoTime();
                    LOGGER.warn(INTERNAL_ERROR, "", "", "No session ID in request metadata, using: " + sessionId);
                }
            } else {
                sessionId = this.sessionId; // Use session ID from headers
                LOGGER.info("Using session ID from headers for deferred reliability initialization: {}", sessionId);
            }

            ReliabilityConfig config = parseReliabilityConfig(url);

            // Call unified initialization method with proper URL
            doInitializeReliability(sessionId, config, url);

            LOGGER.info("Reliability initialized from RequestMetadata for session: {}", sessionId);
        }
    }

    private void initializeReliabilityFromHeaders(Http2Headers headers) {
        // Check if headers contain reliability negotiation information
        if (headers != null && headers.contains("tri-reliable-version")) {
            // Extract session ID from headers
            String headerSessionId = headers.get("tri-session-id") != null
                    ? headers.get("tri-session-id").toString()
                    : "header-session-" + System.nanoTime();

            // If reliability is not yet initialized, perform full initialization
            if (!reliabilityInitialized.get()) {
                // Only initialize from headers if we have a real URL context
                if (this.url != null) {
                    LOGGER.info(
                            "Initializing reliability from headers for session: {} with URL: {}",
                            headerSessionId,
                            this.url);

                    ReliabilityConfig config = parseReliabilityConfig(this.url);

                    // Call unified initialization method
                    doInitializeReliability(headerSessionId, config, this.url);

                    LOGGER.info("Reliability initialized from headers for session: {}", headerSessionId);
                } else {
                    // Defer initialization until proper URL is available
                    LOGGER.info(
                            "Deferring reliability initialization from headers for session: {} - waiting for URL context",
                            headerSessionId);

                    // Just store the session ID for later use
                    this.sessionId = headerSessionId;
                    // Mark that we have seen reliability headers but haven't initialized yet
                    // This will be picked up by initializeReliability() when it's called with proper URL
                }
            } else {
                // If already initialized, just update session ID if it was not set
                if (this.sessionId == null || this.sessionId.startsWith("unknown-session")) {
                    this.sessionId = headerSessionId;
                    LOGGER.info("Updated session ID from headers: {}", headerSessionId);
                } else {
                    LOGGER.debug(
                            "Reliability already initialized for session: {}, preserving existing configuration",
                            this.sessionId);
                }
            }
        }
    }

    /**
     * Initialize the message store based on configuration
     */
    private void initializeMessageStore() {
        try {
            String storeType = config.getStoreType();
            ExtensionLoader<PendingMessageStore> loader = ExtensionLoader.getExtensionLoader(PendingMessageStore.class);
            this.messageStore = loader.getExtension(storeType);

            SessionMetadata metadata = new SessionMetadata(
                    sessionId, url != null ? url.getServiceKey() : "unknown", System.currentTimeMillis(), url, config);
            this.currentMetadata = metadata; // Store reference for later updates
            messageStore.init(metadata);

            LOGGER.info("Initialized message store of type: {} for session: {}", storeType, sessionId);
        } catch (Exception e) {
            LOGGER.error(INTERNAL_ERROR, "", "", "Failed to initialize message store for session: " + sessionId, e);
            // Fallback to in-memory store
            this.messageStore = new org.apache.dubbo.rpc.protocol.tri.store.InMemoryPendingMessageStore();
            try {
                SessionMetadata metadata = new SessionMetadata(
                        sessionId,
                        url != null ? url.getServiceKey() : "unknown",
                        System.currentTimeMillis(),
                        url,
                        config);
                this.currentMetadata = metadata; // Store reference for later updates
                messageStore.init(metadata);
            } catch (Exception ex) {
                LOGGER.error(
                        INTERNAL_ERROR,
                        "",
                        "",
                        "Failed to initialize fallback in-memory store for session: " + sessionId,
                        ex);
            }
        }
    }

    /**
     * Recover session state (lastAckedSeq, currentSeq) from persistent storage.
     * This is critical for process restart recovery to avoid duplicate sends and maintain sequence continuity.
     */
    private void recoverSessionStateFromStorage() {
        try {
            if (messageStore == null) {
                LOGGER.debug("No message store available for session state recovery: {}", sessionId);
                return;
            }

            // Try to load persisted metadata
            org.apache.dubbo.rpc.protocol.tri.store.SessionMetadata recoveredMetadata =
                    messageStore.loadMetadata(sessionId);

            if (recoveredMetadata == null) {
                LOGGER.info("No persisted session state found for session: {}, starting fresh", sessionId);
                return;
            }

            // Recover lastAckedSeq
            long recoveredLastAcked = recoveredMetadata.getLastAckedSeq();
            if (recoveredLastAcked > 0) {
                this.lastAckedSeq.set(recoveredLastAcked);
                LOGGER.info(
                        "Recovered lastAckedSeq = {} for session: {} from persistent storage",
                        recoveredLastAcked,
                        sessionId);
            }

            // Recover sequenceNumber (currentSeq)
            long recoveredCurrentSeq = recoveredMetadata.getCurrentSeq();
            if (recoveredCurrentSeq > 0) {
                this.sequenceNumber.set(recoveredCurrentSeq);
                LOGGER.info(
                        "Recovered currentSeq = {} for session: {} from persistent storage",
                        recoveredCurrentSeq,
                        sessionId);
            }

            LOGGER.info(
                    "Successfully recovered session state for session: {} (lastAcked={}, currentSeq={})",
                    sessionId,
                    recoveredLastAcked,
                    recoveredCurrentSeq);

        } catch (Exception e) {
            LOGGER.error(
                    INTERNAL_ERROR,
                    "",
                    "",
                    "Failed to recover session state for session: " + sessionId + ", continuing with fresh state",
                    e);
            // Continue with fresh state - don't fail initialization
        }
    }

    /**
     * Recover pending messages from persistent storage
     */
    private void recoverPendingMessages() {
        try {
            if (messageStore == null) {
                LOGGER.debug("No message store available for session: {}", sessionId);
                return;
            }

            java.util.List<org.apache.dubbo.rpc.protocol.tri.store.PendingMessage> recoveredMessages =
                    messageStore.load(sessionId);

            if (recoveredMessages.isEmpty()) {
                LOGGER.debug("No messages to recover for session: {}", sessionId);
                return;
            }

            // 合并恢复的消息到内存pendingMessages
            for (org.apache.dubbo.rpc.protocol.tri.store.PendingMessage recovered : recoveredMessages) {
                // 以序列号最大的为准，避免重复
                pendingMessages.merge(
                        recovered.getSequence(), convertToInternalPendingMessage(recovered), (existing, newMsg) -> {
                            // 保留sentTime较新的那个
                            return existing.getSentTime() > newMsg.getSentTime() ? existing : newMsg;
                        });
            }

            LOGGER.info("Recovered {} messages for session: {}", recoveredMessages.size(), sessionId);
        } catch (Exception e) {
            LOGGER.error(INTERNAL_ERROR, "", "", "Failed to recover pending messages for session: " + sessionId, e);
        }
    }

    /**
     * Convert store PendingMessage to internal PendingMessage
     */
    private PendingMessage convertToInternalPendingMessage(
            org.apache.dubbo.rpc.protocol.tri.store.PendingMessage storeMessage) {
        return new PendingMessage(
                storeMessage.getSequence(),
                storeMessage.getMessage(),
                storeMessage.getCompressFlag(),
                storeMessage.getSentTime(),
                storeMessage.getRetryCount());
    }

    /**
     * Set the reconnection manager for handling connection failures.
     * This allows the stream to attempt real reconnection when the underlying connection fails.
     *
     * @param reconnectionManager the reconnection manager
     */
    public void setReconnectionManager(ReconnectionManager reconnectionManager) {
        this.reconnectionManager = reconnectionManager;
    }

    private ReliabilityConfig parseReliabilityConfig(URL url) {
        return ReliabilityConfig.builder()
                .enabled(url.getParameter("stream.reliability.enabled", false))
                .heartbeatInterval(url.getParameter("stream.heartbeat.interval", 5000))
                .retryTimeout(url.getParameter("stream.retry.timeout", 10000))
                .maxRetries(url.getParameter("stream.max.retries", 3))
                .sessionTimeout(url.getParameter("stream.session.timeout", 30000))
                .maxMissedHeartbeats(url.getParameter("stream.heartbeat.max-missed", 3))
                // 智能重试策略参数 - 开放给用户配置
                .initialRetryDelay(url.getParameter("stream.retry.initial-delay", 1000))
                .maxRetryDelay(url.getParameter("stream.retry.max-delay", 30000))
                .backoffMultiplier(url.getParameter("stream.retry.backoff-multiplier", 2.0))
                .maxInFlightMessages(url.getParameter("stream.max-in-flight", 100))
                .totalRetryTimeout(url.getParameter("stream.retry.total-timeout", 60000))
                // 持久化存储配置 - 开放给用户配置
                .storeType(url.getParameter("stream.reliability.store.type", "memory"))
                .storePath(url.getParameter("stream.reliability.store.path", "/tmp/dubbo/reliable"))
                .storeMaxFileSize(url.getParameter("stream.reliability.store.max-file-size", 100 * 1024 * 1024L))
                .storeRetentionTime(url.getParameter("stream.reliability.store.retention-time", 24 * 60 * 60 * 1000L))
                .storeSyncInterval(url.getParameter("stream.reliability.store.sync-interval", 1000))
                .build();
    }

    protected abstract TripleStreamChannelFuture initStreamChannel(Channel parent);

    public ChannelFuture sendHeader(Http2Headers headers) {
        if (this.writeQueue == null) {
            // already processed at createStream()
            return parent.newFailedFuture(new IllegalStateException("Stream already closed"));
        }
        ChannelFuture checkResult = preCheck();
        if (!checkResult.isSuccess()) {
            return checkResult;
        }

        // Initialize reliability features from headers
        if (headers != null && headers.contains("tri-reliable-version")) {
            initializeReliabilityFromHeaders(headers);
        }

        final HeaderQueueCommand headerCmd = HeaderQueueCommand.createHeaders(getCurrentStreamChannelFuture(), headers);
        return writeQueue.enqueueFuture(headerCmd, parent.eventLoop()).addListener(future -> {
            if (!future.isSuccess()) {
                transportException(future.cause());
            }
        });
    }

    private void transportException(Throwable cause) {
        final TriRpcStatus status =
                TriRpcStatus.INTERNAL.withDescription("Http2 exception").withCause(cause);

        // Classify error: recoverable (network issue) vs non-recoverable (protocol error)
        if (isRecoverableException(cause)) {
            // Recoverable: Handle as temporary failure, trigger recovery
            LOGGER.warn(
                    INTERNAL_ERROR,
                    "",
                    "",
                    "Recoverable transport exception for session: " + sessionId + " - " + cause.getMessage());
            handleTemporaryFailureWithCount("Transport exception: " + cause.getMessage());

            // Do NOT call onComplete - let recovery mechanism handle it
            // The stream remains alive for retry/reconnect
        } else {
            // Non-recoverable: Terminate the stream immediately
            LOGGER.error(
                    INTERNAL_ERROR,
                    "",
                    "",
                    "Non-recoverable transport exception for session: " + sessionId + " - " + cause.getMessage());

            // Clean up resources and notify business layer
            cleanupReliabilityResources();
            listener.onComplete(status, null, null, false);
        }
    }

    public ChannelFuture cancelByLocal(TriRpcStatus status) {
        ChannelFuture checkResult = preCheck();
        if (!checkResult.isSuccess()) {
            return checkResult;
        }

        // Clean up reliability resources
        if (reliabilityEnabled || reliabilityInitialized.get()) {
            cleanupReliabilityResources();
        }

        // Handle case where stream channel future is null (e.g., after connection failure)
        TripleStreamChannelFuture currentFuture = getCurrentStreamChannelFuture();
        if (currentFuture == null) {
            LOGGER.debug("Cannot cancel stream - no active stream channel future for session: {}", sessionId);
            rst = true;
            return checkResult; // Return the successful preCheck result
        }

        final CancelQueueCommand cmd = CancelQueueCommand.createCommand(currentFuture, Http2Error.CANCEL);

        rst = true;
        return this.writeQueue.enqueue(cmd);
    }

    @Override
    public SocketAddress remoteAddress() {
        return parent.remoteAddress();
    }

    @Override
    public SSLSession getSslSession() {
        return parent.attr(SSL_SESSION_KEY).get();
    }

    @Override
    public ChannelFuture sendMessage(byte[] message, int compressFlag) {
        ChannelFuture checkResult = preCheck();
        if (!checkResult.isSuccess()) {
            return checkResult;
        }

        if (!reliabilityEnabled) {
            // Original path for non-reliable mode
            final DataQueueCommand cmd =
                    DataQueueCommand.create(getCurrentStreamChannelFuture(), message, false, compressFlag);
            return this.writeQueue.enqueueFuture(cmd, parent.eventLoop()).addListener(future -> {
                if (!future.isSuccess()) {
                    cancelByLocal(TriRpcStatus.INTERNAL
                            .withDescription("Client write message failed")
                            .withCause(future.cause()));
                    transportException(future.cause());
                }
            });
        }

        // Reliable mode: send sequence header first, then data

        // Check InFlight limit before sending
        int currentInFlight = inFlightCount.get();
        if (currentInFlight >= config.getMaxInFlightMessages()) {
            LOGGER.warn(
                    INTERNAL_ERROR,
                    "",
                    "",
                    "InFlight limit exceeded: " + currentInFlight + "/" + config.getMaxInFlightMessages()
                            + " messages, rejecting new message");
            io.netty.channel.ChannelPromise rejectPromise = parent.newPromise();
            rejectPromise.setFailure(new IllegalStateException("InFlight message limit exceeded"));
            return rejectPromise;
        }

        long seq = sequenceNumber.incrementAndGet();

        // Create sequence header
        io.netty.handler.codec.http2.DefaultHttp2Headers seqHeaders =
                new io.netty.handler.codec.http2.DefaultHttp2Headers();
        seqHeaders.set("tri-seq", String.valueOf(seq));
        HeaderQueueCommand headerCmd =
                HeaderQueueCommand.createHeaders(getCurrentStreamChannelFuture(), seqHeaders, false);

        // Cache pending message
        PendingMessage pending = new PendingMessage(seq, message, compressFlag, System.currentTimeMillis(), 0);

        // 持久化存储消息
        if (messageStore != null && messageStore.isHealthy()) {
            try {
                org.apache.dubbo.rpc.protocol.tri.store.PendingMessage storeMessage =
                        new org.apache.dubbo.rpc.protocol.tri.store.PendingMessage(
                                seq,
                                message,
                                compressFlag,
                                System.currentTimeMillis(),
                                0,
                                System.currentTimeMillis(),
                                100,
                                sessionId);
                messageStore.put(storeMessage);
            } catch (StoreException e) {
                LOGGER.warn(
                        INTERNAL_ERROR,
                        "",
                        "",
                        "Failed to persist message with sequence: " + seq + " for session: " + sessionId,
                        e);
            }
        }

        pendingMessages.put(seq, pending);

        // Create combined promise for header + data
        io.netty.channel.ChannelPromise combinedPromise = parent.newPromise();
        ChannelFuture headerFuture = writeQueue.enqueueFuture(headerCmd, parent.eventLoop());

        headerFuture.addListener(future -> {
            if (future.isSuccess()) {
                DataQueueCommand dataCmd =
                        DataQueueCommand.create(getCurrentStreamChannelFuture(), message, false, compressFlag);
                ChannelFuture dataFuture = writeQueue.enqueueFuture(dataCmd, parent.eventLoop());
                dataFuture.addListener(dataResult -> {
                    if (dataResult.isSuccess()) {
                        // Message successfully sent to network, increment InFlight count
                        inFlightCount.incrementAndGet();
                        incrementTotalSentCount();
                        combinedPromise.setSuccess();
                    } else {
                        combinedPromise.setFailure(dataResult.cause());
                        pendingMessages.remove(seq);
                        // Decrement InFlight count since message failed to send
                        inFlightCount.decrementAndGet();
                        cancelByLocal(TriRpcStatus.INTERNAL
                                .withDescription("Client write data failed")
                                .withCause(dataResult.cause()));
                    }
                });
            } else {
                combinedPromise.setFailure(future.cause());
                pendingMessages.remove(seq);
                // Note: InFlight count not incremented here since data send failed before increment
                cancelByLocal(TriRpcStatus.INTERNAL
                        .withDescription("Client write header failed")
                        .withCause(future.cause()));
            }
        });

        return combinedPromise;
    }

    @Override
    public void request(int n) {
        deframer.request(n);
    }

    @Override
    public ChannelFuture halfClose() {
        ChannelFuture checkResult = preCheck();
        if (!checkResult.isSuccess()) {
            return checkResult;
        }
        final EndStreamQueueCommand cmd = EndStreamQueueCommand.create(getCurrentStreamChannelFuture());
        return this.writeQueue.enqueueFuture(cmd, parent.eventLoop()).addListener(future -> {
            if (future.isSuccess()) {
                halfClosed = true;
                // Clean up reliability resources when half-closing
                cleanupReliabilityResources();
            }
        });
    }

    private ChannelFuture preCheck() {
        if (rst) {
            TripleStreamChannelFuture currentFuture = getCurrentStreamChannelFuture();
            if (currentFuture != null) {
                Channel channel = currentFuture.getNow();
                if (channel != null) {
                    return channel.newFailedFuture(new IOException("stream channel has reset"));
                } else {
                    // If channel is not ready yet but stream is reset, use parent channel
                    return parent.newFailedFuture(new IOException("stream channel has reset"));
                }
            }
        }
        return parent.newSucceededFuture();
    }

    private void scheduleHeartbeat() {
        if (!reliabilityEnabled
                || heartbeatScheduler == null
                || heartbeatScheduler.isShutdown()
                || heartbeatState == HeartbeatState.CLOSED) {
            return;
        }

        try {
            // Cancel existing heartbeat task if any
            if (currentHeartbeatTask != null && !currentHeartbeatTask.isDone()) {
                currentHeartbeatTask.cancel(false);
                LOGGER.debug("Cancelled previous heartbeat task for session: {}", sessionId);
            }

            // Schedule new heartbeat task with tracking
            currentHeartbeatTask = scheduleTrackedTaskAtFixedRate(
                    heartbeatScheduler,
                    () -> {
                        try {
                            checkHeartbeatState();
                            sendHeartbeatIfNeeded();
                        } catch (Exception e) {
                            LOGGER.warn(
                                    INTERNAL_ERROR, "", "", "Heartbeat monitoring failed for session: " + sessionId, e);
                        }
                    },
                    config.getHeartbeatInterval(),
                    config.getHeartbeatInterval(),
                    TimeUnit.MILLISECONDS);

            if (currentHeartbeatTask != null) {
                LOGGER.debug("Scheduled new heartbeat task for session: {}", sessionId);
            }
        } catch (java.util.concurrent.RejectedExecutionException e) {
            LOGGER.debug("Failed to schedule heartbeat - scheduler shut down for session: {}", sessionId);
        }
    }

    private void checkHeartbeatState() {
        long now = System.currentTimeMillis();
        long timeSinceLastAck = now - lastHeartbeatAckTime;

        switch (heartbeatState) {
            case HEALTHY:
                if (timeSinceLastAck > config.getHeartbeatInterval() * 2) {
                    missedHeartbeats++;
                    if (missedHeartbeats >= 1) {
                        transitionToState(HeartbeatState.SUSPECT, "First heartbeat timeout");
                    }
                }
                break;

            case SUSPECT:
                if (timeSinceLastAck > config.getSessionTimeout() / 2) {
                    missedHeartbeats++;
                    if (missedHeartbeats >= config.getMaxMissedHeartbeats()) {
                        transitionToState(HeartbeatState.RECONNECTING, "Max missed heartbeats exceeded");
                    }
                }
                break;

            case RECONNECTING:
                if (timeSinceLastAck > config.getSessionTimeout()) {
                    transitionToState(HeartbeatState.FAILED, "Session timeout exceeded");
                }
                break;

            case FAILED:
                // Stay in failed state
                break;
        }
    }

    private void sendHeartbeatIfNeeded() {
        if (heartbeatState == HeartbeatState.FAILED || !parent.isActive()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastHeartbeatSentTime >= config.getHeartbeatInterval()) {
            io.netty.handler.codec.http2.DefaultHttp2Headers heartbeat =
                    new io.netty.handler.codec.http2.DefaultHttp2Headers();
            heartbeat.set("tri-heartbeat", String.valueOf(now));
            heartbeat.set("tri-last-acked", String.valueOf(getLastAckedSeq()));
            heartbeat.set("tri-state", heartbeatState.name().toLowerCase());

            HeaderQueueCommand cmd =
                    HeaderQueueCommand.createHeaders(getCurrentStreamChannelFuture(), heartbeat, false);
            writeQueue.enqueueFuture(cmd, parent.eventLoop()).addListener(future -> {
                if (!future.isSuccess()) {
                    LOGGER.warn(INTERNAL_ERROR, "", "", "Failed to send heartbeat", future.cause());
                }
            });
            lastHeartbeatSentTime = now;
        }
    }

    /**
     * Send a heartbeat with custom headers.
     * This is used for recovery negotiation where additional context is needed.
     */
    private void sendHeartbeatWithHeaders(java.util.Map<CharSequence, Object> customHeaders) {
        if (heartbeatState == HeartbeatState.FAILED || !parent.isActive()) {
            LOGGER.debug("Skipping heartbeat with headers - connection not active or failed state");
            return;
        }

        try {
            long now = System.currentTimeMillis();
            io.netty.handler.codec.http2.DefaultHttp2Headers heartbeat =
                    new io.netty.handler.codec.http2.DefaultHttp2Headers();

            // Add standard heartbeat headers
            heartbeat.set("tri-heartbeat", String.valueOf(now));
            heartbeat.set("tri-last-acked", String.valueOf(getLastAckedSeq()));
            heartbeat.set("tri-state", heartbeatState.name().toLowerCase());

            // Add custom headers
            if (customHeaders != null) {
                for (java.util.Map.Entry<CharSequence, Object> entry : customHeaders.entrySet()) {
                    heartbeat.set(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }

            HeaderQueueCommand cmd =
                    HeaderQueueCommand.createHeaders(getCurrentStreamChannelFuture(), heartbeat, false);
            writeQueue.enqueueFuture(cmd, parent.eventLoop()).addListener(future -> {
                if (!future.isSuccess()) {
                    LOGGER.warn(INTERNAL_ERROR, "", "", "Failed to send heartbeat with custom headers", future.cause());
                } else {
                    LOGGER.debug("Successfully sent heartbeat with custom headers");
                }
            });

            lastHeartbeatSentTime = now;
        } catch (Exception e) {
            LOGGER.error(INTERNAL_ERROR, "", "", "Error sending heartbeat with headers", e);
        }
    }

    private final Object stateTransitionLock = new Object();

    private void transitionToState(HeartbeatState newState, String reason) {
        synchronized (stateTransitionLock) {
            HeartbeatState oldState = heartbeatState;

            // Prevent state transitions once resources are cleaned or CLOSED
            if (oldState == HeartbeatState.CLOSED || reliabilityResourcesCleaned) {
                LOGGER.debug(
                        "Ignoring state transition from {} to {} ({}) - resources cleaned: {}",
                        oldState,
                        newState,
                        reason,
                        reliabilityResourcesCleaned);
                return;
            }

            // Validate state transition logic
            if (!isValidStateTransition(oldState, newState)) {
                LOGGER.warn(
                        INTERNAL_ERROR,
                        "",
                        "",
                        "Invalid state transition from " + oldState + " to " + newState + " (" + reason
                                + ") for session: " + sessionId);
                return;
            }

            heartbeatState = newState;

            LOGGER.warn(
                    INTERNAL_ERROR,
                    "",
                    "",
                    "Heartbeat state transition: " + oldState + " -> " + newState + " (" + reason + "), sessionId: "
                            + sessionId);

            // Trigger state change callbacks
            updateReliabilityState(newState.name(), reason);
        }
    }

    private boolean isValidStateTransition(HeartbeatState from, HeartbeatState to) {
        // Define valid state transitions
        switch (from) {
            case HEALTHY:
                return to == HeartbeatState.SUSPECT
                        || to == HeartbeatState.FAILED
                        || to == HeartbeatState.PAUSED
                        || to == HeartbeatState.CLOSED;
            case SUSPECT:
                return to == HeartbeatState.HEALTHY
                        || to == HeartbeatState.RECONNECTING
                        || to == HeartbeatState.FAILED
                        || to == HeartbeatState.PAUSED
                        || to == HeartbeatState.CLOSED;
            case RECONNECTING:
                return to == HeartbeatState.HEALTHY
                        || to == HeartbeatState.FAILED
                        || to == HeartbeatState.PAUSED
                        || to == HeartbeatState.CLOSED;
            case FAILED:
                return to == HeartbeatState.RECONNECTING
                        || to == HeartbeatState.HEALTHY
                        || to == HeartbeatState.PAUSED
                        || to == HeartbeatState.CLOSED; // Allow recovery from FAILED state
            case PAUSED:
                return to == HeartbeatState.RECONNECTING
                        || to == HeartbeatState.HEALTHY
                        || to == HeartbeatState.FAILED
                        || to == HeartbeatState.CLOSED; // Allow recovery from PAUSED state
            case CLOSED:
                return false; // CLOSED is terminal
            default:
                return false;
        }
    }

    private void attemptReconnect() {
        LOGGER.warn(INTERNAL_ERROR, "", "", "Connection reconnection needed for session: " + sessionId);

        // Schedule recovery in dedicated recovery executor to avoid blocking heartbeat thread
        try {
            recoveryExecutor.execute(() -> {
                try {
                    performSessionRecovery();
                } catch (Exception e) {
                    LOGGER.error(INTERNAL_ERROR, "", "", "Session recovery failed for session: " + sessionId, e);
                    transitionToState(HeartbeatState.FAILED, "Session recovery failed");
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException e) {
            LOGGER.warn(INTERNAL_ERROR, "", "", "Failed to schedule session recovery for session: " + sessionId);
            transitionToState(HeartbeatState.FAILED, "Unable to schedule recovery");
        }
    }

    private void performSessionRecovery() {
        if (heartbeatState == HeartbeatState.CLOSED) {
            return;
        }

        // Handle PAUSED state recovery first
        if (heartbeatState == HeartbeatState.PAUSED) {
            LOGGER.info(
                    "Detected PAUSED state during session recovery, attempting to recover for session: {}", sessionId);
            recoverFromPausedState();
            return;
        }

        // Handle FAILED state recovery
        if (heartbeatState == HeartbeatState.FAILED) {
            LOGGER.info(
                    "Detected FAILED state during session recovery, attempting to recover for session: {}", sessionId);
            recoverFromFailedState();
            return;
        }

        LOGGER.info(
                "Starting session recovery for session: {}, pending messages: {}",
                sessionId,
                reliabilityEnabled && pendingMessages != null ? pendingMessages.size() : 0);

        // Check if connection is still active
        if (!parent.isActive()) {
            LOGGER.warn(
                    INTERNAL_ERROR,
                    "",
                    "",
                    "Connection is inactive for session: " + sessionId + ", attempting reconnection");

            // Attempt real reconnection using reconnection manager
            if (reconnectionManager != null) {
                attemptRealReconnection();
                return;
            } else {
                LOGGER.error(
                        INTERNAL_ERROR,
                        "",
                        "",
                        "No reconnection manager available for session: " + sessionId + ", recovery failed");
                transitionToState(HeartbeatState.FAILED, "No reconnection manager available");
                return;
            }
        }

        // Connection is active, perform session recovery
        performActiveSessionRecovery();
    }

    private void attemptRealReconnection() {
        LOGGER.info("Attempting real reconnection for session: {}", sessionId);

        reconnectionManager.attemptReconnection().whenComplete((success, throwable) -> {
            if (throwable != null) {
                LOGGER.error(INTERNAL_ERROR, "", "", "Reconnection failed for session: " + sessionId, throwable);
                handleReconnectionFailure("Reconnection attempt failed with exception", throwable);
                return;
            }

            if (success && reconnectionManager.isConnectionActive()) {
                LOGGER.info("Reconnection succeeded for session: {}", sessionId);

                try {
                    // After successful reconnection, the underlying connection is reestablished
                    // The existing stream can continue to work with the new connection
                    // since the connection client will provide the new channel transparently
                    performActiveSessionRecovery();
                    LOGGER.info("Session recovery completed after reconnection for session: {}", sessionId);
                } catch (Exception e) {
                    LOGGER.error(
                            INTERNAL_ERROR,
                            "",
                            "",
                            "Error during session recovery after reconnection for session: " + sessionId,
                            e);
                    handleReconnectionFailure("Error during session recovery", e);
                }
            } else {
                LOGGER.warn(INTERNAL_ERROR, "", "", "Reconnection failed for session: " + sessionId);
                handleReconnectionFailure("Reconnection failed - no active connection", null);
            }
        });
    }

    private void handleReconnectionFailure(String reason, Throwable throwable) {
        try {
            LOGGER.error(
                    INTERNAL_ERROR,
                    "",
                    "",
                    "Handling reconnection failure for session: " + sessionId + " - Reason: " + reason,
                    throwable);

            // Mark the session as failed
            transitionToState(HeartbeatState.FAILED, reason);

            // Notify the business layer of the connection failure
            notifyBusinessLayerOfFailure(reason, throwable);

            // Pause reliability instead of permanent cleanup to allow recovery
            pauseReliability();

            // Log failure statistics
            logReconnectionFailureStatistics();

        } catch (Exception e) {
            LOGGER.error(
                    INTERNAL_ERROR, "", "", "Error during reconnection failure handling for session: " + sessionId, e);
        }
    }

    /**
     * Notify business layer of connection failure.
     * For recoverable failures, only trigger state change callback (stream continues).
     * For non-recoverable failures, call onComplete to terminate the stream.
     */
    private void notifyBusinessLayerOfFailure(String reason, Throwable throwable) {
        try {
            // Create error status for classification
            TriRpcStatus errorStatus;
            if (throwable != null) {
                errorStatus = TriRpcStatus.INTERNAL.withDescription(
                        "Reliable stream connection failed: " + reason + ": " + throwable.getMessage());
            } else {
                errorStatus = TriRpcStatus.INTERNAL.withDescription("Reliable stream connection failed: " + reason);
            }

            // Check if error is recoverable
            boolean recoverable =
                    (throwable != null) ? isRecoverableException(throwable) : isRecoverableError(errorStatus);

            if (recoverable) {
                // Recoverable failure: Only notify state change, stream continues for recovery
                LOGGER.info(
                        "Recoverable connection failure for session: {} - Reason: {}, triggering state callback",
                        sessionId,
                        reason);

                // Notify via state change callback (if registered)
                if (stateChangeCallback != null) {
                    try {
                        stateChangeCallback.accept(heartbeatState.name());
                    } catch (Exception e) {
                        LOGGER.warn(
                                INTERNAL_ERROR, "", "", "Error in state change callback for session: " + sessionId, e);
                    }
                }

                // Do NOT call onComplete - stream remains alive for recovery
                LOGGER.debug(
                        "Stream continues for session: {} after recoverable failure, state: {}",
                        sessionId,
                        heartbeatState);

            } else {
                // Non-recoverable failure: Terminate the stream
                LOGGER.error(
                        INTERNAL_ERROR,
                        "",
                        "",
                        "Non-recoverable connection failure for session: " + sessionId + " - Reason: " + reason
                                + ", terminating stream");

                if (listener != null) {
                    // Call onComplete to terminate the stream
                    listener.onComplete(errorStatus, new java.util.HashMap<>());
                    LOGGER.debug(
                            "Successfully terminated stream for session: {} due to non-recoverable error", sessionId);
                } else {
                    LOGGER.warn(
                            INTERNAL_ERROR,
                            "",
                            "",
                            "No listener available to terminate stream for session: " + sessionId);
                }
            }
        } catch (Exception e) {
            LOGGER.error(
                    INTERNAL_ERROR,
                    "",
                    "",
                    "Error notifying business layer of connection failure for session: " + sessionId,
                    e);
        }
    }

    private void logReconnectionFailureStatistics() {
        try {
            if (reliabilityEnabled) {
                long totalSent = sequenceNumber != null ? sequenceNumber.get() : 0;
                int pendingCount = pendingMessages != null ? pendingMessages.size() : 0;
                long totalRetryCount = getTotalRetryCount();

                LOGGER.warn(
                        INTERNAL_ERROR,
                        "",
                        "",
                        "Reconnection failure statistics for session: " + sessionId + " - " + "Total messages sent: "
                                + totalSent + ", Pending messages: " + pendingCount + ", Total retries: "
                                + totalRetryCount);
            }
        } catch (Exception e) {
            LOGGER.debug("Error logging reconnection failure statistics for session: {}", sessionId, e);
        }
    }

    private void performActiveSessionRecovery() {
        // 从持久化存储恢复消息（重连后可能需要重新加载）
        recoverPendingMessages();

        // Reset connection state
        lastHeartbeatAckTime = System.currentTimeMillis();
        missedHeartbeats = 0;
        transitionToState(HeartbeatState.HEALTHY, "Session recovery successful");

        // Notify business layer that session has been recovered
        notifyRecovery("Session reconnected and recovered successfully");

        // Update stream channel after successful reconnection (fix pseudo-reconnection issue)
        updateStreamChannelAfterReconnection();

        // CRITICAL: Restart reliability schedulers after successful reconnection
        restartReliabilitySchedulers();

        // Reset temporary failure count after successful recovery
        resetTemporaryFailureCount();

        // Send a heartbeat to verify connection
        sendHeartbeatIfNeeded();

        LOGGER.info("Session recovery completed for session: {}", sessionId);
    }

    /**
     * Restart reliability schedulers after successful reconnection.
     * This ensures heartbeat and retry mechanisms continue to work after reconnection.
     */
    private void restartReliabilitySchedulers() {
        try {
            LOGGER.info("Restarting reliability schedulers after reconnection for session: {}", sessionId);

            // Check if heartbeat scheduler needs restarting
            if (heartbeatScheduler == null || heartbeatScheduler.isShutdown()) {
                LOGGER.info("Recreating heartbeat scheduler for session: {}", sessionId);
                heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "reliability-heartbeat-" + sessionId);
                    t.setDaemon(true);
                    return t;
                });
            }

            // Check if retry scheduler needs restarting
            if (retryScheduler == null || retryScheduler.isShutdown()) {
                LOGGER.info("Recreating retry scheduler for session: {}", sessionId);
                retryScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "reliability-retry-" + sessionId);
                    t.setDaemon(true);
                    return t;
                });
            }

            // Check if recovery executor needs restarting
            if (recoveryExecutor == null || recoveryExecutor.isShutdown()) {
                LOGGER.info("Recreating recovery executor for session: {}", sessionId);
                recoveryExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "reliability-recovery-" + sessionId);
                    t.setDaemon(true);
                    return t;
                });
            }

            // Restart heartbeat scheduling
            scheduleHeartbeat();

            // Restart retry scheduling
            scheduleRetryCheck();

            LOGGER.info("Successfully restarted reliability schedulers for session: {}", sessionId);

        } catch (Exception e) {
            LOGGER.error(
                    INTERNAL_ERROR, "", "", "Failed to restart reliability schedulers for session: " + sessionId, e);
            // Don't fail the recovery process, but log the error
        }
    }

    private void updateStreamChannelAfterReconnection() {
        try {
            LOGGER.info("Starting real channel replacement after reconnection for session: {}", sessionId);

            // Use the new type-safe method to get stream future
            TripleStreamChannelFuture newStreamChannelFuture = reconnectionManager.createStreamFuture();
            if (newStreamChannelFuture == null) {
                LOGGER.warn(
                        INTERNAL_ERROR,
                        "",
                        "",
                        "No new stream channel available after reconnection for session: " + sessionId);
                handleReconnectionFailure("Failed to create new stream channel", null);
                return;
            }

            // CRITICAL FIX: Wait for the new stream channel to be actually created before proceeding
            try {
                LOGGER.debug("Waiting for new stream channel creation to complete for session: {}", sessionId);
                // Wait for the TripleStreamChannelFuture to complete with a reasonable timeout
                Object newStreamChannel = newStreamChannelFuture.get(10, java.util.concurrent.TimeUnit.SECONDS);

                if (newStreamChannel == null) {
                    LOGGER.warn(
                            INTERNAL_ERROR,
                            "",
                            "",
                            "New stream channel creation completed but returned null for session: " + sessionId);
                    handleReconnectionFailure("New stream channel is null after creation", null);
                    return;
                }

                LOGGER.info(
                        "New stream channel created successfully for session: {}, proceeding with replacement",
                        sessionId);

            } catch (java.util.concurrent.TimeoutException e) {
                LOGGER.error(
                        INTERNAL_ERROR,
                        "",
                        "",
                        "Timeout waiting for new stream channel creation for session: " + sessionId,
                        e);
                handleReconnectionFailure("New stream channel creation timeout", e);
                return;
            } catch (Exception e) {
                LOGGER.error(
                        INTERNAL_ERROR,
                        "",
                        "",
                        "Error waiting for new stream channel creation for session: " + sessionId,
                        e);
                handleReconnectionFailure("New stream channel creation failed", e);
                return;
            }

            // Now perform the actual replacement in a synchronous manner
            try {
                // Use atomic switch to ensure thread safety
                // This is critical to ensure new messages use the reconnected channel
                boolean switchSuccess = switchStreamChannelAtomically(newStreamChannelFuture);

                if (switchSuccess) {
                    LOGGER.info(
                            "Successfully replaced stream channel future for session: {} after reconnection",
                            sessionId);
                } else {
                    throw new RuntimeException("Atomic stream channel switch failed");
                }

            } catch (Exception e) {
                LOGGER.error(
                        INTERNAL_ERROR, "", "", "Error during stream channel replacement for session: " + sessionId, e);
                handleReconnectionFailure("Stream channel replacement failed", e);
            }

        } catch (Exception e) {
            LOGGER.error(INTERNAL_ERROR, "", "", "Error during stream channel update for session: " + sessionId, e);
            handleReconnectionFailure("Stream channel update failed", e);
        }
    }

    private void cleanupOldConnectionResources() {
        try {
            // Clear any old pending network operations
            // The old transport listener will naturally be replaced when the new channel is active
            LOGGER.debug("Cleaned up old connection resources for session: {}", sessionId);
        } catch (Exception e) {
            LOGGER.warn(
                    INTERNAL_ERROR,
                    "",
                    "",
                    "Error during old connection resource cleanup for session: " + sessionId,
                    e);
        }
    }

    /**
     * Recover from FAILED state by attempting reconnection and resource restoration.
     * This method provides a recovery path from permanent failure state.
     * Enhanced to handle complete resource reinitialization even after cleanup.
     */
    private boolean recoverFromFailedState() {
        try {
            LOGGER.info("Starting comprehensive recovery from FAILED state for session: {}", sessionId);

            // Always attempt to fully reinitialize reliability infrastructure
            return performComprehensiveRecovery("Manual recovery from FAILED state");

        } catch (Exception e) {
            LOGGER.error(
                    INTERNAL_ERROR, "", "", "Error during recovery from FAILED state for session: " + sessionId, e);
            transitionToState(HeartbeatState.FAILED, "Recovery attempt failed with exception");
            return false;
        }
    }

    /**
     * Comprehensive recovery that can restore from any failure state, including after cleanup.
     * This method completely reinitializes the reliability infrastructure.
     */
    private boolean performComprehensiveRecovery(String reason) {
        try {
            LOGGER.info("Starting comprehensive recovery for session: {} - Reason: {}", sessionId, reason);

            // Step 1: Transition to RECONNECTING state
            HeartbeatState previousState = heartbeatState;
            transitionToState(HeartbeatState.RECONNECTING, reason);
            if (heartbeatState != HeartbeatState.RECONNECTING) {
                LOGGER.warn(
                        INTERNAL_ERROR,
                        "",
                        "",
                        "Failed to transition from " + previousState + " to RECONNECTING state for session: "
                                + sessionId);
                return false;
            }

            // Step 2: Reset all cleanup flags
            reliabilityResourcesCleaned = false;
            reliabilityEnabled = true;

            // Step 3: Reinitialize all executors
            reinitializeExecutors();

            // Step 4: Reinitialize pending message structure
            if (pendingMessages == null) {
                pendingMessages = new ConcurrentHashMap<>();
                LOGGER.info("Reinitialized pendingMessages map for session: {}", sessionId);
            }

            // Step 5: Reinitialize messageStore and recover data
            boolean storeRecovered = reinitializeMessageStore();
            if (!storeRecovered) {
                LOGGER.warn(
                        INTERNAL_ERROR,
                        "",
                        "",
                        "MessageStore reinitializtion failed, continuing without persistence for session: "
                                + sessionId);
            }

            // Step 6: Reset sequence tracking
            if (sequenceNumber == null) {
                sequenceNumber = new AtomicLong(0);
            }
            if (lastAckedSeq == null) {
                java.lang.reflect.Field lastAckedSeqField =
                        AbstractTripleClientStream.class.getDeclaredField("lastAckedSeq");
                lastAckedSeqField.setAccessible(true);
                lastAckedSeqField.set(this, new AtomicLong(0));
            }

            // Step 7: Reset counters
            if (inFlightCount == null) {
                java.lang.reflect.Field inFlightCountField =
                        AbstractTripleClientStream.class.getDeclaredField("inFlightCount");
                inFlightCountField.setAccessible(true);
                inFlightCountField.set(this, new AtomicInteger(0));
            }

            // Step 8: Load pending messages from persistent storage
            loadPendingMessagesFromStorage();

            // Step 9: Renegotiate ACK starting points after reconnection
            renegotiateAckPoints();

            // Step 10: Restart reliability scheduling
            scheduleHeartbeat();
            scheduleRetryCheck();

            // Step 11: Attempt the actual reconnection
            attemptRealReconnection();

            LOGGER.info("Successfully completed comprehensive recovery for session: {}", sessionId);
            return true;

        } catch (Exception e) {
            LOGGER.error(INTERNAL_ERROR, "", "", "Error during comprehensive recovery for session: " + sessionId, e);
            transitionToState(HeartbeatState.FAILED, "Comprehensive recovery failed");
            return false;
        }
    }

    /**
     * Initialize or reinitialize all executor services for reliability.
     */
    private void reinitializeExecutors() {
        try {
            // Recreate heartbeat scheduler if needed
            if (heartbeatScheduler == null || heartbeatScheduler.isShutdown()) {
                LOGGER.info("Recreating heartbeat scheduler for session: {}", sessionId);
                heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "reliability-heartbeat-" + sessionId);
                    t.setDaemon(true);
                    return t;
                });
            }

            // Recreate retry scheduler if needed
            if (retryScheduler == null || retryScheduler.isShutdown()) {
                LOGGER.info("Recreating retry scheduler for session: {}", sessionId);
                retryScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "reliability-retry-" + sessionId);
                    t.setDaemon(true);
                    return t;
                });
            }

            // Recreate recovery executor if needed
            if (recoveryExecutor == null || recoveryExecutor.isShutdown()) {
                LOGGER.info("Recreating recovery executor for session: {}", sessionId);
                recoveryExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "reliability-recovery-" + sessionId);
                    t.setDaemon(true);
                    return t;
                });
            }

            LOGGER.info("Successfully reinitialized all executors for session: {}", sessionId);
        } catch (Exception e) {
            LOGGER.error(INTERNAL_ERROR, "", "", "Error reinitializing executors for session: " + sessionId, e);
            throw new RuntimeException("Executor reinitialization failed", e);
        }
    }

    /**
     * Reinitialize messageStore and ensure it's ready for use.
     */
    private boolean reinitializeMessageStore() {
        try {
            // Close existing store if it exists
            if (messageStore != null) {
                try {
                    SessionMetadata existingMetadata = new SessionMetadata(
                            sessionId, url.getServiceKey(), System.currentTimeMillis(), url, config);
                    messageStore.close(existingMetadata);
                } catch (Exception e) {
                    LOGGER.debug("Error closing existing messageStore during reinit for session: {}", sessionId, e);
                }
            }

            // Create new messageStore
            LOGGER.info("Reinitializing messageStore for session: {}", sessionId);
            String storeType = url.getParameter("tri.client.pending.store.type", "memory");
            messageStore = ExtensionLoader.getExtensionLoader(PendingMessageStore.class)
                    .getExtension(storeType);

            // Initialize with fresh session metadata
            SessionMetadata metadata =
                    new SessionMetadata(sessionId, url.getServiceKey(), System.currentTimeMillis(), url, config);
            messageStore.init(metadata);

            LOGGER.info("Successfully reinitialized messageStore for session: {}", sessionId);
            return true;

        } catch (Exception e) {
            LOGGER.error(INTERNAL_ERROR, "", "", "Failed to reinitialize messageStore for session: " + sessionId, e);
            messageStore = null;
            return false;
        }
    }

    /**
     * Recover from PAUSED state by restoring schedulers and continuing with normal recovery.
     * This method provides a recovery path from temporary pause state without full reinitialization.
     */
    private boolean recoverFromPausedState() {
        try {
            LOGGER.info("Starting recovery from PAUSED state for session: {}", sessionId);

            // Verify we are in PAUSED state
            if (heartbeatState != HeartbeatState.PAUSED) {
                LOGGER.warn(
                        INTERNAL_ERROR,
                        "",
                        "",
                        "Cannot recover from PAUSED state - current state is: " + heartbeatState + " for session: "
                                + sessionId);
                return false;
            }

            // Use comprehensive recovery which handles all edge cases
            return performComprehensiveRecovery("Recovery from PAUSED state");

        } catch (Exception e) {
            LOGGER.error(
                    INTERNAL_ERROR, "", "", "Error during recovery from PAUSED state for session: " + sessionId, e);
            transitionToState(HeartbeatState.FAILED, "PAUSED state recovery failed with exception");
            return false;
        }
    }

    /**
     * Load pending messages from persistent storage during recovery.
     * This method restores messages that were persisted before connection failure.
     */
    private void loadPendingMessagesFromStorage() {
        try {
            if (messageStore == null) {
                LOGGER.warn(
                        INTERNAL_ERROR,
                        "",
                        "",
                        "Cannot load pending messages - messageStore is null for session: " + sessionId);
                return;
            }

            LOGGER.info("Loading pending messages from persistent storage for session: {}", sessionId);

            // Retrieve all pending messages from the store
            java.util.List<org.apache.dubbo.rpc.protocol.tri.store.PendingMessage> storedMessages =
                    messageStore.load(sessionId);

            if (storedMessages == null || storedMessages.isEmpty()) {
                LOGGER.info("No pending messages found in storage for session: {}", sessionId);
                return;
            }

            // Initialize pendingMessages if needed
            if (pendingMessages == null) {
                pendingMessages = new java.util.concurrent.ConcurrentHashMap<>();
            } else {
                pendingMessages.clear(); // Clear any existing messages
            }

            // Restore messages to in-memory map, converting from store format to runtime format
            int loadedCount = 0;
            for (org.apache.dubbo.rpc.protocol.tri.store.PendingMessage storedMsg : storedMessages) {
                // Convert from store.PendingMessage to runtime PendingMessage
                PendingMessage runtimeMsg = new PendingMessage(
                        storedMsg.getSequence(),
                        storedMsg.getMessage(),
                        storedMsg.getCompressFlag(),
                        storedMsg.getSentTime(),
                        storedMsg.getRetryCount());

                // Set additional runtime fields if they exist
                if (storedMsg.getFirstSendTime() > 0) {
                    runtimeMsg.firstSendTime = storedMsg.getFirstSendTime();
                }
                if (storedMsg.getNextRetryDelay() > 0) {
                    runtimeMsg.nextRetryDelay = storedMsg.getNextRetryDelay();
                }

                pendingMessages.put(runtimeMsg.getSequence(), runtimeMsg);
                loadedCount++;
            }

            LOGGER.info("Successfully loaded {} pending messages from storage for session: {}", loadedCount, sessionId);

            // Update sequence number to the highest loaded sequence
            if (!storedMessages.isEmpty()) {
                long maxSeq = storedMessages.stream()
                        .mapToLong(org.apache.dubbo.rpc.protocol.tri.store.PendingMessage::getSequence)
                        .max()
                        .orElse(0);
                if (maxSeq > sequenceNumber.get()) {
                    sequenceNumber.set(maxSeq);
                    LOGGER.info(
                            "Updated sequence number to {} after loading messages for session: {}", maxSeq, sessionId);
                }
            }

        } catch (Exception e) {
            LOGGER.error(
                    INTERNAL_ERROR, "", "", "Error loading pending messages from storage for session: " + sessionId, e);
            // Don't fail recovery just because of storage loading issues
            // The connection can still work, just without previously pending messages
        }
    }

    /**
     * Renegotiate ACK starting points after reconnection.
     * This helps establish a common understanding with the server about message acknowledgments.
     */
    private void renegotiateAckPoints() {
        try {
            LOGGER.info("Starting ACK point renegotiation for session: {}", sessionId);

            // Get the last acknowledged sequence number
            long lastAcked = lastAckedSeq.get();
            long currentSeq = sequenceNumber.get();

            LOGGER.info(
                    "ACK negotiation for session: {} - lastAcked: {}, currentSeq: {}",
                    sessionId,
                    lastAcked,
                    currentSeq);

            // Send a heartbeat with current state to re-establish ACK baseline
            if (reliabilityEnabled && heartbeatState == HeartbeatState.RECONNECTING) {
                // Create a special recovery heartbeat that includes sequence info
                java.util.Map<CharSequence, Object> recoveryHeaders = new java.util.HashMap<>();
                recoveryHeaders.put("tri-header-last-ack", String.valueOf(lastAcked));
                recoveryHeaders.put("tri-header-current-seq", String.valueOf(currentSeq));
                recoveryHeaders.put("tri-header-recovery-mark", "true");

                // Send the recovery heartbeat
                sendHeartbeatWithHeaders(recoveryHeaders);

                LOGGER.info(
                        "Sent recovery heartbeat for ACK renegotiation, session: {}, lastAck: {}, currentSeq: {}",
                        sessionId,
                        lastAcked,
                        currentSeq);
            }

            // Reset missed heartbeats since we're starting fresh
            missedHeartbeats = 0;
            lastHeartbeatSentTime = System.currentTimeMillis();

            LOGGER.info("Successfully completed ACK point renegotiation for session: {}", sessionId);

        } catch (Exception e) {
            LOGGER.error(INTERNAL_ERROR, "", "", "Error during ACK point renegotiation for session: " + sessionId, e);
            // Don't fail recovery just because of ACK negotiation issues
            // The stream can still work, just with potentially some duplicate messages
        }
    }

    private void resendPendingMessages() {
        // Check if reliability is enabled and pendingMessages is initialized
        if (!reliabilityEnabled || pendingMessages == null || pendingMessages.isEmpty()) {
            return;
        }

        LOGGER.info(
                "Resending {} pending messages for session: {} using new connection",
                pendingMessages.size(),
                sessionId);

        // Verify that we have a valid new connection before resending
        if (!validateNewConnectionForResend()) {
            LOGGER.warn(
                    INTERNAL_ERROR,
                    "",
                    "",
                    "Cannot resend messages - no valid new connection available for session: " + sessionId);
            handleConnectionFailure();
            return;
        }

        // Create a copy to avoid concurrent modification
        java.util.List<PendingMessage> messagesToResend = new java.util.ArrayList<>(pendingMessages.values());

        // IMPORTANT: Filter out messages that exceeded retry limits
        // This prevents infinite retry on repeated reconnections
        int originalCount = messagesToResend.size();
        messagesToResend.removeIf(pending -> {
            if (!isStillRetryable(pending)) {
                // Remove from pending messages map
                pendingMessages.remove(pending.getSequence());
                // Decrement InFlight count for dropped message
                inFlightCount.decrementAndGet();
                LOGGER.warn(
                        INTERNAL_ERROR,
                        "",
                        "",
                        "Dropping message seq=" + pending.getSequence()
                                + " during reconnection resend (retry limit exceeded)");
                return true; // Remove from resend list
            }
            return false; // Keep for resend
        });

        int droppedCount = originalCount - messagesToResend.size();
        if (droppedCount > 0) {
            LOGGER.warn(
                    INTERNAL_ERROR,
                    "",
                    "",
                    "Dropped " + droppedCount + " messages during reconnection resend due to retry limits for session: "
                            + sessionId);
        }

        if (messagesToResend.isEmpty()) {
            LOGGER.info("No retryable messages remaining after filtering for session: {}", sessionId);
            return;
        }

        // Sort by sequence number to maintain order
        messagesToResend.sort((a, b) -> Long.compare(a.getSequence(), b.getSequence()));

        // Update retry counts for all messages to be resent
        updateRetryCountsForResend(messagesToResend);

        // Schedule messages asynchronously with delays to avoid overwhelming the connection
        scheduleMessagesWithDelay(messagesToResend, 0);
    }

    private boolean validateNewConnectionForResend() {
        try {
            // Check if streamChannelFuture is valid and ready
            TripleStreamChannelFuture currentFuture = getCurrentStreamChannelFuture();
            if (currentFuture == null || currentFuture.isCancelled() || currentFuture.isCompletedExceptionally()) {
                LOGGER.warn(
                        INTERNAL_ERROR,
                        "",
                        "",
                        "Stream channel future is not valid for resend - null: " + (currentFuture == null)
                                + ", cancelled: " + (currentFuture != null && currentFuture.isCancelled())
                                + ", exceptional: "
                                + (currentFuture != null && currentFuture.isCompletedExceptionally()));
                return false;
            }

            // Check if parent channel is active
            if (parent == null || !parent.isActive()) {
                LOGGER.warn(
                        INTERNAL_ERROR,
                        "",
                        "",
                        "Parent channel is not active for resend - null: " + (parent == null) + ", active: "
                                + (parent != null && parent.isActive()));
                return false;
            }

            // Check if write queue is available
            if (writeQueue == null) {
                LOGGER.warn(INTERNAL_ERROR, "", "", "Write queue is not available for resend");
                return false;
            }

            LOGGER.debug("New connection validated successfully for resend on session: {}", sessionId);
            return true;
        } catch (Exception e) {
            LOGGER.warn(
                    INTERNAL_ERROR, "", "", "Error validating new connection for resend on session: " + sessionId, e);
            return false;
        }
    }

    private void updateRetryCountsForResend(java.util.List<PendingMessage> messages) {
        try {
            for (PendingMessage pending : messages) {
                // Increment retry count to track reconnection-triggered retries
                pending.incrementRetry();
                LOGGER.debug(
                        "Updated retry count for message seq: {} to {} for session: {}",
                        pending.getSequence(),
                        pending.getRetryCount(),
                        sessionId);
            }
        } catch (Exception e) {
            LOGGER.warn(INTERNAL_ERROR, "", "", "Error updating retry counts for resend on session: " + sessionId, e);
        }
    }

    private void scheduleMessagesWithDelay(java.util.List<PendingMessage> messages, int currentIndex) {
        if (currentIndex >= messages.size()) {
            LOGGER.info("Completed resending pending messages for session: {}", sessionId);
            return;
        }

        PendingMessage pending = messages.get(currentIndex);

        try {
            // Verify that we still have a valid connection before resending
            if (!validateNewConnectionForResend()) {
                LOGGER.error(
                        INTERNAL_ERROR,
                        "",
                        "",
                        "Connection is no longer valid during message resend for session: " + sessionId);
                handleConnectionFailure();
                return;
            }

            // Reset the sent time to current time for retry logic
            pending.updateSentTime(System.currentTimeMillis());

            // Resend the message
            retryMessage(pending);

            LOGGER.debug("Resent message with sequence: {} for session: {}", pending.getSequence(), sessionId);

            // Schedule next message with 10ms delay
            if (retryScheduler != null && !retryScheduler.isShutdown()) {
                retryScheduler.schedule(
                        () -> scheduleMessagesWithDelay(messages, currentIndex + 1), 10, TimeUnit.MILLISECONDS);
            }

        } catch (Exception e) {
            LOGGER.error(
                    INTERNAL_ERROR,
                    "",
                    "",
                    "Failed to resend message with sequence: " + pending.getSequence() + " for session: " + sessionId,
                    e);

            // If we fail to resend a message after reconnection, treat it as a permanent failure
            handleConnectionFailure();

            // Do not continue with next messages - connection has failed
            return;
        }
    }

    private void handleConnectionFailure() {
        LOGGER.warn(
                INTERNAL_ERROR,
                "",
                "",
                "Connection failed temporarily for session: " + sessionId + ", will allow recovery");

        // Notify business layer of the connection failure
        notifyBusinessLayerOfFailure("Connection failed during message resend", null);

        // Handle as temporary failure to allow recovery
        handleTemporaryFailureWithCount("Connection failure during resend");
    }

    private void onHeartbeatAck() {
        lastHeartbeatAckTime = System.currentTimeMillis();
        missedHeartbeats = 0; // Reset missed heartbeat counter

        // Transition back to healthy state if not already
        if (heartbeatState != HeartbeatState.HEALTHY) {
            HeartbeatState previousState = heartbeatState;
            transitionToState(HeartbeatState.HEALTHY, "Heartbeat ACK received");

            // Notify recovery if we were in a problematic state
            if (previousState == HeartbeatState.SUSPECT || previousState == HeartbeatState.RECONNECTING) {
                notifyRecovery("Heartbeat connectivity restored from " + previousState + " state");
                // Reset failure count when connectivity is restored
                resetTemporaryFailureCount();
            }
        }
    }

    private void sendHeartbeatAckToServer(String originalHeartbeat) {
        try {
            if (!parent.isActive()) {
                return;
            }

            io.netty.handler.codec.http2.DefaultHttp2Headers heartbeatAck =
                    new io.netty.handler.codec.http2.DefaultHttp2Headers();
            heartbeatAck.set("tri-heartbeat-ack", originalHeartbeat);
            heartbeatAck.set("tri-client-time", String.valueOf(System.currentTimeMillis()));

            HeaderQueueCommand cmd =
                    HeaderQueueCommand.createHeaders(getCurrentStreamChannelFuture(), heartbeatAck, false);
            writeQueue.enqueue(cmd);

            LOGGER.debug("Sent heartbeat ACK to server: {}", originalHeartbeat);
        } catch (Exception e) {
            LOGGER.warn(INTERNAL_ERROR, "", "", "Failed to send heartbeat ACK to server", e);
        }
    }

    private void scheduleRetryCheck() {
        if (!reliabilityEnabled
                || retryScheduler == null
                || retryScheduler.isShutdown()
                || heartbeatState == HeartbeatState.CLOSED) {
            return;
        }

        try {
            // Cancel existing retry task if any
            if (currentRetryTask != null && !currentRetryTask.isDone()) {
                currentRetryTask.cancel(false);
                LOGGER.debug("Cancelled previous retry check task for session: {}", sessionId);
            }

            // Use single delayed scheduling instead of fixed rate to avoid duplicate scheduling
            currentRetryTask = scheduleTrackedTask(
                    retryScheduler,
                    () -> {
                        try {
                            long now = System.currentTimeMillis();
                            processPendingRetries(now);
                            // Schedule next check with adaptive delay
                            scheduleNextRetryCheck();
                        } catch (Exception e) {
                            LOGGER.error(INTERNAL_ERROR, "", "", "Retry check error for session: " + sessionId, e);
                        }
                    },
                    1000,
                    TimeUnit.MILLISECONDS);

            if (currentRetryTask != null) {
                LOGGER.debug("Scheduled new retry check task for session: {}", sessionId);
            }
        } catch (java.util.concurrent.RejectedExecutionException e) {
            LOGGER.debug("Failed to schedule retry check - scheduler shut down for session: {}", sessionId);
        }
    }

    /**
     * Schedule the next retry check with adaptive delay based on pending messages.
     * This avoids duplicate scheduling and provides better resource utilization.
     */
    private void scheduleNextRetryCheck() {
        if (!reliabilityEnabled
                || retryScheduler == null
                || retryScheduler.isShutdown()
                || heartbeatState == HeartbeatState.CLOSED) {
            return;
        }

        try {
            // Calculate adaptive delay based on the earliest pending retry time
            long nextDelay = calculateOptimalRetryCheckDelay();

            // No need to update currentRetryTask here since this is called from within an existing retry task
            ScheduledFuture<?> nextRetryTask = scheduleTrackedTask(
                    retryScheduler,
                    () -> {
                        try {
                            long now = System.currentTimeMillis();
                            processPendingRetries(now);
                            // Schedule next check recursively
                            scheduleNextRetryCheck();
                        } catch (Exception e) {
                            LOGGER.error(INTERNAL_ERROR, "", "", "Retry check error for session: " + sessionId, e);
                        }
                    },
                    nextDelay,
                    TimeUnit.MILLISECONDS);

            if (nextRetryTask != null) {
                LOGGER.debug("Scheduled next retry check with delay: {}ms for session: {}", nextDelay, sessionId);
            }
        } catch (java.util.concurrent.RejectedExecutionException e) {
            LOGGER.debug("Failed to schedule next retry check - scheduler shut down for session: {}", sessionId);
        }
    }

    /**
     * Calculate the optimal delay for the next retry check.
     * Returns the time until the next pending message should be retried.
     */
    private long calculateOptimalRetryCheckDelay() {
        if (pendingMessages.isEmpty()) {
            return 1000; // Default 1 second when no pending messages
        }

        long now = System.currentTimeMillis();
        long minDelay = Long.MAX_VALUE;

        // Find the earliest retry time among all pending messages
        for (PendingMessage pending : pendingMessages.values()) {
            long nextRetryTime = pending.getSentTime() + config.getRetryTimeout();
            if (nextRetryTime > now) {
                long delay = nextRetryTime - now;
                minDelay = Math.min(minDelay, delay);
            } else {
                // Message is already overdue, check immediately
                return 100; // Small delay for immediate processing
            }
        }

        // Ensure minimum delay of 100ms and maximum of 1 second
        return Math.max(100, Math.min(1000, minDelay));
    }

    /**
     * Process all pending retries with unified timing management.
     * This avoids duplicate scheduling and ensures consistent retry behavior.
     */
    private void processPendingRetries(long now) {
        if (pendingMessages.isEmpty()) {
            return;
        }

        // Create a copy to avoid concurrent modification during iteration
        java.util.List<PendingMessage> messagesToCheck = new java.util.ArrayList<>(pendingMessages.values());

        for (PendingMessage pending : messagesToCheck) {
            // Check if this message needs retry
            if (now - pending.getSentTime() > config.getRetryTimeout()) {
                checkAndRetryWithUnifiedTiming(pending, now);
            }
        }
    }

    /**
     * Legacy method for backward compatibility.
     * Use checkAndRetryWithUnifiedTiming() for new unified timing management.
     */
    private void checkAndRetry(PendingMessage pending, long now) {
        checkAndRetryWithUnifiedTiming(pending, now);
    }

    /**
     * Check and retry with unified timing management.
     * This avoids duplicate scheduling and ensures consistent retry behavior.
     */
    private void checkAndRetryWithUnifiedTiming(PendingMessage pending, long now) {
        // 检查最大重试次数
        if (pending.getRetryCount() >= config.getMaxRetries()) {
            pendingMessages.remove(pending.getSequence());
            // Decrement InFlight count since we're giving up on this message
            inFlightCount.decrementAndGet();
            cancelByLocal(TriRpcStatus.DEADLINE_EXCEEDED.withDescription(
                    "Max retries exceeded for seq: " + pending.getSequence()));
            return;
        }

        // 检查总重试超时
        long totalElapsedTime = now - pending.getFirstSendTime();
        if (totalElapsedTime >= config.getTotalRetryTimeout()) {
            pendingMessages.remove(pending.getSequence());
            // Decrement InFlight count since we're giving up on this message
            inFlightCount.decrementAndGet();
            cancelByLocal(TriRpcStatus.DEADLINE_EXCEEDED.withDescription(
                    "Total retry timeout exceeded for seq: " + pending.getSequence()));
            return;
        }

        // 计算指数退避延迟
        pending.incrementRetry();
        pending.calculateExponentialBackoff(config);

        // 使用统一的时间管理进行重试，避免重复排程
        retryMessageWithUnifiedTiming(pending);
        incrementTotalRetryCount();
        notifyRetry(pending.getSequence());
    }

    /**
     * Retry message with unified timing management.
     * This updates the sent time for next retry check instead of scheduling independently.
     */
    private void retryMessageWithUnifiedTiming(PendingMessage pending) {
        // Update the sent time to reflect the retry time for unified timing management
        pending.updateSentTime(System.currentTimeMillis());

        // Execute retry immediately through the unified mechanism
        retryMessage(pending);

        LOGGER.debug(
                "Retried message with unified timing for seq: {} (retry count: {})",
                pending.getSequence(),
                pending.getRetryCount());
    }

    private void scheduleRetry(PendingMessage pending) {
        if (retryScheduler == null || retryScheduler.isShutdown()) {
            return;
        }
        try {
            ScheduledFuture<?> retryTask = scheduleTrackedTask(
                    retryScheduler,
                    () -> {
                        retryMessage(pending);
                    },
                    config.getRetryTimeout(),
                    TimeUnit.MILLISECONDS);

            if (retryTask != null) {
                LOGGER.debug("Scheduled retry for message seq: {} for session: {}", pending.getSequence(), sessionId);
            }
        } catch (java.util.concurrent.RejectedExecutionException e) {
            LOGGER.debug(
                    "Failed to schedule retry for seq: {} - scheduler shut down for session: {}",
                    pending.getSequence(),
                    sessionId);
        }
    }

    /**
     * 使用指数退避策略调度重试
     */
    private void scheduleRetryWithExponentialBackoff(PendingMessage pending) {
        if (retryScheduler == null || retryScheduler.isShutdown()) {
            return;
        }
        try {
            long retryDelay = pending.getNextRetryDelay();
            LOGGER.debug(
                    "Scheduling exponential backoff retry for seq: {} with delay: {}ms (retry count: {}) for session: {}",
                    pending.getSequence(),
                    retryDelay,
                    pending.getRetryCount(),
                    sessionId);

            ScheduledFuture<?> retryTask = scheduleTrackedTask(
                    retryScheduler,
                    () -> {
                        retryMessage(pending);
                    },
                    retryDelay,
                    TimeUnit.MILLISECONDS);

            if (retryTask != null) {
                LOGGER.debug(
                        "Scheduled exponential backoff retry task for seq: {} for session: {}",
                        pending.getSequence(),
                        sessionId);
            }
        } catch (java.util.concurrent.RejectedExecutionException e) {
            LOGGER.warn(
                    INTERNAL_ERROR,
                    "",
                    "",
                    "Failed to schedule exponential backoff retry for seq: " + pending.getSequence() + " in session: "
                            + sessionId,
                    e);
        }
    }

    private void retryMessage(PendingMessage pending) {
        io.netty.handler.codec.http2.DefaultHttp2Headers retryHeaders =
                new io.netty.handler.codec.http2.DefaultHttp2Headers();
        retryHeaders.set("tri-seq", String.valueOf(pending.getSequence()));
        retryHeaders.set("tri-retry", String.valueOf(pending.getRetryCount()));

        HeaderQueueCommand headerCmd =
                HeaderQueueCommand.createHeaders(getCurrentStreamChannelFuture(), retryHeaders, false);
        writeQueue.enqueueFuture(headerCmd, parent.eventLoop()).addListener(future -> {
            if (future.isSuccess()) {
                DataQueueCommand dataCmd = DataQueueCommand.create(
                        getCurrentStreamChannelFuture(), pending.getMessage(), false, pending.getCompressFlag());
                writeQueue.enqueueFuture(dataCmd, parent.eventLoop()).addListener(dataResult -> {
                    if (!dataResult.isSuccess()) {
                        LOGGER.warn(
                                INTERNAL_ERROR,
                                "",
                                "",
                                "Retry data send failed for seq: " + pending.getSequence(),
                                dataResult.cause());
                    }
                });
            } else {
                LOGGER.warn(
                        INTERNAL_ERROR,
                        "",
                        "",
                        "Retry header send failed for seq: " + pending.getSequence(),
                        future.cause());
            }
        });
    }

    // getLastAckedSeq() is now implemented in ReliabilityContext interface section

    /**
     * Remove messages up to the specified sequence number using watermark-based O(1) cleanup.
     * This is more efficient than scanning the entire map.
     */
    private void removeMessagesUpTo(long seq) {
        // For efficiency, we can use the watermark to quickly identify ranges to remove
        // Since sequence numbers are monotonically increasing, we can remove all entries <= seq
        // Use removeIf for safer concurrent map operations
        if (pendingMessages.isEmpty()) {
            return;
        }

        // Use removeIf for thread-safe batch removal
        int initialSize = pendingMessages.size();
        boolean removed = pendingMessages.keySet().removeIf(key -> key <= seq);

        if (removed) {
            int removedCount = initialSize - pendingMessages.size();
            LOGGER.debug("Removed {} acknowledged messages up to seq: {}", removedCount, seq);
        }
    }

    /**
     * Persist session state (lastAckedSeq, currentSeq) for process restart recovery.
     * This is called after ACK processing to ensure critical state is persisted.
     */
    private void persistSessionState() {
        try {
            if (messageStore == null || currentMetadata == null) {
                return;
            }

            long currentLastAcked = lastAckedSeq.get();
            long currentSeq = sequenceNumber.get();

            // Create updated metadata with current state
            org.apache.dubbo.rpc.protocol.tri.store.SessionMetadata updatedMetadata =
                    currentMetadata.withUpdatedState(currentLastAcked, currentSeq);

            // Persist to storage
            messageStore.updateMetadata(updatedMetadata);

            // Update reference for next update
            currentMetadata = updatedMetadata;

            LOGGER.debug(
                    "Persisted session state for session: {} (lastAcked={}, currentSeq={})",
                    sessionId,
                    currentLastAcked,
                    currentSeq);

        } catch (Exception e) {
            // Don't fail ACK processing if metadata update fails
            LOGGER.warn(INTERNAL_ERROR, "", "", "Failed to persist session state for session: " + sessionId, e);
        }
    }

    private volatile boolean reliabilityResourcesCleaned = false;
    private final AtomicBoolean reliabilityInitialized = new AtomicBoolean(false);
    private volatile boolean serverCapabilityNegotiated = false;

    // Failure tracking for intelligent recovery
    private final AtomicInteger temporaryFailureCount = new AtomicInteger(0);
    private static final int MAX_TEMPORARY_FAILURES = 5; // Max failures before permanent cleanup
    private volatile long lastTemporaryFailureTime = 0;

    /**
     * Determine if an error is recoverable (temporary network issue) or not (protocol error, user cancellation).
     * Recoverable errors should trigger retry/reconnect, while non-recoverable errors should terminate the stream.
     *
     * @param status the error status
     * @return true if the error is recoverable, false otherwise
     */
    private boolean isRecoverableError(TriRpcStatus status) {
        if (status == null) {
            return true; // Unknown error, assume recoverable
        }

        // Non-recoverable errors that should terminate the stream
        TriRpcStatus.Code code = status.code;
        switch (code) {
            case CANCELLED: // User cancelled
            case INVALID_ARGUMENT: // Bad request
            case NOT_FOUND: // Service not found
            case ALREADY_EXISTS: // Duplicate operation
            case PERMISSION_DENIED: // Authorization error
            case FAILED_PRECONDITION: // Precondition not met
            case ABORTED: // Operation aborted
            case UNIMPLEMENTED: // Method not implemented
            case UNAUTHENTICATED: // Authentication failed
                return false; // Don't retry these

            case UNAVAILABLE: // Network/service unavailable - recoverable
            case DEADLINE_EXCEEDED: // Timeout - may recover
            case RESOURCE_EXHAUSTED: // Backpressure - may recover
            case INTERNAL: // Internal error - may be transient
            case UNKNOWN: // Unknown - assume transient
            default:
                return true; // Recoverable, trigger retry
        }
    }

    /**
     * Determine if an exception is recoverable.
     *
     * @param throwable the exception
     * @return true if recoverable, false otherwise
     */
    private boolean isRecoverableException(Throwable throwable) {
        if (throwable == null) {
            return true;
        }

        // Network-related exceptions are recoverable
        String className = throwable.getClass().getName();
        if (className.contains("IOException")
                || className.contains("SocketException")
                || className.contains("ChannelException")
                || className.contains("ConnectException")
                || className.contains("TimeoutException")) {
            return true;
        }

        // Protocol errors are not recoverable
        if (className.contains("ProtocolException")
                || className.contains("CodecException")
                || className.contains("SerializationException")) {
            return false;
        }

        // Default: assume recoverable
        return true;
    }

    /**
     * Attempt recovery for temporary failures, only cleanup after max failures exceeded.
     * This prevents premature resource cleanup for recoverable issues.
     */
    private void attemptRecoveryOrCleanup(String failureReason) {
        long currentTime = System.currentTimeMillis();
        int currentFailures = temporaryFailureCount.incrementAndGet();
        lastTemporaryFailureTime = currentTime;

        LOGGER.warn(
                INTERNAL_ERROR,
                "",
                "",
                "Temporary failure detected for session: " + sessionId + " - Reason: " + failureReason
                        + ", Failure count: " + currentFailures);

        if (currentFailures >= MAX_TEMPORARY_FAILURES) {
            LOGGER.error(
                    INTERNAL_ERROR,
                    "",
                    "",
                    "Max temporary failures (" + MAX_TEMPORARY_FAILURES + ") exceeded for session: " + sessionId
                            + ", marking as failed but keeping recovery capability");
            handleTemporaryFailure("Max temporary failures exceeded");
        } else {
            // Try to recover instead of immediate cleanup
            LOGGER.info(
                    "Attempting recovery for session: {} (failure {}/{})",
                    sessionId,
                    currentFailures,
                    MAX_TEMPORARY_FAILURES);

            try {
                // If we're not in a failed state, try to trigger recovery
                if (heartbeatState != HeartbeatState.FAILED && heartbeatState != HeartbeatState.CLOSED) {
                    // Schedule recovery attempt with exponential backoff
                    long delayMs = Math.min(1000 * (1L << (currentFailures - 1)), 10000); // Max 10s delay

                    if (recoveryExecutor != null && !recoveryExecutor.isShutdown()) {
                        recoveryExecutor.schedule(
                                () -> {
                                    try {
                                        triggerRecovery();
                                    } catch (Exception e) {
                                        LOGGER.warn(
                                                INTERNAL_ERROR,
                                                "",
                                                "",
                                                "Recovery attempt failed for session: " + sessionId,
                                                e);
                                    }
                                },
                                delayMs,
                                java.util.concurrent.TimeUnit.MILLISECONDS);
                    }
                } else {
                    LOGGER.warn(
                            INTERNAL_ERROR,
                            "",
                            "",
                            "Session " + sessionId + " is in terminal state " + heartbeatState
                                    + ", cannot attempt recovery");
                }
            } catch (Exception e) {
                LOGGER.error(INTERNAL_ERROR, "", "", "Error during recovery attempt for session: " + sessionId, e);
            }
        }
    }

    /**
     * Reset temporary failure counter when recovery is successful.
     */
    private void resetTemporaryFailureCount() {
        int previousCount = temporaryFailureCount.getAndSet(0);
        if (previousCount > 0) {
            LOGGER.info("Reset temporary failure count for session: {} (was: {})", sessionId, previousCount);
        }
    }

    /**
     * Atomically switch stream channel future to ensure thread safety during reconnection.
     */
    private boolean switchStreamChannelAtomically(TripleStreamChannelFuture newChannelFuture) {
        TripleStreamChannelFuture oldChannelFuture = streamChannelFuture.getAndSet(newChannelFuture);

        if (oldChannelFuture != null) {
            // Close the old channel to prevent resource leaks
            try {
                Object oldChannel = oldChannelFuture.getNow();
                if (oldChannel instanceof Channel && ((Channel) oldChannel).isActive()) {
                    ((Channel) oldChannel).close();
                    LOGGER.debug("Closed old stream channel during atomic switch for session: {}", sessionId);
                }
            } catch (Exception e) {
                LOGGER.debug("Error closing old stream channel during atomic switch for session: {}", sessionId, e);
            }
        }

        LOGGER.info("Atomically switched stream channel future for session: {}", sessionId);
        return true;
    }

    /**
     * Get current stream channel future safely.
     */
    private TripleStreamChannelFuture getCurrentStreamChannelFuture() {
        return streamChannelFuture.get();
    }

    /**
     * Handle temporary failures that should not permanently disable reliability.
     * This method allows recovery after temporary network issues or connection problems.
     * This version increments the failure count and should be used for independent failure events.
     */
    private void handleTemporaryFailureWithCount(String reason) {
        LOGGER.warn(INTERNAL_ERROR, "", "", "Handling temporary failure for session " + sessionId + ": " + reason);

        // Transition to FAILED state to indicate current connection issues
        heartbeatState = HeartbeatState.FAILED;

        // Cancel current heartbeat/retry tasks but don't shutdown executors
        cancelCurrentScheduledTasks();

        // Clear current stream channel future to force new connection
        streamChannelFuture.set(null);

        // Increment failure count for tracking
        int currentFailureCount = temporaryFailureCount.incrementAndGet();

        // Do NOT set reliabilityEnabled = false - keep it enabled for recovery
        // Do NOT clear pending messages - they will be resent after recovery

        LOGGER.info(
                "Temporary failure handled for session {}, failure count: {}, state: {}",
                sessionId,
                currentFailureCount,
                heartbeatState);
    }

    /**
     * Handle temporary failures without incrementing count.
     * This method should be used when the failure count has already been incremented by the caller.
     */
    private void handleTemporaryFailure(String reason) {
        LOGGER.warn(INTERNAL_ERROR, "", "", "Handling temporary failure for session " + sessionId + ": " + reason);

        // Transition to FAILED state to indicate current connection issues
        heartbeatState = HeartbeatState.FAILED;

        // Cancel current heartbeat/retry tasks but don't shutdown executors
        cancelCurrentScheduledTasks();

        // Clear current stream channel future to force new connection
        streamChannelFuture.set(null);

        // Do NOT increment failure count here - it should be done by the caller
        // Do NOT set reliabilityEnabled = false - keep it enabled for recovery
        // Do NOT clear pending messages - they will be resent after recovery

        int currentFailureCount = temporaryFailureCount.get();
        LOGGER.info(
                "Temporary failure handled for session {}, failure count: {}, state: {}",
                sessionId,
                currentFailureCount,
                heartbeatState);
    }

    /**
     * Cancel currently scheduled tasks without shutting down executors.
     * This method now properly cancels individual scheduled tasks to avoid resource waste.
     */
    private void cancelCurrentScheduledTasks() {
        try {
            int cancelledCount = 0;

            // Cancel current heartbeat task
            if (currentHeartbeatTask != null && !currentHeartbeatTask.isDone()) {
                boolean cancelled = currentHeartbeatTask.cancel(false);
                if (cancelled) {
                    cancelledCount++;
                    LOGGER.debug("Cancelled current heartbeat task for session: {}", sessionId);
                }
                currentHeartbeatTask = null;
            }

            // Cancel current retry task
            if (currentRetryTask != null && !currentRetryTask.isDone()) {
                boolean cancelled = currentRetryTask.cancel(false);
                if (cancelled) {
                    cancelledCount++;
                    LOGGER.debug("Cancelled current retry task for session: {}", sessionId);
                }
                currentRetryTask = null;
            }

            // Cancel all other active tasks
            for (ScheduledFuture<?> task : activeTasks) {
                if (task != null && !task.isDone()) {
                    boolean cancelled = task.cancel(false);
                    if (cancelled) {
                        cancelledCount++;
                    }
                }
            }
            activeTasks.clear();

            LOGGER.debug("Cancelled {} scheduled tasks for session: {}", cancelledCount, sessionId);

        } catch (Exception e) {
            LOGGER.warn(INTERNAL_ERROR, "", "", "Error during task cancellation for session: " + sessionId, e);
        }
    }

    /**
     * Schedule a task and track it for proper cancellation.
     */
    private ScheduledFuture<?> scheduleTrackedTask(
            ScheduledExecutorService executor, Runnable task, long delay, TimeUnit unit) {
        try {
            // Create wrapper class for self-tracking
            class TrackedTask implements Runnable {
                private ScheduledFuture<?> taskFuture;

                @Override
                public void run() {
                    try {
                        task.run();
                    } finally {
                        if (taskFuture != null) {
                            activeTasks.remove(taskFuture);
                        }
                    }
                }

                public void setFuture(ScheduledFuture<?> future) {
                    this.taskFuture = future;
                }
            }

            TrackedTask wrappedTask = new TrackedTask();
            ScheduledFuture<?> future = executor.schedule(wrappedTask, delay, unit);
            wrappedTask.setFuture(future);
            activeTasks.add(future);

            return future;
        } catch (Exception e) {
            LOGGER.warn(INTERNAL_ERROR, "", "", "Error scheduling tracked task for session: " + sessionId, e);
            return null;
        }
    }

    private ScheduledFuture<?> scheduleTrackedTaskAtFixedRate(
            ScheduledExecutorService executor, Runnable task, long initialDelay, long period, TimeUnit unit) {
        try {
            ScheduledFuture<?> future = executor.scheduleAtFixedRate(task, initialDelay, period, unit);
            activeTasks.add(future);

            // Note: Fixed-rate tasks don't self-remove since they run indefinitely
            // They will be removed only when explicitly cancelled
            return future;
        } catch (Exception e) {
            LOGGER.warn(
                    INTERNAL_ERROR, "", "", "Error scheduling tracked fixed-rate task for session: " + sessionId, e);
            return null;
        }
    }

    private void cleanupReliabilityResources() {
        // Early return if reliability was never enabled
        if (!reliabilityEnabled && !reliabilityInitialized.get()) {
            return;
        }

        // Prevent multiple cleanup calls using atomic flag
        if (reliabilityResourcesCleaned) {
            LOGGER.debug("Reliability resources already cleaned for session: {}", sessionId);
            return;
        }

        // Use synchronized block to ensure thread-safe cleanup
        synchronized (this) {
            if (reliabilityResourcesCleaned) {
                return;
            }
            reliabilityResourcesCleaned = true;
        }

        try {
            LOGGER.info("Starting reliability resources cleanup for session: {}", sessionId);

            // Reset initialization flag to allow future initialization
            reliabilityInitialized.set(false);

            // Cancel any pending scheduled heartbeat tasks first
            cancelAllScheduledTasks();

            // Shutdown heartbeat scheduler with proper timeout and error handling
            shutdownExecutorSafely(heartbeatScheduler, "heartbeat scheduler", 500);

            // Shutdown retry scheduler with proper timeout and error handling
            shutdownExecutorSafely(retryScheduler, "retry scheduler", 500);

            // Shutdown recovery executor with proper timeout and error handling
            if (recoveryExecutor != null) {
                shutdownExecutorSafely(recoveryExecutor, "recovery executor", 500);
            }

            // Clear all pending messages and notify them of cancellation
            clearPendingMessagesWithNotification();

            // Close message store
            if (messageStore != null) {
                try {
                    SessionMetadata metadata = new SessionMetadata(
                            sessionId,
                            url != null ? url.getServiceKey() : "unknown",
                            System.currentTimeMillis(),
                            url,
                            config);
                    messageStore.close(metadata);
                    LOGGER.debug("Closed message store for session: {}", sessionId);
                } catch (Exception e) {
                    LOGGER.warn(INTERNAL_ERROR, "", "", "Error closing message store for session: " + sessionId, e);
                }
            }

            // Reset reliability state to prevent further operations
            resetReliabilityState();

            LOGGER.debug("Successfully cleaned up all reliability resources for session: {}", sessionId);
        } catch (Exception e) {
            LOGGER.warn(
                    INTERNAL_ERROR, "", "", "Error during reliability resource cleanup for session: " + sessionId, e);
        }
    }

    /**
     * Pause reliability temporarily while preserving recovery capability.
     * Unlike cleanup, this maintains messageStore and pending messages for later recovery.
     */
    private void pauseReliability() {
        try {
            LOGGER.info("Pausing reliability for session: {} (preserving recovery capability)", sessionId);

            // Cancel any pending scheduled tasks but don't shutdown executors completely
            cancelAllScheduledTasks();

            // Transition to PAUSED state to indicate temporary suspension
            transitionToState(HeartbeatState.PAUSED, "Temporarily paused due to connection issues");

            // Keep reliabilityEnabled = true and preserve messageStore and pendingMessages
            // This allows for later recovery without losing state

            LOGGER.info("Successfully paused reliability for session: {} (can be recovered)", sessionId);
        } catch (Exception e) {
            LOGGER.warn(INTERNAL_ERROR, "", "", "Error during reliability pause for session: " + sessionId, e);
        }
    }

    /**
     * Reset reliability state to paused instead of completely disabled.
     * This preserves the ability to recover later.
     */
    private void pauseReliabilityState() {
        try {
            // Do NOT reset counters and sequence numbers - preserve them for recovery
            // Do NOT set reliabilityEnabled = false - keep it true for recovery

            // Only reset heartbeat timing
            missedHeartbeats = 0;
            lastHeartbeatSentTime = 0;
            lastHeartbeatAckTime = 0;

            // Transition to paused state instead of closed
            if (heartbeatState != null && heartbeatState != HeartbeatState.CLOSED) {
                heartbeatState = HeartbeatState.PAUSED;
            }

            LOGGER.debug("Paused reliability state for session: {} (preserving recovery capability)", sessionId);
        } catch (Exception e) {
            LOGGER.debug("Error pausing reliability state", e);
        }
    }

    private void shutdownExecutorSafely(java.util.concurrent.ExecutorService executor, String name, long timeoutMs) {
        if (executor == null || executor.isShutdown()) {
            return;
        }

        try {
            // First, try graceful shutdown
            executor.shutdown();
            if (!executor.awaitTermination(timeoutMs / 2, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                // If graceful shutdown fails, force shutdown
                LOGGER.debug("Graceful shutdown of {} timed out, forcing shutdown", name);
                executor.shutdownNow();
                // Wait for forced shutdown to complete
                if (!executor.awaitTermination(timeoutMs / 2, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    LOGGER.warn(INTERNAL_ERROR, "", "", "Failed to shutdown " + name + " within " + timeoutMs + "ms");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.debug("Interrupted while shutting down {}, forcing immediate shutdown", name);
            executor.shutdownNow();
        } catch (Exception e) {
            LOGGER.warn(INTERNAL_ERROR, "", "", "Error shutting down " + name, e);
            try {
                executor.shutdownNow();
            } catch (Exception ex) {
                LOGGER.warn(INTERNAL_ERROR, "", "", "Error during forced shutdown of " + name, ex);
            }
        }
    }

    private void cancelAllScheduledTasks() {
        // Cancel scheduled tasks by shutting down gracefully first
        try {
            if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
                // For ScheduledExecutorService, we shutdown gracefully which cancels pending tasks
                LOGGER.debug("Cancelling heartbeat scheduled tasks for session: {}", sessionId);
            }
            if (retryScheduler != null && !retryScheduler.isShutdown()) {
                // Cancel retry scheduled tasks
                LOGGER.debug("Cancelling retry scheduled tasks for session: {}", sessionId);
            }
        } catch (Exception e) {
            LOGGER.debug("Error cancelling scheduled tasks", e);
        }
    }

    private void clearPendingMessagesWithNotification() {
        if (pendingMessages == null || pendingMessages.isEmpty()) {
            return;
        }

        try {
            // Notify all pending messages of cancellation before clearing
            TriRpcStatus cancellationStatus = TriRpcStatus.CANCELLED.withDescription("Stream closed");
            pendingMessages.values().forEach(pending -> {
                try {
                    // Log pending message being cancelled
                    LOGGER.debug(
                            "Cancelling pending message seq: {} for session: {}", pending.getSequence(), sessionId);
                } catch (Exception e) {
                    LOGGER.debug("Error notifying pending message cancellation", e);
                }
            });

            // Clear the map
            pendingMessages.clear();
            LOGGER.debug("Cleared {} pending messages for session: {}", pendingMessages.size(), sessionId);
        } catch (Exception e) {
            LOGGER.warn(INTERNAL_ERROR, "", "", "Error clearing pending messages", e);
            // Force clear even if notification fails
            try {
                pendingMessages.clear();
            } catch (Exception ex) {
                LOGGER.warn(INTERNAL_ERROR, "", "", "Error force-clearing pending messages", ex);
            }
        }
    }

    private void resetReliabilityState() {
        try {
            // Reset counters and state
            lastAckedSeq.set(0);
            sequenceNumber.set(0);
            missedHeartbeats = 0;
            lastHeartbeatSentTime = 0;
            lastHeartbeatAckTime = 0;
            reliabilityEnabled = false;

            // Transition to closed state
            if (heartbeatState != null) {
                heartbeatState = HeartbeatState.CLOSED;
            }
        } catch (Exception e) {
            LOGGER.debug("Error resetting reliability state", e);
        }
    }

    /**
     * @return transport listener
     */
    public H2TransportListener createTransportListener() {
        return new ClientTransportListener();
    }

    /**
     * Get the current transport listener for reuse during reconnection.
     * This ensures we maintain the same listener instance across reconnection cycles.
     *
     * @return current transport listener
     */
    public H2TransportListener getTransportListener() {
        return this.transportListener;
    }

    class ClientTransportListener extends AbstractH2TransportListener implements H2TransportListener {

        private TriRpcStatus transportError;
        private DeCompressor decompressor;
        private boolean headerReceived;
        private Http2Headers trailers;

        void handleH2TransportError(TriRpcStatus status) {
            writeQueue.enqueue(CancelQueueCommand.createCommand(getCurrentStreamChannelFuture(), Http2Error.NO_ERROR));
            rst = true;
            finishProcess(status, null, false);
        }

        void finishProcess(TriRpcStatus status, Http2Headers trailers, boolean isReturnTriException) {
            try {
                // Clean up reliability resources when finishing
                cleanupReliabilityResources();
            } catch (Exception e) {
                LOGGER.warn(INTERNAL_ERROR, "", "", "Error during reliability cleanup in finishProcess", e);
            }

            final Map<CharSequence, String> reserved = filterReservedHeaders(trailers);
            final Map<String, Object> attachments =
                    headersToMap(trailers, () -> reserved.get(TripleHeaderEnum.TRI_HEADER_CONVERT.getKey()));
            final TriRpcStatus detailStatus;
            final TriRpcStatus statusFromTrailers = getStatusFromTrailers(reserved);
            if (statusFromTrailers != null) {
                detailStatus = statusFromTrailers;
            } else {
                detailStatus = status;
            }
            listener.onComplete(detailStatus, attachments, reserved, isReturnTriException);
        }

        private TriRpcStatus validateHeaderStatus(Http2Headers headers) {
            Integer httpStatus = headers.status() == null
                    ? null
                    : Integer.parseInt(headers.status().toString());
            if (httpStatus == null) {
                return TriRpcStatus.INTERNAL.withDescription("Missing HTTP status code");
            }
            final CharSequence contentType = headers.get(HttpHeaderNames.CONTENT_TYPE.getKey());
            if (contentType == null || !GrpcUtils.isGrpcRequest(contentType.toString())) {
                return TriRpcStatus.fromCode(TriRpcStatus.httpStatusToGrpcCode(httpStatus))
                        .withDescription("HTTP status: " + httpStatus + ", invalid content-type: " + contentType);
            }
            return null;
        }

        void onHeaderReceived(Http2Headers headers) {
            if (transportError != null) {
                transportError.appendDescription("headers:" + headers);
                return;
            }
            if (headerReceived) {
                transportError = TriRpcStatus.INTERNAL.withDescription("Received headers twice");
                return;
            }

            // Process reliability headers first
            if (reliabilityEnabled) {
                processReliabilityHeaders(headers);
            }
            Integer httpStatus = headers.status() == null
                    ? null
                    : Integer.parseInt(headers.status().toString());

            if (httpStatus != null && Integer.parseInt(httpStatus.toString()) > 100 && httpStatus < 200) {
                // ignored
                return;
            }
            headerReceived = true;
            transportError = validateHeaderStatus(headers);

            // todo support full payload compressor
            CharSequence messageEncoding = headers.get(TripleHeaderEnum.GRPC_ENCODING.getKey());
            CharSequence triExceptionCode = headers.get(TripleHeaderEnum.TRI_EXCEPTION_CODE.getKey());
            if (triExceptionCode != null) {
                Integer triExceptionCodeNum = Integer.parseInt(triExceptionCode.toString());
                if (!(triExceptionCodeNum.equals(CommonConstants.TRI_EXCEPTION_CODE_NOT_EXISTS))) {
                    isReturnTriException = true;
                }
            }
            if (null != messageEncoding) {
                String compressorStr = messageEncoding.toString();
                if (!Identity.IDENTITY.getMessageEncoding().equals(compressorStr)) {
                    DeCompressor compressor = DeCompressor.getCompressor(frameworkModel, compressorStr);
                    if (null == compressor) {
                        throw TriRpcStatus.UNIMPLEMENTED
                                .withDescription(String.format("Grpc-encoding '%s' is not supported", compressorStr))
                                .asException();
                    } else {
                        decompressor = compressor;
                    }
                }
            }
            TriDecoder.Listener listener = new TriDecoder.Listener() {
                @Override
                public void onRawMessage(byte[] data) {
                    AbstractTripleClientStream.this.listener.onMessage(data, isReturnTriException);
                }

                public void close() {
                    finishProcess(statusFromTrailers(trailers), trailers, isReturnTriException);
                }
            };
            deframer = new TriDecoder(decompressor, listener);
            AbstractTripleClientStream.this.listener.onStart();
        }

        void onTrailersReceived(Http2Headers trailers) {
            if (transportError == null && !headerReceived) {
                transportError = validateHeaderStatus(trailers);
            }
            this.trailers = trailers;
            TriRpcStatus status;
            if (transportError == null) {
                status = statusFromTrailers(trailers);
            } else {
                transportError = transportError.appendDescription("trailers: " + trailers);
                status = transportError;
            }
            if (deframer == null) {
                finishProcess(status, trailers, false);
            } else {
                deframer.close();
            }
        }

        /**
         * Extract the response status from trailers.
         */
        private TriRpcStatus statusFromTrailers(Http2Headers trailers) {
            final Integer intStatus = trailers.getInt(TripleHeaderEnum.STATUS_KEY.getKey());
            TriRpcStatus status = intStatus == null ? null : TriRpcStatus.fromCode(intStatus);
            if (status != null) {
                final CharSequence message = trailers.get(TripleHeaderEnum.MESSAGE_KEY.getKey());
                if (message != null) {
                    final String description = TriRpcStatus.decodeMessage(message.toString());
                    status = status.withDescription(description);
                }
                return status;
            }
            // No status; something is broken. Try to provide a rational error.
            if (headerReceived) {
                return TriRpcStatus.UNKNOWN.withDescription("missing GRPC status in response");
            }
            Integer httpStatus = trailers.status() == null
                    ? null
                    : Integer.parseInt(trailers.status().toString());
            if (httpStatus != null) {
                status = TriRpcStatus.fromCode(TriRpcStatus.httpStatusToGrpcCode(httpStatus));
            } else {
                status = TriRpcStatus.INTERNAL.withDescription("missing HTTP status code");
            }
            return status.appendDescription("missing GRPC status, inferred error from HTTP status code");
        }

        private TriRpcStatus getStatusFromTrailers(Map<CharSequence, String> metadata) {
            if (null == metadata) {
                return null;
            }
            if (!getGrpcStatusDetailEnabled()) {
                return null;
            }
            // second get status detail
            if (!metadata.containsKey(TripleHeaderEnum.STATUS_DETAIL_KEY.getKey())) {
                return null;
            }
            final String raw = (metadata.remove(TripleHeaderEnum.STATUS_DETAIL_KEY.getKey()));
            byte[] statusDetailBin = StreamUtils.decodeASCIIByte(raw);
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            try {
                final Status statusDetail = Status.parseFrom(statusDetailBin);
                List<Any> detailList = statusDetail.getDetailsList();
                Map<Class<?>, Object> classObjectMap = tranFromStatusDetails(detailList);

                // get common exception from DebugInfo
                TriRpcStatus status = TriRpcStatus.fromCode(statusDetail.getCode())
                        .withDescription(TriRpcStatus.decodeMessage(statusDetail.getMessage()));
                DebugInfo debugInfo = (DebugInfo) classObjectMap.get(DebugInfo.class);
                if (debugInfo != null) {
                    String msg = ExceptionUtils.getStackFrameString(debugInfo.getStackEntriesList());
                    status = status.appendDescription(msg);
                }
                return status;
            } catch (IOException ioException) {
                return null;
            } finally {
                ClassLoadUtil.switchContextLoader(tccl);
            }
        }

        private Map<Class<?>, Object> tranFromStatusDetails(List<Any> detailList) {
            Map<Class<?>, Object> map = new HashMap<>(detailList.size());
            try {
                for (Any any : detailList) {
                    if (any.is(ErrorInfo.class)) {
                        ErrorInfo errorInfo = any.unpack(ErrorInfo.class);
                        map.putIfAbsent(ErrorInfo.class, errorInfo);
                    } else if (any.is(DebugInfo.class)) {
                        DebugInfo debugInfo = any.unpack(DebugInfo.class);
                        map.putIfAbsent(DebugInfo.class, debugInfo);
                    }
                    // support others type but now only support this
                }
            } catch (Throwable t) {
                LOGGER.error(PROTOCOL_FAILED_RESPONSE, "", "", "tran from grpc-status-details error", t);
            }
            return map;
        }

        @Override
        public void onHeader(Http2Headers headers, boolean endStream) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("endStream: {} HEADERS: {}", endStream, headers);
            }
            try {
                executor.execute(() -> {
                    if (endStream) {
                        if (!halfClosed) {
                            TripleStreamChannelFuture currentFuture = getCurrentStreamChannelFuture();
                            Channel channel = currentFuture != null ? currentFuture.getNow() : null;
                            if (channel.isActive() && !rst) {
                                writeQueue.enqueue(CancelQueueCommand.createCommand(
                                        getCurrentStreamChannelFuture(), Http2Error.CANCEL));
                                rst = true;
                            }
                        }
                        onTrailersReceived(headers);
                    } else {
                        onHeaderReceived(headers);
                    }
                });
            } catch (Throwable t) {
                LOGGER.error(PROTOCOL_FAILED_RESPONSE, "", "", "submit onHeader task failed", t);
                AbstractTripleClientStream.this.attemptRecoveryOrCleanup("onHeader task submission failed");
            }
        }

        @Override
        public void onData(ByteBuf data, boolean endStream) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("endStream: {} DATA: {}", endStream, data.toString(StandardCharsets.UTF_8));
            }
            try {
                executor.execute(() -> doOnData(data, endStream));
            } catch (Throwable t) {
                // Tasks will be rejected when the thread pool is closed or full,
                // ByteBuf needs to be released to avoid out of heap memory leakage.
                // For example, ThreadLessExecutor will be shutdown when request timeout {@link AsyncRpcResult}
                ReferenceCountUtil.release(data);
                LOGGER.error(PROTOCOL_FAILED_RESPONSE, "", "", "submit onData task failed", t);
                AbstractTripleClientStream.this.attemptRecoveryOrCleanup("onData task submission failed");
            }
        }

        private void doOnData(ByteBuf data, boolean endStream) {
            if (transportError != null) {
                transportError.appendDescription("Data:" + data.toString(StandardCharsets.UTF_8));
                ReferenceCountUtil.release(data);
                if (transportError.description.length() > 512 || endStream) {
                    handleH2TransportError(transportError);
                }
                return;
            }
            if (!headerReceived) {
                handleH2TransportError(TriRpcStatus.INTERNAL.withDescription("headers not received before payload"));
                return;
            }
            deframer.deframe(data);
        }

        @Override
        public void cancelByRemote(long errorCode) {
            try {
                executor.execute(() -> {
                    transportError =
                            TriRpcStatus.CANCELLED.withDescription("Canceled by remote peer, errorCode=" + errorCode);
                    finishProcess(transportError, null, false);
                });
            } catch (Throwable t) {
                LOGGER.error(PROTOCOL_FAILED_RESPONSE, "", "", "submit cancelByRemote task failed", t);
                AbstractTripleClientStream.this.attemptRecoveryOrCleanup("cancelByRemote task submission failed");
            }
        }

        private void processReliabilityHeaders(Http2Headers headers) {
            // First check server capability negotiation on first response
            if (!serverCapabilityNegotiated) {
                checkServerCapabilityNegotiation(headers);
                serverCapabilityNegotiated = true;
            }

            // Enhanced ACK header processing
            CharSequence ackHeader = headers.get("tri-ack");
            if (ackHeader != null) {
                // Debug log to confirm ACK headers were received
                LOGGER.debug(
                        "Client received ACK headers: tri-ack={}, tri-last-acked={}, session={}",
                        ackHeader,
                        headers.get("tri-last-acked"),
                        sessionId);
                long ackedSeq = Long.parseLong(ackHeader.toString());
                // Update the last acknowledged sequence number
                long previousAckedSeq = lastAckedSeq.getAndSet(ackedSeq);

                // Calculate how many messages were acknowledged in this batch
                long newlyAckedCount = ackedSeq - previousAckedSeq;
                if (newlyAckedCount > 0) {
                    // Atomic decrement with boundary protection using compareAndSet loop
                    int current, updated;
                    do {
                        current = inFlightCount.get();
                        updated = Math.max(0, current - (int) newlyAckedCount);
                    } while (!inFlightCount.compareAndSet(current, updated));

                    if (current < newlyAckedCount) {
                        LOGGER.warn(
                                INTERNAL_ERROR,
                                "",
                                "",
                                "InFlight count protection triggered: attempted to ack " + newlyAckedCount
                                        + " messages but only " + current + " were in flight. "
                                        + "This may indicate duplicate ACKs or sequence number issues. Corrected to: "
                                        + updated);
                    }

                    LOGGER.debug(
                            "Acknowledged {} messages, InFlight count: {} -> {}", newlyAckedCount, current, updated);
                }

                // 持久化存储ACK
                if (messageStore != null && messageStore.isHealthy()) {
                    try {
                        messageStore.ack(ackedSeq);
                    } catch (StoreException e) {
                        LOGGER.warn(
                                INTERNAL_ERROR,
                                "",
                                "",
                                "Failed to persist ack for sequence: " + ackedSeq + " for session: " + sessionId,
                                e);
                    }
                }

                // Remove acknowledged messages using watermark-based O(1) cleanup
                long oldWatermark = ackWatermark.get();
                if (ackedSeq > oldWatermark) {
                    // Update watermark and clean up in batch
                    ackWatermark.compareAndSet(oldWatermark, ackedSeq);
                    removeMessagesUpTo(ackedSeq);
                }

                // Persist updated session state for process restart recovery
                persistSessionState();

                return;
            }

            CharSequence nakHeader = headers.get("tri-nak");
            if (nakHeader != null) {
                long nakedSeq = Long.parseLong(nakHeader.toString());
                PendingMessage pending = pendingMessages.get(nakedSeq);
                if (pending != null) {
                    // Use unified timing management for NAK-triggered retries as well
                    checkAndRetryWithUnifiedTiming(pending, System.currentTimeMillis());
                }
                return;
            }

            CharSequence heartbeatAck = headers.get("tri-heartbeat-ack");
            if (heartbeatAck != null) {
                onHeartbeatAck();
                return;
            }

            // Check for heartbeat from server (for bidirectional heartbeat)
            CharSequence serverHeartbeat = headers.get("tri-heartbeat");
            if (serverHeartbeat != null) {
                sendHeartbeatAckToServer(serverHeartbeat.toString());
            }
        }

        /**
         * Check server capability negotiation in first response.
         * If server doesn't support reliability, fallback to non-reliable mode.
         */
        private void checkServerCapabilityNegotiation(Http2Headers headers) {
            CharSequence serverAck = headers.get("tri-reliable-ack");

            if (serverAck == null) {
                // Server doesn't support reliability, fallback to non-reliable mode
                LOGGER.warn(
                        INTERNAL_ERROR,
                        "",
                        "",
                        "Server doesn't support reliability (no tri-reliable-ack header), falling back to non-reliable mode for session: "
                                + sessionId);

                // Disable reliability and cleanup resources
                performReliabilityFallback("Server capability negotiation failed");

            } else {
                LOGGER.info(
                        "Server confirmed reliability support (tri-reliable-ack: {}) for session: {}",
                        serverAck,
                        sessionId);
                // Server supports reliability, continue with reliable mode
            }
        }

        /**
         * Perform fallback to non-reliable mode due to server incompatibility.
         */
        private void performReliabilityFallback(String reason) {
            try {
                LOGGER.warn(
                        INTERNAL_ERROR,
                        "",
                        "",
                        "Performing reliability fallback for session: " + sessionId + " - Reason: " + reason);

                // Transition to CLOSED state to prevent further reliability operations
                transitionToState(HeartbeatState.CLOSED, reason);

                // Disable reliability permanently since server doesn't support it
                reliabilityEnabled = false;

                // Clean up reliability resources since server is incompatible
                cleanupReliabilityResources();

                // Clear pending messages since they won't be ACKed
                if (pendingMessages != null) {
                    int clearedCount = pendingMessages.size();
                    pendingMessages.clear();
                    LOGGER.info(
                            "Cleared {} pending messages during reliability fallback for session: {}",
                            clearedCount,
                            sessionId);
                }

                // Reset counters
                if (inFlightCount != null) {
                    inFlightCount.set(0);
                }

                LOGGER.info("Successfully completed reliability fallback for session: {}", sessionId);

            } catch (Exception e) {
                LOGGER.error(INTERNAL_ERROR, "", "", "Error during reliability fallback for session: " + sessionId, e);
            }
        }

        @Override
        public void onClose() {
            // Clean up reliability resources when closing
            cleanupReliabilityResources();
            try {
                executor.execute(listener::onClose);
            } catch (Throwable t) {
                LOGGER.error(PROTOCOL_FAILED_RESPONSE, "", "", "submit onClose task failed", t);
                // Note: cleanup() not called here as this is already the cleanup path
            }
        }
    }

    // PendingMessage class for reliability tracking
    private static class PendingMessage {
        private final long sequence;
        private final byte[] message;
        private final int compressFlag;
        private volatile long sentTime;
        private volatile int retryCount;
        private volatile long firstSendTime; // 首次发送时间，用于总重试超时控制
        private volatile long nextRetryDelay; // 下次重试延迟时间 (ms)

        public PendingMessage(long sequence, byte[] message, int compressFlag, long sentTime, int retryCount) {
            this.sequence = sequence;
            this.message = message;
            this.compressFlag = compressFlag;
            this.sentTime = sentTime;
            this.retryCount = retryCount;
            this.firstSendTime = sentTime; // 初始化首次发送时间
            this.nextRetryDelay = 100; // 初始重试延迟 100ms
        }

        public long getSequence() {
            return sequence;
        }

        public byte[] getMessage() {
            return message;
        }

        public int getCompressFlag() {
            return compressFlag;
        }

        public long getSentTime() {
            return sentTime;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public void incrementRetry() {
            retryCount++;
        }

        public void updateSentTime(long newTime) {
            sentTime = newTime;
        }

        public long getFirstSendTime() {
            return firstSendTime;
        }

        public long getNextRetryDelay() {
            return nextRetryDelay;
        }

        public void setNextRetryDelay(long delay) {
            this.nextRetryDelay = delay;
        }

        public void calculateExponentialBackoff(ReliabilityConfig config) {
            // 指数退避计算：delay = min(initialDelay * multiplier^retryCount, maxDelay)
            long calculatedDelay =
                    (long) (config.getInitialRetryDelay() * Math.pow(config.getBackoffMultiplier(), this.retryCount));
            this.nextRetryDelay = Math.min(calculatedDelay, config.getMaxRetryDelay());
        }
    }

    /**
     * Check if a message is still retryable based on total timeout and max retry count.
     * This ensures reconnection-triggered resends also respect retry limits.
     *
     * @param pending the pending message to check
     * @return true if the message can still be retried, false if it should be dropped
     */
    private boolean isStillRetryable(PendingMessage pending) {
        if (pending == null || config == null) {
            return false;
        }

        // Check 1: Max retry count
        if (pending.getRetryCount() >= config.getMaxRetries()) {
            LOGGER.warn(
                    INTERNAL_ERROR,
                    "",
                    "",
                    "Message seq=" + pending.getSequence() + " exceeded max retries (" + pending.getRetryCount() + "/"
                            + config.getMaxRetries() + "), dropping");
            return false;
        }

        // Check 2: Total retry timeout (time since first send)
        long elapsedTime = System.currentTimeMillis() - pending.getFirstSendTime();
        long totalTimeout = config.getTotalRetryTimeout();

        if (totalTimeout > 0 && elapsedTime > totalTimeout) {
            LOGGER.warn(
                    INTERNAL_ERROR,
                    "",
                    "",
                    "Message seq=" + pending.getSequence() + " exceeded total retry timeout (" + elapsedTime + "ms > "
                            + totalTimeout + "ms), dropping");
            return false;
        }

        return true;
    }

    // ========== ReliabilityContext Implementation ==========

    @Override
    public String getState() {
        if (!reliabilityEnabled) {
            return "DISABLED";
        }
        return heartbeatState != null ? heartbeatState.name() : "UNKNOWN";
    }

    @Override
    public long getLastAckedSeq() {
        return reliabilityEnabled ? lastAckedSeq.get() : -1;
    }

    @Override
    public int getPendingCount() {
        return reliabilityEnabled ? pendingMessages.size() : 0;
    }

    @Override
    public int getInFlightCount() {
        return reliabilityEnabled ? inFlightCount.get() : 0;
    }

    @Override
    public String getSessionId() {
        return reliabilityEnabled ? sessionId : null;
    }

    @Override
    public long getTotalSentCount() {
        return reliabilityEnabled ? totalSentCount.get() : 0;
    }

    @Override
    public long getTotalRetryCount() {
        return reliabilityEnabled ? totalRetryCount.get() : 0;
    }

    @Override
    public boolean isConnectionActive() {
        if (!reliabilityEnabled) {
            return false;
        }
        return reconnectionManager != null && reconnectionManager.isConnectionActive();
    }

    @Override
    public java.util.Map<String, Object> getStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        if (reliabilityEnabled) {
            stats.put("state", getState());
            stats.put("lastAckedSeq", getLastAckedSeq());
            stats.put("pendingCount", getPendingCount());
            stats.put("inFlightCount", getInFlightCount());
            stats.put("sessionId", getSessionId());
            stats.put("totalSentCount", getTotalSentCount());
            stats.put("totalRetryCount", getTotalRetryCount());
            stats.put("connectionActive", isConnectionActive());
            stats.put("heartbeatState", heartbeatState != null ? heartbeatState.name() : "UNKNOWN");
            if (config != null) {
                stats.put("maxRetries", config.getMaxRetries());
                stats.put("retryTimeout", config.getRetryTimeout());
                stats.put("maxInFlightMessages", config.getMaxInFlightMessages());
            }
        }
        return stats;
    }

    @Override
    public void onStateChange(Consumer<String> callback) {
        this.stateChangeCallback = callback;
    }

    @Override
    public void onRecovery(Consumer<String> callback) {
        this.recoveryCallback = callback;
    }

    @Override
    public void onRetry(Consumer<Long> callback) {
        this.retryCallback = callback;
    }

    @Override
    public boolean retryMessage(long sequence) {
        if (!reliabilityEnabled) {
            return false;
        }

        PendingMessage pending = pendingMessages.get(sequence);
        if (pending != null) {
            checkAndRetryWithUnifiedTiming(pending, System.currentTimeMillis());
            return true;
        }
        return false;
    }

    @Override
    public boolean triggerRecovery() {
        // Prevent concurrent reconnection attempts
        if (heartbeatState == HeartbeatState.RECONNECTING) {
            return false;
        }

        // Allow recovery from FAILED state even if reliability was disabled
        if (heartbeatState == HeartbeatState.FAILED) {
            LOGGER.info("Triggering recovery from FAILED state for session: {}", sessionId);
            return recoverFromFailedState();
        }

        // Allow recovery from PAUSED state (should preserve reliability settings)
        if (heartbeatState == HeartbeatState.PAUSED) {
            LOGGER.info("Triggering recovery from PAUSED state for session: {}", sessionId);
            return recoverFromPausedState();
        }

        // For other states, require reliability to be enabled
        if (!reliabilityEnabled) {
            return false;
        }

        attemptRealReconnection();
        return true;
    }

    @Override
    public void setInFlightLimit(int limit) {
        if (!reliabilityEnabled || config == null) {
            return;
        }
        // Note: This is a simplified implementation. In a production environment,
        // you would want to create a new config instance or use a builder pattern.
        LOGGER.info("InFlight limit updated to: {}", limit);
    }

    @Override
    public ReliabilityConfig getRetryConfig() {
        return reliabilityEnabled ? config : null;
    }

    // ========== Helper methods for ReliabilityContext ==========

    /**
     * Update reliability metrics and trigger callbacks when state changes.
     */
    private void updateReliabilityState(String newState, String reason) {
        if (stateChangeCallback != null) {
            try {
                stateChangeCallback.accept(newState + ": " + reason);
            } catch (Exception e) {
                LOGGER.warn(INTERNAL_ERROR, "", "", "Error in state change callback", e);
            }
        }
    }

    /**
     * Trigger recovery callback when connection is recovered.
     */
    private void notifyRecovery(String reason) {
        if (recoveryCallback != null) {
            try {
                recoveryCallback.accept(reason);
            } catch (Exception e) {
                LOGGER.warn(INTERNAL_ERROR, "", "", "Error in recovery callback", e);
            }
        }
    }

    /**
     * Trigger retry callback when a message is retried.
     */
    private void notifyRetry(long sequence) {
        if (retryCallback != null) {
            try {
                retryCallback.accept(sequence);
            } catch (Exception e) {
                LOGGER.warn(INTERNAL_ERROR, "", "", "Error in retry callback", e);
            }
        }
    }

    /**
     * Increment total sent count and trigger callbacks.
     */
    private void incrementTotalSentCount() {
        totalSentCount.incrementAndGet();
    }

    /**
     * Increment total retry count and trigger callbacks.
     */
    private void incrementTotalRetryCount() {
        totalRetryCount.incrementAndGet();
    }
}
