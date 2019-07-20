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
package org.apache.dubbo.config.builders;

import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.api.DemoService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class ReferenceBuilderTest {

    @Test
    void interfaceName() {
        ReferenceBuilder builder = new ReferenceBuilder();
        builder.interfaceName(DemoService.class.getName());
        Assertions.assertEquals("org.apache.dubbo.config.api.DemoService", builder.build().getInterface());
    }

    @Test
    void interfaceClass() {
        ReferenceBuilder builder = new ReferenceBuilder();
        builder.interfaceClass(DemoService.class);
        Assertions.assertEquals(DemoService.class, builder.build().getInterfaceClass());
    }

    @Test
    void client() {
        ReferenceBuilder builder = new ReferenceBuilder();
        builder.client("client");
        Assertions.assertEquals("client", builder.build().getClient());
    }

    @Test
    void url() {
        ReferenceBuilder builder = new ReferenceBuilder();
        builder.url("url");
        Assertions.assertEquals("url", builder.build().getUrl());
    }

    @Test
    void addMethods() {
        MethodConfig method = new MethodConfig();
        ReferenceBuilder builder = new ReferenceBuilder();
        builder.addMethods(Collections.singletonList(method));
        Assertions.assertTrue(builder.build().getMethods().contains(method));
        Assertions.assertEquals(1, builder.build().getMethods().size());
    }

    @Test
    void addMethod() {
        MethodConfig method = new MethodConfig();
        ReferenceBuilder builder = new ReferenceBuilder();
        builder.addMethod(method);
        Assertions.assertTrue(builder.build().getMethods().contains(method));
        Assertions.assertEquals(1, builder.build().getMethods().size());
    }

    @Test
    void consumer() {
        ConsumerConfig consumer = new ConsumerConfig();
        ReferenceBuilder builder = new ReferenceBuilder();
        builder.consumer(consumer);
        Assertions.assertSame(consumer, builder.build().getConsumer());
    }

    @Test
    void protocol() {
        ReferenceBuilder builder = new ReferenceBuilder();
        builder.protocol("protocol");
        Assertions.assertEquals("protocol", builder.build().getProtocol());
    }

    @Test
    void build() {
        ConsumerConfig consumer = new ConsumerConfig();
        MethodConfig method = new MethodConfig();

        ReferenceBuilder<DemoService> builder = new ReferenceBuilder<>();
        builder.id("id").interfaceClass(DemoService.class).protocol("protocol").client("client").url("url")
                .consumer(consumer).addMethod(method);

        ReferenceConfig config = builder.build();
        ReferenceConfig config2 = builder.build();

        Assertions.assertEquals("org.apache.dubbo.config.api.DemoService", config.getInterface());
        Assertions.assertEquals(DemoService.class, config.getInterfaceClass());
        Assertions.assertEquals("protocol", config.getProtocol());
        Assertions.assertEquals("client", config.getClient());
        Assertions.assertEquals("url", config.getUrl());
        Assertions.assertEquals(consumer, config.getConsumer());
        Assertions.assertTrue(config.getMethods().contains(method));
        Assertions.assertEquals(1, config.getMethods().size());
        Assertions.assertNotSame(config, config2);
    }
}