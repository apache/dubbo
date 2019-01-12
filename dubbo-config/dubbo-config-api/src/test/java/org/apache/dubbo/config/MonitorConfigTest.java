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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MonitorConfigTest {
    @Test
    public void testAddress() throws Exception {
        MonitorConfig monitor = new MonitorConfig();
        monitor.setAddress("monitor-addr");
        assertThat(monitor.getAddress(), equalTo("monitor-addr"));
        Map<String, String> parameters = new HashMap<String, String>();
        MonitorConfig.appendParameters(parameters, monitor);
        assertThat(parameters.isEmpty(), is(true));
    }

    @Test
    public void testProtocol() throws Exception {
        MonitorConfig monitor = new MonitorConfig();
        monitor.setProtocol("protocol");
        assertThat(monitor.getProtocol(), equalTo("protocol"));
        Map<String, String> parameters = new HashMap<String, String>();
        MonitorConfig.appendParameters(parameters, monitor);
        assertThat(parameters.isEmpty(), is(true));
    }

    @Test
    public void testUsername() throws Exception {
        MonitorConfig monitor = new MonitorConfig();
        monitor.setUsername("user");
        assertThat(monitor.getUsername(), equalTo("user"));
        Map<String, String> parameters = new HashMap<String, String>();
        MonitorConfig.appendParameters(parameters, monitor);
        assertThat(parameters.isEmpty(), is(true));
    }

    @Test
    public void testPassword() throws Exception {
        MonitorConfig monitor = new MonitorConfig();
        monitor.setPassword("secret");
        assertThat(monitor.getPassword(), equalTo("secret"));
        Map<String, String> parameters = new HashMap<String, String>();
        MonitorConfig.appendParameters(parameters, monitor);
        assertThat(parameters.isEmpty(), is(true));
    }

    @Test
    public void testGroup() throws Exception {
        MonitorConfig monitor = new MonitorConfig();
        monitor.setGroup("group");
        assertThat(monitor.getGroup(), equalTo("group"));
    }

    @Test
    public void testVersion() throws Exception {
        MonitorConfig monitor = new MonitorConfig();
        monitor.setVersion("1.0.0");
        assertThat(monitor.getVersion(), equalTo("1.0.0"));
    }

    @Test
    public void testParameters() throws Exception {
        MonitorConfig monitor = new MonitorConfig();
        Map<String, String> parameters = Collections.singletonMap("k1", "v1");
        monitor.setParameters(parameters);
        assertThat(monitor.getParameters(), hasEntry("k1", "v1"));
    }

    @Test
    public void testDefault() throws Exception {
        MonitorConfig monitor = new MonitorConfig();
        monitor.setDefault(true);
        assertThat(monitor.isDefault(), is(true));
    }

    @Test
    public void testInterval() throws Exception {
        MonitorConfig monitor = new MonitorConfig();
        monitor.setInterval("100");
        assertThat(monitor.getInterval(), equalTo("100"));
    }
}
