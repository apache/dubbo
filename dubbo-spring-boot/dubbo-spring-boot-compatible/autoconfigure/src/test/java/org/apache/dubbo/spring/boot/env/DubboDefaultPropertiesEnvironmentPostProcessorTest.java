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
package org.apache.dubbo.spring.boot.env;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.HashMap;

/**
 * {@link DubboDefaultPropertiesEnvironmentPostProcessor} Test
 */
public class DubboDefaultPropertiesEnvironmentPostProcessorTest {

    private DubboDefaultPropertiesEnvironmentPostProcessor instance =
            new DubboDefaultPropertiesEnvironmentPostProcessor();

    private SpringApplication springApplication = new SpringApplication();

    @Test
    public void testOrder() {
        Assert.assertEquals(Ordered.LOWEST_PRECEDENCE, instance.getOrder());
    }

    @Test
    public void testPostProcessEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        // Case 1 : Not Any property
        instance.postProcessEnvironment(environment, springApplication);
        // Get PropertySources
        MutablePropertySources propertySources = environment.getPropertySources();
        // Nothing to change
        PropertySource defaultPropertySource = propertySources.get("defaultProperties");
        Assert.assertNotNull(defaultPropertySource);
        Assert.assertEquals("true", defaultPropertySource.getProperty("dubbo.config.multiple"));
        Assert.assertEquals("false", defaultPropertySource.getProperty("dubbo.application.qos-enable"));

        // Case 2 :  Only set property "spring.application.name"
        environment.setProperty("spring.application.name", "demo-dubbo-application");
        instance.postProcessEnvironment(environment, springApplication);
        defaultPropertySource = propertySources.get("defaultProperties");
        Object dubboApplicationName = defaultPropertySource.getProperty("dubbo.application.name");
        Assert.assertEquals("demo-dubbo-application", dubboApplicationName);

        // Case 3 : Only set property "dubbo.application.name"
        // Rest environment
        environment = new MockEnvironment();
        propertySources = environment.getPropertySources();
        environment.setProperty("dubbo.application.name", "demo-dubbo-application");
        instance.postProcessEnvironment(environment, springApplication);
        defaultPropertySource = propertySources.get("defaultProperties");
        Assert.assertNotNull(defaultPropertySource);
        dubboApplicationName = environment.getProperty("dubbo.application.name");
        Assert.assertEquals("demo-dubbo-application", dubboApplicationName);

        // Case 4 : If "defaultProperties" PropertySource is present in PropertySources
        // Rest environment
        environment = new MockEnvironment();
        propertySources = environment.getPropertySources();
        propertySources.addLast(new MapPropertySource("defaultProperties", new HashMap<String, Object>()));
        environment.setProperty("spring.application.name", "demo-dubbo-application");
        instance.postProcessEnvironment(environment, springApplication);
        defaultPropertySource = propertySources.get("defaultProperties");
        dubboApplicationName = defaultPropertySource.getProperty("dubbo.application.name");
        Assert.assertEquals("demo-dubbo-application", dubboApplicationName);

        // Case 5 : Rest dubbo.config.multiple and dubbo.application.qos-enable
        environment = new MockEnvironment();
        propertySources = environment.getPropertySources();
        propertySources.addLast(new MapPropertySource("defaultProperties", new HashMap<String, Object>()));
        environment.setProperty("dubbo.config.multiple", "false");
        environment.setProperty("dubbo.application.qos-enable", "true");
        instance.postProcessEnvironment(environment, springApplication);
        Assert.assertEquals("false", environment.getProperty("dubbo.config.multiple"));
        Assert.assertEquals("true", environment.getProperty("dubbo.application.qos-enable"));
    }
}