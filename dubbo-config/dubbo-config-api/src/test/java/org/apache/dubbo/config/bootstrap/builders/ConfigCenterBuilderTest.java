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

import org.apache.dubbo.config.ConfigCenterConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class ConfigCenterBuilderTest {

    @Test
    void protocol() {
        ConfigCenterBuilder builder = new ConfigCenterBuilder();
        builder.protocol("protocol");
        Assertions.assertEquals("protocol", builder.build().getProtocol());
    }

    @Test
    void address() {
        ConfigCenterBuilder builder = new ConfigCenterBuilder();
        builder.address("address");
        Assertions.assertEquals("address", builder.build().getAddress());
    }

    @Test
    void cluster() {
        ConfigCenterBuilder builder = new ConfigCenterBuilder();
        builder.cluster("cluster");
        Assertions.assertEquals("cluster", builder.build().getCluster());
    }

    @Test
    void namespace() {
        ConfigCenterBuilder builder = new ConfigCenterBuilder();
        builder.namespace("namespace");
        Assertions.assertEquals("namespace", builder.build().getNamespace());
    }

    @Test
    void group() {
        ConfigCenterBuilder builder = new ConfigCenterBuilder();
        builder.group("group");
        Assertions.assertEquals("group", builder.build().getGroup());
    }

    @Test
    void username() {
        ConfigCenterBuilder builder = new ConfigCenterBuilder();
        builder.username("username");
        Assertions.assertEquals("username", builder.build().getUsername());
    }

    @Test
    void password() {
        ConfigCenterBuilder builder = new ConfigCenterBuilder();
        builder.password("password");
        Assertions.assertEquals("password", builder.build().getPassword());
    }

    @Test
    void timeout() {
        ConfigCenterBuilder builder = new ConfigCenterBuilder();
        builder.timeout(1000L);
        Assertions.assertEquals(1000L, builder.build().getTimeout());
    }

    @Test
    void highestPriority() {
        ConfigCenterBuilder builder = new ConfigCenterBuilder();
        builder.highestPriority(true);
        Assertions.assertTrue(builder.build().isHighestPriority());
    }

    @Test
    void check() {
        ConfigCenterBuilder builder = new ConfigCenterBuilder();
        builder.check(true);
        Assertions.assertTrue(builder.build().isCheck());
    }

    @Test
    void configFile() {
        ConfigCenterBuilder builder = new ConfigCenterBuilder();
        builder.configFile("configFile");
        Assertions.assertEquals("configFile", builder.build().getConfigFile());
    }

    @Test
    void appConfigFile() {
        ConfigCenterBuilder builder = new ConfigCenterBuilder();
        builder.appConfigFile("appConfigFile");
        Assertions.assertEquals("appConfigFile", builder.build().getAppConfigFile());
    }

    @Test
    void appendParameter() {
        ConfigCenterBuilder builder = new ConfigCenterBuilder();
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

        ConfigCenterBuilder builder = new ConfigCenterBuilder();
        builder.appendParameters(source);

        Map<String, String> parameters = builder.build().getParameters();

        Assertions.assertTrue(parameters.containsKey("default.num"));
        Assertions.assertEquals("ONE", parameters.get("num"));
    }

    @Test
    void build() {
        ConfigCenterBuilder builder = new ConfigCenterBuilder();
        builder.check(true).protocol("protocol").address("address").appConfigFile("appConfigFile")
                .cluster("cluster").configFile("configFile").group("group").highestPriority(false)
                .namespace("namespace").password("password").timeout(1000L).username("usernama")
                .appendParameter("default.num", "one").id("id");

        ConfigCenterConfig config = builder.build();
        ConfigCenterConfig config2 = builder.build();

        Assertions.assertTrue(config.isCheck());
        Assertions.assertFalse(config.isHighestPriority());
        Assertions.assertEquals(1000L, config.getTimeout());
        Assertions.assertEquals("protocol", config.getProtocol());
        Assertions.assertEquals("address", config.getAddress());
        Assertions.assertEquals("appConfigFile", config.getAppConfigFile());
        Assertions.assertEquals("cluster", config.getCluster());
        Assertions.assertEquals("configFile", config.getConfigFile());
        Assertions.assertEquals("group", config.getGroup());
        Assertions.assertEquals("namespace", config.getNamespace());
        Assertions.assertEquals("password", config.getPassword());
        Assertions.assertEquals("usernama", config.getUsername());
        Assertions.assertTrue(config.getParameters().containsKey("default.num"));
        Assertions.assertEquals("one", config.getParameters().get("default.num"));
        Assertions.assertEquals("id", config.getId());

        Assertions.assertNotSame(config, config2);
    }
}