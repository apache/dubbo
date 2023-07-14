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

import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.hamcrest.MatcherAssert;
import org.testcontainers.shaded.org.hamcrest.Matchers;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.ClusterConstants.CLUSTER_STICKY_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.*;

class AbstractReferenceConfigTest {

    @AfterAll
    public static void afterAll() throws Exception {
        FrameworkModel.destroyAll();
    }

    @Test
    void testCheck() {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setCheck(true);
        MatcherAssert.assertThat(referenceConfig.isCheck(), Matchers.is(true));
    }

    @Test
    void testInit() {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setInit(true);
        MatcherAssert.assertThat(referenceConfig.isInit(), Matchers.is(true));
    }

    @Test
    void testGeneric() {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setGeneric(true);
        MatcherAssert.assertThat(referenceConfig.isGeneric(), Matchers.is(true));
        Map<String, String> parameters = new HashMap<String, String>();
        AbstractInterfaceConfig.appendParameters(parameters, referenceConfig);
        // FIXME: not sure why AbstractReferenceConfig has both isGeneric and getGeneric
        MatcherAssert.assertThat(parameters, Matchers.hasKey("generic"));
    }

    @Test
    void testInjvm() {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setInjvm(true);
        MatcherAssert.assertThat(referenceConfig.isInjvm(), Matchers.is(true));
    }

    @Test
    void testFilter() {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setFilter("mockfilter");
        MatcherAssert.assertThat(referenceConfig.getFilter(), Matchers.equalTo("mockfilter"));
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(REFERENCE_FILTER_KEY, "prefilter");
        AbstractInterfaceConfig.appendParameters(parameters, referenceConfig);
        MatcherAssert.assertThat(parameters, Matchers.hasValue("prefilter,mockfilter"));
    }

//    @Test
//    void testRouter() {
//        ReferenceConfig referenceConfig = new ReferenceConfig();
//        referenceConfig.setRouter("condition");
//        MatcherAssert.assertThat(referenceConfig.getRouter(), Matchers.equalTo("condition"));
//        Map<String, String> parameters = new HashMap<String, String>();
//        parameters.put(ROUTER_KEY, "tag");
//        AbstractInterfaceConfig.appendParameters(parameters, referenceConfig);
//        MatcherAssert.assertThat(parameters, Matchers.hasValue("tag,condition"));
//        URL url = Mockito.mock(URL.class);
//        Mockito.when(url.getParameter(ROUTER_KEY)).thenReturn("condition");
//        List<StateRouterFactory> routerFactories = ExtensionLoader.getExtensionLoader(StateRouterFactory.class).getActivateExtension(url, ROUTER_KEY);
//        MatcherAssert.assertThat(routerFactories.stream().anyMatch(routerFactory -> routerFactory.getClass().equals(ConditionStateRouterFactory.class)), Matchers.is(true));
//        Mockito.when(url.getParameter(ROUTER_KEY)).thenReturn("-tag,-app");
//        routerFactories = ExtensionLoader.getExtensionLoader(StateRouterFactory.class).getActivateExtension(url, ROUTER_KEY);
//        MatcherAssert.assertThat(routerFactories.stream()
//            .allMatch(routerFactory -> !routerFactory.getClass().equals(TagStateRouterFactory.class)
//                && !routerFactory.getClass().equals(AppStateRouterFactory.class)), Matchers.is(true));
//    }

    @Test
    void testListener() {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setListener("mockinvokerlistener");
        MatcherAssert.assertThat(referenceConfig.getListener(), Matchers.equalTo("mockinvokerlistener"));
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(INVOKER_LISTENER_KEY, "prelistener");
        AbstractInterfaceConfig.appendParameters(parameters, referenceConfig);
        MatcherAssert.assertThat(parameters, Matchers.hasValue("prelistener,mockinvokerlistener"));
    }

    @Test
    void testLazy() {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setLazy(true);
        MatcherAssert.assertThat(referenceConfig.getLazy(), Matchers.is(true));
    }

    @Test
    void testOnconnect() {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setOnconnect("onConnect");
        MatcherAssert.assertThat(referenceConfig.getOnconnect(), Matchers.equalTo("onConnect"));
        MatcherAssert.assertThat(referenceConfig.getStubevent(), Matchers.is(true));
    }

    @Test
    void testOndisconnect() {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setOndisconnect("onDisconnect");
        MatcherAssert.assertThat(referenceConfig.getOndisconnect(), Matchers.equalTo("onDisconnect"));
        MatcherAssert.assertThat(referenceConfig.getStubevent(), Matchers.is(true));
    }

    @Test
    void testStubevent() {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setOnconnect("onConnect");
        Map<String, String> parameters = new HashMap<String, String>();
        AbstractInterfaceConfig.appendParameters(parameters, referenceConfig);
        MatcherAssert.assertThat(parameters, Matchers.hasKey(STUB_EVENT_KEY));
    }

    @Test
    void testReconnect() {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setReconnect("reconnect");
        Map<String, String> parameters = new HashMap<String, String>();
        AbstractInterfaceConfig.appendParameters(parameters, referenceConfig);
        MatcherAssert.assertThat(referenceConfig.getReconnect(), Matchers.equalTo("reconnect"));
        MatcherAssert.assertThat(parameters, Matchers.hasKey(Constants.RECONNECT_KEY));
    }

    @Test
    void testSticky() {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setSticky(true);
        Map<String, String> parameters = new HashMap<String, String>();
        AbstractInterfaceConfig.appendParameters(parameters, referenceConfig);
        MatcherAssert.assertThat(referenceConfig.getSticky(), Matchers.is(true));
        MatcherAssert.assertThat(parameters, Matchers.hasKey(CLUSTER_STICKY_KEY));
    }

    @Test
    void testVersion() {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setVersion("version");
        MatcherAssert.assertThat(referenceConfig.getVersion(), Matchers.equalTo("version"));
    }

    @Test
    void testGroup() {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setGroup("group");
        MatcherAssert.assertThat(referenceConfig.getGroup(), Matchers.equalTo("group"));
    }

    @Test
    void testGenericOverride() {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setGeneric("false");
        referenceConfig.refresh();
        Assertions.assertFalse(referenceConfig.isGeneric());
        Assertions.assertEquals("false", referenceConfig.getGeneric());

        ReferenceConfig referenceConfig1 = new ReferenceConfig();
        referenceConfig1.setGeneric(GENERIC_SERIALIZATION_NATIVE_JAVA);
        referenceConfig1.refresh();
        Assertions.assertEquals(GENERIC_SERIALIZATION_NATIVE_JAVA, referenceConfig1.getGeneric());
        Assertions.assertTrue(referenceConfig1.isGeneric());

        ReferenceConfig referenceConfig2 = new ReferenceConfig();
        referenceConfig2.refresh();
        Assertions.assertNull(referenceConfig2.getGeneric());
    }

    private static class ReferenceConfig extends AbstractReferenceConfig {

    }
}
