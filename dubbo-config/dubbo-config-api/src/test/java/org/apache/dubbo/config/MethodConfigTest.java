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
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.apache.dubbo.config.Constants.*;
import static org.hamcrest.Matchers.*;

class MethodConfigTest {
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
    void testStaticConstructor() throws NoSuchFieldException {
        Method[] methods = this.getClass().getDeclaredField("testField").getAnnotation(Reference.class).methods();
        List<MethodConfig> methodConfigs = MethodConfig.constructMethodConfig(methods);
        MethodConfig methodConfig = methodConfigs.get(0);

        MatcherAssert.assertThat(METHOD_NAME,  Matchers.equalTo(methodConfig.getName()));
        MatcherAssert.assertThat(TIMEOUT, Matchers.equalTo(methodConfig.getTimeout()));
        MatcherAssert.assertThat(RETRIES, Matchers.equalTo(methodConfig.getRetries()));
        MatcherAssert.assertThat(LOADBALANCE, Matchers.equalTo(methodConfig.getLoadbalance()));
        MatcherAssert.assertThat(ASYNC, Matchers.equalTo(methodConfig.isAsync()));
        MatcherAssert.assertThat(ACTIVES, Matchers.equalTo(methodConfig.getActives()));
        MatcherAssert.assertThat(EXECUTES, Matchers.equalTo(methodConfig.getExecutes()));
        MatcherAssert.assertThat(DEPERECATED, Matchers.equalTo(methodConfig.getDeprecated()));
        MatcherAssert.assertThat(STICKY, Matchers.equalTo(methodConfig.getSticky()));
        MatcherAssert.assertThat(ONINVOKE, Matchers.equalTo(methodConfig.getOninvoke()));
        MatcherAssert.assertThat(ONINVOKE_METHOD, Matchers.equalTo(methodConfig.getOninvokeMethod()));
        MatcherAssert.assertThat(ONTHROW, Matchers.equalTo(methodConfig.getOnthrow()));
        MatcherAssert.assertThat(ONTHROW_METHOD, Matchers.equalTo(methodConfig.getOnthrowMethod()));
        MatcherAssert.assertThat(ONRETURN, Matchers.equalTo(methodConfig.getOnreturn()));
        MatcherAssert.assertThat(ONRETURN_METHOD, Matchers.equalTo(methodConfig.getOnreturnMethod()));
        MatcherAssert.assertThat(CACHE, Matchers.equalTo(methodConfig.getCache()));
        MatcherAssert.assertThat(VALIDATION, Matchers.equalTo(methodConfig.getValidation()));
        MatcherAssert.assertThat(ARGUMENTS_INDEX, Matchers.equalTo(methodConfig.getArguments().get(0).getIndex()));
        MatcherAssert.assertThat(ARGUMENTS_CALLBACK, Matchers.equalTo(methodConfig.getArguments().get(0).isCallback()));
        MatcherAssert.assertThat(ARGUMENTS_TYPE, Matchers.equalTo(methodConfig.getArguments().get(0).getType()));
    }

