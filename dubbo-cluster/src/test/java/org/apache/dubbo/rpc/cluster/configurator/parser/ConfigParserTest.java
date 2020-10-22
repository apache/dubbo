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
import org.apache.dubbo.rpc.cluster.configurator.parser.model.ConfigItem;
import org.apache.dubbo.rpc.cluster.configurator.parser.model.ConfiguratorConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.LOADBALANCE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.OVERRIDE_PROVIDERS_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.WEIGHT_KEY;

/**
 *
 */
public class ConfigParserTest {

    private String streamToString(InputStream stream) throws IOException {
        byte[] bytes = new byte[stream.available()];
        stream.read(bytes);
        return new String(bytes);
    }

    @Test
    public void snakeYamlBasicTest() throws IOException {
        try (InputStream yamlStream = this.getClass().getResourceAsStream("/ServiceNoApp.yml")) {

            Constructor constructor = new Constructor(ConfiguratorConfig.class);
            TypeDescription carDescription = new TypeDescription(ConfiguratorConfig.class);
            carDescription.addPropertyParameters("items", ConfigItem.class);
            constructor.addTypeDescription(carDescription);

            Yaml yaml = new Yaml(constructor);
            ConfiguratorConfig config = yaml.load(yamlStream);
            System.out.println(config);
        }
    }

    @Test
    public void parseConfiguratorsServiceNoAppTest() throws Exception {
        try (InputStream yamlStream = this.getClass().getResourceAsStream("/ServiceNoApp.yml")) {
            List<URL> urls = ConfigParser.parseConfigurators(streamToString(yamlStream));
            Assertions.assertNotNull(urls);
            Assertions.assertEquals(2, urls.size());
            URL url = urls.get(0);
            Assertions.assertEquals(url.getAddress(), "127.0.0.1:20880");
            Assertions.assertEquals(url.getParameter(WEIGHT_KEY, 0), 222);
        }
    }

    @Test
    public void parseConfiguratorsServiceGroupVersionTest() throws Exception {
        try (InputStream yamlStream = this.getClass().getResourceAsStream("/ServiceGroupVersion.yml")) {
            List<URL> urls = ConfigParser.parseConfigurators(streamToString(yamlStream));
            Assertions.assertNotNull(urls);
            Assertions.assertEquals(1, urls.size());
            URL url = urls.get(0);
            Assertions.assertEquals("testgroup", url.getParameter(GROUP_KEY));
            Assertions.assertEquals("1.0.0", url.getParameter(VERSION_KEY));
        }
    }

    @Test
    public void parseConfiguratorsServiceMultiAppsTest() throws IOException {
        try (InputStream yamlStream = this.getClass().getResourceAsStream("/ServiceMultiApps.yml")) {
            List<URL> urls = ConfigParser.parseConfigurators(streamToString(yamlStream));
            Assertions.assertNotNull(urls);
            Assertions.assertEquals(4, urls.size());
            URL url = urls.get(0);
            Assertions.assertEquals("127.0.0.1", url.getAddress());
            Assertions.assertEquals(6666, url.getParameter(TIMEOUT_KEY, 0));
            Assertions.assertNotNull(url.getParameter(APPLICATION_KEY));
        }
    }

    @Test
    public void parseConfiguratorsServiceNoRuleTest() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            try (InputStream yamlStream = this.getClass().getResourceAsStream("/ServiceNoRule.yml")) {
                ConfigParser.parseConfigurators(streamToString(yamlStream));
                Assertions.fail();
            }
        });
    }

    @Test
    public void parseConfiguratorsAppMultiServicesTest() throws IOException {
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
            Assertions.assertEquals(url.getParameter(APPLICATION_KEY), "demo-consumer");
        }
    }


    @Test
    public void parseConfiguratorsAppAnyServicesTest() throws IOException {
        try (InputStream yamlStream = this.getClass().getResourceAsStream("/AppAnyServices.yml")) {
            List<URL> urls = ConfigParser.parseConfigurators(streamToString(yamlStream));
            Assertions.assertNotNull(urls);
            Assertions.assertEquals(2, urls.size());
            URL url = urls.get(0);
            Assertions.assertEquals("127.0.0.1", url.getAddress());
            Assertions.assertEquals("*", url.getServiceInterface());
            Assertions.assertEquals(6666, url.getParameter(TIMEOUT_KEY, 0));
            Assertions.assertEquals("random", url.getParameter(LOADBALANCE_KEY));
            Assertions.assertEquals(url.getParameter(APPLICATION_KEY), "demo-consumer");
        }
    }

    @Test
    public void parseConfiguratorsAppNoServiceTest() throws IOException {
        try (InputStream yamlStream = this.getClass().getResourceAsStream("/AppNoService.yml")) {
            List<URL> urls = ConfigParser.parseConfigurators(streamToString(yamlStream));
            Assertions.assertNotNull(urls);
            Assertions.assertEquals(1, urls.size());
            URL url = urls.get(0);
            Assertions.assertEquals("127.0.0.1", url.getAddress());
            Assertions.assertEquals("*", url.getServiceInterface());
            Assertions.assertEquals(6666, url.getParameter(TIMEOUT_KEY, 0));
            Assertions.assertEquals("random", url.getParameter(LOADBALANCE_KEY));
            Assertions.assertEquals(url.getParameter(APPLICATION_KEY), "demo-consumer");
        }
    }

    @Test
    public void parseConsumerSpecificProvidersTest() throws IOException {
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
            Assertions.assertEquals(url.getParameter(APPLICATION_KEY), "demo-consumer");
        }
    }

    @Test
    public void parseURLJsonArrayCompatible() {

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
