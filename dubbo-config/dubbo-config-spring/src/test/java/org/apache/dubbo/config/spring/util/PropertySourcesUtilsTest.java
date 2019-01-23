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
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link PropertySourcesUtils} Test
 *
 * @see PropertySourcesUtils
 * @since 2.5.8
 */
public class PropertySourcesUtilsTest {

    @Test
    public void testGetSubProperties() {

        MutablePropertySources propertySources = new MutablePropertySources();

        Map<String, Object> source = new HashMap<String, Object>();
        Map<String, Object> source2 = new HashMap<String, Object>();

        MapPropertySource propertySource = new MapPropertySource("propertySource", source);
        MapPropertySource propertySource2 = new MapPropertySource("propertySource2", source2);

        propertySources.addLast(propertySource);
        propertySources.addLast(propertySource2);

        Map<String, Object> result = PropertySourcesUtils.getSubProperties(propertySources, "user");

        Assertions.assertEquals(Collections.emptyMap(), result);

        source.put("age", "31");
        source.put("user.name", "Mercy");
        source.put("user.age", "${age}");

        source2.put("user.name", "mercyblitz");
        source2.put("user.age", "32");

        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("name", "Mercy");
        expected.put("age", "31");

        result = PropertySourcesUtils.getSubProperties(propertySources, "user");
        Assertions.assertEquals(expected, result);

        result = PropertySourcesUtils.getSubProperties(propertySources, "");

        Assertions.assertEquals(Collections.emptyMap(), result);

        result = PropertySourcesUtils.getSubProperties(propertySources, "no-exists");

        Assertions.assertEquals(Collections.emptyMap(), result);

    }

}