/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.config;

import org.apache.dubbo.config.annotation.Argument;
import org.apache.dubbo.config.annotation.Method;
import org.apache.dubbo.config.annotation.Reference;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.config.Constants.ON_INVOKE_INSTANCE_KEY;
import static org.apache.dubbo.config.Constants.ON_INVOKE_METHOD_KEY;
import static org.apache.dubbo.config.Constants.ON_RETURN_INSTANCE_KEY;
import static org.apache.dubbo.config.Constants.ON_RETURN_METHOD_KEY;
import static org.apache.dubbo.config.Constants.ON_THROW_INSTANCE_KEY;
import static org.apache.dubbo.config.Constants.ON_THROW_METHOD_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class MethodConfigTest {
    private static final String METHOD_NAME = "sayHello";
    private static final int TIMEOUT = 1300;
    private static final int RETRIES = 4;
    private static final String LOADBALANCE = "random";
    private static final boolean ASYNC = true;
    private static final int ACTIVES = 3;
    private static final int EXECUTES = 5;
    private static final boolean DEPERECATED = true;
    private static final boolean STICKY = true;
    private static final String ONINVOKE = "i";
    private static final String ONTHROW = "t";
    private static final String ONRETURN = "r";
    private static final String CACHE = "c";
    private static final String VALIDATION = "v";
    private static final int ARGUMENTS_INDEX = 24;
    private static final boolean ARGUMENTS_CALLBACK = true;
    private static final String ARGUMENTS_TYPE = "sss";

    @Reference(methods = {@Method(name = METHOD_NAME, timeout = TIMEOUT, retries = RETRIES, loadbalance = LOADBALANCE, async = ASYNC,
            actives = ACTIVES, executes = EXECUTES, deprecated = DEPERECATED, sticky = STICKY, oninvoke = ONINVOKE, onthrow = ONTHROW, onreturn = ONRETURN, cache = CACHE, validation = VALIDATION,
            arguments = {@Argument(index = ARGUMENTS_INDEX, callback = ARGUMENTS_CALLBACK, type = ARGUMENTS_TYPE)})})
    private String testField;

    @Test
    public void testStaticConstructor() throws NoSuchFieldException {
        Method[] methods = this.getClass().getDeclaredField("testField").getAnnotation(Reference.class).methods();
        List<MethodConfig> methodConfigs = MethodConfig.constructMethodConfig(methods);
        MethodConfig methodConfig = methodConfigs.get(0);

        assertThat(METHOD_NAME, equalTo(methodConfig.getName()));
        assertThat(TIMEOUT, equalTo(methodConfig.getTimeout().intValue()));
        assertThat(RETRIES, equalTo(methodConfig.getRetries().intValue()));
        assertThat(LOADBALANCE, equalTo(methodConfig.getLoadbalance()));
        assertThat(ASYNC, equalTo(methodConfig.isAsync()));
        assertThat(ACTIVES, equalTo(methodConfig.getActives().intValue()));
        assertThat(EXECUTES, equalTo(methodConfig.getExecutes().intValue()));
        assertThat(DEPERECATED, equalTo(methodConfig.getDeprecated()));
        assertThat(STICKY, equalTo(methodConfig.getSticky()));
        assertThat(ONINVOKE, equalTo(methodConfig.getOninvoke()));
        assertThat(ONTHROW, equalTo(methodConfig.getOnthrow()));
        assertThat(ONRETURN, equalTo(methodConfig.getOnreturn()));
        assertThat(CACHE, equalTo(methodConfig.getCache()));
        assertThat(VALIDATION, equalTo(methodConfig.getValidation()));
        assertThat(ARGUMENTS_INDEX, equalTo(methodConfig.getArguments().get(0).getIndex().intValue()));
        assertThat(ARGUMENTS_CALLBACK, equalTo(methodConfig.getArguments().get(0).isCallback()));
        assertThat(ARGUMENTS_TYPE, equalTo(methodConfig.getArguments().get(0).getType()));
    }

    @Test
    public void testName() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setName("hello");
        assertThat(method.getName(), equalTo("hello"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        assertThat(parameters, not(hasKey("name")));
    }

    @Test
    public void testStat() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setStat(10);
        assertThat(method.getStat(), equalTo(10));
    }

    @Test
    public void testRetry() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setRetry(true);
        assertThat(method.isRetry(), is(true));
    }

    @Test
    public void testReliable() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setReliable(true);
        assertThat(method.isReliable(), is(true));
    }

    @Test
    public void testExecutes() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setExecutes(10);
        assertThat(method.getExecutes(), equalTo(10));
    }

    @Test
    public void testDeprecated() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setDeprecated(true);
        assertThat(method.getDeprecated(), is(true));
    }

    @Test
    public void testArguments() throws Exception {
        MethodConfig method = new MethodConfig();
        ArgumentConfig argument = new ArgumentConfig();
        method.setArguments(Collections.singletonList(argument));
        assertThat(method.getArguments(), contains(argument));
        assertThat(method.getArguments(), Matchers.<ArgumentConfig>hasSize(1));
    }

    @Test
    public void testSticky() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setSticky(true);
        assertThat(method.getSticky(), is(true));
    }

    @Test
    public void testOnreturn() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setOnreturn("on-return-object");
        assertThat(method.getOnreturn(), equalTo((Object) "on-return-object"));
        Map<String, Object> attribute = new HashMap<String, Object>();
        MethodConfig.appendAttributes(attribute, method);
        assertThat(attribute, hasEntry((Object) ON_RETURN_INSTANCE_KEY, (Object) "on-return-object"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        assertThat(parameters.size(), is(0));
    }

    @Test
    public void testOnreturnMethod() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setOnreturnMethod("on-return-method");
        assertThat(method.getOnreturnMethod(), equalTo("on-return-method"));
        Map<String, Object> attribute = new HashMap<String, Object>();
        MethodConfig.appendAttributes(attribute, method);
        assertThat(attribute, hasEntry((Object) ON_RETURN_METHOD_KEY, (Object) "on-return-method"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        assertThat(parameters.size(), is(0));
    }

    @Test
    public void testOnthrow() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setOnthrow("on-throw-object");
        assertThat(method.getOnthrow(), equalTo((Object) "on-throw-object"));
        Map<String, Object> attribute = new HashMap<String, Object>();
        MethodConfig.appendAttributes(attribute, method);
        assertThat(attribute, hasEntry((Object) ON_THROW_INSTANCE_KEY, (Object) "on-throw-object"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        assertThat(parameters.size(), is(0));
    }

    @Test
    public void testOnthrowMethod() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setOnthrowMethod("on-throw-method");
        assertThat(method.getOnthrowMethod(), equalTo("on-throw-method"));
        Map<String, Object> attribute = new HashMap<String, Object>();
        MethodConfig.appendAttributes(attribute, method);
        assertThat(attribute, hasEntry((Object) ON_THROW_METHOD_KEY, (Object) "on-throw-method"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        assertThat(parameters.size(), is(0));
    }

    @Test
    public void testOninvoke() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setOninvoke("on-invoke-object");
        assertThat(method.getOninvoke(), equalTo((Object) "on-invoke-object"));
        Map<String, Object> attribute = new HashMap<String, Object>();
        MethodConfig.appendAttributes(attribute, method);
        assertThat(attribute, hasEntry((Object) ON_INVOKE_INSTANCE_KEY, (Object) "on-invoke-object"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        assertThat(parameters.size(), is(0));
    }

    @Test
    public void testOninvokeMethod() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setOninvokeMethod("on-invoke-method");
        assertThat(method.getOninvokeMethod(), equalTo("on-invoke-method"));
        Map<String, Object> attribute = new HashMap<String, Object>();
        MethodConfig.appendAttributes(attribute, method);
        assertThat(attribute, hasEntry((Object) ON_INVOKE_METHOD_KEY, (Object) "on-invoke-method"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        assertThat(parameters.size(), is(0));
    }

    @Test
    public void testReturn() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setReturn(true);
        assertThat(method.isReturn(), is(true));
    }
}
