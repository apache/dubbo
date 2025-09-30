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

import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;

/**
 * SPI interface for pending message store in reliable streaming.
 * Provides persistence and recovery capabilities for unacknowledged messages.
 */
@SPI(scope = ExtensionScope.FRAMEWORK)
public interface PendingMessageStore {

    /**
     * Initialize the store for a session
     *
     * @param metadata session metadata
     * @throws StoreException if initialization fails
     */
    void init(SessionMetadata metadata) throws StoreException;

    /**
     * Store a pending message
     *
     * @param message the pending message to store
     * @throws StoreException if storage fails
     */
    void put(PendingMessage message) throws StoreException;

    /**
     * Acknowledge a message by sequence number
     *
     * @param sequence the sequence number to acknowledge
     * @throws StoreException if ack operation fails
     */
    void ack(long sequence) throws StoreException;

    /**
     * Remove a message by sequence number
     *
     * @param sequence the sequence number to remove
     * @throws StoreException if removal fails
     */
    void remove(long sequence) throws StoreException;

    /**
     * Load all pending messages for a session
     *
     * @param sessionId the session id
     * @return list of pending messages, empty list if none found
     * @throws StoreException if loading fails
     */
    java.util.List<PendingMessage> load(String sessionId) throws StoreException;

    /**
     * Close the store for a session
     *
     * @param metadata session metadata
     * @throws StoreException if close fails
     */
    void close(SessionMetadata metadata) throws StoreException;

    /**
     * Check if the store is healthy
     *
     * @return true if healthy, false otherwise
     */
    boolean isHealthy();

    /**
     * Load session metadata for recovery after process restart.
     * Returns null if no metadata found for the session.
     *
     * @param sessionId the session id
     * @return session metadata, or null if not found
     * @throws StoreException if loading fails
     */
    SessionMetadata loadMetadata(String sessionId) throws StoreException;

    /**
     * Update session metadata (lastAckedSeq, currentSeq, etc.)
     * This is called periodically to persist critical state for process restart recovery.
     *
     * @param metadata the updated session metadata
     * @throws StoreException if update fails
     */
    void updateMetadata(SessionMetadata metadata) throws StoreException;
}
