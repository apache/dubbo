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

import org.apache.dubbo.config.RegistryConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class RegistryBuilderTest {

    @Test
    void address() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.address("address");
        Assertions.assertEquals("address", builder.build().getAddress());
    }

    @Test
    void username() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.username("username");
        Assertions.assertEquals("username", builder.build().getUsername());
    }

    @Test
    void password() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.password("password");
        Assertions.assertEquals("password", builder.build().getPassword());
    }

    @Test
    void port() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.port(8080);
        Assertions.assertEquals(8080, builder.build().getPort());
    }

    @Test
    void protocol() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.protocol("protocol");
        Assertions.assertEquals("protocol", builder.build().getProtocol());
    }

    @Test
    void transporter() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.transporter("transporter");
        Assertions.assertEquals("transporter", builder.build().getTransporter());
    }

    @Test
    void transport() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.transport("transport");
        Assertions.assertEquals("transport", builder.build().getTransport());
    }

    @Test
    void server() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.server("server");
        Assertions.assertEquals("server", builder.build().getServer());
    }

    @Test
    void client() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.client("client");
        Assertions.assertEquals("client", builder.build().getClient());
    }

    @Test
    void cluster() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.cluster("cluster");
        Assertions.assertEquals("cluster", builder.build().getCluster());
    }

    @Test
    void group() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.group("group");
        Assertions.assertEquals("group", builder.build().getGroup());
    }

    @Test
    void version() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.version("version");
        Assertions.assertEquals("version", builder.build().getVersion());
    }

    @Test
    void timeout() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.timeout(1000);
        Assertions.assertEquals(1000, builder.build().getTimeout());
    }

    @Test
    void session() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.session(2000);
        Assertions.assertEquals(2000, builder.build().getSession());
    }

    @Test
    void file() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.file("file");
        Assertions.assertEquals("file", builder.build().getFile());
    }

    @Test
    void testWait() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.wait(Integer.valueOf(1000));
        Assertions.assertEquals(1000, builder.build().getWait());
    }

    @Test
    void isCheck() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.isCheck(true);
        Assertions.assertTrue(builder.build().isCheck());
    }

    @Test
    void isDynamic() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.isDynamic(true);
        Assertions.assertTrue(builder.build().isDynamic());
    }

    @Test
    void register() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.register(true);
        Assertions.assertTrue(builder.build().isRegister());
    }

    @Test
    void subscribe() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.subscribe(true);
        Assertions.assertTrue(builder.build().isSubscribe());
    }

    @Test
    void appendParameter() {
        RegistryBuilder builder = new RegistryBuilder();
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

        RegistryBuilder builder = new RegistryBuilder();
        builder.appendParameters(source);

        Map<String, String> parameters = builder.build().getParameters();

        Assertions.assertTrue(parameters.containsKey("default.num"));
        Assertions.assertEquals("ONE", parameters.get("num"));
    }

    @Test
    void isDefault() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.isDefault(true);
        Assertions.assertTrue(builder.build().isDefault());
    }

    @Test
    void simplified() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.simplified(true);
        Assertions.assertTrue(builder.build().getSimplified());
    }

    @Test
    void extraKeys() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.extraKeys("extraKeys");
        Assertions.assertEquals("extraKeys", builder.build().getExtraKeys());
    }

    @Test
    void build() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.address("address").username("username").password("password").port(8080).protocol("protocol")
                .transporter("transporter").server("server").client("client").cluster("cluster").group("group")
                .version("version").timeout(1000).session(2000).file("file").wait(Integer.valueOf(10)).isCheck(true)
                .isDynamic(false).register(true).subscribe(false).isDefault(true).simplified(false).extraKeys("A")
                .parameter("default.num", "one").id("id");

        RegistryConfig config = builder.build();
        RegistryConfig config2 = builder.build();

        Assertions.assertEquals(8080, config.getPort());
        Assertions.assertEquals(1000, config.getTimeout());
        Assertions.assertEquals(2000, config.getSession());
        Assertions.assertEquals(10, config.getWait());
        Assertions.assertTrue(config.isCheck());
        Assertions.assertFalse(config.isDynamic());
        Assertions.assertTrue(config.isRegister());
        Assertions.assertFalse(config.isSubscribe());
        Assertions.assertTrue(config.isDefault());
        Assertions.assertFalse(config.getSimplified());
        Assertions.assertEquals("address", config.getAddress());
        Assertions.assertEquals("username", config.getUsername());
        Assertions.assertEquals("password", config.getPassword());
        Assertions.assertEquals("protocol", config.getProtocol());
        Assertions.assertEquals("transporter", config.getTransporter());
        Assertions.assertEquals("server", config.getServer());
        Assertions.assertEquals("client", config.getClient());
        Assertions.assertEquals("cluster", config.getCluster());
        Assertions.assertEquals("group", config.getGroup());
        Assertions.assertEquals("version", config.getVersion());
        Assertions.assertEquals("file", config.getFile());
        Assertions.assertEquals("A", config.getExtraKeys());
        Assertions.assertTrue(config.getParameters().containsKey("default.num"));
        Assertions.assertEquals("one", config.getParameters().get("default.num"));
        Assertions.assertEquals("id", config.getId());
        Assertions.assertNotSame(config, config2);
    }
}
