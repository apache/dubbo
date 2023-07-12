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


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.hamcrest.MatcherAssert;
import org.testcontainers.shaded.org.hamcrest.Matchers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.EXPORTER_LISTENER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SERVICE_FILTER_KEY;

class AbstractServiceConfigTest {
    @Test
    void testVersion() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setVersion("version");
        MatcherAssert.assertThat(serviceConfig.getVersion(), Matchers.equalTo("version"));
    }

    @Test
    void testGroup() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setGroup("group");
        MatcherAssert.assertThat(serviceConfig.getGroup(), Matchers.equalTo("group"));
    }

    @Test
    void testDelay() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setDelay(1000);
        MatcherAssert.assertThat(serviceConfig.getDelay(), Matchers.equalTo(1000));
    }

    @Test
    void testExport() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setExport(true);
        MatcherAssert.assertThat(serviceConfig.getExport(), Matchers.is(true));
    }

    @Test
    void testWeight() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setWeight(500);
        MatcherAssert.assertThat(serviceConfig.getWeight(), Matchers.equalTo(500));
    }

    @Test
    void testDocument() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setDocument("http://dubbo.apache.org");
        MatcherAssert.assertThat(serviceConfig.getDocument(), Matchers.equalTo("http://dubbo.apache.org"));
        Map<String, String> parameters = new HashMap<String, String>();
        AbstractServiceConfig.appendParameters(parameters, serviceConfig);
        MatcherAssert.assertThat(parameters, Matchers.hasEntry("document", "http%3A%2F%2Fdubbo.apache.org"));
    }

    @Test
    void testToken() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setToken("token");
        MatcherAssert.assertThat(serviceConfig.getToken(), Matchers.equalTo("token"));
        serviceConfig.setToken((Boolean) null);
        MatcherAssert.assertThat(serviceConfig.getToken(), Matchers.nullValue());
        serviceConfig.setToken(true);
        MatcherAssert.assertThat(serviceConfig.getToken(), Matchers.is("true"));
    }

    @Test
    void testDeprecated() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setDeprecated(true);
        MatcherAssert.assertThat(serviceConfig.isDeprecated(), Matchers.is(true));
    }

    @Test
    void testDynamic() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setDynamic(true);
        MatcherAssert.assertThat(serviceConfig.isDynamic(), Matchers.is(true));
    }

    @Test
    void testProtocol() {
        ServiceConfig serviceConfig = new ServiceConfig();
        MatcherAssert.assertThat(serviceConfig.getProtocol(), Matchers.nullValue());
        serviceConfig.setProtocol(new ProtocolConfig());
        MatcherAssert.assertThat(serviceConfig.getProtocol(), Matchers.notNullValue());
        serviceConfig.setProtocols(new ArrayList<>(Collections.singletonList(new ProtocolConfig())));
        MatcherAssert.assertThat(serviceConfig.getProtocols(), Matchers.hasSize(1));
    }

    @Test
    void testAccesslog() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setAccesslog("access.log");
        MatcherAssert.assertThat(serviceConfig.getAccesslog(), Matchers.equalTo("access.log"));
        serviceConfig.setAccesslog((Boolean) null);
        MatcherAssert.assertThat(serviceConfig.getAccesslog(), Matchers.nullValue());
        serviceConfig.setAccesslog(true);
        MatcherAssert.assertThat(serviceConfig.getAccesslog(), Matchers.equalTo("true"));
    }

    @Test
    void testExecutes() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setExecutes(10);
        MatcherAssert.assertThat(serviceConfig.getExecutes(), Matchers.equalTo(10));
    }

    @Test
    void testFilter() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setFilter("mockfilter");
        MatcherAssert.assertThat(serviceConfig.getFilter(), Matchers.equalTo("mockfilter"));
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(SERVICE_FILTER_KEY, "prefilter");
        AbstractServiceConfig.appendParameters(parameters, serviceConfig);
        MatcherAssert.assertThat(parameters, Matchers.hasEntry(SERVICE_FILTER_KEY, "prefilter,mockfilter"));
    }

    @Test
    void testListener() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setListener("mockexporterlistener");
        MatcherAssert.assertThat(serviceConfig.getListener(), Matchers.equalTo("mockexporterlistener"));
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(EXPORTER_LISTENER_KEY, "prelistener");
        AbstractServiceConfig.appendParameters(parameters, serviceConfig);
        MatcherAssert.assertThat(parameters, Matchers.hasEntry(EXPORTER_LISTENER_KEY, "prelistener,mockexporterlistener"));
    }

    @Test
    void testRegister() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setRegister(true);
        MatcherAssert.assertThat(serviceConfig.isRegister(), Matchers.is(true));
    }

    @Test
    void testWarmup() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setWarmup(100);
        MatcherAssert.assertThat(serviceConfig.getWarmup(), Matchers.equalTo(100));
    }

    @Test
    void testSerialization() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setSerialization("serialization");
        MatcherAssert.assertThat(serviceConfig.getSerialization(), Matchers.equalTo("serialization"));
    }

    @Test
    void testPreferSerialization() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setPreferSerialization("preferSerialization");
        MatcherAssert.assertThat(serviceConfig.getPreferSerialization(), Matchers.equalTo("preferSerialization"));
    }

    @Test
    void testPreferSerializationDefault1() {
        ServiceConfig serviceConfig = new ServiceConfig();
        Assertions.assertNull(serviceConfig.getPreferSerialization());

        serviceConfig.checkDefault();
        Assertions.assertNull(serviceConfig.getPreferSerialization());

        serviceConfig = new ServiceConfig();
        serviceConfig.setSerialization("x-serialization");
        Assertions.assertNull(serviceConfig.getPreferSerialization());

        serviceConfig.checkDefault();
        MatcherAssert.assertThat(serviceConfig.getPreferSerialization(), Matchers.equalTo("x-serialization"));
    }

    @Test
    void testPreferSerializationDefault2() {
        ServiceConfig serviceConfig = new ServiceConfig();
        Assertions.assertNull(serviceConfig.getPreferSerialization());

        serviceConfig.refresh();
        Assertions.assertNull(serviceConfig.getPreferSerialization());

        serviceConfig = new ServiceConfig();
        serviceConfig.setSerialization("x-serialization");
        Assertions.assertNull(serviceConfig.getPreferSerialization());

        serviceConfig.refresh();
        MatcherAssert.assertThat(serviceConfig.getPreferSerialization(), Matchers.equalTo("x-serialization"));
    }


    private static class ServiceConfig extends AbstractServiceConfig {

    }
}
