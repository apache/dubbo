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
package org.apache.dubbo.rpc.cluster.configurator.parser;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.cluster.configurator.parser.model.ConditionMatch;
import org.apache.dubbo.rpc.cluster.configurator.parser.model.ConfiguratorConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.LOADBALANCE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.OVERRIDE_PROVIDERS_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.WEIGHT_KEY;
import static org.apache.dubbo.rpc.cluster.configurator.parser.model.ConfiguratorConfig.MATCH_CONDITION;

/**
 *
 */
class ConfigParserTest {

    private String streamToString(InputStream stream) throws IOException {
        byte[] bytes = new byte[stream.available()];
        stream.read(bytes);
        return new String(bytes);
    }

    @Test
    void snakeYamlBasicTest() throws IOException {
        try (InputStream yamlStream = this.getClass().getResourceAsStream("/ServiceNoApp.yml")) {
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Map<String, Object> map = yaml.load(yamlStream);
            ConfiguratorConfig config = ConfiguratorConfig.parseFromMap(map);
            Assertions.assertNotNull(config);
        }
    }

    @Test
    void parseConfiguratorsServiceNoAppTest() throws Exception {
        try (InputStream yamlStream = this.getClass().getResourceAsStream("/ServiceNoApp.yml")) {
            List<URL> urls = ConfigParser.parseConfigurators(streamToString(yamlStream));
            Assertions.assertNotNull(urls);
            Assertions.assertEquals(2, urls.size());
            URL url = urls.get(0);
            Assertions.assertEquals("127.0.0.1:20880", url.getAddress());
            Assertions.assertEquals(222, url.getParameter(WEIGHT_KEY, 0));
        }
    }

    @Test
    void parseConfiguratorsServiceGroupVersionTest() throws Exception {
        try (InputStream yamlStream = this.getClass().getResourceAsStream("/ServiceGroupVersion.yml")) {
            List<URL> urls = ConfigParser.parseConfigurators(streamToString(yamlStream));
            Assertions.assertNotNull(urls);
            Assertions.assertEquals(1, urls.size());
            URL url = urls.get(0);
            Assertions.assertEquals("testgroup", url.getGroup());
            Assertions.assertEquals("1.0.0", url.getVersion());
        }
    }

    @Test
    void parseConfiguratorsServiceMultiAppsTest() throws IOException {
        try (InputStream yamlStream = this.getClass().getResourceAsStream("/ServiceMultiApps.yml")) {
            List<URL> urls = ConfigParser.parseConfigurators(streamToString(yamlStream));
            Assertions.assertNotNull(urls);
            Assertions.assertEquals(4, urls.size());
            URL url = urls.get(0);
            Assertions.assertEquals("127.0.0.1", url.getAddress());
            Assertions.assertEquals(6666, url.getParameter(TIMEOUT_KEY, 0));
            Assertions.assertNotNull(url.getApplication());
        }
    }

