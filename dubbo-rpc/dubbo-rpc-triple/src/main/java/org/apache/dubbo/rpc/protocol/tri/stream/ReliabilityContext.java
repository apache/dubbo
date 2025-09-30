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

import java.util.Map;
import java.util.function.Consumer;

/**
 * ReliabilityContext provides business observability APIs for reliable streaming.
 * This interface allows business logic to monitor and control reliability aspects
 * of the streaming communication.
 */
public interface ReliabilityContext {

    /**
     * Get the current reliability state of the stream.
     *
     * @return current state string (e.g., "HEALTHY", "RECONNECTING", "FAILED")
     */
    String getState();

    /**
     * Get the last acknowledged sequence number.
     *
     * @return last acknowledged sequence number, or -1 if reliability is not enabled
     */
    long getLastAckedSeq();

    /**
     * Get the current pending message count.
     *
     * @return number of messages waiting for acknowledgement
     */
    int getPendingCount();

    /**
     * Get the current InFlight message count.
     *
     * @return number of messages currently in flight
     */
    int getInFlightCount();

    /**
     * Get the session ID for this reliable stream.
     *
     * @return session ID, or null if reliability is not enabled
     */
    String getSessionId();

    /**
     * Get the total number of messages sent.
     *
     * @return total sent message count
     */
    long getTotalSentCount();

    /**
     * Get the total number of retries attempted.
     *
     * @return total retry count
     */
    long getTotalRetryCount();

    /**
     * Get the connection status.
     *
     * @return true if connection is active, false otherwise
     */
    boolean isConnectionActive();

    /**
     * Get reliability statistics as a map.
     *
     * @return map containing all reliability metrics
     */
    Map<String, Object> getStatistics();

    /**
     * Register a callback for reliability state changes.
     *
     * @param callback consumer that receives state change events
     */
    void onStateChange(Consumer<String> callback);

    /**
     * Register a callback for connection recovery events.
     *
     * @param callback consumer that receives recovery events
     */
    void onRecovery(Consumer<String> callback);

    /**
     * Register a callback for retry events.
     *
     * @param callback consumer that receives retry events with sequence number
     */
    void onRetry(Consumer<Long> callback);

    /**
     * Manually trigger a retry for a specific message.
     *
     * @param sequence the sequence number to retry
     * @return true if retry was triggered, false if message not found or not retryable
     */
    boolean retryMessage(long sequence);

    /**
     * Manually trigger connection recovery.
     *
     * @return true if recovery was triggered, false if recovery is already in progress
     */
    boolean triggerRecovery();

    /**
     * Set the maximum InFlight message limit dynamically.
     *
     * @param limit the new InFlight limit
     */
    void setInFlightLimit(int limit);

    /**
     * Get the current retry configuration.
     *
     * @return ReliabilityConfig instance
     */
    ReliabilityConfig getRetryConfig();
}
