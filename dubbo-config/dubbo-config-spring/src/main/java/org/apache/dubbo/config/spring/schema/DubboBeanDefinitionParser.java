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
package org.apache.dubbo.config.spring.schema;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ArgumentConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.ServiceBean;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.apache.dubbo.common.constants.CommonConstants.HIDE_KEY_PREFIX;

/**
 * AbstractBeanDefinitionParser
 *
 * @export
 */
public class DubboBeanDefinitionParser implements BeanDefinitionParser {

    private static final Logger logger = LoggerFactory.getLogger(DubboBeanDefinitionParser.class);
    private static final Pattern GROUP_AND_VERION = Pattern.compile("^[\\-.0-9_a-zA-Z]+(\\:[\\-.0-9_a-zA-Z]+)?$");
    private static final String ONRETURN = "onreturn";
    private static final String ONTHROW = "onthrow";
    private static final String ONINVOKE = "oninvoke";
    private static final String METHOD = "Method";
    private final Class<?> beanClass;
    private final boolean required;

    public DubboBeanDefinitionParser(Class<?> beanClass, boolean required) {
        this.beanClass = beanClass;
        this.required = required;
    }

    @SuppressWarnings("unchecked")
    private static BeanDefinition parse(Element element, ParserContext parserContext, Class<?> beanClass, boolean required) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(beanClass);
        beanDefinition.setLazyInit(false);
        String id = element.getAttribute("id");
        if (StringUtils.isEmpty(id) && required) {
            String generatedBeanName = element.getAttribute("name");
            if (StringUtils.isEmpty(generatedBeanName)) {
                if (ProtocolConfig.class.equals(beanClass)) {
                    generatedBeanName = "dubbo";
                } else {
                    generatedBeanName = element.getAttribute("interface");
                }
            }
            if (StringUtils.isEmpty(generatedBeanName)) {
                generatedBeanName = beanClass.getName();
            }
            id = generatedBeanName;
            int counter = 2;
            while (parserContext.getRegistry().containsBeanDefinition(id)) {
                id = generatedBeanName + (counter++);
            }
        }
        if (StringUtils.isNotEmpty(id)) {
            if (parserContext.getRegistry().containsBeanDefinition(id)) {
                throw new IllegalStateException("Duplicate spring bean id " + id);
            }
            parserContext.getRegistry().registerBeanDefinition(id, beanDefinition);
            beanDefinition.getPropertyValues().addPropertyValue("id", id);
        }
        if (ProtocolConfig.class.equals(beanClass)) {
            for (String name : parserContext.getRegistry().getBeanDefinitionNames()) {
                BeanDefinition definition = parserContext.getRegistry().getBeanDefinition(name);
                PropertyValue property = definition.getPropertyValues().getPropertyValue("protocol");
                if (property != null) {
                    Object value = property.getValue();
                    if (value instanceof ProtocolConfig && id.equals(((ProtocolConfig) value).getName())) {
                        definition.getPropertyValues().addPropertyValue("protocol", new RuntimeBeanReference(id));
                    }
                }
            }
        } else if (ServiceBean.class.equals(beanClass)) {
            String className = element.getAttribute("class");
            if (className != null && className.length() > 0) {
                RootBeanDefinition classDefinition = new RootBeanDefinition();
                classDefinition.setBeanClass(ReflectUtils.forName(className));
                classDefinition.setLazyInit(false);
                parseProperties(element.getChildNodes(), classDefinition);
                beanDefinition.getPropertyValues().addPropertyValue("ref", new BeanDefinitionHolder(classDefinition, id + "Impl"));
            }
        } else if (ProviderConfig.class.equals(beanClass)) {
            parseNested(element, parserContext, ServiceBean.class, true, "service", "provider", id, beanDefinition);
        } else if (ConsumerConfig.class.equals(beanClass)) {
            parseNested(element, parserContext, ReferenceBean.class, false, "reference", "consumer", id, beanDefinition);
        }
        Set<String> props = new HashSet<>();
        ManagedMap parameters = null;
        for (Method setter : beanClass.getMethods()) {
            String name = setter.getName();
            if (name.length() > 3 && name.startsWith("set")
                    && Modifier.isPublic(setter.getModifiers())
                    && setter.getParameterTypes().length == 1) {
                Class<?> type = setter.getParameterTypes()[0];
                String beanProperty = name.substring(3, 4).toLowerCase() + name.substring(4);
                String property = StringUtils.camelToSplitName(beanProperty, "-");
                props.add(property);
                // check the setter/getter whether match
                Method getter = null;
                try {
                    getter = beanClass.getMethod("get" + name.substring(3), new Class<?>[0]);
                } catch (NoSuchMethodException e) {
                    try {
                        getter = beanClass.getMethod("is" + name.substring(3), new Class<?>[0]);
                    } catch (NoSuchMethodException e2) {
                        // ignore, there is no need any log here since some class implement the interface: EnvironmentAware,
                        // ApplicationAware, etc. They only have setter method, otherwise will cause the error log during application start up.
                    }
                }
                if (getter == null
                        || !Modifier.isPublic(getter.getModifiers())
                        || !type.equals(getter.getReturnType())) {
                    continue;
                }
                if ("parameters".equals(property)) {
                    parameters = parseParameters(element.getChildNodes(), beanDefinition);
                } else if ("methods".equals(property)) {
                    parseMethods(id, element.getChildNodes(), beanDefinition, parserContext);
                } else if ("arguments".equals(property)) {
                    parseArguments(id, element.getChildNodes(), beanDefinition, parserContext);
                } else {
                    String value = element.getAttribute(property);
                    if (value != null) {
                        value = value.trim();
                        if (value.length() > 0) {
                            if ("registry".equals(property) && RegistryConfig.NO_AVAILABLE.equalsIgnoreCase(value)) {
                                RegistryConfig registryConfig = new RegistryConfig();
                                registryConfig.setAddress(RegistryConfig.NO_AVAILABLE);
                                beanDefinition.getPropertyValues().addPropertyValue(beanProperty, registryConfig);
                            } else if ("provider".equals(property) || "registry".equals(property) || ("protocol".equals(property) && ServiceBean.class.equals(beanClass))) {
                                /**
                                 * For 'provider' 'protocol' 'registry', keep literal value (should be id/name) and set the value to 'registryIds' 'providerIds' protocolIds'
                                 * The following process should make sure each id refers to the corresponding instance, here's how to find the instance for different use cases:
                                 * 1. Spring, check existing bean by id, see{@link ServiceBean#afterPropertiesSet()}; then try to use id to find configs defined in remote Config Center
                                 * 2. API, directly use id to find configs defined in remote Config Center; if all config instances are defined locally, please use {@link org.apache.dubbo.config.ServiceConfig#setRegistries(List)}
                                 */
                                beanDefinition.getPropertyValues().addPropertyValue(beanProperty + "Ids", value);
                            } else {
                                Object reference;
                                if (isPrimitive(type)) {
                                    if ("async".equals(property) && "false".equals(value)
                                            || "timeout".equals(property) && "0".equals(value)
                                            || "delay".equals(property) && "0".equals(value)
                                            || "version".equals(property) && "0.0.0".equals(value)
                                            || "stat".equals(property) && "-1".equals(value)
                                            || "reliable".equals(property) && "false".equals(value)) {
                                        // backward compatibility for the default value in old version's xsd
                                        value = null;
                                    }
                                    reference = value;
                                } else if(ONRETURN.equals(property) || ONTHROW.equals(property) || ONINVOKE.equals(property)) {
                                    int index = value.lastIndexOf(".");
                                    String ref = value.substring(0, index);
                                    String method = value.substring(index + 1);
                                    reference = new RuntimeBeanReference(ref);
                                    beanDefinition.getPropertyValues().addPropertyValue(property + METHOD, method);
                                } else {
                                    if ("ref".equals(property) && parserContext.getRegistry().containsBeanDefinition(value)) {
                                        BeanDefinition refBean = parserContext.getRegistry().getBeanDefinition(value);
                                        if (!refBean.isSingleton()) {
                                            throw new IllegalStateException("The exported service ref " + value + " must be singleton! Please set the " + value + " bean scope to singleton, eg: <bean id=\"" + value + "\" scope=\"singleton\" ...>");
                                        }
                                    }
                                    reference = new RuntimeBeanReference(value);
                                }
                                beanDefinition.getPropertyValues().addPropertyValue(beanProperty, reference);
                            }
                        }
                    }
                }
            }
        }
        NamedNodeMap attributes = element.getAttributes();
        int len = attributes.getLength();
        for (int i = 0; i < len; i++) {
            Node node = attributes.item(i);
            String name = node.getLocalName();
            if (!props.contains(name)) {
                if (parameters == null) {
                    parameters = new ManagedMap();
                }
                String value = node.getNodeValue();
                parameters.put(name, new TypedStringValue(value, String.class));
            }
        }
        if (parameters != null) {
            beanDefinition.getPropertyValues().addPropertyValue("parameters", parameters);
        }
        return beanDefinition;
    }

    private static boolean isPrimitive(Class<?> cls) {
        return cls.isPrimitive() || cls == Boolean.class || cls == Byte.class
                || cls == Character.class || cls == Short.class || cls == Integer.class
                || cls == Long.class || cls == Float.class || cls == Double.class
                || cls == String.class || cls == Date.class || cls == Class.class;
    }

    private static void parseNested(Element element, ParserContext parserContext, Class<?> beanClass, boolean required, String tag, String property, String ref, BeanDefinition beanDefinition) {
        NodeList nodeList = element.getChildNodes();
        if (nodeList != null && nodeList.getLength() > 0) {
            boolean first = true;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    if (tag.equals(node.getNodeName())
                            || tag.equals(node.getLocalName())) {
                        if (first) {
                            first = false;
                            String isDefault = element.getAttribute("default");
                            if (StringUtils.isEmpty(isDefault)) {
                                beanDefinition.getPropertyValues().addPropertyValue("default", "false");
                            }
                        }
                        BeanDefinition subDefinition = parse((Element) node, parserContext, beanClass, required);
                        if (subDefinition != null && ref != null && ref.length() > 0) {
                            subDefinition.getPropertyValues().addPropertyValue(property, new RuntimeBeanReference(ref));
                        }
                    }
                }
            }
        }
    }

    private static void parseProperties(NodeList nodeList, RootBeanDefinition beanDefinition) {
        if (nodeList != null && nodeList.getLength() > 0) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    if ("property".equals(node.getNodeName())
                            || "property".equals(node.getLocalName())) {
                        String name = ((Element) node).getAttribute("name");
                        if (name != null && name.length() > 0) {
                            String value = ((Element) node).getAttribute("value");
                            String ref = ((Element) node).getAttribute("ref");
                            if (value != null && value.length() > 0) {
                                beanDefinition.getPropertyValues().addPropertyValue(name, value);
                            } else if (ref != null && ref.length() > 0) {
                                beanDefinition.getPropertyValues().addPropertyValue(name, new RuntimeBeanReference(ref));
                            } else {
                                throw new UnsupportedOperationException("Unsupported <property name=\"" + name + "\"> sub tag, Only supported <property name=\"" + name + "\" ref=\"...\" /> or <property name=\"" + name + "\" value=\"...\" />");
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static ManagedMap parseParameters(NodeList nodeList, RootBeanDefinition beanDefinition) {
        if (nodeList != null && nodeList.getLength() > 0) {
            ManagedMap parameters = null;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    if ("parameter".equals(node.getNodeName())
                            || "parameter".equals(node.getLocalName())) {
                        if (parameters == null) {
                            parameters = new ManagedMap();
                        }
                        String key = ((Element) node).getAttribute("key");
                        String value = ((Element) node).getAttribute("value");
                        boolean hide = "true".equals(((Element) node).getAttribute("hide"));
                        if (hide) {
                            key = HIDE_KEY_PREFIX + key;
                        }
                        parameters.put(key, new TypedStringValue(value, String.class));
                    }
                }
            }
            return parameters;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static void parseMethods(String id, NodeList nodeList, RootBeanDefinition beanDefinition,
                                     ParserContext parserContext) {
        if (nodeList != null && nodeList.getLength() > 0) {
            ManagedList methods = null;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    Element element = (Element) node;
                    if ("method".equals(node.getNodeName()) || "method".equals(node.getLocalName())) {
                        String methodName = element.getAttribute("name");
                        if (StringUtils.isEmpty(methodName)) {
                            throw new IllegalStateException("<dubbo:method> name attribute == null");
                        }
                        if (methods == null) {
                            methods = new ManagedList();
                        }
                        BeanDefinition methodBeanDefinition = parse(((Element) node),
                                parserContext, MethodConfig.class, false);
                        String name = id + "." + methodName;
                        BeanDefinitionHolder methodBeanDefinitionHolder = new BeanDefinitionHolder(
                                methodBeanDefinition, name);
                        methods.add(methodBeanDefinitionHolder);
                    }
                }
            }
            if (methods != null) {
                beanDefinition.getPropertyValues().addPropertyValue("methods", methods);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void parseArguments(String id, NodeList nodeList, RootBeanDefinition beanDefinition,
                                       ParserContext parserContext) {
        if (nodeList != null && nodeList.getLength() > 0) {
            ManagedList arguments = null;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    Element element = (Element) node;
                    if ("argument".equals(node.getNodeName()) || "argument".equals(node.getLocalName())) {
                        String argumentIndex = element.getAttribute("index");
                        if (arguments == null) {
                            arguments = new ManagedList();
                        }
                        BeanDefinition argumentBeanDefinition = parse(((Element) node),
                                parserContext, ArgumentConfig.class, false);
                        String name = id + "." + argumentIndex;
                        BeanDefinitionHolder argumentBeanDefinitionHolder = new BeanDefinitionHolder(
                                argumentBeanDefinition, name);
                        arguments.add(argumentBeanDefinitionHolder);
                    }
                }
            }
            if (arguments != null) {
                beanDefinition.getPropertyValues().addPropertyValue("arguments", arguments);
            }
        }
    }

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        return parse(element, parserContext, beanClass, required);
    }

}
