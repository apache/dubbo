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
package org.apache.dubbo.config.builders;

import org.apache.dubbo.config.ConsumerConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ConsumerBuilderTest {

    @Test
    void isDefault() {
        ConsumerBuilder builder = new ConsumerBuilder();
        builder.isDefault(false);
        Assertions.assertFalse(builder.build().isDefault());
    }

    @Test
    void client() {
        ConsumerBuilder builder = new ConsumerBuilder();
        builder.client("client");
        Assertions.assertEquals("client", builder.build().getClient());
    }

    @Test
    void threadPool() {
        ConsumerBuilder builder = new ConsumerBuilder();
        builder.threadPool("threadPool");
        Assertions.assertEquals("threadPool", builder.build().getThreadpool());
    }

    @Test
    void coreThreads() {
        ConsumerBuilder builder = new ConsumerBuilder();
        builder.coreThreads(10);
        Assertions.assertEquals(10, builder.build().getCorethreads());
    }

    @Test
    void threads() {
        ConsumerBuilder builder = new ConsumerBuilder();
        builder.threads(100);
        Assertions.assertEquals(100, builder.build().getThreads());
    }

    @Test
    void queues() {
        ConsumerBuilder builder = new ConsumerBuilder();
        builder.queues(200);
        Assertions.assertEquals(200, builder.build().getQueues());
    }

    @Test
    void shareConnections() {
        ConsumerBuilder builder = new ConsumerBuilder();
        builder.shareConnections(300);
        Assertions.assertEquals(300, builder.build().getShareconnections());
    }

    @Test
    void build() {
        ConsumerBuilder builder = new ConsumerBuilder();
        builder.isDefault(true).client("client").threadPool("threadPool").coreThreads(10).threads(100).queues(200)
                .shareConnections(300).id("id").prefix("prefix");

        ConsumerConfig config = builder.build();
        ConsumerConfig config2 = builder.build();

        Assertions.assertTrue(config.isDefault());
        Assertions.assertEquals("client", config.getClient());
        Assertions.assertEquals("threadPool", config.getThreadpool());
        Assertions.assertEquals("id", config.getId());
        Assertions.assertEquals("prefix", config.getPrefix());
        Assertions.assertEquals(10, config.getCorethreads());
        Assertions.assertEquals(100, config.getThreads());
        Assertions.assertEquals(200, config.getQueues());
        Assertions.assertEquals(300, config.getShareconnections());
        Assertions.assertNotSame(config, config2);
    }
}