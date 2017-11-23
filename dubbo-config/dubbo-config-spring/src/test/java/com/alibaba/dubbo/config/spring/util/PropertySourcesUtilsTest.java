package com.alibaba.dubbo.config.spring.util;

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
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
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

        Map<String, String> result = PropertySourcesUtils.getSubProperties(propertySources, "user");

        Assert.assertEquals(Collections.emptyMap(), result);

        source.put("user.name", "Mercy");
        source.put("user.age", "31");

        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("name", "Mercy");
        expected.put("age", "31");

        result = PropertySourcesUtils.getSubProperties(propertySources, "user");

        Assert.assertEquals(expected, result);

        result = PropertySourcesUtils.getSubProperties(propertySources, "");

        Assert.assertEquals(Collections.emptyMap(), result);

        result = PropertySourcesUtils.getSubProperties(propertySources, "no-exists");

        Assert.assertEquals(Collections.emptyMap(), result);

    }

}
