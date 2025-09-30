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

public class ReliabilityConfig {
    private final boolean enabled;
    private final int heartbeatInterval;
    private final int retryTimeout;
    private final int maxRetries;
    private final int sessionTimeout;
    private final int maxMissedHeartbeats;

    // 智能重试策略参数
    private final int initialRetryDelay; // 初始重试延迟 (ms)
    private final int maxRetryDelay; // 最大重试延迟 (ms)
    private final double backoffMultiplier; // 退避倍数
    private final int maxInFlightMessages; // 最大 InFlight 消息数
    private final int totalRetryTimeout; // 总重试超时时间 (ms)

    // 持久化存储配置
    private final String storeType; // 存储类型: memory, file, redis
    private final String storePath; // 存储路径
    private final long storeMaxFileSize; // 存储文件最大大小 (bytes)
    private final long storeRetentionTime; // 存储保留时间 (ms)
    private final int storeSyncInterval; // 存储同步间隔 (ms)

    private ReliabilityConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.heartbeatInterval = builder.heartbeatInterval;
        this.retryTimeout = builder.retryTimeout;
        this.maxRetries = builder.maxRetries;
        this.sessionTimeout = builder.sessionTimeout;
        this.maxMissedHeartbeats = builder.maxMissedHeartbeats;
        this.initialRetryDelay = builder.initialRetryDelay;
        this.maxRetryDelay = builder.maxRetryDelay;
        this.backoffMultiplier = builder.backoffMultiplier;
        this.maxInFlightMessages = builder.maxInFlightMessages;
        this.totalRetryTimeout = builder.totalRetryTimeout;
        this.storeType = builder.storeType;
        this.storePath = builder.storePath;
        this.storeMaxFileSize = builder.storeMaxFileSize;
        this.storeRetentionTime = builder.storeRetentionTime;
        this.storeSyncInterval = builder.storeSyncInterval;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public int getRetryTimeout() {
        return retryTimeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public int getMaxMissedHeartbeats() {
        return maxMissedHeartbeats;
    }

    // 智能重试策略 getter 方法
    public int getInitialRetryDelay() {
        return initialRetryDelay;
    }

    public int getMaxRetryDelay() {
        return maxRetryDelay;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public int getMaxInFlightMessages() {
        return maxInFlightMessages;
    }

    public int getTotalRetryTimeout() {
        return totalRetryTimeout;
    }

    // 持久化存储配置 getter 方法
    public String getStoreType() {
        return storeType;
    }

    public String getStorePath() {
        return storePath;
    }

    public long getStoreMaxFileSize() {
        return storeMaxFileSize;
    }

    public long getStoreRetentionTime() {
        return storeRetentionTime;
    }

    public int getStoreSyncInterval() {
        return storeSyncInterval;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean enabled = false;
        private int heartbeatInterval = 5000;
        private int retryTimeout = 10000;
        private int maxRetries = 3;
        private int sessionTimeout = 30000;
        private int maxMissedHeartbeats = 3;

        // 智能重试策略默认值
        private int initialRetryDelay = 100; // 初始重试延迟 100ms
        private int maxRetryDelay = 5000; // 最大重试延迟 5s
        private double backoffMultiplier = 2.0; // 退避倍数 2.0
        private int maxInFlightMessages = 100; // 最大 InFlight 消息数 100
        private int totalRetryTimeout = 30000; // 总重试超时 30s

        // 持久化存储配置默认值
        private String storeType = "memory"; // 默认内存存储
        private String storePath = "/tmp/dubbo/reliable"; // 默认存储路径
        private long storeMaxFileSize = 100 * 1024 * 1024; // 默认 100MB
        private long storeRetentionTime = 24 * 60 * 60 * 1000; // 默认 24小时
        private int storeSyncInterval = 1000; // 默认 1s同步间隔

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder heartbeatInterval(int interval) {
            this.heartbeatInterval = interval;
            return this;
        }

        public Builder retryTimeout(int timeout) {
            this.retryTimeout = timeout;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder sessionTimeout(int sessionTimeout) {
            this.sessionTimeout = sessionTimeout;
            return this;
        }

        public Builder maxMissedHeartbeats(int maxMissedHeartbeats) {
            this.maxMissedHeartbeats = maxMissedHeartbeats;
            return this;
        }

        // 智能重试策略 Builder 方法
        public Builder initialRetryDelay(int initialRetryDelay) {
            this.initialRetryDelay = initialRetryDelay;
            return this;
        }

        public Builder maxRetryDelay(int maxRetryDelay) {
            this.maxRetryDelay = maxRetryDelay;
            return this;
        }

        public Builder backoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }

        public Builder maxInFlightMessages(int maxInFlightMessages) {
            this.maxInFlightMessages = maxInFlightMessages;
            return this;
        }

        public Builder totalRetryTimeout(int totalRetryTimeout) {
            this.totalRetryTimeout = totalRetryTimeout;
            return this;
        }

        // 持久化存储配置 Builder 方法
        public Builder storeType(String storeType) {
            this.storeType = storeType;
            return this;
        }

        public Builder storePath(String storePath) {
            this.storePath = storePath;
            return this;
        }

        public Builder storeMaxFileSize(long storeMaxFileSize) {
            this.storeMaxFileSize = storeMaxFileSize;
            return this;
        }

        public Builder storeRetentionTime(long storeRetentionTime) {
            this.storeRetentionTime = storeRetentionTime;
            return this;
        }

        public Builder storeSyncInterval(int storeSyncInterval) {
            this.storeSyncInterval = storeSyncInterval;
            return this;
        }

        public ReliabilityConfig build() {
            return new ReliabilityConfig(this);
        }
    }
}
