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
package org.apache.dubbo.rpc.protocol.tri.h12;

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.http12.FlowControlStreamObserver;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.tri.server.ServerSequenceException;
import org.apache.dubbo.rpc.protocol.tri.server.ServerSequenceStore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class BiStreamServerCallListener extends AbstractServerCallListener {

    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(BiStreamServerCallListener.class);

    private StreamObserver<Object> requestObserver;
    private final boolean reliabilityEnabled;
    private final String sessionId;
    private final AtomicLong lastAckedSeq;
    private volatile Long currentSequence;

    // 序列号存储接口（支持持久化）
    private ServerSequenceStore sequenceStore;

    // 幂等性支持：已处理序列号的滑动窗口（内存缓存）
    private final ConcurrentHashMap<Long, Boolean> processedSequences;
    private final AtomicLong minProcessedSeq; // 最小已处理序列号，用于窗口清理
    private static final int MAX_WINDOW_SIZE = 10000; // 最大窗口大小

    public BiStreamServerCallListener(
            RpcInvocation invocation, Invoker<?> invoker, FlowControlStreamObserver<Object> responseObserver) {
        super(invocation, invoker, responseObserver);
        this.reliabilityEnabled = false;
        this.sessionId = null;
        this.lastAckedSeq = new AtomicLong(0);
        this.processedSequences = new ConcurrentHashMap<>();
        this.minProcessedSeq = new AtomicLong(0);
        this.sequenceStore = null; // 不可靠模式下不使用序列号存储
        invocation.setArguments(new Object[] {responseObserver});
        invoke();
    }

    public BiStreamServerCallListener(
            RpcInvocation invocation,
            Invoker<?> invoker,
            FlowControlStreamObserver<Object> responseObserver,
            boolean reliabilityEnabled,
            String sessionId) {
        super(invocation, invoker, responseObserver);
        this.reliabilityEnabled = reliabilityEnabled;
        this.sessionId = sessionId;
        this.lastAckedSeq = new AtomicLong(0);
        this.processedSequences = new ConcurrentHashMap<>();
        this.minProcessedSeq = new AtomicLong(0);

        // 初始化序列号存储（仅在可靠模式下）
        if (reliabilityEnabled) {
            initializeSequenceStore(invocation);
        } else {
            this.sequenceStore = null;
        }

        invocation.setArguments(new Object[] {responseObserver});
        invoke();
    }

    public void setCurrentSequence(Long sequence) {
        this.currentSequence = sequence;
    }

    /**
     * 初始化序列号存储
     */
    private void initializeSequenceStore(RpcInvocation invocation) {
        try {
            // 从URL参数获取存储类型配置，默认为内存存储
            String storeType = invoker.getUrl().getParameter("tri.server.sequence.store.type", "memory");

            ExtensionLoader<ServerSequenceStore> loader = ExtensionLoader.getExtensionLoader(ServerSequenceStore.class);
            this.sequenceStore = loader.getExtension(storeType);

            String serviceKey = invocation.getTargetServiceUniqueName() + "." + invocation.getMethodName();
            sequenceStore.init(sessionId, serviceKey);

            LOGGER.info(
                    "Initialized server sequence store of type: {} for session: {} from URL parameter",
                    storeType,
                    sessionId);
        } catch (Exception e) {
            LOGGER.error(
                    LoggerCodeConstants.INTERNAL_ERROR,
                    "",
                    "",
                    "Failed to initialize sequence store for session: " + sessionId,
                    e);
            // 降级到内存窗口模式
            this.sequenceStore = null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onReturn(Object value) {
        requestObserver = (StreamObserver<Object>) value;
    }

    @Override
    public void onMessage(Object message) {
        // Process reliability ACK first
        if (reliabilityEnabled && currentSequence != null) {
            // 检查幂等性：如果序列号已经处理过，直接ACK并跳过
            if (isDuplicateMessage(currentSequence)) {
                sendAck(currentSequence);
                return;
            }

            // 记录已处理的序列号（同时更新内存和持久化存储）
            markSequenceAsProcessed(currentSequence);
            lastAckedSeq.set(currentSequence);
            sendAck(currentSequence);
        }

        if (message instanceof Object[]) {
            message = ((Object[]) message)[0];
        }
        requestObserver.onNext(message);
        if (((FlowControlStreamObserver<Object>) responseObserver).isAutoRequestN()) {
            ((FlowControlStreamObserver<Object>) responseObserver).request(1);
        }
    }

    private void sendAck(long sequence) {
        if (responseObserver instanceof AttachmentHolder) {
            // Merge with existing attachments to avoid overwriting
            Map<String, Object> existingAttachments = ((AttachmentHolder) responseObserver).getResponseAttachments();
            Map<String, Object> ackAttachments =
                    existingAttachments != null ? new HashMap<>(existingAttachments) : new HashMap<>();

            ackAttachments.put("tri-ack", String.valueOf(sequence));
            ackAttachments.put("tri-last-acked", String.valueOf(lastAckedSeq.get()));

            // 添加更丰富的状态信息
            if (sequenceStore != null && sequenceStore.isHealthy()) {
                try {
                    ServerSequenceStore.StoreStats stats = sequenceStore.getStats();
                    ackAttachments.put("tri-server-stats", stats.toString());

                    // 添加服务端处理窗口信息
                    ackAttachments.put("tri-server-window-info", getWindowInfo());
                } catch (ServerSequenceException e) {
                    LOGGER.warn(
                            LoggerCodeConstants.INTERNAL_ERROR,
                            "",
                            "",
                            "Failed to get server stats for session: " + sessionId,
                            e);
                }
            }

            ((AttachmentHolder) responseObserver).setResponseAttachments(ackAttachments);

            try {
                // Use specialized method to send ACK headers immediately via HTTP/2 HEADERS frame
                if (responseObserver instanceof org.apache.dubbo.rpc.protocol.tri.h12.http2.Http2ServerStreamObserver) {
                    ((org.apache.dubbo.rpc.protocol.tri.h12.http2.Http2ServerStreamObserver) responseObserver)
                            .sendAckHeaders(ackAttachments);
                    LOGGER.debug(
                            "Sent ACK headers for sequence: {}, tri-ack: {}, tri-last-acked: {}",
                            sequence,
                            sequence,
                            lastAckedSeq.get());
                } else {
                    LOGGER.warn(
                            LoggerCodeConstants.INTERNAL_ERROR,
                            "",
                            "",
                            "Cannot send ACK headers for sequence: " + sequence
                                    + " - responseObserver is not Http2ServerStreamObserver: "
                                    + responseObserver.getClass().getSimpleName());
                }
            } catch (Exception e) {
                LOGGER.warn(
                        LoggerCodeConstants.INTERNAL_ERROR,
                        "",
                        "",
                        "Failed to send ACK headers for sequence: " + sequence,
                        e);
            }
        }
    }

    /**
     * 检查是否为重复消息
     */
    private boolean isDuplicateMessage(Long sequence) {
        if (sequence == null) {
            return false;
        }

        // 首先检查持久化存储（如果有）
        if (sequenceStore != null && sequenceStore.isHealthy()) {
            try {
                if (sequenceStore.isSequenceProcessed(sequence)) {
                    return true;
                }
            } catch (ServerSequenceException e) {
                LOGGER.warn(
                        LoggerCodeConstants.INTERNAL_ERROR,
                        "",
                        "",
                        "Failed to check sequence from persistent store: " + sequence,
                        e);
                // 降级到内存检查
            }
        }

        // 如果序列号小于最小已处理序列号，认为是重复的（已在窗口清理时被移除）
        if (sequence <= minProcessedSeq.get()) {
            return true;
        }

        // 检查是否在已处理序列号窗口中
        return processedSequences.containsKey(sequence);
    }

    /**
     * 标记序列号为已处理
     */
    private void markSequenceAsProcessed(Long sequence) {
        if (sequence == null) {
            return;
        }

        // 首先记录到持久化存储（如果有）
        if (sequenceStore != null && sequenceStore.isHealthy()) {
            try {
                sequenceStore.recordSequence(sequence);
            } catch (ServerSequenceException e) {
                LOGGER.warn(
                        LoggerCodeConstants.INTERNAL_ERROR,
                        "",
                        "",
                        "Failed to record sequence to persistent store: " + sequence,
                        e);
                // 继续处理内存存储，不中断流程
            }
        }

        // 同时记录到内存窗口（用于快速访问和窗口管理）
        processedSequences.put(sequence, true);

        // 清理滑动窗口，避免内存无限增长
        cleanupSequenceWindow();
    }

    /**
     * 清理序列号窗口，保持窗口大小在限制范围内
     */
    private void cleanupSequenceWindow() {
        long currentSeq = currentSequence != null ? currentSequence : 0;
        long minSeq = minProcessedSeq.get();

        // 如果窗口大小超过限制，清理旧的序列号
        if (processedSequences.size() > MAX_WINDOW_SIZE && currentSeq > minSeq + MAX_WINDOW_SIZE) {
            long newMinSeq = currentSeq - MAX_WINDOW_SIZE;

            // 同步清理持久化存储（使用新的cleanupAcknowledged方法）
            if (sequenceStore != null && sequenceStore.isHealthy()) {
                try {
                    sequenceStore.cleanupAcknowledged(newMinSeq);
                    LOGGER.debug("Synced persistent store cleanup up to: {} for session: {}", newMinSeq, sessionId);
                } catch (ServerSequenceException e) {
                    LOGGER.warn(
                            LoggerCodeConstants.INTERNAL_ERROR,
                            "",
                            "",
                            "Failed to cleanup persistent store up to: " + newMinSeq,
                            e);
                    // 继续清理内存存储，保持系统可用性
                }
            }

            // 移除小于等于新最小序列号的记录
            processedSequences.entrySet().removeIf(entry -> entry.getKey() <= newMinSeq);

            // 更新最小序列号
            minProcessedSeq.compareAndSet(minSeq, newMinSeq);

            LOGGER.debug("Cleaned up sequence window: minSeq={}, windowSize={}", newMinSeq, processedSequences.size());
        }
    }

    /**
     * 获取已处理序列号数量（用于监控）
     */
    public int getProcessedSequenceCount() {
        return processedSequences.size();
    }

    /**
     * 获取当前处理窗口信息（用于监控和调试）
     */
    public String getWindowInfo() {
        return String.format(
                "minSeq=%d, currentSeq=%s, windowSize=%d",
                minProcessedSeq.get(), currentSequence, processedSequences.size());
    }

    @Override
    public void onCancel(long code) {
        requestObserver.onError(new HttpStatusException((int) code));
    }

    @Override
    public void onComplete() {
        // 清理序列号存储资源
        cleanupSequenceStore();
        requestObserver.onCompleted();
    }

    /**
     * 清理序列号存储资源
     */
    private void cleanupSequenceStore() {
        if (sequenceStore != null) {
            try {
                sequenceStore.close();
                LOGGER.debug("Closed sequence store for session: {}", sessionId);
            } catch (ServerSequenceException e) {
                LOGGER.warn(
                        LoggerCodeConstants.INTERNAL_ERROR,
                        "",
                        "",
                        "Error closing sequence store for session: " + sessionId,
                        e);
            }
        }
    }
}
