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

import org.apache.dubbo.config.ProviderConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ProviderBuilderTest {

    @Test
    void setHost() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.host("host");
        Assertions.assertEquals("host", builder.build().getHost());
    }

    @Test
    void port() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.port(8080);
        Assertions.assertEquals(8080, builder.build().getPort());
    }

    @Test
    void contextPath() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.contextPath("contextpath");
        Assertions.assertEquals("contextpath", builder.build().getContextpath());
    }

    @Test
    void threadPool() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.threadPool("mockthreadpool");
        Assertions.assertEquals("mockthreadpool", builder.build().getThreadpool());
    }

    @Test
    void threads() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.threads(20);
        Assertions.assertEquals(20, builder.build().getThreads());
    }

    @Test
    void ioThreads() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.ioThreads(25);
        Assertions.assertEquals(25, builder.build().getIothreads());
    }

    @Test
    void queues() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.queues(30);
        Assertions.assertEquals(30, builder.build().getQueues());
    }

    @Test
    void accepts() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.accepts(35);
        Assertions.assertEquals(35, builder.build().getAccepts());
    }

    @Test
    void codec() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.codec("mockcodec");
        Assertions.assertEquals("mockcodec", builder.build().getCodec());
    }

    @Test
    void charset() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.charset("utf-8");
        Assertions.assertEquals("utf-8", builder.build().getCharset());
    }

    @Test
    void payload() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.payload(40);
        Assertions.assertEquals(40, builder.build().getPayload());
    }

    @Test
    void buffer() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.buffer(1024);
        Assertions.assertEquals(1024, builder.build().getBuffer());
    }

    @Test
    void transporter() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.transporter("mocktransporter");
        Assertions.assertEquals("mocktransporter", builder.build().getTransporter());
    }

    @Test
    void exchanger() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.exchanger("mockexchanger");
        Assertions.assertEquals("mockexchanger", builder.build().getExchanger());
    }

    @Test
    void dispatcher() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.dispatcher("mockdispatcher");
        Assertions.assertEquals("mockdispatcher", builder.build().getDispatcher());
    }

    @Test
    void networker() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.networker("networker");
        Assertions.assertEquals("networker", builder.build().getNetworker());
    }

    @Test
    void server() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.server("server");
        Assertions.assertEquals("server", builder.build().getServer());
    }

    @Test
    void client() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.client("client");
        Assertions.assertEquals("client", builder.build().getClient());
    }

    @Test
    void telnet() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.telnet("mocktelnethandler");
        Assertions.assertEquals("mocktelnethandler", builder.build().getTelnet());
    }

    @Test
    void prompt() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.prompt("prompt");
        Assertions.assertEquals("prompt", builder.build().getPrompt());
    }

    @Test
    void status() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.status("mockstatuschecker");
        Assertions.assertEquals("mockstatuschecker", builder.build().getStatus());
    }

    @Test
    void Wait() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.wait(Integer.valueOf(1000));
        Assertions.assertEquals(1000, builder.build().getWait());
    }

    @Test
    void isDefault() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.isDefault(true);
        Assertions.assertTrue(builder.build().isDefault());
    }

    @Test
    void build() {
        ProviderBuilder builder = new ProviderBuilder();
        builder.host("host").port(8080).contextPath("contextpath").threadPool("mockthreadpool")
                .threads(2).ioThreads(3).queues(4).accepts(5).codec("mockcodec")
                .charset("utf-8").payload(6).buffer(1024).transporter("mocktransporter").exchanger("mockexchanger")
                .dispatcher("mockdispatcher").networker("networker").server("server").client("client")
                .telnet("mocktelnethandler").prompt("prompt").status("mockstatuschecker").wait(Integer.valueOf(1000))
                .isDefault(true).id("id");

        ProviderConfig config = builder.build();
        ProviderConfig config2 = builder.build();

        Assertions.assertEquals(8080, config.getPort());
        Assertions.assertEquals(2, config.getThreads());
        Assertions.assertEquals(3, config.getIothreads());
        Assertions.assertEquals(4, config.getQueues());
        Assertions.assertEquals(5, config.getAccepts());
        Assertions.assertEquals(6, config.getPayload());
        Assertions.assertEquals(1024, config.getBuffer());
        Assertions.assertEquals(1000, config.getWait());
        Assertions.assertEquals("host", config.getHost());
        Assertions.assertEquals("contextpath", config.getContextpath());
        Assertions.assertEquals("mockthreadpool", config.getThreadpool());
        Assertions.assertEquals("mockcodec", config.getCodec());
        Assertions.assertEquals("utf-8", config.getCharset());
        Assertions.assertEquals("mocktransporter", config.getTransporter());
        Assertions.assertEquals("mockexchanger", config.getExchanger());
        Assertions.assertEquals("mockdispatcher", config.getDispatcher());
        Assertions.assertEquals("networker", config.getNetworker());
        Assertions.assertEquals("server", config.getServer());
        Assertions.assertEquals("client", config.getClient());
        Assertions.assertEquals("mocktelnethandler", config.getTelnet());
        Assertions.assertEquals("prompt", config.getPrompt());
        Assertions.assertEquals("mockstatuschecker", config.getStatus());
        Assertions.assertTrue(config.isDefault());
        Assertions.assertEquals("id", config.getId());
        Assertions.assertNotSame(config, config2);
    }
}