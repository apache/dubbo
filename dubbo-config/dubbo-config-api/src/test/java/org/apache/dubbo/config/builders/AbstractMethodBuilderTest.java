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

import org.apache.dubbo.config.AbstractMethodConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class AbstractMethodBuilderTest {

    @Test
    void timeout() {
        MethodBuilder builder = new MethodBuilder();
        builder.timeout(10);

        Assertions.assertEquals(10, builder.build().getTimeout());
    }

    @Test
    void retries() {
        MethodBuilder builder = new MethodBuilder();
        builder.retries(3);

        Assertions.assertEquals(3, builder.build().getRetries());
    }

    @Test
    void actives() {
        MethodBuilder builder = new MethodBuilder();
        builder.actives(3);

        Assertions.assertEquals(3, builder.build().getActives());
    }

    @Test
    void loadbalance() {
        MethodBuilder builder = new MethodBuilder();
        builder.loadbalance("mockloadbalance");

        Assertions.assertEquals("mockloadbalance", builder.build().getLoadbalance());
    }

    @Test
    void async() {
        MethodBuilder builder = new MethodBuilder();
        builder.async(true);

        Assertions.assertTrue(builder.build().isAsync());
    }

    @Test
    void sent() {
        MethodBuilder builder = new MethodBuilder();
        builder.sent(true);

        Assertions.assertTrue(builder.build().getSent());
    }

    @Test
    void mock() {
        MethodBuilder builder = new MethodBuilder();
        builder.mock("mock");
        Assertions.assertEquals("mock", builder.build().getMock());
        builder.mock("return null");
        Assertions.assertEquals("return null", builder.build().getMock());
    }

    @Test
    void mock1() {
        MethodBuilder builder = new MethodBuilder();
        builder.mock(true);
        Assertions.assertEquals("true", builder.build().getMock());
        builder.mock(false);
        Assertions.assertEquals("false", builder.build().getMock());
    }

    @Test
    void merger() {
        MethodBuilder builder = new MethodBuilder();
        builder.merger("merger");
        Assertions.assertEquals("merger", builder.build().getMerger());
    }

    @Test
    void cache() {
        MethodBuilder builder = new MethodBuilder();
        builder.cache("cache");
        Assertions.assertEquals("cache", builder.build().getCache());
    }

    @Test
    void validation() {
        MethodBuilder builder = new MethodBuilder();
        builder.validation("validation");
        Assertions.assertEquals("validation", builder.build().getValidation());
    }

    @Test
    void appendParameter() {
        MethodBuilder builder = new MethodBuilder();
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

        MethodBuilder builder = new MethodBuilder();
        builder.appendParameters(source);

        Map<String, String> parameters = builder.build().getParameters();

        Assertions.assertTrue(parameters.containsKey("default.num"));
        Assertions.assertEquals("ONE", parameters.get("num"));
    }

    @Test
    void forks() {
        MethodBuilder builder = new MethodBuilder();
        builder.forks(5);

        Assertions.assertEquals(5, builder.build().getForks());
    }

    @Test
    void build() {
        MethodBuilder builder = new MethodBuilder();
        builder.id("id").prefix("prefix").timeout(1).retries(2).actives(3).loadbalance("mockloadbalance").async(true)
            .sent(false).mock("mock").merger("merger").cache("cache").validation("validation")
            .appendParameter("default.num", "one");

        MethodConfig config = builder.build();
        MethodConfig config2 = builder.build();

        Assertions.assertEquals("id", config.getId());
        Assertions.assertEquals("prefix", config.getPrefix());
        Assertions.assertEquals(1, config.getTimeout());
        Assertions.assertEquals(2, config.getRetries());
        Assertions.assertEquals(3, config.getActives());
        Assertions.assertEquals("mockloadbalance", config.getLoadbalance());
        Assertions.assertTrue(config.isAsync());
        Assertions.assertFalse(config.getSent());
        Assertions.assertEquals("mock", config.getMock());
        Assertions.assertEquals("merger", config.getMerger());
        Assertions.assertEquals("cache", config.getCache());
        Assertions.assertEquals("validation", config.getValidation());
        Assertions.assertTrue(config.getParameters().containsKey("default.num"));
        Assertions.assertEquals("one", config.getParameters().get("default.num"));

        Assertions.assertNotSame(config, config2);

    }

    private static class MethodBuilder extends AbstractMethodBuilder<MethodConfig, MethodBuilder> {

        public MethodConfig build() {
            MethodConfig parameterConfig = new MethodConfig();
            super.build(parameterConfig);

            return parameterConfig;
        }

        @Override
        protected MethodBuilder getThis() {
            return this;
        }
    }

    private static class MethodConfig extends AbstractMethodConfig { }
}