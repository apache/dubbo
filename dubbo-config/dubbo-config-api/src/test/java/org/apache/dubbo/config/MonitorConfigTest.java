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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class MonitorConfigTest {
    @Test
    void testAddress() throws Exception {
        MonitorConfig monitor = new MonitorConfig();
        monitor.setAddress("monitor-addr");
        MatcherAssert.assertThat(monitor.getAddress(), Matchers.equalTo("monitor-addr"));
        Map<String, String> parameters = new HashMap<String, String>();
        MonitorConfig.appendParameters(parameters, monitor);
        MatcherAssert.assertThat(parameters.isEmpty(), Matchers.is(true));
    }

    @Test
    void testProtocol() throws Exception {
        MonitorConfig monitor = new MonitorConfig();
        monitor.setProtocol("protocol");
        MatcherAssert.assertThat(monitor.getProtocol(), Matchers.equalTo("protocol"));
        Map<String, String> parameters = new HashMap<String, String>();
        MonitorConfig.appendParameters(parameters, monitor);
        MatcherAssert.assertThat(parameters.isEmpty(), Matchers.is(true));
    }

    @Test
    void testUsername() throws Exception {
        MonitorConfig monitor = new MonitorConfig();
        monitor.setUsername("user");
        MatcherAssert.assertThat(monitor.getUsername(), Matchers.equalTo("user"));
        Map<String, String> parameters = new HashMap<String, String>();
        MonitorConfig.appendParameters(parameters, monitor);
        MatcherAssert.assertThat(parameters.isEmpty(), Matchers.is(true));
    }

    @Test
    void testPassword() throws Exception {
        MonitorConfig monitor = new MonitorConfig();
        monitor.setPassword("secret");
        MatcherAssert.assertThat(monitor.getPassword(), Matchers.equalTo("secret"));
        Map<String, String> parameters = new HashMap<String, String>();
        MonitorConfig.appendParameters(parameters, monitor);
        MatcherAssert.assertThat(parameters.isEmpty(), Matchers.is(true));
    }

    @Test
    void testGroup() throws Exception {
        MonitorConfig monitor = new MonitorConfig();
        monitor.setGroup("group");
        MatcherAssert.assertThat(monitor.getGroup(), Matchers.equalTo("group"));
    }

    @Test
    void testVersion() throws Exception {
        MonitorConfig monitor = new MonitorConfig();
        monitor.setVersion("1.0.0");
        MatcherAssert.assertThat(monitor.getVersion(), Matchers.equalTo("1.0.0"));
    }

    @Test
    void testParameters() throws Exception {
        MonitorConfig monitor = new MonitorConfig();
        Map<String, String> parameters = Collections.singletonMap("k1", "v1");
        monitor.setParameters(parameters);
        MatcherAssert.assertThat(monitor.getParameters(), Matchers.hasEntry("k1", "v1"));
    }

    @Test
    void testDefault() throws Exception {
        MonitorConfig monitor = new MonitorConfig();
        monitor.setDefault(true);
        MatcherAssert.assertThat(monitor.isDefault(), Matchers.is(true));
    }

    @Test
    void testInterval() throws Exception {
        MonitorConfig monitor = new MonitorConfig();
        monitor.setInterval("100");
        MatcherAssert.assertThat(monitor.getInterval(), Matchers.equalTo("100"));
    }

    @Test
    void testMetaData() {
        MonitorConfig config = new MonitorConfig();
        Map<String, String> metaData = config.getMetaData();
        Assertions.assertEquals(0, metaData.size(), "Expect empty metadata but found: "+metaData);
    }
}
