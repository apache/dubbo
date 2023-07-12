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

import org.apache.dubbo.config.ProviderConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

class ProviderConfigTest {

    @Test
    void testProtocol() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setProtocol("protocol");
        MatcherAssert.assertThat(provider.getProtocol().getName(), Matchers.equalTo("protocol"));
    }

    @Test
    void testDefault() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setDefault(true);
        Map<String, String> parameters = new HashMap<String, String>();
        ProviderConfig.appendParameters(parameters, provider);
        MatcherAssert.assertThat(provider.isDefault(), Matchers.is(true));
        MatcherAssert.assertThat(parameters, Matchers.not(Matchers.hasKey("default")));
    }

    @Test
    void testHost() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setHost("demo-host");
        Map<String, String> parameters = new HashMap<String, String>();
        ProviderConfig.appendParameters(parameters, provider);
        MatcherAssert.assertThat(provider.getHost(), Matchers.equalTo("demo-host"));
        MatcherAssert.assertThat(parameters, Matchers.not(Matchers.hasKey("host")));
    }

    @Test
    void testPort() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setPort(8080);
        Map<String, String> parameters = new HashMap<String, String>();
        ProviderConfig.appendParameters(parameters, provider);
        MatcherAssert.assertThat(provider.getPort(), Matchers.is(8080));
        MatcherAssert.assertThat(parameters, Matchers.not(Matchers.hasKey("port")));
    }

    @Test
    void testPath() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setPath("/path");
        Map<String, String> parameters = new HashMap<String, String>();
        ProviderConfig.appendParameters(parameters, provider);
        MatcherAssert.assertThat(provider.getPath(), Matchers.equalTo("/path"));
        MatcherAssert.assertThat(provider.getContextpath(), Matchers.equalTo("/path"));
        MatcherAssert.assertThat(parameters, Matchers.not(Matchers.hasKey("path")));
    }

    @Test
    void testContextPath() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setContextpath("/context-path");
        Map<String, String> parameters = new HashMap<String, String>();
        ProviderConfig.appendParameters(parameters, provider);
        MatcherAssert.assertThat(provider.getContextpath(), Matchers.equalTo("/context-path"));
        MatcherAssert.assertThat(parameters, Matchers.not(Matchers.hasKey("/context-path")));
    }

    @Test
    void testThreadpool() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setThreadpool("mockthreadpool");
        MatcherAssert.assertThat(provider.getThreadpool(), Matchers.equalTo("mockthreadpool"));
    }

    @Test
    void testThreads() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setThreads(10);
        MatcherAssert.assertThat(provider.getThreads(), Matchers.is(10));
    }

    @Test
    void testIothreads() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setIothreads(10);
        MatcherAssert.assertThat(provider.getIothreads(), Matchers.is(10));
    }

    @Test
    void testQueues() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setQueues(10);
        MatcherAssert.assertThat(provider.getQueues(), Matchers.is(10));
    }

    @Test
    void testAccepts() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setAccepts(10);
        MatcherAssert.assertThat(provider.getAccepts(), Matchers.is(10));
    }

    @Test
    void testCharset() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setCharset("utf-8");
        MatcherAssert.assertThat(provider.getCharset(), Matchers.equalTo("utf-8"));
    }

    @Test
    void testPayload() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setPayload(10);
        MatcherAssert.assertThat(provider.getPayload(), Matchers.is(10));
    }

    @Test
    void testBuffer() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setBuffer(10);
        MatcherAssert.assertThat(provider.getBuffer(), Matchers.is(10));
    }

    @Test
    void testServer() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setServer("demo-server");
        MatcherAssert.assertThat(provider.getServer(), Matchers.equalTo("demo-server"));
    }

    @Test
    void testClient() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setClient("client");
        MatcherAssert.assertThat(provider.getClient(), Matchers.equalTo("client"));
    }

    @Test
    void testTelnet() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setTelnet("mocktelnethandler");
        MatcherAssert.assertThat(provider.getTelnet(), Matchers.equalTo("mocktelnethandler"));
    }

    @Test
    void testPrompt() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setPrompt("#");
        Map<String, String> parameters = new HashMap<String, String>();
        ProviderConfig.appendParameters(parameters, provider);
        MatcherAssert.assertThat(provider.getPrompt(), Matchers.equalTo("#"));
        MatcherAssert.assertThat(parameters, Matchers.hasEntry("prompt", "%23"));
    }

    @Test
    void testStatus() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setStatus("mockstatuschecker");
        MatcherAssert.assertThat(provider.getStatus(), Matchers.equalTo("mockstatuschecker"));
    }

    @Test
    void testTransporter() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setTransporter("mocktransporter");
        MatcherAssert.assertThat(provider.getTransporter(), Matchers.equalTo("mocktransporter"));
    }

    @Test
    void testExchanger() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setExchanger("mockexchanger");
        MatcherAssert.assertThat(provider.getExchanger(), Matchers.equalTo("mockexchanger"));
    }

    @Test
    void testDispatcher() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setDispatcher("mockdispatcher");
        MatcherAssert.assertThat(provider.getDispatcher(), Matchers.equalTo("mockdispatcher"));
    }

    @Test
    void testNetworker() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setNetworker("networker");
        MatcherAssert.assertThat(provider.getNetworker(), Matchers.equalTo("networker"));
    }

    @Test
    void testWait() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setWait(10);
        MatcherAssert.assertThat(provider.getWait(), Matchers.equalTo(10));
    }

    @Test
    void testMetaData() {
        ProviderConfig config = new ProviderConfig();
        Map<String, String> metaData = config.getMetaData();
        Assertions.assertEquals(0, metaData.size(), "Expect empty metadata but found: "+metaData);
    }
}
