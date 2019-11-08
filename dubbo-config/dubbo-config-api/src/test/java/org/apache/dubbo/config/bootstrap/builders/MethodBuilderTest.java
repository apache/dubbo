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

import org.apache.dubbo.config.ArgumentConfig;
import org.apache.dubbo.config.MethodConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class MethodBuilderTest {

    @Test
    void name() {
        MethodBuilder builder = new MethodBuilder();
        builder.name("name");
        Assertions.assertEquals("name", builder.build().getName());
    }

    @Test
    void stat() {
        MethodBuilder builder = new MethodBuilder();
        builder.stat(1);
        Assertions.assertEquals(1, builder.build().getStat());
    }

    @Test
    void retry() {
        MethodBuilder builder = new MethodBuilder();
        builder.retry(true);
        Assertions.assertTrue(builder.build().isRetry());
    }

    @Test
    void reliable() {
        MethodBuilder builder = new MethodBuilder();
        builder.reliable(true);
        Assertions.assertTrue(builder.build().isReliable());
    }

    @Test
    void executes() {
        MethodBuilder builder = new MethodBuilder();
        builder.executes(1);
        Assertions.assertEquals(1, builder.build().getExecutes());
    }

    @Test
    void deprecated() {
        MethodBuilder builder = new MethodBuilder();
        builder.deprecated(true);
        Assertions.assertTrue(builder.build().getDeprecated());
    }

    @Test
    void sticky() {
        MethodBuilder builder = new MethodBuilder();
        builder.sticky(true);
        Assertions.assertTrue(builder.build().getSticky());
    }

    @Test
    void isReturn() {
        MethodBuilder builder = new MethodBuilder();
        builder.isReturn(true);
        Assertions.assertTrue(builder.build().isReturn());
    }

    @Test
    void oninvoke() {
        MethodBuilder builder = new MethodBuilder();
        builder.oninvoke("on-invoke-object");
        Assertions.assertEquals("on-invoke-object", builder.build().getOninvoke());
    }

    @Test
    void oninvokeMethod() {
        MethodBuilder builder = new MethodBuilder();
        builder.oninvokeMethod("on-invoke-method");
        Assertions.assertEquals("on-invoke-method", builder.build().getOninvokeMethod());
    }

    @Test
    void onreturn() {
        MethodBuilder builder = new MethodBuilder();
        builder.onreturn("on-return-object");
        Assertions.assertEquals("on-return-object", builder.build().getOnreturn());
    }

    @Test
    void onreturnMethod() {
        MethodBuilder builder = new MethodBuilder();
        builder.onreturnMethod("on-return-method");
        Assertions.assertEquals("on-return-method", builder.build().getOnreturnMethod());
    }

    @Test
    void onthrow() {
        MethodBuilder builder = new MethodBuilder();
        builder.onthrow("on-throw-object");
        Assertions.assertEquals("on-throw-object", builder.build().getOnthrow());
    }

    @Test
    void onthrowMethod() {
        MethodBuilder builder = new MethodBuilder();
        builder.onthrowMethod("on-throw-method");
        Assertions.assertEquals("on-throw-method", builder.build().getOnthrowMethod());
    }

    @Test
    void addArguments() {
        ArgumentConfig argument = new ArgumentConfig();
        MethodBuilder builder = new MethodBuilder();
        builder.addArguments(Collections.singletonList(argument));
        Assertions.assertTrue(builder.build().getArguments().contains(argument));
        Assertions.assertEquals(1, builder.build().getArguments().size());
    }

    @Test
    void addArgument() {
        ArgumentConfig argument = new ArgumentConfig();
        MethodBuilder builder = new MethodBuilder();
        builder.addArgument(argument);
        Assertions.assertTrue(builder.build().getArguments().contains(argument));
        Assertions.assertEquals(1, builder.build().getArguments().size());
    }

    @Test
    void service() {
        MethodBuilder builder = new MethodBuilder();
        builder.service("service");
        Assertions.assertEquals("service", builder.build().getService());
    }

    @Test
    void serviceId() {
        MethodBuilder builder = new MethodBuilder();
        builder.serviceId("serviceId");
        Assertions.assertEquals("serviceId", builder.build().getServiceId());
    }

    @Test
    void build() {
        ArgumentConfig argument = new ArgumentConfig();
        MethodBuilder builder = new MethodBuilder();
        builder.name("name").stat(1).retry(true).reliable(false).executes(2).deprecated(true).sticky(false)
                .isReturn(true).oninvoke("on-invoke-object").oninvokeMethod("on-invoke-method").service("service")
                .onreturn("on-return-object").onreturnMethod("on-return-method").serviceId("serviceId")
                .onthrow("on-throw-object").onthrowMethod("on-throw-method").addArgument(argument);

        MethodConfig config = builder.build();
        MethodConfig config2 = builder.build();

        Assertions.assertTrue(config.isRetry());
        Assertions.assertFalse(config.isReliable());
        Assertions.assertTrue(config.getDeprecated());
        Assertions.assertFalse(config.getSticky());
        Assertions.assertTrue(config.isReturn());
        Assertions.assertEquals(1, config.getStat());
        Assertions.assertEquals(2, config.getExecutes());
        Assertions.assertEquals("on-invoke-object", config.getOninvoke());
        Assertions.assertEquals("on-invoke-method", config.getOninvokeMethod());
        Assertions.assertEquals("on-return-object", config.getOnreturn());
        Assertions.assertEquals("on-return-method", config.getOnreturnMethod());
        Assertions.assertEquals("on-throw-object", config.getOnthrow());
        Assertions.assertEquals("on-throw-method", config.getOnthrowMethod());
        Assertions.assertEquals("name", config.getName());
        Assertions.assertEquals("service", config.getService());
        Assertions.assertEquals("serviceId", config.getServiceId());
        Assertions.assertNotSame(config, config2);
    }
}