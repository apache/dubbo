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

import org.apache.dubbo.config.AbstractConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class AbstractBuilderTest {

    @Test
    void id() {
        Builder builder = new Builder();
        builder.id("id");
        Assertions.assertEquals("id", builder.build().getId());
    }

    @Test
    void appendParameter() {
        Map<String, String> source = null;

        Map<String, String> parameters = new HashMap<>();
        parameters.put("default.num", "one");
        parameters.put("num", "ONE");
        source = AbstractBuilder.appendParameters(source, parameters);

        Assertions.assertTrue(source.containsKey("default.num"));
        Assertions.assertEquals("ONE", source.get("num"));
    }

    @Test
    void appendParameter2() {
        Map<String, String> source = new HashMap<>();
        source.put("default.num", "one1");
        source.put("num", "ONE1");

        Map<String, String> parameters = new HashMap<>();
        parameters.put("default.num", "one");
        parameters.put("num", "ONE");
        source = AbstractBuilder.appendParameters(source, parameters);

        Assertions.assertTrue(source.containsKey("default.num"));
        Assertions.assertEquals("ONE", source.get("num"));
    }

    @Test
    void appendParameters() {
        Map<String, String> source = null;

        source = AbstractBuilder.appendParameter(source, "default.num", "one");
        source = AbstractBuilder.appendParameter(source, "num", "ONE");

        Assertions.assertTrue(source.containsKey("default.num"));
        Assertions.assertEquals("ONE", source.get("num"));
    }

    @Test
    void appendParameters2() {
        Map<String, String> source = new HashMap<>();
        source.put("default.num", "one1");
        source.put("num", "ONE1");

        source = AbstractBuilder.appendParameter(source, "default.num", "one");
        source = AbstractBuilder.appendParameter(source, "num", "ONE");

        Assertions.assertTrue(source.containsKey("default.num"));
        Assertions.assertEquals("ONE", source.get("num"));
    }

    @Test
    void build() {
        Builder builder = new Builder();
        builder.id("id");

        Config config = builder.build();
        Config config2 = builder.build();

        Assertions.assertEquals("id", config.getId());

        Assertions.assertNotSame(config, config2);
    }

    private static class Builder extends AbstractBuilder<Config, Builder> {
        public Config build() {
            Config parameterConfig = new Config();
            super.build(parameterConfig);

            return parameterConfig;
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }

    private static class Config extends AbstractConfig {
    }
}