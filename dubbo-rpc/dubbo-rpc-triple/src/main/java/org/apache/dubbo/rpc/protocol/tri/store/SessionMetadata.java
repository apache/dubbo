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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.protocol.tri.stream.ReliabilityConfig;

import java.util.Objects;

/**
 * Metadata for a reliable streaming session.
 * Includes critical state information for process restart recovery.
 */
public class SessionMetadata {

    private final String sessionId;
    private final String serviceKey;
    private final long createTime;
    private final URL url;
    private final ReliabilityConfig config;

    // Critical state for recovery after process restart
    private final long lastAckedSeq; // Last acknowledged sequence number
    private final long currentSeq; // Current sequence number
    private final long lastUpdateTime; // Last update timestamp

    /**
     * Constructor for new session (no recovery)
     */
    public SessionMetadata(String sessionId, String serviceKey, long createTime, URL url, ReliabilityConfig config) {
        this(sessionId, serviceKey, createTime, url, config, 0L, 0L, System.currentTimeMillis());
    }

    /**
     * Constructor for session with state (for recovery)
     */
    public SessionMetadata(
            String sessionId,
            String serviceKey,
            long createTime,
            URL url,
            ReliabilityConfig config,
            long lastAckedSeq,
            long currentSeq,
            long lastUpdateTime) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId cannot be null");
        this.serviceKey = Objects.requireNonNull(serviceKey, "serviceKey cannot be null");
        this.createTime = createTime;
        this.url = Objects.requireNonNull(url, "url cannot be null");
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.lastAckedSeq = lastAckedSeq;
        this.currentSeq = currentSeq;
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public long getCreateTime() {
        return createTime;
    }

    public URL getUrl() {
        return url;
    }

    public ReliabilityConfig getConfig() {
        return config;
    }

    public long getLastAckedSeq() {
        return lastAckedSeq;
    }

    public long getCurrentSeq() {
        return currentSeq;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    /**
     * Create a new SessionMetadata with updated state
     */
    public SessionMetadata withUpdatedState(long newLastAckedSeq, long newCurrentSeq) {
        return new SessionMetadata(
                sessionId,
                serviceKey,
                createTime,
                url,
                config,
                newLastAckedSeq,
                newCurrentSeq,
                System.currentTimeMillis());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionMetadata that = (SessionMetadata) o;
        return createTime == that.createTime
                && lastAckedSeq == that.lastAckedSeq
                && currentSeq == that.currentSeq
                && lastUpdateTime == that.lastUpdateTime
                && Objects.equals(sessionId, that.sessionId)
                && Objects.equals(serviceKey, that.serviceKey)
                && Objects.equals(url, that.url)
                && Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, serviceKey, createTime, url, config, lastAckedSeq, currentSeq, lastUpdateTime);
    }

    @Override
    public String toString() {
        return "SessionMetadata{" + "sessionId='"
                + sessionId + '\'' + ", serviceKey='"
                + serviceKey + '\'' + ", createTime="
                + createTime + ", lastAckedSeq="
                + lastAckedSeq + ", currentSeq="
                + currentSeq + ", lastUpdateTime="
                + lastUpdateTime + ", url="
                + url + ", config="
                + config + '}';
    }
}