    @Test
    void parseConfiguratorsServiceNoRuleTest() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            try (InputStream yamlStream = this.getClass().getResourceAsStream("/ServiceNoRule.yml")) {
                ConfigParser.parseConfigurators(streamToString(yamlStream));
                Assertions.fail();
            }
        });
    }

    @Test
    void parseConfiguratorsAppMultiServicesTest() throws IOException {
        try (InputStream yamlStream = this.getClass().getResourceAsStream("/AppMultiServices.yml")) {
            String yamlFile = streamToString(yamlStream);
            List<URL> urls = ConfigParser.parseConfigurators(yamlFile);
            Assertions.assertNotNull(urls);
            Assertions.assertEquals(4, urls.size());
            URL url = urls.get(0);
            Assertions.assertEquals("127.0.0.1", url.getAddress());
            Assertions.assertEquals("service1", url.getServiceInterface());
            Assertions.assertEquals(6666, url.getParameter(TIMEOUT_KEY, 0));
            Assertions.assertEquals("random", url.getParameter(LOADBALANCE_KEY));
            Assertions.assertEquals("demo-consumer", url.getApplication());
        }
    }


    @Test
    void parseConfiguratorsAppAnyServicesTest() throws IOException {
        try (InputStream yamlStream = this.getClass().getResourceAsStream("/AppAnyServices.yml")) {
            List<URL> urls = ConfigParser.parseConfigurators(streamToString(yamlStream));
            Assertions.assertNotNull(urls);
            Assertions.assertEquals(2, urls.size());
            URL url = urls.get(0);
            Assertions.assertEquals("127.0.0.1", url.getAddress());
            Assertions.assertEquals("*", url.getServiceInterface());
            Assertions.assertEquals(6666, url.getParameter(TIMEOUT_KEY, 0));
            Assertions.assertEquals("random", url.getParameter(LOADBALANCE_KEY));
            Assertions.assertEquals("demo-consumer", url.getApplication());
        }
    }

    @Test
    void parseConfiguratorsAppNoServiceTest() throws IOException {
        try (InputStream yamlStream = this.getClass().getResourceAsStream("/AppNoService.yml")) {
            List<URL> urls = ConfigParser.parseConfigurators(streamToString(yamlStream));
            Assertions.assertNotNull(urls);
            Assertions.assertEquals(1, urls.size());
            URL url = urls.get(0);
            Assertions.assertEquals("127.0.0.1", url.getAddress());
            Assertions.assertEquals("*", url.getServiceInterface());
            Assertions.assertEquals(6666, url.getParameter(TIMEOUT_KEY, 0));
            Assertions.assertEquals("random", url.getParameter(LOADBALANCE_KEY));
            Assertions.assertEquals("demo-consumer", url.getApplication());
        }
    }

    @Test
    void parseConsumerSpecificProvidersTest() throws IOException {
        try (InputStream yamlStream = this.getClass().getResourceAsStream("/ConsumerSpecificProviders.yml")) {
            List<URL> urls = ConfigParser.parseConfigurators(streamToString(yamlStream));
            Assertions.assertNotNull(urls);
            Assertions.assertEquals(1, urls.size());
            URL url = urls.get(0);
            Assertions.assertEquals("127.0.0.1", url.getAddress());
            Assertions.assertEquals("*", url.getServiceInterface());
            Assertions.assertEquals(6666, url.getParameter(TIMEOUT_KEY, 0));
            Assertions.assertEquals("random", url.getParameter(LOADBALANCE_KEY));
            Assertions.assertEquals("127.0.0.1:20880", url.getParameter(OVERRIDE_PROVIDERS_KEY));
            Assertions.assertEquals("demo-consumer", url.getApplication());
        }
    }

    @Test
    void parseProviderConfigurationV3() throws IOException {
        try (InputStream yamlStream = this.getClass().getResourceAsStream("/ConfiguratorV3.yml")) {
            List<URL> urls = ConfigParser.parseConfigurators(streamToString(yamlStream));
            Assertions.assertNotNull(urls);
            Assertions.assertEquals(1, urls.size());
            URL url = urls.get(0);
            Assertions.assertEquals("0.0.0.0", url.getAddress());
            Assertions.assertEquals("*", url.getServiceInterface());
            Assertions.assertEquals(200, url.getParameter(WEIGHT_KEY, 0));
            Assertions.assertEquals("demo-provider", url.getApplication());

            URL matchURL1 = URL.valueOf("dubbo://10.0.0.1:20880/DemoService?match_key1=value1");
            URL matchURL2 = URL.valueOf("dubbo://10.0.0.1:20880/DemoService2?match_key1=value1");
            URL notMatchURL1 = URL.valueOf("dubbo://10.0.0.2:20880/DemoService?match_key1=value1");// address not match
            URL notMatchURL2 = URL.valueOf("dubbo://10.0.0.1:20880/DemoServiceNotMatch?match_key1=value1");// service not match
            URL notMatchURL3 = URL.valueOf("dubbo://10.0.0.1:20880/DemoService?match_key1=value_not_match");// key not match

            ConditionMatch matcher = (ConditionMatch) url.getAttribute(MATCH_CONDITION);
            Assertions.assertTrue(matcher.isMatch(matchURL1));
            Assertions.assertTrue(matcher.isMatch(matchURL2));
            Assertions.assertFalse(matcher.isMatch(notMatchURL1));
            Assertions.assertFalse(matcher.isMatch(notMatchURL2));
            Assertions.assertFalse(matcher.isMatch(notMatchURL3));
        }
    }

    @Test
    void parseProviderConfigurationV3Compatibility() throws IOException {
        try (InputStream yamlStream = this.getClass().getResourceAsStream("/ConfiguratorV3Compatibility.yml")) {
            List<URL> urls = ConfigParser.parseConfigurators(streamToString(yamlStream));
            Assertions.assertNotNull(urls);
            Assertions.assertEquals(1, urls.size());
            URL url = urls.get(0);
            Assertions.assertEquals("10.0.0.1:20880", url.getAddress());
            Assertions.assertEquals("DemoService", url.getServiceInterface());
            Assertions.assertEquals(200, url.getParameter(WEIGHT_KEY, 0));
            Assertions.assertEquals("demo-provider", url.getApplication());

            URL matchURL = URL.valueOf("dubbo://10.0.0.1:20880/DemoService?match_key1=value1");
            URL notMatchURL = URL.valueOf("dubbo://10.0.0.1:20880/DemoService?match_key1=value_not_match");// key not match

            ConditionMatch matcher = (ConditionMatch) url.getAttribute(MATCH_CONDITION);
            Assertions.assertTrue(matcher.isMatch(matchURL));
            Assertions.assertFalse(matcher.isMatch(notMatchURL));
        }
    }

    @Test
    void parseProviderConfigurationV3Conflict() throws IOException {
        try (InputStream yamlStream = this.getClass().getResourceAsStream("/ConfiguratorV3Duplicate.yml")) {
            List<URL> urls = ConfigParser.parseConfigurators(streamToString(yamlStream));
            Assertions.assertNotNull(urls);
            Assertions.assertEquals(1, urls.size());
            URL url = urls.get(0);
            Assertions.assertEquals("10.0.0.1:20880", url.getAddress());
            Assertions.assertEquals("DemoService", url.getServiceInterface());
            Assertions.assertEquals(200, url.getParameter(WEIGHT_KEY, 0));
            Assertions.assertEquals("demo-provider", url.getApplication());

            URL matchURL = URL.valueOf("dubbo://10.0.0.1:20880/DemoService?match_key1=value1");
            URL notMatchURL = URL.valueOf("dubbo://10.0.0.1:20880/DemoService?match_key1=value_not_match");// key not match

            ConditionMatch matcher = (ConditionMatch) url.getAttribute(MATCH_CONDITION);
            Assertions.assertTrue(matcher.isMatch(matchURL));
            Assertions.assertFalse(matcher.isMatch(notMatchURL));
        }
    }

    @Test
    void parseURLJsonArrayCompatible() {

        String configData = "[\"override://0.0.0.0/com.xx.Service?category=configurators&timeout=6666&disabled=true&dynamic=false&enabled=true&group=dubbo&priority=1&version=1.0\" ]";

        List<URL> urls = ConfigParser.parseConfigurators(configData);

        Assertions.assertNotNull(urls);
        Assertions.assertEquals(1, urls.size());
        URL url = urls.get(0);

        Assertions.assertEquals("0.0.0.0", url.getAddress());
        Assertions.assertEquals("com.xx.Service", url.getServiceInterface());
        Assertions.assertEquals(6666, url.getParameter(TIMEOUT_KEY, 0));
    }

}
