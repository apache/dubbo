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

import org.apache.dubbo.config.ProtocolConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class ProtocolBuilderTest {

    @Test
    void name() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.name("name");
        Assertions.assertEquals("name", builder.build().getName());
    }

    @Test
    void host() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.host("host");
        Assertions.assertEquals("host", builder.build().getHost());
    }

    @Test
    void port() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.port(8080);
        Assertions.assertEquals(8080, builder.build().getPort());
    }

    @Test
    void contextpath() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.contextpath("contextpath");
        Assertions.assertEquals("contextpath", builder.build().getContextpath());
    }

    @Test
    void path() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.path("path");
        Assertions.assertEquals("path", builder.build().getPath());
    }

    @Test
    void threadpool() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.threadpool("mockthreadpool");
        Assertions.assertEquals("mockthreadpool", builder.build().getThreadpool());
    }

    @Test
    void corethreads() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.corethreads(10);
        Assertions.assertEquals(10, builder.build().getCorethreads());
    }

    @Test
    void threads() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.threads(20);
        Assertions.assertEquals(20, builder.build().getThreads());
    }

    @Test
    void iothreads() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.iothreads(25);
        Assertions.assertEquals(25, builder.build().getIothreads());
    }

    @Test
    void queues() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.queues(30);
        Assertions.assertEquals(30, builder.build().getQueues());
    }

    @Test
    void accepts() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.accepts(35);
        Assertions.assertEquals(35, builder.build().getAccepts());
    }

    @Test
    void codec() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.codec("mockcodec");
        Assertions.assertEquals("mockcodec", builder.build().getCodec());
    }

    @Test
    void serialization() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.serialization("serialization");
        Assertions.assertEquals("serialization", builder.build().getSerialization());
    }

    @Test
    void charset() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.charset("utf-8");
        Assertions.assertEquals("utf-8", builder.build().getCharset());
    }

    @Test
    void payload() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.payload(40);
        Assertions.assertEquals(40, builder.build().getPayload());
    }

    @Test
    void buffer() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.buffer(1024);
        Assertions.assertEquals(1024, builder.build().getBuffer());
    }

    @Test
    void heartbeat() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.heartbeat(1000);
        Assertions.assertEquals(1000, builder.build().getHeartbeat());
    }

    @Test
    void accesslog() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.accesslog("accesslog");
        Assertions.assertEquals("accesslog", builder.build().getAccesslog());
    }

    @Test
    void transporter() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.transporter("mocktransporter");
        Assertions.assertEquals("mocktransporter", builder.build().getTransporter());
    }

    @Test
    void exchanger() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.exchanger("mockexchanger");
        Assertions.assertEquals("mockexchanger", builder.build().getExchanger());
    }

    @Test
    void dispatcher() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.dispatcher("mockdispatcher");
        Assertions.assertEquals("mockdispatcher", builder.build().getDispatcher());
    }

    @Test
    void dispather() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.dispather("mockdispatcher");
        Assertions.assertEquals("mockdispatcher", builder.build().getDispather());
    }

    @Test
    void networker() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.networker("networker");
        Assertions.assertEquals("networker", builder.build().getNetworker());
    }

    @Test
    void server() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.server("server");
        Assertions.assertEquals("server", builder.build().getServer());
    }

    @Test
    void client() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.client("client");
        Assertions.assertEquals("client", builder.build().getClient());
    }

    @Test
    void telnet() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.telnet("mocktelnethandler");
        Assertions.assertEquals("mocktelnethandler", builder.build().getTelnet());
    }

    @Test
    void prompt() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.prompt("prompt");
        Assertions.assertEquals("prompt", builder.build().getPrompt());
    }

    @Test
    void status() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.status("mockstatuschecker");
        Assertions.assertEquals("mockstatuschecker", builder.build().getStatus());
    }

    @Test
    void register() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.register(true);
        Assertions.assertTrue(builder.build().isRegister());
    }

    @Test
    void keepAlive() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.keepAlive(true);
        Assertions.assertTrue(builder.build().getKeepAlive());
    }

    @Test
    void optimizer() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.optimizer("optimizer");
        Assertions.assertEquals("optimizer", builder.build().getOptimizer());
    }

    @Test
    void extension() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.extension("extension");
        Assertions.assertEquals("extension", builder.build().getExtension());
    }

    @Test
    void appendParameter() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.appendParameter("default.num", "one").appendParameter("num", "ONE");

        Map<String, String> parameters = builder.build().getParameters();

        Assertions.assertTrue(parameters.containsKey("default.num"));
        Assertions.assertEquals("ONE", parameters.get("num"));
    }

    @Test
    void appendParameters() {
        Map<String, String> source = new HashMap<>();
        source.put("default.num", "one");
        source.put("num", "ONE");

        ProtocolBuilder builder = new ProtocolBuilder();
        builder.appendParameters(source);

        Map<String, String> parameters = builder.build().getParameters();

        Assertions.assertTrue(parameters.containsKey("default.num"));
        Assertions.assertEquals("ONE", parameters.get("num"));
    }

    @Test
    void isDefault() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.isDefault(true);
        Assertions.assertTrue(builder.build().isDefault());
    }

    @Test
    void build() {
        ProtocolBuilder builder = new ProtocolBuilder();
        builder.name("name").host("host").port(8080).contextpath("contextpath").threadpool("mockthreadpool")
                .corethreads(1).threads(2).iothreads(3).queues(4).accepts(5).codec("mockcodec")
                .serialization("serialization").charset("utf-8").payload(6).buffer(1024).heartbeat(1000)
                .accesslog("accesslog").transporter("mocktransporter").exchanger("mockexchanger")
                .dispatcher("mockdispatcher").networker("networker").server("server").client("client")
                .telnet("mocktelnethandler").prompt("prompt").status("mockstatuschecker").register(true).keepAlive(false)
                .optimizer("optimizer").extension("extension").isDefault(true)
                .appendParameter("default.num", "one").id("id");

        ProtocolConfig config = builder.build();
        ProtocolConfig config2 = builder.build();

        Assertions.assertEquals(8080, config.getPort());
        Assertions.assertEquals(1, config.getCorethreads());
        Assertions.assertEquals(2, config.getThreads());
        Assertions.assertEquals(3, config.getIothreads());
        Assertions.assertEquals(4, config.getQueues());
        Assertions.assertEquals(5, config.getAccepts());
        Assertions.assertEquals(6, config.getPayload());
        Assertions.assertEquals(1024, config.getBuffer());
        Assertions.assertEquals(1000, config.getHeartbeat());
        Assertions.assertEquals("name", config.getName());
        Assertions.assertEquals("host", config.getHost());
        Assertions.assertEquals("contextpath", config.getContextpath());
        Assertions.assertEquals("mockthreadpool", config.getThreadpool());
        Assertions.assertEquals("mockcodec", config.getCodec());
        Assertions.assertEquals("serialization", config.getSerialization());
        Assertions.assertEquals("utf-8", config.getCharset());
        Assertions.assertEquals("accesslog", config.getAccesslog());
        Assertions.assertEquals("mocktransporter", config.getTransporter());
        Assertions.assertEquals("mockexchanger", config.getExchanger());
        Assertions.assertEquals("mockdispatcher", config.getDispatcher());
        Assertions.assertEquals("networker", config.getNetworker());
        Assertions.assertEquals("server", config.getServer());
        Assertions.assertEquals("client", config.getClient());
        Assertions.assertEquals("mocktelnethandler", config.getTelnet());
        Assertions.assertEquals("prompt", config.getPrompt());
        Assertions.assertEquals("mockstatuschecker", config.getStatus());
        Assertions.assertEquals("optimizer", config.getOptimizer());
        Assertions.assertEquals("extension", config.getExtension());
        Assertions.assertTrue(config.isRegister());
        Assertions.assertFalse(config.getKeepAlive());
        Assertions.assertTrue(config.isDefault());
        Assertions.assertTrue(config.getParameters().containsKey("default.num"));
        Assertions.assertEquals("one", config.getParameters().get("default.num"));
        Assertions.assertEquals("id", config.getId());
        Assertions.assertNotSame(config, config2);
    }
}