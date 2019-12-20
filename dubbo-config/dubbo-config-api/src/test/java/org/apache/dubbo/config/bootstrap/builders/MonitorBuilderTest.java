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

import org.apache.dubbo.config.MonitorConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class MonitorBuilderTest {

    @Test
    void protocol() {
        MonitorBuilder builder = new MonitorBuilder();
        builder.protocol("protocol");
        Assertions.assertEquals("protocol", builder.build().getProtocol());
    }

    @Test
    void address() {
        MonitorBuilder builder = new MonitorBuilder();
        builder.address("address");
        Assertions.assertEquals("address", builder.build().getAddress());
    }

    @Test
    void username() {
        MonitorBuilder builder = new MonitorBuilder();
        builder.username("username");
        Assertions.assertEquals("username", builder.build().getUsername());
    }

    @Test
    void password() {
        MonitorBuilder builder = new MonitorBuilder();
        builder.password("password");
        Assertions.assertEquals("password", builder.build().getPassword());
    }

    @Test
    void group() {
        MonitorBuilder builder = new MonitorBuilder();
        builder.group("group");
        Assertions.assertEquals("group", builder.build().getGroup());
    }

    @Test
    void version() {
        MonitorBuilder builder = new MonitorBuilder();
        builder.version("version");
        Assertions.assertEquals("version", builder.build().getVersion());
    }

    @Test
    void interval() {
        MonitorBuilder builder = new MonitorBuilder();
        builder.interval("interval");
        Assertions.assertEquals("interval", builder.build().getInterval());
    }

    @Test
    void isDefault() {
        MonitorBuilder builder = new MonitorBuilder();
        builder.isDefault(true);
        Assertions.assertTrue(builder.build().isDefault());
    }

    @Test
    void appendParameter() {
        MonitorBuilder builder = new MonitorBuilder();
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

        MonitorBuilder builder = new MonitorBuilder();
        builder.appendParameters(source);

        Map<String, String> parameters = builder.build().getParameters();

        Assertions.assertTrue(parameters.containsKey("default.num"));
        Assertions.assertEquals("ONE", parameters.get("num"));
    }

    @Test
    void build() {
        MonitorBuilder builder = new MonitorBuilder();
        builder.protocol("protocol").address("address").group("group").interval("interval").isDefault(true)
                .password("password").username("username").version("version")
                .appendParameter("default.num", "one").id("id").prefix("prefix");

        MonitorConfig config = builder.build();
        MonitorConfig config2 = builder.build();

        Assertions.assertEquals("protocol", config.getProtocol());
        Assertions.assertEquals("address", config.getAddress());
        Assertions.assertEquals("group", config.getGroup());
        Assertions.assertEquals("interval", config.getInterval());
        Assertions.assertEquals("password", config.getPassword());
        Assertions.assertEquals("username", config.getUsername());
        Assertions.assertEquals("version", config.getVersion());
        Assertions.assertTrue(config.isDefault());
        Assertions.assertTrue(config.getParameters().containsKey("default.num"));
        Assertions.assertEquals("one", config.getParameters().get("default.num"));
        Assertions.assertEquals("id", config.getId());
        Assertions.assertEquals("prefix", config.getPrefix());
        Assertions.assertNotSame(config, config2);
    }
}