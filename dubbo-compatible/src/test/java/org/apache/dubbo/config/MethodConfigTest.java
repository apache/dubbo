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

import org.apache.dubbo.rpc.model.AsyncMethodInfo;
import org.apache.dubbo.service.Person;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.dubbo.config.ArgumentConfig;
import com.alibaba.dubbo.config.MethodConfig;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.config.Constants.ON_INVOKE_INSTANCE_ATTRIBUTE_KEY;
import static org.apache.dubbo.config.Constants.ON_INVOKE_METHOD_ATTRIBUTE_KEY;
import static org.apache.dubbo.config.Constants.ON_RETURN_INSTANCE_ATTRIBUTE_KEY;
import static org.apache.dubbo.config.Constants.ON_RETURN_METHOD_ATTRIBUTE_KEY;
import static org.apache.dubbo.config.Constants.ON_THROW_INSTANCE_ATTRIBUTE_KEY;
import static org.apache.dubbo.config.Constants.ON_THROW_METHOD_ATTRIBUTE_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MethodConfigTest {
    @Test
    void testName() {
        MethodConfig method = new MethodConfig();
        method.setName("hello");
        assertThat(method.getName(), equalTo("hello"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        assertThat(parameters, not(hasKey("name")));
    }

    @Test
    void testStat() {
        MethodConfig method = new MethodConfig();
        method.setStat(10);
        assertThat(method.getStat(), equalTo(10));
    }

    @Test
    void testRetry() {
        MethodConfig method = new MethodConfig();
        method.setRetry(true);
        assertThat(method.isRetry(), is(true));
    }

    @Test
    void testReliable() {
        MethodConfig method = new MethodConfig();
        method.setReliable(true);
        assertThat(method.isReliable(), is(true));
    }

    @Test
    void testExecutes() {
        MethodConfig method = new MethodConfig();
        method.setExecutes(10);
        assertThat(method.getExecutes(), equalTo(10));
    }

    @Test
    void testDeprecated() {
        MethodConfig method = new MethodConfig();
        method.setDeprecated(true);
        assertThat(method.getDeprecated(), is(true));
    }

    @Test
    void testArguments() {
        MethodConfig method = new MethodConfig();
        ArgumentConfig argument = new ArgumentConfig();
        method.setArguments(Collections.singletonList(argument));
        assertThat(method.getArguments(), contains(argument));
        assertThat(method.getArguments(), Matchers.<org.apache.dubbo.config.ArgumentConfig>hasSize(1));
    }

    @Test
    void testSticky() {
        MethodConfig method = new MethodConfig();
        method.setSticky(true);
        assertThat(method.getSticky(), is(true));
    }

    @Test
    void testConvertMethodConfig2AsyncInfo() throws Exception {
        MethodConfig methodConfig = new MethodConfig();
        String methodName = "setName";
        methodConfig.setOninvokeMethod(methodName);
        methodConfig.setOnthrowMethod(methodName);
        methodConfig.setOnreturnMethod(methodName);
        methodConfig.setOninvoke(new Person());
        methodConfig.setOnthrow(new Person());
        methodConfig.setOnreturn(new Person());

        AsyncMethodInfo methodInfo = methodConfig.convertMethodConfig2AsyncInfo();

        assertEquals(methodInfo.getOninvokeMethod(), Person.class.getMethod(methodName, String.class));
        assertEquals(methodInfo.getOnthrowMethod(), Person.class.getMethod(methodName, String.class));
        assertEquals(methodInfo.getOnreturnMethod(), Person.class.getMethod(methodName, String.class));
    }

    // @Test
    void testOnreturn() {
        MethodConfig method = new MethodConfig();
        method.setOnreturn("on-return-object");
        assertThat(method.getOnreturn(), equalTo("on-return-object"));
        Map<String, String> attributes = new HashMap<>();
        MethodConfig.appendAttributes(attributes, method);
        assertThat(attributes, hasEntry(ON_RETURN_INSTANCE_ATTRIBUTE_KEY, "on-return-object"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        assertThat(parameters.size(), is(0));
    }

    @Test
    void testOnreturnMethod() {
        MethodConfig method = new MethodConfig();
        method.setOnreturnMethod("on-return-method");
        assertThat(method.getOnreturnMethod(), equalTo("on-return-method"));
        Map<String, String> attributes = new HashMap<>();
        MethodConfig.appendAttributes(attributes, method);
        assertThat(attributes, hasEntry(ON_RETURN_METHOD_ATTRIBUTE_KEY, "on-return-method"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        assertThat(parameters.size(), is(0));
    }

    // @Test
    void testOnthrow() {
        MethodConfig method = new MethodConfig();
        method.setOnthrow("on-throw-object");
        assertThat(method.getOnthrow(), equalTo("on-throw-object"));
        Map<String, String> attributes = new HashMap<>();
        MethodConfig.appendAttributes(attributes, method);
        assertThat(attributes, hasEntry(ON_THROW_INSTANCE_ATTRIBUTE_KEY, "on-throw-object"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        assertThat(parameters.size(), is(0));
    }

    @Test
    void testOnthrowMethod() {
        MethodConfig method = new MethodConfig();
        method.setOnthrowMethod("on-throw-method");
        assertThat(method.getOnthrowMethod(), equalTo("on-throw-method"));
        Map<String, String> attributes = new HashMap<>();
        MethodConfig.appendAttributes(attributes, method);
        assertThat(attributes, hasEntry(ON_THROW_METHOD_ATTRIBUTE_KEY, "on-throw-method"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        assertThat(parameters.size(), is(0));
    }

    // @Test
    void testOninvoke() {
        MethodConfig method = new MethodConfig();
        method.setOninvoke("on-invoke-object");
        assertThat(method.getOninvoke(), equalTo("on-invoke-object"));
        Map<String, String> attributes = new HashMap<>();
        MethodConfig.appendAttributes(attributes, method);
        assertThat(attributes, hasEntry(ON_INVOKE_INSTANCE_ATTRIBUTE_KEY, "on-invoke-object"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        assertThat(parameters.size(), is(0));
    }

    @Test
    void testOninvokeMethod() {
        MethodConfig method = new MethodConfig();
        method.setOninvokeMethod("on-invoke-method");
        assertThat(method.getOninvokeMethod(), equalTo("on-invoke-method"));
        Map<String, String> attributes = new HashMap<>();
        MethodConfig.appendAttributes(attributes, method);
        assertThat(attributes, hasEntry(ON_INVOKE_METHOD_ATTRIBUTE_KEY, "on-invoke-method"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        assertThat(parameters.size(), is(0));
    }

    @Test
    void testReturn() {
        MethodConfig method = new MethodConfig();
        method.setReturn(true);
        assertThat(method.isReturn(), is(true));
    }
}
