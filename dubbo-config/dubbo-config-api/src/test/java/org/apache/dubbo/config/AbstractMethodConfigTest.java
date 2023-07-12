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
package org.apache.dubbo.config;

import org.apache.dubbo.config.AbstractMethodConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.sameInstance;

class AbstractMethodConfigTest {

    @AfterAll
    public static void afterAll() {
        DubboBootstrap.reset();
    }

    @Test
    void testTimeout() {
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setTimeout(10);
        MatcherAssert.assertThat(methodConfig.getTimeout(), Matchers.equalTo(10));
    }

    @Test
    void testForks() {
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setForks(10);
        MatcherAssert.assertThat(methodConfig.getForks(), Matchers.equalTo(10));
    }

    @Test
    void testRetries() {
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setRetries(3);
        MatcherAssert.assertThat(methodConfig.getRetries(), Matchers.equalTo(3));
    }

    @Test
    void testLoadbalance() {
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setLoadbalance("mockloadbalance");
        MatcherAssert.assertThat(methodConfig.getLoadbalance(), Matchers.equalTo("mockloadbalance"));
    }

    @Test
    void testAsync() {
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setAsync(true);
        MatcherAssert.assertThat(methodConfig.isAsync(), Matchers.is(true));
    }

    @Test
    void testActives() {
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setActives(10);
        MatcherAssert.assertThat(methodConfig.getActives(), Matchers.equalTo(10));
    }

    @Test
    void testSent() {
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setSent(true);
        MatcherAssert.assertThat(methodConfig.getSent(), Matchers.is(true));
    }

    @Test
    void testMock() {
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setMock((Boolean) null);
        MatcherAssert.assertThat(methodConfig.getMock(), Matchers.isEmptyOrNullString());
        methodConfig.setMock(true);
        MatcherAssert.assertThat(methodConfig.getMock(), Matchers.equalTo("true"));
        methodConfig.setMock("return null");
        MatcherAssert.assertThat(methodConfig.getMock(), Matchers.equalTo("return null"));
        methodConfig.setMock("mock");
        MatcherAssert.assertThat(methodConfig.getMock(), Matchers.equalTo("mock"));
    }

    @Test
    void testMerger() {
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setMerger("merger");
        MatcherAssert.assertThat(methodConfig.getMerger(), Matchers.equalTo("merger"));
    }

    @Test
    void testCache() {
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setCache("cache");
        MatcherAssert.assertThat(methodConfig.getCache(), Matchers.equalTo("cache"));
    }

    @Test
    void testValidation() {
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setValidation("validation");
        MatcherAssert.assertThat(methodConfig.getValidation(), Matchers.equalTo("validation"));
    }

    @Test
    void testParameters() throws Exception {
        MethodConfig methodConfig = new MethodConfig();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("key", "value");
        methodConfig.setParameters(parameters);
        MatcherAssert.assertThat(methodConfig.getParameters(), Matchers.sameInstance(parameters));
    }

    private static class MethodConfig extends AbstractMethodConfig {

    }
}
