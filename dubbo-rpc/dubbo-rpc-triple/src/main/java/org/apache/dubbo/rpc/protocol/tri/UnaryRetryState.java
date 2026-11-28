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
package org.apache.dubbo.rpc.protocol.tri;

/** A bounded immutable unary request shared by attempts; response commitment belongs to each attempt. */
public final class UnaryRetryState implements AutoCloseable {
    public static final String ATTRIBUTE = "dubbo.triple.unary-retry-attempt";
    private static final int MAX_REQUEST_BYTES = 1024 * 1024;
    private byte[] request;
    private boolean replayable = true;
    private boolean closed;

    public synchronized byte[] getRequest() {
        return request;
    }

    public synchronized void remember(byte[] bytes) {
        if (closed) {
            return;
        }
        if (bytes.length > MAX_REQUEST_BYTES) {
            replayable = false;
        } else {
            request = bytes;
        }
    }

    public synchronized boolean isReplayable() {
        return replayable && !closed;
    }

    @Override
    public synchronized void close() {
        closed = true;
        request = null;
    }

    public static final class Attempt {
        private final UnaryRetryState state;
        private volatile boolean committed;
        private volatile String pushback;

        public Attempt(UnaryRetryState state) {
            this.state = state;
        }

        public UnaryRetryState getState() {
            return state;
        }

        public void commit() {
            committed = true;
        }

        public boolean isCommitted() {
            return committed;
        }

        public String getPushback() {
            return pushback;
        }

        public void setPushback(Object value) {
            pushback = value == null ? null : value.toString();
        }
    }
}
