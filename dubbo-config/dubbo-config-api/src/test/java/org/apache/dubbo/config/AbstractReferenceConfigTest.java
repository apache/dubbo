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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.cluster.router.condition.ConditionStateRouterFactory;
import org.apache.dubbo.rpc.cluster.router.condition.config.AppStateRouterFactory;
import org.apache.dubbo.rpc.cluster.router.state.StateRouterFactory;
import org.apache.dubbo.rpc.cluster.router.tag.TagStateRouterFactory;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.GENERIC_SERIALIZATION_NATIVE_JAVA;
import static org.apache.dubbo.common.constants.CommonConstants.INVOKER_LISTENER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REFERENCE_FILTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.STUB_EVENT_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.CLUSTER_STICKY_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.ROUTER_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractReferenceConfigTest {

    @AfterAll
    public static void afterAll() throws Exception {
        FrameworkModel.destroyAll();
    }

    @Test
    public void testCheck() throws Exception {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setCheck(true);
        assertThat(referenceConfig.isCheck(), is(true));
    }

    @Test
    public void testInit() throws Exception {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setInit(true);
        assertThat(referenceConfig.isInit(), is(true));
    }

    @Test
    public void testGeneric() throws Exception {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setGeneric(true);
        assertThat(referenceConfig.isGeneric(), is(true));
        Map<String, String> parameters = new HashMap<String, String>();
        AbstractInterfaceConfig.appendParameters(parameters, referenceConfig);
        // FIXME: not sure why AbstractReferenceConfig has both isGeneric and getGeneric
        assertThat(parameters, hasKey("generic"));
    }

    @Test
    public void testInjvm() throws Exception {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setInjvm(true);
        assertThat(referenceConfig.isInjvm(), is(true));
    }

    @Test
    public void testFilter() throws Exception {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setFilter("mockfilter");
        assertThat(referenceConfig.getFilter(), equalTo("mockfilter"));
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(REFERENCE_FILTER_KEY, "prefilter");
        AbstractInterfaceConfig.appendParameters(parameters, referenceConfig);
        assertThat(parameters, hasValue("prefilter,mockfilter"));
    }

    @Test
    public void testRouter() throws Exception {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setRouter("condition");
        assertThat(referenceConfig.getRouter(), equalTo("condition"));
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(ROUTER_KEY, "tag");
        AbstractInterfaceConfig.appendParameters(parameters, referenceConfig);
        assertThat(parameters, hasValue("tag,condition"));
        URL url = mock(URL.class);
        when(url.getParameter(ROUTER_KEY)).thenReturn("condition");
        List<StateRouterFactory> routerFactories = ExtensionLoader.getExtensionLoader(StateRouterFactory.class).getActivateExtension(url, ROUTER_KEY);
        assertThat(routerFactories.stream().anyMatch(routerFactory -> routerFactory.getClass().equals(ConditionStateRouterFactory.class)), is(true));
        when(url.getParameter(ROUTER_KEY)).thenReturn("-tag,-app");
        routerFactories = ExtensionLoader.getExtensionLoader(StateRouterFactory.class).getActivateExtension(url, ROUTER_KEY);
        assertThat(routerFactories.stream()
            .allMatch(routerFactory -> !routerFactory.getClass().equals(TagStateRouterFactory.class)
                && !routerFactory.getClass().equals(AppStateRouterFactory.class)), is(true));
    }

    @Test
    public void testListener() throws Exception {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setListener("mockinvokerlistener");
        assertThat(referenceConfig.getListener(), equalTo("mockinvokerlistener"));
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(INVOKER_LISTENER_KEY, "prelistener");
        AbstractInterfaceConfig.appendParameters(parameters, referenceConfig);
        assertThat(parameters, hasValue("prelistener,mockinvokerlistener"));
    }

    @Test
    public void testLazy() throws Exception {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setLazy(true);
        assertThat(referenceConfig.getLazy(), is(true));
    }

    @Test
    public void testOnconnect() throws Exception {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setOnconnect("onConnect");
        assertThat(referenceConfig.getOnconnect(), equalTo("onConnect"));
        assertThat(referenceConfig.getStubevent(), is(true));
    }

    @Test
    public void testOndisconnect() throws Exception {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setOndisconnect("onDisconnect");
        assertThat(referenceConfig.getOndisconnect(), equalTo("onDisconnect"));
        assertThat(referenceConfig.getStubevent(), is(true));
    }

    @Test
    public void testStubevent() throws Exception {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setOnconnect("onConnect");
        Map<String, String> parameters = new HashMap<String, String>();
        AbstractInterfaceConfig.appendParameters(parameters, referenceConfig);
        assertThat(parameters, hasKey(STUB_EVENT_KEY));
    }

    @Test
    public void testReconnect() throws Exception {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setReconnect("reconnect");
        Map<String, String> parameters = new HashMap<String, String>();
        AbstractInterfaceConfig.appendParameters(parameters, referenceConfig);
        assertThat(referenceConfig.getReconnect(), equalTo("reconnect"));
        assertThat(parameters, hasKey(Constants.RECONNECT_KEY));
    }

    @Test
    public void testSticky() throws Exception {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setSticky(true);
        Map<String, String> parameters = new HashMap<String, String>();
        AbstractInterfaceConfig.appendParameters(parameters, referenceConfig);
        assertThat(referenceConfig.getSticky(), is(true));
        assertThat(parameters, hasKey(CLUSTER_STICKY_KEY));
    }

    @Test
    public void testVersion() throws Exception {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setVersion("version");
        assertThat(referenceConfig.getVersion(), equalTo("version"));
    }

    @Test
    public void testGroup() throws Exception {
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setGroup("group");
        assertThat(referenceConfig.getGroup(), equalTo("group"));
    }

    @Test
    public void testGenericOverride() {
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
