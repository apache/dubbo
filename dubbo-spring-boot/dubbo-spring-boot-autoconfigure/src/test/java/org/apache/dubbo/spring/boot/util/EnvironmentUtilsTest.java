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
package org.apache.dubbo.spring.boot.util;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link EnvironmentUtils} Test
 *
 * @see EnvironmentUtils
 * @since 2.7.0
 */
public class EnvironmentUtilsTest {

    @Test
    public void testExtraProperties() {

        System.setProperty("user.name", "mercyblitz");

        StandardEnvironment environment = new StandardEnvironment();

        Map<String, Object> map = new HashMap<>();

        map.put("user.name", "Mercy");

        MapPropertySource propertySource = new MapPropertySource("first", map);

        CompositePropertySource compositePropertySource = new CompositePropertySource("comp");

        compositePropertySource.addFirstPropertySource(propertySource);

        MutablePropertySources propertySources = environment.getPropertySources();

        propertySources.addFirst(compositePropertySource);

        Map<String, Object> properties = EnvironmentUtils.extractProperties(environment);

        Assert.assertEquals("Mercy", properties.get("user.name"));

    }
}
