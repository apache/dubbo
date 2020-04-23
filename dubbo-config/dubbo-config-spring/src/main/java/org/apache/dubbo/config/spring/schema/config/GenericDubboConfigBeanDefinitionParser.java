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
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.SslConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.beans.PropertyDescriptor;

import static java.lang.String.format;
import static org.apache.dubbo.common.utils.CaseStyleUtils.kebabToCamel;
import static org.apache.dubbo.common.utils.ClassUtils.isConcreteClass;
import static org.springframework.beans.BeanUtils.getPropertyDescriptor;

/**
 * Generic {@link BeanDefinitionParser} implementation class is used to parse the {@link BeanDefinition} for the Dubbo
 * {@link AbstractConfig config} in XML meta configuration.
 * <ul>
 *     <li>{@link ModuleConfig}</li>
 *     <li>{@link MetricsConfig}</li>
 *     <li>{@link SslConfig}</li>
 * </ul>
 *
 * @see BeanDefinition
 * @see BeanDefinitionParser
 * @see AbstractConfig
 * @see ModuleConfig
 * @see MetricsConfig
 * @see SslConfig
 * @see AbstractSingleBeanDefinitionParser
 * @since 2.7.7
 */
public class GenericDubboConfigBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    protected final Log logger = LogFactory.getLog(getClass());

    private final Class<? extends AbstractConfig> configClass;

    public GenericDubboConfigBeanDefinitionParser(Class<? extends AbstractConfig> configClass) {
        if (!isConcreteClass(configClass)) {
            throw new IllegalArgumentException("The argument must be the concrete class.");
        }
        this.configClass = configClass;
    }

    @Override
    protected final void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        preParse(element, parserContext, builder);
        parseElement(element, parserContext, builder);
        parseAttributes(element, parserContext, builder);
        parseChildElements(element, parserContext, builder);
        postParse(element, parserContext, builder);
    }

    /**
     * Pre-parse the target {@link Element element}.
     *
     * @param element       {@link Element element}
     * @param parserContext {@link ParserContext}
     * @param builder       {@link BeanDefinitionBuilder}
     */
    protected void preParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
    }

    /**
     * Parse the target {@link Element element}.
     *
     * @param element       {@link Element element}
     * @param parserContext {@link ParserContext}
     * @param builder       {@link BeanDefinitionBuilder}
     */
    protected void parseElement(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
    }

    /**
     * Parse the child {@link Element elements} of the target {@link Element element}.
     *
     * @param element       {@link Element element}
     * @param parserContext {@link ParserContext}
     * @param builder       {@link BeanDefinitionBuilder}
     */
    private void parseChildElements(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        NodeList nodeList = element.getChildNodes();
        int nodeListLength = nodeList.getLength();
        for (int i = 0; i < nodeListLength; i++) {
            Node node = nodeList.item(i);
            if (Node.ELEMENT_NODE == node.getNodeType()) { // Filter {@link Element} only
                Element childElement = (Element) node;
                parseChildElement(childElement, parserContext, builder);
            }
        }
    }

    /**
     * Parse the child {@link Element element} of the target {@link Element element}.
     *
     * @param childElement  {@link Element element}
     * @param parserContext {@link ParserContext}
     * @param builder       {@link BeanDefinitionBuilder}
     */
    protected void parseChildElement(Element childElement, ParserContext parserContext, BeanDefinitionBuilder builder) {
    }

    /**
     * Post-parse the target {@link Element element}.
     *
     * @param element       {@link Element element}
     * @param parserContext {@link ParserContext}
     * @param builder       {@link BeanDefinitionBuilder}
     */
    protected void postParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
    }

    private void parseAttributes(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        NamedNodeMap attributes = element.getAttributes();
        int attributesLength = attributes.getLength();
        for (int i = 0; i < attributesLength; i++) {
            Node attribute = attributes.item(i);
            if (Node.ATTRIBUTE_NODE != attribute.getNodeType()) {
                continue;
            }
            parseAttribute(attribute, element, parserContext, builder);
        }
    }

    /**
     * Parse the {@link Node attribute} on the target {@link Element element}
     *
     * @param attribute     the {@link Node attribute}
     * @param element       {@link Element element}
     * @param parserContext {@link ParserContext}
     * @param builder       {@link BeanDefinitionBuilder}
     * @return if parsing is successful, return <code>true</code>
     */
    protected boolean parseAttribute(Node attribute, Element element, ParserContext parserContext,
                                     BeanDefinitionBuilder builder) {
        // Build {@link PropertyValue} if possible
        return addPropertyValue(attribute, element, parserContext, builder);
    }

    private boolean addPropertyValue(Node attribute, Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {

        String attributeName = attribute.getLocalName();
        String attributeValue = attribute.getNodeValue();
        String propertyName = resolvePropertyName(attributeName);
        String propertyValue = resolvePropertyValue(attributeValue, parserContext);

        PropertyDescriptor propertyDescriptor = getPropertyDescriptor(configClass, propertyName);

        if (propertyDescriptor == null) { // The property of target bean type can't be found.
            if (logger.isWarnEnabled()) {
                logger.warn(format("The property[name : %s] can't be found in the target config[type : %s]," +
                                " however the attribute[xmlns : %s , name : %s , value : %s] was configured in the XML " +
                                "metadata configuration.", propertyName, configClass.getName(), attribute.getNamespaceURI(),
                        attribute.getNodeName(), attributeValue));
            }
            return false;
        }

        Class<?> propertyType = propertyDescriptor.getPropertyType();

        // If propertyType is the subtype of AbstractConfig, take the attribute value as the Spring Bean Name
        if (isConfigType(propertyType)) {
            builder.addPropertyValue(propertyName, new RuntimeBeanReference(attributeValue));
        } else {
            builder.addPropertyValue(propertyName, propertyValue);
        }

        return true;
    }

    private boolean isConfigType(Class<?> propertyType) {
        return AbstractConfig.class.isAssignableFrom(propertyType);
    }

    /**
     * Resolve the property name for {@link PropertyValue} from the attribute name.
     *
     * @param attributeName the name of attribute of XML element
     * @return If the pattern of <code>attributeName</code> is like "user-name", the resolved result will be "userName"
     * , or return <code>attributeName</code>
     */
    protected String resolvePropertyName(String attributeName) {
        return kebabToCamel(attributeName);
    }

    /**
     * Resolve the property value of {@link PropertyValue}
     *
     * @param attributeValue the value of attribute of XML element
     * @param parserContext  {@link ParserContext}
     * @return Resolve the placeholders from <code>attributeValue</code>
     */
    protected String resolvePropertyValue(String attributeValue, ParserContext parserContext) {
        PropertyResolver propertyResolver = parserContext.getReaderContext().getEnvironment();
        return resolvePropertyValue(attributeValue, propertyResolver);
    }

    /**
     * Resolve the property value of {@link PropertyValue}
     *
     * @param attributeValue   the value of attribute of XML element
     * @param propertyResolver {@link PropertyResolver}
     * @return Resolve the placeholders from <code>attributeValue</code>
     */
    protected String resolvePropertyValue(String attributeValue, PropertyResolver propertyResolver) {
        return propertyResolver.resolvePlaceholders(attributeValue);
    }

    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
            throws BeanDefinitionStoreException {
        // try "id" attribute first
        String id = element.getAttribute(ID_ATTRIBUTE);
        if (!StringUtils.hasText(id)) {
            // try "name" attribute latter
            id = element.getAttribute(NAME_ATTRIBUTE);
        }
        // generate default id as fallback
        if (!StringUtils.hasText(id) && shouldGenerateIdAsFallback()) {
            id = parserContext.getReaderContext().generateBeanName(definition);
        }
        return id;
    }

    @Override
    protected boolean shouldGenerateIdAsFallback() {
        return true;
    }

    @Override
    protected Class<?> getBeanClass(Element element) {
        return configClass;
    }

    public Class<? extends AbstractConfig> getConfigClass() {
        return configClass;
    }
}