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
package org.apache.dubbo.rpc.protocol.tri.server;

import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;

/**
 * Service-side sequence number state storage interface for reliable streaming
 * 服务端序列号状态存储接口，用于实现重启后的幂等性保证
 */
@SPI(scope = ExtensionScope.FRAMEWORK)
public interface ServerSequenceStore {

    /**
     * Initialize the store for a session
     * 初始化会话的存储
     *
     * @param sessionId session identifier
     * @param serviceKey service key
     * @throws ServerSequenceException if initialization fails
     */
    void init(String sessionId, String serviceKey) throws ServerSequenceException;

    /**
     * Record a processed sequence number
     * 记录已处理的序列号
     *
     * @param sequence sequence number
     * @throws ServerSequenceException if storage fails
     */
    void recordSequence(long sequence) throws ServerSequenceException;

    /**
     * Get the last acknowledged sequence number
     * 获取最后确认的序列号
     *
     * @return last acknowledged sequence number, or -1 if not found
     * @throws ServerSequenceException if retrieval fails
     */
    long getLastAcknowledgedSequence() throws ServerSequenceException;

    /**
     * Check if a sequence number has been processed
     * 检查序列号是否已处理过
     *
     * @param sequence sequence number to check
     * @return true if processed, false otherwise
     * @throws ServerSequenceException if check fails
     */
    boolean isSequenceProcessed(long sequence) throws ServerSequenceException;

    /**
     * Clean up sequences up to the specified sequence number (inclusive)
     * 清理指定序列号之前的记录（包含该序列号）
     *
     * @param sequence sequence number to clean up to
     * @throws ServerSequenceException if cleanup fails
     */
    void cleanupSequencesUpTo(long sequence) throws ServerSequenceException;

    /**
     * Get statistics about the store
     * 获取存储统计信息
     *
     * @return store statistics
     * @throws ServerSequenceException if retrieval fails
     */
    StoreStats getStats() throws ServerSequenceException;

    /**
     * Clean up acknowledged sequences up to the specified sequence number (inclusive)
     * 清理已确认的序列号，支持基于ACK的增量清理
     *
     * @param sequence sequence number to clean up to
     * @throws ServerSequenceException if cleanup fails
     */
    void cleanupAcknowledged(long sequence) throws ServerSequenceException;

    /**
     * Close the store and cleanup resources
     * 关闭存储并清理资源
     *
     * @throws ServerSequenceException if close fails
     */
    void close() throws ServerSequenceException;

    /**
     * Check if the store is healthy
     * 检查存储是否健康
     *
     * @return true if healthy, false otherwise
     */
    boolean isHealthy();

    /**
     * Store statistics
     * 存储统计信息
     */
    class StoreStats {
        private final long totalSequencesRecorded;
        private final long currentSequenceCount;
        private final long lastAcknowledgedSequence;
        private final long cleanupCount;

        public StoreStats(
                long totalSequencesRecorded,
                long currentSequenceCount,
                long lastAcknowledgedSequence,
                long cleanupCount) {
            this.totalSequencesRecorded = totalSequencesRecorded;
            this.currentSequenceCount = currentSequenceCount;
            this.lastAcknowledgedSequence = lastAcknowledgedSequence;
            this.cleanupCount = cleanupCount;
        }

        public long getTotalSequencesRecorded() {
            return totalSequencesRecorded;
        }

        public long getCurrentSequenceCount() {
            return currentSequenceCount;
        }

        public long getLastAcknowledgedSequence() {
            return lastAcknowledgedSequence;
        }

        public long getCleanupCount() {
            return cleanupCount;
        }

        @Override
        public String toString() {
            return String.format(
                    "StoreStats{total=%d, current=%d, lastAcked=%d, cleanups=%d}",
                    totalSequencesRecorded, currentSequenceCount, lastAcknowledgedSequence, cleanupCount);
        }
    }
}
