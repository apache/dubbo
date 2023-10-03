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

import org.apache.dubbo.config.AbstractServiceConfig;
import org.apache.dubbo.config.ProtocolConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class AbstractServiceBuilderTest {

    @Test
    void version() {
        ServiceBuilder builder = new ServiceBuilder();
        builder.version("version");
        Assertions.assertEquals("version", builder.build().getVersion());
    }

    @Test
    void group() {
        ServiceBuilder builder = new ServiceBuilder();
        builder.group("group");
        Assertions.assertEquals("group", builder.build().getGroup());
    }

    @Test
    void deprecated() {
        ServiceBuilder builder = new ServiceBuilder();
        builder.deprecated(true);
        Assertions.assertTrue(builder.build().isDeprecated());
        builder.deprecated(false);
        Assertions.assertFalse(builder.build().isDeprecated());
    }

    @Test
    void delay() {
        ServiceBuilder builder = new ServiceBuilder();
        builder.delay(1000);
        Assertions.assertEquals(1000, builder.build().getDelay());
    }

    @Test
    void export() {
        ServiceBuilder builder = new ServiceBuilder();
        builder.export(true);
        Assertions.assertTrue(builder.build().getExport());
        builder.export(false);
        Assertions.assertFalse(builder.build().getExport());
    }

    @Test
    void weight() {
        ServiceBuilder builder = new ServiceBuilder();
        builder.weight(500);
        Assertions.assertEquals(500, builder.build().getWeight());
    }

    @Test
    void document() {
        ServiceBuilder builder = new ServiceBuilder();
        builder.document("http://dubbo.apache.org");
        Assertions.assertEquals("http://dubbo.apache.org", builder.build().getDocument());
    }

    @Test
    void dynamic() {
        ServiceBuilder builder = new ServiceBuilder();
        builder.dynamic(true);
        Assertions.assertTrue(builder.build().isDynamic());
        builder.dynamic(false);
        Assertions.assertFalse(builder.build().isDynamic());
    }

    @Test
    void token() {
        ServiceBuilder builder = new ServiceBuilder();
        builder.token("token");
        Assertions.assertEquals("token", builder.build().getToken());
    }

    @Test
    void token1() {
        ServiceBuilder builder = new ServiceBuilder();
        builder.token(true);
        Assertions.assertEquals("true", builder.build().getToken());
        builder.token(false);
        Assertions.assertEquals("false", builder.build().getToken());
        builder.token((Boolean) null);
        Assertions.assertNull(builder.build().getToken());
    }

    @Test
    void accesslog() {
        ServiceBuilder builder = new ServiceBuilder();
        builder.accesslog("accesslog");
        Assertions.assertEquals("accesslog", builder.build().getAccesslog());
    }

    @Test
    void accesslog1() {
        ServiceBuilder builder = new ServiceBuilder();
        builder.accesslog(true);
        Assertions.assertEquals("true", builder.build().getAccesslog());
        builder.accesslog(false);
        Assertions.assertEquals("false", builder.build().getAccesslog());
        builder.accesslog((Boolean) null);
        Assertions.assertNull(builder.build().getAccesslog());
    }

    @Test
    void addProtocols() {
        ProtocolConfig protocol = new ProtocolConfig();
        ServiceBuilder builder = new ServiceBuilder();
        Assertions.assertNull(builder.build().getProtocols());
        builder.addProtocols(Collections.singletonList(protocol));
        Assertions.assertNotNull(builder.build().getProtocols());
        Assertions.assertEquals(1, builder.build().getProtocols().size());
    }

    @Test
    void addProtocol() {
        ProtocolConfig protocol = new ProtocolConfig();
        ServiceBuilder builder = new ServiceBuilder();
        Assertions.assertNull(builder.build().getProtocols());
        builder.addProtocol(protocol);
        Assertions.assertNotNull(builder.build().getProtocols());
        Assertions.assertEquals(1, builder.build().getProtocols().size());
        Assertions.assertEquals(protocol, builder.build().getProtocol());
    }

    @Test
    void protocolIds() {
        ServiceBuilder builder = new ServiceBuilder();
        builder.protocolIds("protocolIds");
        Assertions.assertEquals("protocolIds", builder.build().getProtocolIds());
    }

    @Test
    void tag() {
        ServiceBuilder builder = new ServiceBuilder();
        builder.tag("tag");
        Assertions.assertEquals("tag", builder.build().getTag());
    }

    @Test
    void executes() {
        ServiceBuilder builder = new ServiceBuilder();
        builder.executes(10);
        Assertions.assertEquals(10, builder.build().getExecutes());
    }

    @Test
    void register() {
        ServiceBuilder builder = new ServiceBuilder();
        builder.register(true);
        Assertions.assertTrue(builder.build().isRegister());
        builder.register(false);
        Assertions.assertFalse(builder.build().isRegister());
    }

    @Test
    void warmup() {
        ServiceBuilder builder = new ServiceBuilder();
        builder.warmup(100);
        Assertions.assertEquals(100, builder.build().getWarmup());
    }

    @Test
    void serialization() {
        ServiceBuilder builder = new ServiceBuilder();
        builder.serialization("serialization");
        Assertions.assertEquals("serialization", builder.build().getSerialization());
    }

    @Test
    void build() {
        ProtocolConfig protocol = new ProtocolConfig();

        ServiceBuilder builder = new ServiceBuilder();
        builder.version("version").group("group").deprecated(true).delay(1000).export(false).weight(1)
                .document("document").dynamic(true).token("token").accesslog("accesslog")
                .addProtocol(protocol).protocolIds("protocolIds").tag("tag").executes(100).register(false)
                .warmup(200).serialization("serialization").id("id");

        ServiceConfig config = builder.build();
        ServiceConfig config2 = builder.build();

        Assertions.assertEquals("id", config.getId());
        Assertions.assertEquals("version", config.getVersion());
        Assertions.assertEquals("group", config.getGroup());
        Assertions.assertEquals("document", config.getDocument());
        Assertions.assertEquals("token", config.getToken());
        Assertions.assertEquals("accesslog", config.getAccesslog());
        Assertions.assertEquals("protocolIds", config.getProtocolIds());
        Assertions.assertEquals("tag", config.getTag());
        Assertions.assertEquals("serialization", config.getSerialization());
        Assertions.assertTrue(config.isDeprecated());
        Assertions.assertFalse(config.getExport());
        Assertions.assertTrue(config.isDynamic());
        Assertions.assertFalse(config.isRegister());
        Assertions.assertEquals(1000, config.getDelay());
        Assertions.assertEquals(1, config.getWeight());
        Assertions.assertEquals(100, config.getExecutes());
        Assertions.assertEquals(200, config.getWarmup());

        Assertions.assertNotSame(config, config2);
    }

    private static class ServiceBuilder extends AbstractServiceBuilder<ServiceConfig, ServiceBuilder> {

        public ServiceConfig build() {
            ServiceConfig parameterConfig = new ServiceConfig();
            super.build(parameterConfig);

            return parameterConfig;
        }

        @Override
        protected ServiceBuilder getThis() {
            return this;
        }
    }

    private static class ServiceConfig extends AbstractServiceConfig {

    }
}