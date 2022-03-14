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
package org.apache.dubbo.config.spring.issues.issue9207;

import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.spring.ConfigCenterBean;
import org.apache.dubbo.config.spring.SysProps;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class ConfigCenterBeanTest {

    private static final String DUBBO_PROPERTIES_FILE = "/META-INF/issues/issue9207/dubbo-properties-in-configcenter.properties";
    private static final String DUBBO_EXTERNAL_CONFIG_KEY = "my-dubbo.properties";

    @Test
    public void testConfigCenterBeanFromProps() throws IOException {
        SysProps.setProperty("dubbo.config-center.include-spring-env", "true");
        SysProps.setProperty("dubbo.config-center.config-file", DUBBO_EXTERNAL_CONFIG_KEY);

        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(ProviderConfiguration.class);
        try {
            ConfigManager configManager = DubboBeanUtils.getApplicationModel(applicationContext).getApplicationConfigManager();
            Collection<ConfigCenterConfig> configCenters = configManager.getConfigCenters();
            Assertions.assertEquals(1, configCenters.size());

            ConfigCenterConfig cc = configCenters.stream().findFirst().get();
            Assertions.assertFalse(cc.getExternalConfiguration().isEmpty());
            Assertions.assertTrue( cc instanceof ConfigCenterBean);

            // check loaded external config
            String content = readContent(DUBBO_PROPERTIES_FILE);
            content = applicationContext.getEnvironment().resolvePlaceholders(content);
            Properties properties = new Properties();
            properties.load(new StringReader(content));
            Assertions.assertEquals(properties, cc.getExternalConfiguration());
        } finally {
            SysProps.clear();
            if (applicationContext != null) {
                applicationContext.close();
            }
        }

    }

    @EnableDubbo(scanBasePackages = "")
    @Configuration
    static class ProviderConfiguration {

        @Bean
        public BeanFactoryPostProcessor envPostProcessor(ConfigurableEnvironment environment) {
            return new BeanFactoryPostProcessor() {
                @Override
                public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                    // add custom external configs to spring environment early before processing normal bean.
                    // e.g. we can do it in BeanFactoryPostProcessor or EnvironmentPostProcessor
                    Map<String, Object> dubboProperties = new HashMap<>();
                    String content = readContent(DUBBO_PROPERTIES_FILE);
                    dubboProperties.put(DUBBO_EXTERNAL_CONFIG_KEY, content);
                    MapPropertySource dubboPropertySource = new MapPropertySource("dubbo external config", dubboProperties);
                    environment.getPropertySources().addLast(dubboPropertySource);
                }
            };
        }

    }

    private static String readContent(String file) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(ConfigCenterBeanTest.class.getResourceAsStream(file)));
        String content = reader.lines().collect(Collectors.joining("\n"));
        return content;
    }
}
