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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.EXPORTER_LISTENER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SERVICE_FILTER_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNull;

class AbstractServiceConfigTest {
    @Test
    void testVersion() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setVersion("version");
        assertThat(serviceConfig.getVersion(), equalTo("version"));
    }

    @Test
    void testGroup() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setGroup("group");
        assertThat(serviceConfig.getGroup(), equalTo("group"));
    }

    @Test
    void testDelay() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setDelay(1000);
        assertThat(serviceConfig.getDelay(), equalTo(1000));
    }

    @Test
    void testExport() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setExport(true);
        assertThat(serviceConfig.getExport(), is(true));
    }

    @Test
    void testWeight() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setWeight(500);
        assertThat(serviceConfig.getWeight(), equalTo(500));
    }

    @Test
    void testDocument() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setDocument("http://dubbo.apache.org");
        assertThat(serviceConfig.getDocument(), equalTo("http://dubbo.apache.org"));
        Map<String, String> parameters = new HashMap<String, String>();
        AbstractServiceConfig.appendParameters(parameters, serviceConfig);
        assertThat(parameters, hasEntry("document", "http%3A%2F%2Fdubbo.apache.org"));
    }

    @Test
    void testToken() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setToken("token");
        assertThat(serviceConfig.getToken(), equalTo("token"));
        serviceConfig.setToken((Boolean) null);
        assertThat(serviceConfig.getToken(), nullValue());
        serviceConfig.setToken(true);
        assertThat(serviceConfig.getToken(), is("true"));
    }

    @Test
    void testDeprecated() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setDeprecated(true);
        assertThat(serviceConfig.isDeprecated(), is(true));
    }

    @Test
    void testDynamic() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setDynamic(true);
        assertThat(serviceConfig.isDynamic(), is(true));
    }

    @Test
    void testProtocol() {
        ServiceConfig serviceConfig = new ServiceConfig();
        assertThat(serviceConfig.getProtocol(), nullValue());
        serviceConfig.setProtocol(new ProtocolConfig());
        assertThat(serviceConfig.getProtocol(), notNullValue());
        serviceConfig.setProtocols(new ArrayList<>(Collections.singletonList(new ProtocolConfig())));
        assertThat(serviceConfig.getProtocols(), hasSize(1));
    }

    @Test
    void testAccesslog() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setAccesslog("access.log");
        assertThat(serviceConfig.getAccesslog(), equalTo("access.log"));
        serviceConfig.setAccesslog((Boolean) null);
        assertThat(serviceConfig.getAccesslog(), nullValue());
        serviceConfig.setAccesslog(true);
        assertThat(serviceConfig.getAccesslog(), equalTo("true"));
    }

    @Test
    void testExecutes() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setExecutes(10);
        assertThat(serviceConfig.getExecutes(), equalTo(10));
    }

    @Test
    void testFilter() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setFilter("mockfilter");
        assertThat(serviceConfig.getFilter(), equalTo("mockfilter"));
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(SERVICE_FILTER_KEY, "prefilter");
        AbstractServiceConfig.appendParameters(parameters, serviceConfig);
        assertThat(parameters, hasEntry(SERVICE_FILTER_KEY, "prefilter,mockfilter"));
    }

    @Test
    void testListener() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setListener("mockexporterlistener");
        assertThat(serviceConfig.getListener(), equalTo("mockexporterlistener"));
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(EXPORTER_LISTENER_KEY, "prelistener");
        AbstractServiceConfig.appendParameters(parameters, serviceConfig);
        assertThat(parameters, hasEntry(EXPORTER_LISTENER_KEY, "prelistener,mockexporterlistener"));
    }

    @Test
    void testRegister() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setRegister(true);
        assertThat(serviceConfig.isRegister(), is(true));
    }

    @Test
    void testWarmup() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setWarmup(100);
        assertThat(serviceConfig.getWarmup(), equalTo(100));
    }

    @Test
    void testSerialization() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setSerialization("serialization");
        assertThat(serviceConfig.getSerialization(), equalTo("serialization"));
    }

    @Test
    void testPreferSerialization() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setPreferSerialization("preferSerialization");
        assertThat(serviceConfig.getPreferSerialization(), equalTo("preferSerialization"));
    }

    @Test
    void testPreferSerializationDefault1() {
        ServiceConfig serviceConfig = new ServiceConfig();
        assertNull(serviceConfig.getPreferSerialization());

        serviceConfig.checkDefault();
        assertNull(serviceConfig.getPreferSerialization());

        serviceConfig = new ServiceConfig();
        serviceConfig.setSerialization("x-serialization");
        assertNull(serviceConfig.getPreferSerialization());

        serviceConfig.checkDefault();
        assertThat(serviceConfig.getPreferSerialization(), equalTo("x-serialization"));
    }

    @Test
    void testPreferSerializationDefault2() {
        ServiceConfig serviceConfig = new ServiceConfig();
        assertNull(serviceConfig.getPreferSerialization());

        serviceConfig.refresh();
        assertNull(serviceConfig.getPreferSerialization());

        serviceConfig = new ServiceConfig();
        serviceConfig.setSerialization("x-serialization");
        assertNull(serviceConfig.getPreferSerialization());

        serviceConfig.refresh();
        assertThat(serviceConfig.getPreferSerialization(), equalTo("x-serialization"));
    }


    private static class ServiceConfig extends AbstractServiceConfig {

    }
}
