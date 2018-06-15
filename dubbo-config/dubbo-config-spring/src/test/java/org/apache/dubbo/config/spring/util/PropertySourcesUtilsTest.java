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

import org.junit.Assert;
import org.junit.Test;
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

        MapPropertySource propertySource = new MapPropertySource("test", source);

        propertySources.addFirst(propertySource);

        String KEY_PREFIX = "user";
        String KEY_NAME = "name";
        String KEY_AGE = "age";
        Map<String, String> result = PropertySourcesUtils.getSubProperties(propertySources, KEY_PREFIX);

        Assert.assertEquals(Collections.emptyMap(), result);

        source.put(KEY_PREFIX + "." + KEY_NAME, "Mercy");
        source.put(KEY_PREFIX + "." + KEY_AGE, 31);

        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put(KEY_NAME, "Mercy");
        expected.put(KEY_AGE, "31");

        result = PropertySourcesUtils.getSubProperties(propertySources, KEY_PREFIX);
        Assert.assertEquals(expected, result);

        result = PropertySourcesUtils.getSubProperties(propertySources, "");

        Assert.assertEquals(Collections.emptyMap(), result);

        result = PropertySourcesUtils.getSubProperties(propertySources, "no-exists");

        Assert.assertEquals(Collections.emptyMap(), result);

        source.put(KEY_PREFIX + ".app.name", "${info.name}");
        source.put("info.name", "Hello app");

        result = PropertySourcesUtils.getSubProperties(propertySources, KEY_PREFIX);

        String appName = result.get("app.name");

        Assert.assertEquals("Hello app", appName);

    }

}
