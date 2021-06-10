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
package org.apache.dubbo.config.spring.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import static org.apache.dubbo.config.spring.util.EnvironmentUtils.filterDubboProperties;

/**
 * {@link EnvironmentUtils} Test
 *
 * @see EnvironmentUtils
 * @since 2.7.0
 */
public class EnvironmentUtilsTest {

    @Test
    public void testExtraProperties() {

        String key = "test.name";
        System.setProperty(key, "Tom");

        try {
            StandardEnvironment environment = new StandardEnvironment();

            Map<String, Object> map = new HashMap<>();

            map.put(key, "Mercy");

            MapPropertySource propertySource = new MapPropertySource("first", map);

            CompositePropertySource compositePropertySource = new CompositePropertySource("comp");

            compositePropertySource.addFirstPropertySource(propertySource);

            MutablePropertySources propertySources = environment.getPropertySources();

            propertySources.addFirst(compositePropertySource);

            Map<String, Object> properties = EnvironmentUtils.extractProperties(environment);

            Assertions.assertEquals("Mercy", properties.get(key));
        } finally {
            System.clearProperty(key);
        }

    }

    @Test
    public void testFilterDubboProperties() {

        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("message", "Hello,World");
        environment.setProperty("dubbo.registry.address", "zookeeper://10.10.10.1:2181");
        environment.setProperty("dubbo.consumer.check", "false");

        SortedMap<String, String> dubboProperties = filterDubboProperties(environment);

        Assertions.assertEquals(2, dubboProperties.size());
        Assertions.assertEquals("zookeeper://10.10.10.1:2181", dubboProperties.get("dubbo.registry.address"));
        Assertions.assertEquals("false", dubboProperties.get("dubbo.consumer.check"));

    }
}
