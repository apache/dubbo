package com.alibaba.dubbo.config.spring.util;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * {@link PropertySources} Utilities
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see PropertySources
 * @since 2.5.8
 */
public abstract class PropertySourcesUtils {

    /**
     * Get Sub {@link Properties}
     *
     * @param propertySources {@link PropertySources}
     * @param prefix          the prefix of property name
     * @return Map<String, String>
     * @see Properties
     */
    public static Map<String, String> getSubProperties(PropertySources propertySources, String prefix) {

        Map<String, String> subProperties = new LinkedHashMap<String, String>();

        String normalizedPrefix = prefix.endsWith(".") ? prefix : prefix + ".";

        for (PropertySource<?> source : propertySources) {
            if (source instanceof EnumerablePropertySource) {
                for (String name : ((EnumerablePropertySource<?>) source).getPropertyNames()) {
                    if (name.startsWith(normalizedPrefix)) {
                        String subName = name.substring(normalizedPrefix.length());
                        Object value = source.getProperty(name);
                        if (value instanceof String) {
                            subProperties.put(subName, String.valueOf(value));
                        }
                    }
                }
            }
        }

        return subProperties;

    }

}
