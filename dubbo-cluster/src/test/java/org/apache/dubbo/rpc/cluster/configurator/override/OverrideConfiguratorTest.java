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
package org.apache.dubbo.rpc.cluster.configurator.override;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.cluster.configurator.absent.AbsentConfigurator;
import org.apache.dubbo.rpc.cluster.configurator.consts.UrlConstant;
import org.apache.dubbo.rpc.cluster.configurator.parser.model.ConditionMatch;
import org.apache.dubbo.rpc.cluster.configurator.parser.model.ParamMatch;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match.StringMatch;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.rpc.cluster.configurator.parser.model.ConfiguratorConfig.MATCH_CONDITION;

/**
 * OverrideConfiguratorTest
 */
class OverrideConfiguratorTest {

    @Test
    void testOverride_Application() {
        OverrideConfigurator configurator = new OverrideConfigurator(URL.valueOf("override://foo@0.0.0.0/com.foo.BarService?timeout=200"));

        URL url = configurator.configure(URL.valueOf(UrlConstant.URL_CONSUMER));
        Assertions.assertEquals("200", url.getParameter("timeout"));

        url = configurator.configure(URL.valueOf(UrlConstant.URL_ONE));
        Assertions.assertEquals("200", url.getParameter("timeout"));

        url = configurator.configure(URL.valueOf(UrlConstant.APPLICATION_BAR_SIDE_CONSUMER_11));
        Assertions.assertNull(url.getParameter("timeout"));

        url = configurator.configure(URL.valueOf(UrlConstant.TIMEOUT_1000_SIDE_CONSUMER_11));
        Assertions.assertEquals("1000", url.getParameter("timeout"));
    }

    @Test
    void testOverride_Host() {
        OverrideConfigurator configurator = new OverrideConfigurator(URL.valueOf("override://" + NetUtils.getLocalHost() + "/com.foo.BarService?timeout=200"));

        URL url = configurator.configure(URL.valueOf(UrlConstant.URL_CONSUMER));
        Assertions.assertEquals("200", url.getParameter("timeout"));

        url = configurator.configure(URL.valueOf(UrlConstant.URL_ONE));
        Assertions.assertEquals("200", url.getParameter("timeout"));

        AbsentConfigurator configurator1 = new AbsentConfigurator(URL.valueOf("override://10.20.153.10/com.foo.BarService?timeout=200"));

        url = configurator1.configure(URL.valueOf(UrlConstant.APPLICATION_BAR_SIDE_CONSUMER_10));
        Assertions.assertNull(url.getParameter("timeout"));

        url = configurator1.configure(URL.valueOf(UrlConstant.TIMEOUT_1000_SIDE_CONSUMER_10));
        Assertions.assertEquals("1000", url.getParameter("timeout"));
    }

    // Test the version after 2.7
    @Test
    void testOverrideForVersion27() {
        {
            String consumerUrlV27 = "dubbo://172.24.160.179/com.foo.BarService?application=foo&side=consumer&timeout=100";

            URL consumerConfiguratorUrl = URL.valueOf("override://0.0.0.0/com.foo.BarService");
            Map<String, String> params = new HashMap<>();
            params.put("side", "consumer");
            params.put("configVersion", "2.7");
            params.put("application", "foo");
            params.put("timeout", "10000");

            consumerConfiguratorUrl = consumerConfiguratorUrl.addParameters(params);

            OverrideConfigurator configurator = new OverrideConfigurator(consumerConfiguratorUrl);
            // Meet the configured conditions:
            // same side
            // The port of configuratorUrl is 0
            // The host of configuratorUrl is 0.0.0.0 or the local address is the same as consumerUrlV27
            // same appName
            URL url = configurator.configure(URL.valueOf(consumerUrlV27));
            Assertions.assertEquals(url.getParameter("timeout"), "10000");
        }

        {
            String providerUrlV27 = "dubbo://172.24.160.179:21880/com.foo.BarService?application=foo&side=provider&weight=100";

            URL providerConfiguratorUrl = URL.valueOf("override://172.24.160.179:21880/com.foo.BarService");
            Map<String, String> params = new HashMap<>();
            params.put("side", "provider");
            params.put("configVersion", "2.7");
            params.put("application", "foo");
            params.put("weight", "200");
            providerConfiguratorUrl = providerConfiguratorUrl.addParameters(params);
            // Meet the configured conditions:
            // same side
            // same port
            // The host of configuratorUrl is 0.0.0.0 or the host of providerConfiguratorUrl is the same as consumerUrlV27
            // same appName
            OverrideConfigurator configurator = new OverrideConfigurator(providerConfiguratorUrl);
            URL url = configurator.configure(URL.valueOf(providerUrlV27));
            Assertions.assertEquals(url.getParameter("weight"), "200");
        }

    }