    @Test
    void testName() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setName("hello");
        MatcherAssert.assertThat(method.getName(), Matchers.equalTo("hello"));
        Map<String, String> parameters = new HashMap<>();
        MethodConfig.appendParameters(parameters, method);
        MatcherAssert.assertThat(parameters, not(hasKey("name")));
    }

    @Test
    void testStat() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setStat(10);
        MatcherAssert.assertThat(method.getStat(), Matchers.equalTo(10));
    }

    @Test
    void testRetry() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setRetry(true);
        MatcherAssert.assertThat(method.isRetry(), is(true));
    }

    @Test
    void testReliable() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setReliable(true);
        MatcherAssert.assertThat(method.isReliable(), is(true));
    }

    @Test
    void testExecutes() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setExecutes(10);
        MatcherAssert.assertThat(method.getExecutes(), Matchers.equalTo(10));
    }

    @Test
    void testDeprecated() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setDeprecated(true);
        MatcherAssert.assertThat(method.getDeprecated(), is(true));
    }

    @Test
    void testArguments() throws Exception {
        MethodConfig method = new MethodConfig();
        ArgumentConfig argument = new ArgumentConfig();
        method.setArguments(Collections.singletonList(argument));
        MatcherAssert.assertThat(method.getArguments(), contains(argument));
        MatcherAssert.assertThat(method.getArguments(), Matchers.<ArgumentConfig>hasSize(1));
    }

    @Test
    void testSticky() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setSticky(true);
        MatcherAssert.assertThat(method.getSticky(), is(true));
    }

    //@Test
    public void testOnReturn() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setOnreturn("on-return-object");
        MatcherAssert.assertThat(method.getOnreturn(), Matchers.equalTo("on-return-object"));
        Map<String, String> attributes = new HashMap<>();
        MethodConfig.appendAttributes(attributes, method);
        MatcherAssert.assertThat(attributes, hasEntry(ON_RETURN_INSTANCE_ATTRIBUTE_KEY, "on-return-object"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        MatcherAssert.assertThat(parameters.size(), is(0));
    }

    @Test
    void testOnReturnMethod() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setOnreturnMethod("on-return-method");
        MatcherAssert.assertThat(method.getOnreturnMethod(), Matchers.equalTo("on-return-method"));
        Map<String, String> attributes = new HashMap<>();
        MethodConfig.appendAttributes(attributes, method);
        MatcherAssert.assertThat(attributes, hasEntry((Object) ON_RETURN_METHOD_ATTRIBUTE_KEY, (Object) "on-return-method"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        MatcherAssert.assertThat(parameters.size(), is(0));
    }

    //@Test
    public void testOnThrow() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setOnthrow("on-throw-object");
        MatcherAssert.assertThat(method.getOnthrow(), Matchers.equalTo((Object) "on-throw-object"));
        Map<String, String> attributes = new HashMap<>();
        MethodConfig.appendAttributes(attributes, method);
        MatcherAssert.assertThat(attributes, hasEntry((Object) ON_THROW_INSTANCE_ATTRIBUTE_KEY, (Object) "on-throw-object"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        MatcherAssert.assertThat(parameters.size(), is(0));
    }

    @Test
    void testOnThrowMethod() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setOnthrowMethod("on-throw-method");
        MatcherAssert.assertThat(method.getOnthrowMethod(), Matchers.equalTo("on-throw-method"));
        Map<String, String> attributes = new HashMap<>();
        MethodConfig.appendAttributes(attributes, method);
        MatcherAssert.assertThat(attributes, hasEntry((Object) ON_THROW_METHOD_ATTRIBUTE_KEY, (Object) "on-throw-method"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        MatcherAssert.assertThat(parameters.size(), is(0));
    }

    //@Test
    public void testOnInvoke() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setOninvoke("on-invoke-object");
        MatcherAssert.assertThat(method.getOninvoke(), Matchers.equalTo((Object) "on-invoke-object"));
        Map<String, String> attributes = new HashMap<>();
        MethodConfig.appendAttributes(attributes, method);
        MatcherAssert.assertThat(attributes, hasEntry((Object) ON_INVOKE_INSTANCE_ATTRIBUTE_KEY, (Object) "on-invoke-object"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        MatcherAssert.assertThat(parameters.size(), is(0));
    }

    @Test
    void testOnInvokeMethod() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setOninvokeMethod("on-invoke-method");
        MatcherAssert.assertThat(method.getOninvokeMethod(), Matchers.equalTo("on-invoke-method"));
        Map<String, String> attributes = new HashMap<>();
        MethodConfig.appendAttributes(attributes, method);
        MatcherAssert.assertThat(attributes, hasEntry((Object) ON_INVOKE_METHOD_ATTRIBUTE_KEY, (Object) "on-invoke-method"));
        Map<String, String> parameters = new HashMap<String, String>();
        MethodConfig.appendParameters(parameters, method);
        MatcherAssert.assertThat(parameters.size(), is(0));
    }

    @Test
    void testReturn() throws Exception {
        MethodConfig method = new MethodConfig();
        method.setReturn(true);
        MatcherAssert.assertThat(method.isReturn(), is(true));
    }

    @Test
    void testOverrideMethodConfigOfReference() {

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
    void testAddMethodConfigOfReference() {

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
    void testOverrideMethodConfigOfService() {

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
    void testAddMethodConfigOfService() {

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
    void testVerifyMethodConfigOfService() {

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
    void testIgnoreInvalidMethodConfigOfService() {

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
    void testMetaData() {
        MethodConfig methodConfig = new MethodConfig();
        Map<String, String> metaData = methodConfig.getMetaData();
        Assertions.assertEquals(0, metaData.size(), "Expect empty metadata but found: "+metaData);
    }
}
