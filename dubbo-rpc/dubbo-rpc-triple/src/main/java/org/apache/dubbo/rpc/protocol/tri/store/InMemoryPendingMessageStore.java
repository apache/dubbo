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
package org.apache.dubbo.rpc.protocol.tri.store;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of PendingMessageStore.
 * This is the default implementation that provides no persistence across restarts.
 */
public class InMemoryPendingMessageStore implements PendingMessageStore {

    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(InMemoryPendingMessageStore.class);

    // sessionId -> sequence -> PendingMessage
    private final Map<String, Map<Long, PendingMessage>> sessionStore = new ConcurrentHashMap<>();

    private SessionMetadata currentMetadata;

    @Override
    public void init(SessionMetadata metadata) throws StoreException {
        this.currentMetadata = metadata;
        sessionStore.putIfAbsent(metadata.getSessionId(), new ConcurrentHashMap<>());
        LOGGER.info("Initialized InMemoryPendingMessageStore for session: {}", metadata.getSessionId());
    }

    @Override
    public void put(PendingMessage message) throws StoreException {
        try {
            Map<Long, PendingMessage> sessionMessages = sessionStore.get(message.getSessionId());
            if (sessionMessages == null) {
                throw new StoreException("Session not initialized: " + message.getSessionId());
            }

            sessionMessages.put(message.getSequence(), message);
            LOGGER.debug(
                    "Stored message with sequence: {} for session: {}", message.getSequence(), message.getSessionId());
        } catch (Exception e) {
            throw new StoreException("Failed to store message: " + message.getSequence(), e);
        }
    }

    @Override
    public void ack(long sequence) throws StoreException {
        try {
            if (currentMetadata == null) {
                throw new StoreException("Store not initialized");
            }

            Map<Long, PendingMessage> sessionMessages = sessionStore.get(currentMetadata.getSessionId());
            if (sessionMessages != null) {
                // Remove all messages with sequence <= acknowledged sequence
                int removedCount = 0;
                Iterator<Map.Entry<Long, PendingMessage>> iterator =
                        sessionMessages.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Long, PendingMessage> entry = iterator.next();
                    if (entry.getKey() <= sequence) {
                        iterator.remove();
                        removedCount++;
                    }
                }
                LOGGER.debug(
                        "Acknowledged {} messages up to sequence: {} for session: {}",
                        removedCount,
                        sequence,
                        currentMetadata.getSessionId());
            }
        } catch (Exception e) {
            throw new StoreException("Failed to ack message: " + sequence, e);
        }
    }

    @Override
    public void remove(long sequence) throws StoreException {
        try {
            if (currentMetadata == null) {
                throw new StoreException("Store not initialized");
            }

            Map<Long, PendingMessage> sessionMessages = sessionStore.get(currentMetadata.getSessionId());
            if (sessionMessages != null) {
                sessionMessages.remove(sequence);
                LOGGER.debug(
                        "Removed message with sequence: {} for session: {}", sequence, currentMetadata.getSessionId());
            }
        } catch (Exception e) {
            throw new StoreException("Failed to remove message: " + sequence, e);
        }
    }

    @Override
    public List<PendingMessage> load(String sessionId) throws StoreException {
        try {
            Map<Long, PendingMessage> sessionMessages = sessionStore.get(sessionId);
            if (sessionMessages == null) {
                LOGGER.debug("No messages found for session: {}", sessionId);
                return new ArrayList<>();
            }

            List<PendingMessage> messages = new ArrayList<>(sessionMessages.values());
            LOGGER.info("Loaded {} messages for session: {}", messages.size(), sessionId);
            return messages;
        } catch (Exception e) {
            throw new StoreException("Failed to load messages for session: " + sessionId, e);
        }
    }

    @Override
    public void close(SessionMetadata metadata) throws StoreException {
        try {
            Map<Long, PendingMessage> sessionMessages = sessionStore.remove(metadata.getSessionId());
            if (sessionMessages != null) {
                LOGGER.info(
                        "Closed InMemoryPendingMessageStore for session: {}, removed {} messages",
                        metadata.getSessionId(),
                        sessionMessages.size());
            }
        } catch (Exception e) {
            throw new StoreException("Failed to close store for session: " + metadata.getSessionId(), e);
        }
    }

    @Override
    public boolean isHealthy() {
        return true; // In-memory store is always healthy
    }

    @Override
    public SessionMetadata loadMetadata(String sessionId) throws StoreException {
        // In-memory store does not persist metadata across process restarts
        // Return null to indicate no persisted metadata available
        return null;
    }

    @Override
    public void updateMetadata(SessionMetadata metadata) throws StoreException {
        // In-memory store does not persist metadata
        // Update current metadata in memory for current session
        this.currentMetadata = metadata;
        LOGGER.debug("Updated in-memory metadata for session: {}", metadata.getSessionId());
    }

    /**
     * Get the number of pending messages for a session
     */
    public int getPendingCount(String sessionId) {
        Map<Long, PendingMessage> sessionMessages = sessionStore.get(sessionId);
        return sessionMessages != null ? sessionMessages.size() : 0;
    }

    /**
     * Get the total number of sessions
     */
    public int getSessionCount() {
        return sessionStore.size();
    }

    /**
     * Clear all data (for testing)
     */
    public void clear() {
        sessionStore.clear();
        LOGGER.info("Cleared all data from InMemoryPendingMessageStore");
    }
}
