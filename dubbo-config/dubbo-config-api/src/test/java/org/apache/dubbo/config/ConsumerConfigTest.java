/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.config;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ConsumerConfigTest {
    @Test
    public void testTimeout() throws Exception {
        try {
            System.clearProperty("sun.rmi.transport.tcp.responseTimeout");
            ConsumerConfig consumer = new ConsumerConfig();
            consumer.setTimeout(10);
            assertThat(consumer.getTimeout(), is(10));
            assertThat(System.getProperty("sun.rmi.transport.tcp.responseTimeout"), equalTo("10"));
        } finally {
            System.clearProperty("sun.rmi.transport.tcp.responseTimeout");
        }
    }

    @Test
    public void testDefault() throws Exception {
        ConsumerConfig consumer = new ConsumerConfig();
        consumer.setDefault(true);
        assertThat(consumer.isDefault(), is(true));
    }

    @Test
    public void testClient() throws Exception {
        ConsumerConfig consumer = new ConsumerConfig();
        consumer.setClient("client");
        assertThat(consumer.getClient(), equalTo("client"));
    }

    @Test
    public void testThreadpool() throws Exception {
        ConsumerConfig consumer = new ConsumerConfig();
        consumer.setThreadpool("fixed");
        assertThat(consumer.getThreadpool(), equalTo("fixed"));
    }

    @Test
    public void testCorethreads() throws Exception {
        ConsumerConfig consumer = new ConsumerConfig();
        consumer.setCorethreads(10);
        assertThat(consumer.getCorethreads(), equalTo(10));
    }

    @Test
    public void testThreads() throws Exception {
        ConsumerConfig consumer = new ConsumerConfig();
        consumer.setThreads(20);
        assertThat(consumer.getThreads(), equalTo(20));
    }

    @Test
    public void testQueues() throws Exception {
        ConsumerConfig consumer = new ConsumerConfig();
        consumer.setQueues(5);
        assertThat(consumer.getQueues(), equalTo(5));
    }
}
