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
package org.apache.dubbo.config.spring.schema.config;

import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.AbstractParameterizedConfig;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import static org.apache.dubbo.common.constants.CommonConstants.HIDE_KEY_PREFIX;

/**
 * {@link BeanDefinitionParser} implementation class is used to parse the {@link BeanDefinition} for
 * {@link AbstractParameterizedConfig The Dubbo config with parameters} in XML meta configuration.
 *
 * @see BeanDefinition
 * @see BeanDefinitionParser
 * @see AbstractConfig
 * @see GenericDubboConfigBeanDefinitionParser
 * @since 2.7.7
 */
public class ParameterizedDubboConfigBeanDefinitionParser extends GenericDubboConfigBeanDefinitionParser {

    public ParameterizedDubboConfigBeanDefinitionParser(Class<? extends AbstractParameterizedConfig> configClass) {
        super(configClass);
    }

    protected void parseChildElement(Element childElement, ParserContext parserContext, BeanDefinitionBuilder builder) {
        String tagName = childElement.getLocalName();

        switch (tagName) {
            case "parameter":
                parseParameterElement(childElement, parserContext, builder);
                break;
        }
    }

    private void parseParameterElement(Element parameterElement, ParserContext parserContext, BeanDefinitionBuilder builder) {
        AbstractBeanDefinition beanDefinition = builder.getRawBeanDefinition();
        MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
        String propertyName = "parameters";
        PropertyValue propertyValue = propertyValues.getPropertyValue(propertyName);

        if (propertyValue == null) {
            propertyValue = new PropertyValue(propertyName, new ManagedMap());
            propertyValues.addPropertyValue(propertyValue);
        }

        ManagedMap parameters = (ManagedMap) propertyValue.getValue();

        String key = parameterElement.getAttribute("key");
        String value = resolvePropertyValue(parameterElement.getAttribute("value"), parserContext);
        boolean hide = "true".equals(parameterElement.getAttribute("hide"));
        if (hide) {
            key = HIDE_KEY_PREFIX + key;
        }

        parameters.put(key, new TypedStringValue(value, String.class));
    }
}
