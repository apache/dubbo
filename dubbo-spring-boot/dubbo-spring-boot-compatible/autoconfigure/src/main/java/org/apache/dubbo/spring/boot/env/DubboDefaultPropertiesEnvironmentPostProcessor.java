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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_APPLICATION_NAME_PROPERTY;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_APPLICATION_QOS_ENABLE_PROPERTY;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_CONFIG_MULTIPLE_PROPERTY;
import static org.apache.dubbo.spring.boot.util.DubboUtils.SPRING_APPLICATION_NAME_PROPERTY;

/**
 * The lowest precedence {@link EnvironmentPostProcessor} processes
 * {@link SpringApplication#setDefaultProperties(Properties) Spring Boot default properties} for Dubbo
 * as late as possible before {@link ConfigurableApplicationContext#refresh() application context refresh}.
 */
public class DubboDefaultPropertiesEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /**
     * The name of default {@link PropertySource} defined in SpringApplication#configurePropertySources method.
     */
    public static final String PROPERTY_SOURCE_NAME = "defaultProperties";

    /**
     * The property name of "spring.main.allow-bean-definition-overriding".
     * Please refer to: https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.1-Release-Notes#bean-overriding
     */
    public static final String ALLOW_BEAN_DEFINITION_OVERRIDING_PROPERTY = "spring.main.allow-bean-definition-overriding";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        MutablePropertySources propertySources = environment.getPropertySources();
        Map<String, Object> defaultProperties = createDefaultProperties(environment);
        if (!CollectionUtils.isEmpty(defaultProperties)) {
            addOrReplace(propertySources, defaultProperties);
        }
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    private Map<String, Object> createDefaultProperties(ConfigurableEnvironment environment) {
        Map<String, Object> defaultProperties = new HashMap<>();
        setDubboApplicationNameProperty(environment, defaultProperties);
        setDubboConfigMultipleProperty(defaultProperties);
        setDubboApplicationQosEnableProperty(defaultProperties);
        //setAllowBeanDefinitionOverriding(defaultProperties);
        return defaultProperties;
    }

    private void setDubboApplicationNameProperty(Environment environment, Map<String, Object> defaultProperties) {
        String springApplicationName = environment.getProperty(SPRING_APPLICATION_NAME_PROPERTY);
        if (StringUtils.hasLength(springApplicationName)
                && !environment.containsProperty(DUBBO_APPLICATION_NAME_PROPERTY)) {
            defaultProperties.put(DUBBO_APPLICATION_NAME_PROPERTY, springApplicationName);
        }
    }

    private void setDubboConfigMultipleProperty(Map<String, Object> defaultProperties) {
        defaultProperties.put(DUBBO_CONFIG_MULTIPLE_PROPERTY, Boolean.TRUE.toString());
    }

    private void setDubboApplicationQosEnableProperty(Map<String, Object> defaultProperties) {
        defaultProperties.put(DUBBO_APPLICATION_QOS_ENABLE_PROPERTY, Boolean.TRUE.toString());
    }

    /**
     * Set {@link #ALLOW_BEAN_DEFINITION_OVERRIDING_PROPERTY "spring.main.allow-bean-definition-overriding"} to be
     * <code>true</code> as default.
     *
     * @param defaultProperties the default {@link Properties properties}
     * @see #ALLOW_BEAN_DEFINITION_OVERRIDING_PROPERTY
     * @since 2.7.1
     */
    private void setAllowBeanDefinitionOverriding(Map<String, Object> defaultProperties) {
        defaultProperties.put(ALLOW_BEAN_DEFINITION_OVERRIDING_PROPERTY, Boolean.TRUE.toString());
    }

    /**
     * Copy from BusEnvironmentPostProcessor#addOrReplace(MutablePropertySources, Map)
     *
     * @param propertySources {@link MutablePropertySources}
     * @param map             Default Dubbo Properties
     */
    private void addOrReplace(MutablePropertySources propertySources,
                              Map<String, Object> map) {
        MapPropertySource target = null;
        if (propertySources.contains(PROPERTY_SOURCE_NAME)) {
            PropertySource<?> source = propertySources.get(PROPERTY_SOURCE_NAME);
            if (source instanceof MapPropertySource) {
                target = (MapPropertySource) source;
                for (Map.Entry<String,Object> entry : map.entrySet()) {
                    String key = entry.getKey();
                    if (!target.containsProperty(key)) {
                        target.getSource().put(key, entry.getValue());
                    }
                }
            }
        }
        if (target == null) {
            target = new MapPropertySource(PROPERTY_SOURCE_NAME, map);
        }
        if (!propertySources.contains(PROPERTY_SOURCE_NAME)) {
            propertySources.addLast(target);
        }
    }
}
