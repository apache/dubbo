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

import java.io.Serializable;
import java.util.Objects;

/**
 * Pending message for reliable streaming with persistence support
 */
public class PendingMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long sequence;
    private final byte[] message;
    private final int compressFlag;
    private final long sentTime;
    private final int retryCount;
    private final long firstSendTime;
    private final long nextRetryDelay;
    private final String sessionId;

    public PendingMessage(
            long sequence,
            byte[] message,
            int compressFlag,
            long sentTime,
            int retryCount,
            long firstSendTime,
            long nextRetryDelay,
            String sessionId) {
        this.sequence = sequence;
        this.message = Objects.requireNonNull(message, "message cannot be null");
        this.compressFlag = compressFlag;
        this.sentTime = sentTime;
        this.retryCount = retryCount;
        this.firstSendTime = firstSendTime;
        this.nextRetryDelay = nextRetryDelay;
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId cannot be null");
    }

    // Constructor for compatibility with existing code
    public PendingMessage(long sequence, byte[] message, int compressFlag, long sentTime, int retryCount) {
        this(sequence, message, compressFlag, sentTime, retryCount, sentTime, 100, "unknown");
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

    public long getFirstSendTime() {
        return firstSendTime;
    }

    public long getNextRetryDelay() {
        return nextRetryDelay;
    }

    public String getSessionId() {
        return sessionId;
    }

    public PendingMessage withRetryCount(int newRetryCount) {
        return new PendingMessage(
                sequence, message, compressFlag, sentTime, newRetryCount, firstSendTime, nextRetryDelay, sessionId);
    }

    public PendingMessage withNextRetryDelay(long newNextRetryDelay) {
        return new PendingMessage(
                sequence, message, compressFlag, sentTime, retryCount, firstSendTime, newNextRetryDelay, sessionId);
    }

    public PendingMessage withSessionId(String newSessionId) {
        return new PendingMessage(
                sequence, message, compressFlag, sentTime, retryCount, firstSendTime, nextRetryDelay, newSessionId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PendingMessage that = (PendingMessage) o;
        return sequence == that.sequence
                && compressFlag == that.compressFlag
                && sentTime == that.sentTime
                && retryCount == that.retryCount
                && firstSendTime == that.firstSendTime
                && nextRetryDelay == that.nextRetryDelay
                && Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequence, compressFlag, sentTime, retryCount, firstSendTime, nextRetryDelay, sessionId);
    }

    @Override
    public String toString() {
        return "PendingMessage{" + "sequence="
                + sequence + ", messageLength="
                + message.length + ", compressFlag="
                + compressFlag + ", sentTime="
                + sentTime + ", retryCount="
                + retryCount + ", firstSendTime="
                + firstSendTime + ", nextRetryDelay="
                + nextRetryDelay + ", sessionId='"
                + sessionId + '\'' + '}';
    }
}
