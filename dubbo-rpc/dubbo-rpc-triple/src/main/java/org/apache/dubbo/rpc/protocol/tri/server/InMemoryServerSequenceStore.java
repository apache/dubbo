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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory implementation of ServerSequenceStore
 * 内存实现的服务端序列号存储，提供基本功能但不支持跨进程重启
 */
public class InMemoryServerSequenceStore implements ServerSequenceStore {

    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(InMemoryServerSequenceStore.class);

    // Session-specific storage
    private final ConcurrentHashMap<String, SessionStorage> sessionStores = new ConcurrentHashMap<>();
    private volatile String currentSessionId;
    private volatile String currentServiceKey;

    // Session-specific storage data
    private static class SessionStorage {
        private final ConcurrentSkipListSet<Long> processedSequences = new ConcurrentSkipListSet<>();
        private final AtomicLong lastAcknowledgedSequence = new AtomicLong(-1);
        private final AtomicLong totalSequencesRecorded = new AtomicLong(0);
        private final AtomicLong cleanupCount = new AtomicLong(0);

        boolean recordSequence(long sequence) {
            if (processedSequences.add(sequence)) {
                totalSequencesRecorded.incrementAndGet();
                return true;
            }
            return false;
        }

        void updateLastAcknowledged(long sequence) {
            lastAcknowledgedSequence.updateAndGet(current -> Math.max(current, sequence));
        }

        void cleanupSequencesUpTo(long sequence) {
            if (processedSequences.headSet(sequence).size() > 0) {
                processedSequences.headSet(sequence).clear();
                cleanupCount.incrementAndGet();
            }
        }
    }

    @Override
    public void init(String sessionId, String serviceKey) throws ServerSequenceException {
        this.currentSessionId = sessionId;
        this.currentServiceKey = serviceKey;
        sessionStores.putIfAbsent(sessionId, new SessionStorage());
        LOGGER.info("Initialized InMemoryServerSequenceStore for session: {}", sessionId);
    }

    @Override
    public void recordSequence(long sequence) throws ServerSequenceException {
        try {
            SessionStorage storage = sessionStores.get(currentSessionId);
            if (storage == null) {
                throw new ServerSequenceException("Session not initialized: " + currentSessionId);
            }

            storage.recordSequence(sequence);
            storage.updateLastAcknowledged(sequence);

            LOGGER.debug("Recorded sequence: {} for session: {}", sequence, currentSessionId);
        } catch (Exception e) {
            throw new ServerSequenceException("Failed to record sequence: " + sequence, e);
        }
    }

    @Override
    public long getLastAcknowledgedSequence() throws ServerSequenceException {
        try {
            SessionStorage storage = sessionStores.get(currentSessionId);
            if (storage == null) {
                return -1;
            }
            return storage.lastAcknowledgedSequence.get();
        } catch (Exception e) {
            throw new ServerSequenceException("Failed to get last acknowledged sequence", e);
        }
    }

    @Override
    public boolean isSequenceProcessed(long sequence) throws ServerSequenceException {
        try {
            SessionStorage storage = sessionStores.get(currentSessionId);
            if (storage == null) {
                return false;
            }
            return storage.processedSequences.contains(sequence);
        } catch (Exception e) {
            throw new ServerSequenceException("Failed to check sequence processed status", e);
        }
    }

    @Override
    public void cleanupSequencesUpTo(long sequence) throws ServerSequenceException {
        try {
            SessionStorage storage = sessionStores.get(currentSessionId);
            if (storage != null) {
                storage.cleanupSequencesUpTo(sequence);
                LOGGER.debug("Cleaned up sequences up to: {} for session: {}", sequence, currentSessionId);
            }
        } catch (Exception e) {
            throw new ServerSequenceException("Failed to cleanup sequences up to: " + sequence, e);
        }
    }

    @Override
    public void cleanupAcknowledged(long sequence) throws ServerSequenceException {
        try {
            SessionStorage storage = sessionStores.get(currentSessionId);
            if (storage != null) {
                // Clean up sequences up to the acknowledged sequence
                storage.cleanupSequencesUpTo(sequence);

                // Update last acknowledged sequence if needed
                storage.updateLastAcknowledged(sequence);

                LOGGER.debug("Cleaned up acknowledged sequences up to: {} for session: {}", sequence, currentSessionId);
            }
        } catch (Exception e) {
            throw new ServerSequenceException("Failed to cleanup acknowledged sequences up to: " + sequence, e);
        }
    }

    @Override
    public StoreStats getStats() throws ServerSequenceException {
        try {
            SessionStorage storage = sessionStores.get(currentSessionId);
            if (storage == null) {
                return new StoreStats(0, 0, -1, 0);
            }

            return new StoreStats(
                    storage.totalSequencesRecorded.get(),
                    storage.processedSequences.size(),
                    storage.lastAcknowledgedSequence.get(),
                    storage.cleanupCount.get());
        } catch (Exception e) {
            throw new ServerSequenceException("Failed to get store stats", e);
        }
    }

    @Override
    public void close() throws ServerSequenceException {
        try {
            SessionStorage storage = sessionStores.remove(currentSessionId);
            if (storage != null) {
                LOGGER.info(
                        "Closed InMemoryServerSequenceStore for session: {}, cleaned {} sequences",
                        currentSessionId,
                        storage.processedSequences.size());
            }
        } catch (Exception e) {
            throw new ServerSequenceException("Failed to close store for session: " + currentSessionId, e);
        }
    }

    @Override
    public boolean isHealthy() {
        return true; // In-memory store is always healthy
    }

    /**
     * Get the number of sessions in the store (for monitoring)
     */
    public int getSessionCount() {
        return sessionStores.size();
    }

    /**
     * Get the number of processed sequences for a session (for monitoring)
     */
    public int getProcessedSequenceCount(String sessionId) {
        SessionStorage storage = sessionStores.get(sessionId);
        return storage != null ? storage.processedSequences.size() : 0;
    }

    /**
     * Clear all data (for testing)
     */
    public void clear() {
        sessionStores.clear();
        LOGGER.info("Cleared all data from InMemoryServerSequenceStore");
    }
}
