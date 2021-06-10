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

import org.apache.dubbo.config.AbstractReferenceConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.GENERIC_SERIALIZATION_BEAN;

class AbstractReferenceBuilderTest {

    @Test
    void check() {
        ReferenceBuilder builder = new ReferenceBuilder();
        builder.check(true);
        Assertions.assertTrue(builder.build().isCheck());
        builder.check(false);
        Assertions.assertFalse(builder.build().isCheck());
    }

    @Test
    void init() {
        ReferenceBuilder builder = new ReferenceBuilder();
        builder.init(true);
        Assertions.assertTrue(builder.build().isInit());
        builder.init(false);
        Assertions.assertFalse(builder.build().isInit());
    }

    @Test
    void generic() {
        ReferenceBuilder builder = new ReferenceBuilder();
        builder.generic(true);
        Assertions.assertTrue(builder.build().isGeneric());
        builder.generic(false);
        Assertions.assertFalse(builder.build().isGeneric());
    }

    @Test
    void generic1() {
        ReferenceBuilder builder = new ReferenceBuilder();
        builder.generic(GENERIC_SERIALIZATION_BEAN);
        Assertions.assertEquals(GENERIC_SERIALIZATION_BEAN, builder.build().getGeneric());
    }

    @Test
    void injvm() {
        ReferenceBuilder builder = new ReferenceBuilder();
        builder.injvm(true);
        Assertions.assertTrue(builder.build().isInjvm());
        builder.injvm(false);
        Assertions.assertFalse(builder.build().isInjvm());
    }

    @Test
    void lazy() {
        ReferenceBuilder builder = new ReferenceBuilder();
        builder.lazy(true);
        Assertions.assertTrue(builder.build().getLazy());
        builder.lazy(false);
        Assertions.assertFalse(builder.build().getLazy());
    }

    @Test
    void reconnect() {
        ReferenceBuilder builder = new ReferenceBuilder();
        builder.reconnect("reconnect");
        Assertions.assertEquals("reconnect", builder.build().getReconnect());
    }

    @Test
    void sticky() {
        ReferenceBuilder builder = new ReferenceBuilder();
        builder.sticky(true);
        Assertions.assertTrue(builder.build().getSticky());
        builder.sticky(false);
        Assertions.assertFalse(builder.build().getSticky());
    }

    @Test
    void version() {
        ReferenceBuilder builder = new ReferenceBuilder();
        builder.version("version");
        Assertions.assertEquals("version", builder.build().getVersion());
    }

    @Test
    void group() {
        ReferenceBuilder builder = new ReferenceBuilder();
        builder.group("group");
        Assertions.assertEquals("group", builder.build().getGroup());
    }

    @Test
    void build() {
        ReferenceBuilder builder = new ReferenceBuilder();
        builder.check(true).init(false).generic(true).injvm(false).lazy(true).reconnect("reconnect").sticky(false)
                .version("version").group("group").id("id");

        ReferenceConfig config = builder.build();
        ReferenceConfig config2 = builder.build();

        Assertions.assertEquals("id", config.getId());
        Assertions.assertTrue(config.isCheck());
        Assertions.assertFalse(config.isInit());
        Assertions.assertTrue(config.isGeneric());
        Assertions.assertFalse(config.isInjvm());
        Assertions.assertTrue(config.getLazy());
        Assertions.assertFalse(config.getSticky());
        Assertions.assertEquals("reconnect", config.getReconnect());
        Assertions.assertEquals("version", config.getVersion());
        Assertions.assertEquals("group", config.getGroup());

        Assertions.assertNotSame(config, config2);
    }

    private static class ReferenceBuilder extends AbstractReferenceBuilder<ReferenceConfig, ReferenceBuilder> {

        public ReferenceConfig build() {
            ReferenceConfig parameterConfig = new ReferenceConfig();
            super.build(parameterConfig);

            return parameterConfig;
        }

        @Override
        protected ReferenceBuilder getThis() {
            return this;
        }
    }

    private static class ReferenceConfig extends AbstractReferenceConfig {
    }
}