    // Test the version after 2.7
    @Test
    void testOverrideForVersion3() {
        // match
        {
            String consumerUrlV3 = "dubbo://172.24.160.179/com.foo.BarService?match_key=value&application=foo&side=consumer&timeout=100";

            URL consumerConfiguratorUrl = URL.valueOf("override://0.0.0.0/com.foo.BarService");
            Map<String, String> params = new HashMap<>();
            params.put("side", "consumer");
            params.put("configVersion", "v3.0");
            params.put("application", "foo");
            params.put("timeout", "10000");

            ConditionMatch matcher = new ConditionMatch();
            ParamMatch paramMatch = new ParamMatch();
            paramMatch.setKey("match_key");
            StringMatch stringMatch = new StringMatch();
            stringMatch.setExact("value");
            paramMatch.setValue(stringMatch);
            matcher.setParam(Arrays.asList(paramMatch));
            consumerConfiguratorUrl = consumerConfiguratorUrl.putAttribute(MATCH_CONDITION, matcher);

            consumerConfiguratorUrl = consumerConfiguratorUrl.addParameters(params);

            OverrideConfigurator configurator = new OverrideConfigurator(consumerConfiguratorUrl);
            // Meet the configured conditions:
            // same side
            // The port of configuratorUrl is 0
            // The host of configuratorUrl is 0.0.0.0 or the local address is the same as consumerUrlV27
            // same appName
            URL originalURL = URL.valueOf(consumerUrlV3);
            Assertions.assertEquals("100", originalURL.getParameter("timeout"));
            URL url = configurator.configure(originalURL);
            Assertions.assertEquals("10000", url.getParameter("timeout"));
        }

        // mismatch
        {
            String consumerUrlV3 = "dubbo://172.24.160.179/com.foo.BarService?match_key=value&application=foo&side=consumer&timeout=100";

            URL consumerConfiguratorUrl = URL.valueOf("override://0.0.0.0/com.foo.BarService");
            Map<String, String> params = new HashMap<>();
            params.put("side", "consumer");
            params.put("configVersion", "v3.0");
            params.put("application", "foo");
            params.put("timeout", "10000");

            ConditionMatch matcher = new ConditionMatch();
            ParamMatch paramMatch = new ParamMatch();
            paramMatch.setKey("match_key");
            StringMatch stringMatch = new StringMatch();
            stringMatch.setExact("not_match_value");
            paramMatch.setValue(stringMatch);
            matcher.setParam(Arrays.asList(paramMatch));
            consumerConfiguratorUrl = consumerConfiguratorUrl.putAttribute(MATCH_CONDITION, matcher);

            consumerConfiguratorUrl = consumerConfiguratorUrl.addParameters(params);

            OverrideConfigurator configurator = new OverrideConfigurator(consumerConfiguratorUrl);
            // Meet the configured conditions:
            // same side
            // The port of configuratorUrl is 0
            // The host of configuratorUrl is 0.0.0.0 or the local address is the same as consumerUrlV27
            // same appName
            URL originalURL = URL.valueOf(consumerUrlV3);
            Assertions.assertEquals("100", originalURL.getParameter("timeout"));
            URL url = configurator.configure(originalURL);
            Assertions.assertEquals("100", url.getParameter("timeout"));
        }
    }

}
