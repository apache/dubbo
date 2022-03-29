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
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private static final String ONINVOKE = "invokeNotify";
    private static final String ONINVOKE_METHOD = "onInvoke";
    private static final String ONTHROW = "throwNotify";
    private static final String ONTHROW_METHOD = "onThrow";
    private static final String ONRETURN = "returnNotify";
    private static final String ONRETURN_METHOD = "onReturn";
    private static final String CACHE = "c";
    private static final String VALIDATION = "v";
    private static final int ARGUMENTS_INDEX = 24;
    private static final boolean ARGUMENTS_CALLBACK = true;
    private static final String ARGUMENTS_TYPE = "sss";

    @Reference(methods = {@Method(name = METHOD_NAME, timeout = TIMEOUT, retries = RETRIES, loadbalance = LOADBALANCE, async = ASYNC,
            actives = ACTIVES, executes = EXECUTES, deprecated = DEPERECATED, sticky = STICKY, oninvoke = ONINVOKE+"."+ONINVOKE_METHOD,
            onthrow = ONTHROW+"."+ONTHROW_METHOD, onreturn = ONRETURN+"."+ONRETURN_METHOD, cache = CACHE, validation = VALIDATION,
            arguments = {@Argument(index = ARGUMENTS_INDEX, callback = ARGUMENTS_CALLBACK, type = ARGUMENTS_TYPE)})})
    private String testField;

    @BeforeEach
    public void beforeEach() {
        DubboBootstrap.reset();
    }

    @AfterEach
    public void afterEach() {
        SysProps.clear();
    }

    //TODO remove this test
    @Test
    public void testStaticConstructor() throws NoSuchFieldException {
        Method[] methods = this.getClass().getDeclaredField("testField").getAnnotation(Reference.class).methods();
        List<MethodConfig> methodConfigs = MethodConfig.constructMethodConfig(methods);
        MethodConfig methodConfig = methodConfigs.get(0);

        assertThat(METHOD_NAME, equalTo(methodConfig.getName()));
        assertThat(TIMEOUT, equalTo(methodConfig.getTimeout()));
        assertThat(RETRIES, equalTo(methodConfig.getRetries()));
        assertThat(LOADBALANCE, equalTo(methodConfig.getLoadbalance()));
        assertThat(ASYNC, equalTo(methodConfig.isAsync()));
        assertThat(ACTIVES, equalTo(methodConfig.getActives()));
        assertThat(EXECUTES, equalTo(methodConfig.getExecutes()));
        assertThat(DEPERECATED, equalTo(methodConfig.getDeprecated()));
        assertThat(STICKY, equalTo(methodConfig.getSticky()));
        assertThat(ONINVOKE, equalTo(methodConfig.getOninvoke()));
        assertThat(ONINVOKE_METHOD, equalTo(methodConfig.getOninvokeMethod()));
        assertThat(ONTHROW, equalTo(methodConfig.getOnthrow()));
        assertThat(ONTHROW_METHOD, equalTo(methodConfig.getOnthrowMethod()));
        assertThat(ONRETURN, equalTo(methodConfig.getOnreturn()));
        assertThat(ONRETURN_METHOD, equalTo(methodConfig.getOnreturnMethod()));
        assertThat(CACHE, equalTo(methodConfig.getCache()));
        assertThat(VALIDATION, equalTo(methodConfig.getValidation()));
        assertThat(ARGUMENTS_INDEX, equalTo(methodConfig.getArguments().get(0).getIndex()));
        assertThat(ARGUMENTS_CALLBACK, equalTo(methodConfig.getArguments().get(0).isCallback()));
        assertThat(ARGUMENTS_TYPE, equalTo(methodConfig.getArguments().get(0).getType()));
    }

    @Test
    public void testName() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setName("hello");
        assertThat(method.getName(), equalTo("hello"));
        Map<String, String> parameters = new HashMap<>();
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

    //@Test
    public void testOnReturn() throws Exception {
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
    public void testOnReturnMethod() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setOnreturnMethod("on-return-method");
        assertThat(method.getOnreturnMethod(), equalTo("on-return-method"));
        Map<String, String> attributes = new HashMap<>();
        MethodConfig.appendAttributes(attributes, method);
        assertThat(attributes, hasEntry((Object) ON_RETURN_METHOD_ATTRIBUTE_KEY, (Object) "on-return-method"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        assertThat(parameters.size(), is(0));
    }

    //@Test
    public void testOnThrow() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setOnthrow("on-throw-object");
        assertThat(method.getOnthrow(), equalTo((Object) "on-throw-object"));
        Map<String, String> attributes = new HashMap<>();
        MethodConfig.appendAttributes(attributes, method);
        assertThat(attributes, hasEntry((Object) ON_THROW_INSTANCE_ATTRIBUTE_KEY, (Object) "on-throw-object"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        assertThat(parameters.size(), is(0));
    }

    @Test
    public void testOnThrowMethod() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setOnthrowMethod("on-throw-method");
        assertThat(method.getOnthrowMethod(), equalTo("on-throw-method"));
        Map<String, String> attributes = new HashMap<>();
        MethodConfig.appendAttributes(attributes, method);
        assertThat(attributes, hasEntry((Object) ON_THROW_METHOD_ATTRIBUTE_KEY, (Object) "on-throw-method"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        assertThat(parameters.size(), is(0));
    }

    //@Test
    public void testOnInvoke() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setOninvoke("on-invoke-object");
        assertThat(method.getOninvoke(), equalTo((Object) "on-invoke-object"));
        Map<String, String> attributes = new HashMap<>();
        MethodConfig.appendAttributes(attributes, method);
        assertThat(attributes, hasEntry((Object) ON_INVOKE_INSTANCE_ATTRIBUTE_KEY, (Object) "on-invoke-object"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        assertThat(parameters.size(), is(0));
    }

    @Test
    public void testOnInvokeMethod() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setOninvokeMethod("on-invoke-method");
        assertThat(method.getOninvokeMethod(), equalTo("on-invoke-method"));
        Map<String, String> attributes = new HashMap<>();
        MethodConfig.appendAttributes(attributes, method);
        assertThat(attributes, hasEntry((Object) ON_INVOKE_METHOD_ATTRIBUTE_KEY, (Object) "on-invoke-method"));
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

    @Test
    public void testOverrideMethodConfigOfReference() {

        String interfaceName = DemoService.class.getName();
        SysProps.setProperty("dubbo.reference."+ interfaceName +".sayName.timeout", "1234");
        SysProps.setProperty("dubbo.reference."+ interfaceName +".sayName.sticky", "true");
        SysProps.setProperty("dubbo.reference."+ interfaceName +".sayName.parameters", "[{a:1},{b:2}]");
        SysProps.setProperty("dubbo.reference."+ interfaceName +".init", "false");

        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(interfaceName);
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName("sayName");
        methodConfig.setTimeout(1000);
        referenceConfig.setMethods(Arrays.asList(methodConfig));

        DubboBootstrap.getInstance()
            .application("demo-app")
            .reference(referenceConfig)
            .initialize();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("a", "1");
        params.put("b", "2");

        Assertions.assertEquals(1234, methodConfig.getTimeout());
        Assertions.assertEquals(true, methodConfig.getSticky());
        Assertions.assertEquals(params, methodConfig.getParameters());
        Assertions.assertEquals(false, referenceConfig.isInit());

    }

    @Test
    public void testAddMethodConfigOfReference() {

        String interfaceName = DemoService.class.getName();
        SysProps.setProperty("dubbo.reference."+ interfaceName +".sayName.timeout", "1234");
        SysProps.setProperty("dubbo.reference."+ interfaceName +".sayName.sticky", "true");
        SysProps.setProperty("dubbo.reference."+ interfaceName +".sayName.parameters", "[{a:1},{b:2}]");
        SysProps.setProperty("dubbo.reference."+ interfaceName +".init", "false");

        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(interfaceName);

        DubboBootstrap.getInstance()
            .application("demo-app")
            .reference(referenceConfig)
            .initialize();

        List<MethodConfig> methodConfigs = referenceConfig.getMethods();
        Assertions.assertEquals(1, methodConfigs.size());
        MethodConfig methodConfig = methodConfigs.get(0);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("a", "1");
        params.put("b", "2");

        Assertions.assertEquals(1234, methodConfig.getTimeout());
        Assertions.assertEquals(true, methodConfig.getSticky());
        Assertions.assertEquals(params, methodConfig.getParameters());
        Assertions.assertEquals(false, referenceConfig.isInit());

        DubboBootstrap.getInstance().destroy();

    }

    @Test
    public void testOverrideMethodConfigOfService() {

        String interfaceName = DemoService.class.getName();
        SysProps.setProperty("dubbo.service."+ interfaceName +".sayName.timeout", "1234");
        SysProps.setProperty("dubbo.service."+ interfaceName +".sayName.sticky", "true");
        SysProps.setProperty("dubbo.service."+ interfaceName +".sayName.parameters", "[{a:1},{b:2}]");
        SysProps.setProperty("dubbo.service."+ interfaceName +".group", "demo");
        SysProps.setProperty("dubbo.registry.address", "N/A");

        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(interfaceName);
        serviceConfig.setRef(new DemoServiceImpl());
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName("sayName");
        methodConfig.setTimeout(1000);
        serviceConfig.setMethods(Collections.singletonList(methodConfig));

        DubboBootstrap.getInstance()
            .application("demo-app")
            .service(serviceConfig)
            .initialize();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("a", "1");
        params.put("b", "2");

        Assertions.assertEquals(1234, methodConfig.getTimeout());
        Assertions.assertEquals(true, methodConfig.getSticky());
        Assertions.assertEquals(params, methodConfig.getParameters());
        Assertions.assertEquals("demo", serviceConfig.getGroup());

        DubboBootstrap.getInstance().destroy();

    }

    @Test
    public void testAddMethodConfigOfService() {

        String interfaceName = DemoService.class.getName();
        SysProps.setProperty("dubbo.service."+ interfaceName +".sayName.timeout", "1234");
        SysProps.setProperty("dubbo.service."+ interfaceName +".sayName.sticky", "true");
        SysProps.setProperty("dubbo.service."+ interfaceName +".sayName.parameters", "[{a:1},{b:2}]");
        SysProps.setProperty("dubbo.service."+ interfaceName +".sayName.0.callback", "true");
        SysProps.setProperty("dubbo.service."+ interfaceName +".group", "demo");
        SysProps.setProperty("dubbo.service."+ interfaceName +".echo", "non-method-config");
        SysProps.setProperty("dubbo.registry.address", "N/A");

        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(interfaceName);
        serviceConfig.setRef(new DemoServiceImpl());

        Assertions.assertNull(serviceConfig.getMethods());

        DubboBootstrap.getInstance()
            .application("demo-app")
            .service(serviceConfig)
            .initialize();

        List<MethodConfig> methodConfigs = serviceConfig.getMethods();
        Assertions.assertEquals(1, methodConfigs.size());
        MethodConfig methodConfig = methodConfigs.get(0);

        List<ArgumentConfig> arguments = methodConfig.getArguments();
        Assertions.assertEquals(1, arguments.size());
        ArgumentConfig argumentConfig = arguments.get(0);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("a", "1");
        params.put("b", "2");

        Assertions.assertEquals("demo", serviceConfig.getGroup());
        Assertions.assertEquals(params, methodConfig.getParameters());
        Assertions.assertEquals(1234, methodConfig.getTimeout());
        Assertions.assertEquals(true, methodConfig.getSticky());
        Assertions.assertEquals(0, argumentConfig.getIndex());
        Assertions.assertEquals(true, argumentConfig.isCallback());
        DubboBootstrap.getInstance().destroy();
    }

    @Test
    public void testVerifyMethodConfigOfService() {

        String interfaceName = DemoService.class.getName();
        SysProps.setProperty("dubbo.service."+ interfaceName +".sayHello.timeout", "1234");
        SysProps.setProperty("dubbo.service."+ interfaceName +".group", "demo");
        SysProps.setProperty("dubbo.registry.address", "N/A");

        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(interfaceName);
        serviceConfig.setRef(new DemoServiceImpl());
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName("sayHello");
        methodConfig.setTimeout(1000);
        serviceConfig.setMethods(Collections.singletonList(methodConfig));

        try {
            DubboBootstrap.getInstance()
                .application("demo-app")
                .service(serviceConfig)
                .initialize();
            Assertions.fail("Method config verification should failed");
        } catch (Exception e) {
            // ignore
            Throwable cause = e.getCause();
            Assertions.assertEquals(IllegalStateException.class, cause.getClass());
            Assertions.assertTrue(cause.getMessage().contains("not found method"), cause.toString());
        }finally {
            DubboBootstrap.getInstance().destroy();
        }
    }

    @Test
    public void testIgnoreInvalidMethodConfigOfService() {

        String interfaceName = DemoService.class.getName();
        SysProps.setProperty("dubbo.service."+ interfaceName +".sayHello.timeout", "1234");
        SysProps.setProperty("dubbo.service."+ interfaceName +".sayName.timeout", "1234");
        SysProps.setProperty("dubbo.registry.address", "N/A");
        SysProps.setProperty(ConfigKeys.DUBBO_CONFIG_IGNORE_INVALID_METHOD_CONFIG, "true");

        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(interfaceName);
        serviceConfig.setRef(new DemoServiceImpl());
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName("sayHello");
        methodConfig.setTimeout(1000);
        serviceConfig.setMethods(Collections.singletonList(methodConfig));

        DubboBootstrap.getInstance()
            .application("demo-app")
            .service(serviceConfig)
            .initialize();

        // expect sayHello method config will be ignored, and sayName method config will be created.
        Assertions.assertEquals(1, serviceConfig.getMethods().size());
        Assertions.assertEquals("sayName", serviceConfig.getMethods().get(0).getName());
        DubboBootstrap.getInstance().destroy();
    }

    @Test
    public void testMetaData() {
        MethodConfig methodConfig = new MethodConfig();
        Map<String, String> metaData = methodConfig.getMetaData();
        Assertions.assertEquals(0, metaData.size(), "Expect empty metadata but found: "+metaData);
    }
}
