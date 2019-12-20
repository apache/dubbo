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
package org.apache.dubbo.config.bootstrap.builders;

import org.apache.dubbo.config.ConsumerConfig;

/**
 * This is a builder for build {@link ConsumerConfig}.
 *
 * @since 2.7
 */
public class ConsumerBuilder extends AbstractReferenceBuilder<ConsumerConfig, ConsumerBuilder> {

    /**
     * Whether to use the default protocol
     */
    private Boolean isDefault;

    /**
     * Networking framework client uses: netty, mina, etc.
     */
    private String client;

    /**
     * Consumer thread pool type: cached, fixed, limit, eager
     */
    private String threadpool;

    /**
     * Consumer threadpool core thread size
     */
    private Integer corethreads;

    /**
     * Consumer threadpool thread size
     */
    private Integer threads;

    /**
     * Consumer threadpool queue size
     */
    private Integer queues;

    /**
     * By default, a TCP long-connection communication is shared between the consumer process and the provider process.
     * This property can be set to share multiple TCP long-connection communications. Note that only the dubbo protocol takes effect.
     */
    private Integer shareconnections;

    public ConsumerBuilder isDefault(Boolean isDefault) {
        this.isDefault = isDefault;
        return getThis();
    }

    public ConsumerBuilder client(String client) {
        this.client = client;
        return getThis();
    }

    public ConsumerBuilder threadPool(String threadPool) {
        this.threadpool = threadPool;
        return getThis();
    }

    public ConsumerBuilder coreThreads(Integer coreThreads) {
        this.corethreads = coreThreads;
        return getThis();
    }

    public ConsumerBuilder threads(Integer threads) {
        this.threads = threads;
        return getThis();
    }

    public ConsumerBuilder queues(Integer queues) {
        this.queues = queues;
        return getThis();
    }

    public ConsumerBuilder shareConnections(Integer shareConnections) {
        this.shareconnections = shareConnections;
        return getThis();
    }

    public ConsumerConfig build() {
        ConsumerConfig consumer = new ConsumerConfig();
        super.build(consumer);

        consumer.setDefault(isDefault);
        consumer.setClient(client);
        consumer.setThreadpool(threadpool);
        consumer.setCorethreads(corethreads);
        consumer.setThreads(threads);
        consumer.setQueues(queues);
        consumer.setShareconnections(shareconnections);

        return consumer;
    }

    @Override
    protected ConsumerBuilder getThis() {
        return this;
    }
}
