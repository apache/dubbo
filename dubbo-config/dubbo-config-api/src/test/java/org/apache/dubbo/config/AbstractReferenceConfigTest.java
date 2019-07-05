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

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.rpc.cluster.Constants.CLUSTER_STICKY_KEY;
import static org.apache.dubbo.rpc.Constants.INVOKER_LISTENER_KEY;
import static org.apache.dubbo.rpc.Constants.REFERENCE_FILTER_KEY;
import static org.apache.dubbo.rpc.Constants.STUB_EVENT_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.is;

public class AbstractReferenceConfigTest {

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
        referenceConfig.setInit(true);
        assertThat(referenceConfig.isInit(), is(true));
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

    private static class ReferenceConfig extends AbstractReferenceConfig {

    }
}
