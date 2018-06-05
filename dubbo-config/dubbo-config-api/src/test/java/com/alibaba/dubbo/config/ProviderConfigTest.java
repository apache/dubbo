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

package com.alibaba.dubbo.config;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class ProviderConfigTest {
    @Test
    public void testProtocol() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setProtocol("protocol");
        assertThat(provider.getProtocol().getName(), equalTo("protocol"));
    }

    @Test
    public void testDefault() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setDefault(true);
        Map<String, String> parameters = new HashMap<String, String>();
        ProviderConfig.appendParameters(parameters, provider);
        assertThat(provider.isDefault(), is(true));
        assertThat(parameters, not(hasKey("default")));
    }

    @Test
    public void testHost() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setHost("demo-host");
        Map<String, String> parameters = new HashMap<String, String>();
        ProviderConfig.appendParameters(parameters, provider);
        assertThat(provider.getHost(), equalTo("demo-host"));
        assertThat(parameters, not(hasKey("host")));
    }

    @Test
    public void testPort() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setPort(8080);
        Map<String, String> parameters = new HashMap<String, String>();
        ProviderConfig.appendParameters(parameters, provider);
        assertThat(provider.getPort(), is(8080));
        assertThat(parameters, not(hasKey("port")));
    }

    @Test
    public void testPath() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setPath("/path");
        Map<String, String> parameters = new HashMap<String, String>();
        ProviderConfig.appendParameters(parameters, provider);
        assertThat(provider.getPath(), equalTo("/path"));
        assertThat(provider.getContextpath(), equalTo("/path"));
        assertThat(parameters, not(hasKey("path")));
    }

    @Test
    public void testContextPath() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setContextpath("/context-path");
        Map<String, String> parameters = new HashMap<String, String>();
        ProviderConfig.appendParameters(parameters, provider);
        assertThat(provider.getContextpath(), equalTo("/context-path"));
        assertThat(parameters, not(hasKey("/context-path")));
    }

    @Test
    public void testThreadpool() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setThreadpool("mockthreadpool");
        assertThat(provider.getThreadpool(), equalTo("mockthreadpool"));
    }

    @Test
    public void testThreads() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setThreads(10);
        assertThat(provider.getThreads(), is(10));
    }

    @Test
    public void testIothreads() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setIothreads(10);
        assertThat(provider.getIothreads(), is(10));
    }

    @Test
    public void testQueues() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setQueues(10);
        assertThat(provider.getQueues(), is(10));
    }

    @Test
    public void testAccepts() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setAccepts(10);
        assertThat(provider.getAccepts(), is(10));
    }

    @Test
    public void testCharset() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setCharset("utf-8");
        assertThat(provider.getCharset(), equalTo("utf-8"));
    }

    @Test
    public void testPayload() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setPayload(10);
        assertThat(provider.getPayload(), is(10));
    }

    @Test
    public void testBuffer() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setBuffer(10);
        assertThat(provider.getBuffer(), is(10));
    }

    @Test
    public void testServer() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setServer("demo-server");
        assertThat(provider.getServer(), equalTo("demo-server"));
    }

    @Test
    public void testClient() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setClient("client");
        assertThat(provider.getClient(), equalTo("client"));
    }

    @Test
    public void testTelnet() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setTelnet("mocktelnethandler");
        assertThat(provider.getTelnet(), equalTo("mocktelnethandler"));
    }

    @Test
    public void testPrompt() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setPrompt("#");
        Map<String, String> parameters = new HashMap<String, String>();
        ProviderConfig.appendParameters(parameters, provider);
        assertThat(provider.getPrompt(), equalTo("#"));
        assertThat(parameters, hasEntry("prompt", "%23"));
    }

    @Test
    public void testStatus() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setStatus("mockstatuschecker");
        assertThat(provider.getStatus(), equalTo("mockstatuschecker"));
    }

    @Test
    public void testTransporter() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setTransporter("mocktransporter");
        assertThat(provider.getTransporter(), equalTo("mocktransporter"));
    }

    @Test
    public void testExchanger() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setExchanger("mockexchanger");
        assertThat(provider.getExchanger(), equalTo("mockexchanger"));
    }

    @Test
    public void testDispatcher() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setDispatcher("mockdispatcher");
        assertThat(provider.getDispatcher(), equalTo("mockdispatcher"));
    }

    @Test
    public void testNetworker() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setNetworker("networker");
        assertThat(provider.getNetworker(), equalTo("networker"));
    }

    @Test
    public void testWait() throws Exception {
        ProviderConfig provider = new ProviderConfig();
        provider.setWait(10);
        assertThat(provider.getWait(), equalTo(10));
    }
}
