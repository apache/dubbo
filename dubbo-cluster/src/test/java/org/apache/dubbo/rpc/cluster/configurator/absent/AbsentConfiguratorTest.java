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
package org.apache.dubbo.rpc.cluster.configurator.absent;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.cluster.configurator.consts.UrlConstant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * OverrideConfiguratorTest
 */
class AbsentConfiguratorTest {


    @Test
    void testOverrideApplication() {
        AbsentConfigurator configurator = new AbsentConfigurator(URL.valueOf("override://foo@0.0.0.0/com.foo.BarService?timeout=200"));

        URL url = configurator.configure(URL.valueOf(UrlConstant.URL_CONSUMER));
        Assertions.assertEquals("200", url.getParameter("timeout"));

        url = configurator.configure(URL.valueOf(UrlConstant.URL_ONE));
        Assertions.assertEquals("1000", url.getParameter("timeout"));

        url = configurator.configure(URL.valueOf(UrlConstant.APPLICATION_BAR_SIDE_CONSUMER_11));
        Assertions.assertNull(url.getParameter("timeout"));

        url = configurator.configure(URL.valueOf(UrlConstant.TIMEOUT_1000_SIDE_CONSUMER_11));
        Assertions.assertEquals("1000", url.getParameter("timeout"));
    }

    @Test
    void testOverrideHost() {
        AbsentConfigurator configurator = new AbsentConfigurator(URL.valueOf("override://" + NetUtils.getLocalHost() + "/com.foo.BarService?timeout=200"));

        URL url = configurator.configure(URL.valueOf(UrlConstant.URL_CONSUMER));
        Assertions.assertEquals("200", url.getParameter("timeout"));

        url = configurator.configure(URL.valueOf(UrlConstant.URL_ONE));
        Assertions.assertEquals("1000", url.getParameter("timeout"));

        AbsentConfigurator configurator1 = new AbsentConfigurator(URL.valueOf(UrlConstant.SERVICE_TIMEOUT_200));

        url = configurator1.configure(URL.valueOf(UrlConstant.APPLICATION_BAR_SIDE_CONSUMER_10));
        Assertions.assertNull(url.getParameter("timeout"));

        url = configurator1.configure(URL.valueOf(UrlConstant.TIMEOUT_1000_SIDE_CONSUMER_10));
        Assertions.assertEquals("1000", url.getParameter("timeout"));
    }

    // Test the version after 2.7
    @Test
    void testAbsentForVersion27() {
        {
            String consumerUrlV27 = "dubbo://172.24.160.179/com.foo.BarService?application=foo&side=consumer&timeout=100";

            URL consumerConfiguratorUrl = URL.valueOf("absent://0.0.0.0/com.foo.BarService");
            Map<String, String> params = new HashMap<>();
            params.put("side", "consumer");
            params.put("configVersion", "2.7");
            params.put("application", "foo");
            params.put("timeout", "10000");
            params.put("weight", "200");
            consumerConfiguratorUrl = consumerConfiguratorUrl.addParameters(params);

            AbsentConfigurator configurator = new AbsentConfigurator(consumerConfiguratorUrl);
            // Meet the configured conditions:
            // same side
            // The port of configuratorUrl is 0
            // The host of configuratorUrl is 0.0.0.0 or the local address is the same as consumerUrlV27
            // same appName
            URL url = configurator.configure(URL.valueOf(consumerUrlV27));
            Assertions.assertEquals("100", url.getParameter("timeout"));
            Assertions.assertEquals("200", url.getParameter("weight"));
        }

        {
            String providerUrlV27 = "dubbo://172.24.160.179:21880/com.foo.BarService?application=foo&side=provider&weight=100";

            URL providerConfiguratorUrl = URL.valueOf("absent://172.24.160.179:21880/com.foo.BarService");
            Map<String, String> params = new HashMap<>();
            params.put("side", "provider");
            params.put("configVersion", "2.7");
            params.put("application", "foo");
            params.put("timeout", "20000");
            params.put("weight", "200");
            providerConfiguratorUrl = providerConfiguratorUrl.addParameters(params);
            // Meet the configured conditions:
            // same side
            // same port
            // The host of configuratorUrl is 0.0.0.0 or the host of providerConfiguratorUrl is the same as consumerUrlV27
            // same appName
            AbsentConfigurator configurator = new AbsentConfigurator(providerConfiguratorUrl);
            URL url = configurator.configure(URL.valueOf(providerUrlV27));
            Assertions.assertEquals("20000", url.getParameter("timeout"));
            Assertions.assertEquals("100", url.getParameter("weight"));
        }
    }

}
