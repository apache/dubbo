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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.AliasRegistry;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static java.beans.Introspector.decapitalize;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableMap;
import static org.springframework.core.annotation.AnnotationAttributes.fromMap;
import static org.springframework.core.annotation.AnnotationUtils.getDefaultValue;
import static org.springframework.core.io.support.SpringFactoriesLoader.loadFactoryNames;
import static org.springframework.util.ClassUtils.getShortName;
import static org.springframework.util.ClassUtils.resolveClassName;
import static org.springframework.util.CollectionUtils.arrayToList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.ObjectUtils.containsElement;
import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ReflectionUtils.findMethod;
import static org.springframework.util.ReflectionUtils.invokeMethod;
import static org.springframework.util.StringUtils.hasText;
import static org.springframework.util.StringUtils.trimWhitespace;


public final class SpringUtils {

    private static final Log log = LogFactory.getLog(SpringUtils.class);

    /**
     * The class name of AnnotatedElementUtils that is introduced since Spring Framework 4
     */
    public static final String ANNOTATED_ELEMENT_UTILS_CLASS_NAME = "org.springframework.core.annotation.AnnotatedElementUtils";


    /**
     * Get Sub {@link Properties}
     *
     * @param propertySources {@link PropertySource} Iterable
     * @param prefix          the prefix of property name
     * @return Map
     * @see Properties
     */
    public static Map<String, Object> getSubProperties(Iterable<PropertySource<?>> propertySources, String prefix) {

        MutablePropertySources mutablePropertySources = new MutablePropertySources();

        for (PropertySource<?> source : propertySources) {
            mutablePropertySources.addLast(source);
        }

        return getSubProperties(mutablePropertySources, prefix);

    }

    /**
     * Get Sub {@link Properties}
     *
     * @param environment {@link ConfigurableEnvironment}
     * @param prefix      the prefix of property name
     * @return Map
     * @see Properties
     */
    public static Map<String, Object> getSubProperties(ConfigurableEnvironment environment, String prefix) {
        return getSubProperties(environment.getPropertySources(), environment, prefix);
    }

    /**
     * Normalize the prefix
     *
     * @param prefix the prefix
     * @return the prefix
     */
    public static String normalizePrefix(String prefix) {
        return prefix.endsWith(".") ? prefix : prefix + ".";
    }

    /**
     * Get prefixed {@link Properties}
     *
     * @param propertySources {@link PropertySources}
     * @param prefix          the prefix of property name
     * @return Map
     * @see Properties
     * @since 1.0.3
     */
    public static Map<String, Object> getSubProperties(PropertySources propertySources, String prefix) {
        return getSubProperties(propertySources, new PropertySourcesPropertyResolver(propertySources), prefix);
    }

    /**
     * Get prefixed {@link Properties}
     *
     * @param propertySources  {@link PropertySources}
     * @param propertyResolver {@link PropertyResolver} to resolve the placeholder if present
     * @param prefix           the prefix of property name
     * @return Map
     * @see Properties
     * @since 1.0.3
     */
    public static Map<String, Object> getSubProperties(PropertySources propertySources, PropertyResolver propertyResolver, String prefix) {

        Map<String, Object> subProperties = new LinkedHashMap<String, Object>();

        String normalizedPrefix = normalizePrefix(prefix);

        Iterator<PropertySource<?>> iterator = propertySources.iterator();

        while (iterator.hasNext()) {
            PropertySource<?> source = iterator.next();
            if (source instanceof EnumerablePropertySource) {
                for (String name : ((EnumerablePropertySource<?>) source).getPropertyNames()) {
                    if (!subProperties.containsKey(name) && name.startsWith(normalizedPrefix)) {
                        String subName = name.substring(normalizedPrefix.length());
                        if (!subProperties.containsKey(subName)) { // take first one
                            Object value = source.getProperty(name);
                            if (value instanceof String) {
                                // Resolve placeholder
                                value = propertyResolver.resolvePlaceholders((String) value);
                            }
                            subProperties.put(subName, value);
                        }
                    }
                }
            }
        }

        return unmodifiableMap(subProperties);
    }


    /**
     * Get the {@link Annotation} attributes
     *
     * @param annotation           specified {@link Annotation}
     * @param ignoreDefaultValue   whether ignore default value or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return non-null
     * @since 1.0.2
     */
    public static Map<String, Object> getAttributes(Annotation annotation, boolean ignoreDefaultValue,
                                                    String... ignoreAttributeNames) {
        return getAttributes(annotation, null, ignoreDefaultValue, ignoreAttributeNames);
    }

    /**
     * Get the {@link Annotation} attributes
     *
     * @param annotation           specified {@link Annotation}
     * @param propertyResolver     {@link PropertyResolver} instance, e.g {@link Environment}
     * @param ignoreDefaultValue   whether ignore default value or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return non-null
     * @since 1.0.2
     */
    public static Map<String, Object> getAttributes(Annotation annotation, PropertyResolver propertyResolver,
                                                    boolean ignoreDefaultValue, String... ignoreAttributeNames) {

        Map<String, Object> annotationAttributes = org.springframework.core.annotation.AnnotationUtils.getAnnotationAttributes(annotation);

        String[] actualIgnoreAttributeNames = ignoreAttributeNames;

        if (ignoreDefaultValue && !isEmpty(annotationAttributes)) {

            List<String> attributeNamesToIgnore = new LinkedList<String>(asList(ignoreAttributeNames));

            for (Map.Entry<String, Object> annotationAttribute : annotationAttributes.entrySet()) {
                String attributeName = annotationAttribute.getKey();
                Object attributeValue = annotationAttribute.getValue();
                if (nullSafeEquals(attributeValue, getDefaultValue(annotation, attributeName))) {
                    attributeNamesToIgnore.add(attributeName);
                }
            }
            // extends the ignored list
            actualIgnoreAttributeNames = attributeNamesToIgnore.toArray(new String[attributeNamesToIgnore.size()]);
        }

        return getAttributes(annotationAttributes, propertyResolver, actualIgnoreAttributeNames);
    }

    /**
     * Get the {@link Annotation} attributes
     *
     * @param annotationAttributes the attributes of specified {@link Annotation}
     * @param propertyResolver     {@link PropertyResolver} instance, e.g {@link Environment}
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return non-null
     * @since 1.0.4
     */
    public static Map<String, Object> getAttributes(Map<String, Object> annotationAttributes,
                                                    PropertyResolver propertyResolver, String... ignoreAttributeNames) {

        Set<String> ignoreAttributeNamesSet = new HashSet<String>(arrayToList(ignoreAttributeNames));

        Map<String, Object> actualAttributes = new LinkedHashMap<String, Object>();

        for (Map.Entry<String, Object> annotationAttribute : annotationAttributes.entrySet()) {

            String attributeName = annotationAttribute.getKey();
            Object attributeValue = annotationAttribute.getValue();

            // ignore attribute name
            if (ignoreAttributeNamesSet.contains(attributeName)) {
                continue;
            }

            if (attributeValue instanceof String) {
                attributeValue = resolvePlaceholders(valueOf(attributeValue), propertyResolver);
            } else if (attributeValue instanceof String[]) {
                String[] values = (String[]) attributeValue;
                for (int i = 0; i < values.length; i++) {
                    values[i] = resolvePlaceholders(values[i], propertyResolver);
                }
                attributeValue = values;
            }
            actualAttributes.put(attributeName, attributeValue);
        }
        return actualAttributes;
    }

    private static String resolvePlaceholders(String attributeValue, PropertyResolver propertyResolver) {
        String resolvedValue = attributeValue;
        if (propertyResolver != null) {
            resolvedValue = propertyResolver.resolvePlaceholders(resolvedValue);
            resolvedValue = trimWhitespace(resolvedValue);
        }
        return resolvedValue;
    }

    /**
     * Get the attribute value
     *
     * @param annotation    {@link Annotation annotation}
     * @param attributeName the name of attribute
     * @param <T>           the type of attribute value
     * @return the attribute value if found
     * @since 1.0.3
     */
    public static <T> T getAttribute(Annotation annotation, String attributeName) {
        return getAttribute(org.springframework.core.annotation.AnnotationUtils.getAnnotationAttributes(annotation), attributeName);
    }

    /**
     * Get the attribute value
     *
     * @param attributes    {@link Map the annotation attributes} or {@link AnnotationAttributes}
     * @param attributeName the name of attribute
     * @param <T>           the type of attribute value
     * @return the attribute value if found
     * @since 1.0.3
     */
    public static <T> T getAttribute(Map<String, Object> attributes, String attributeName) {
        return getAttribute(attributes, attributeName, false);
    }

    /**
     * Get the attribute value the will
     *
     * @param attributes    {@link Map the annotation attributes} or {@link AnnotationAttributes}
     * @param attributeName the name of attribute
     * @param required      the required attribute or not
     * @param <T>           the type of attribute value
     * @return the attribute value if found
     * @throws IllegalStateException if attribute value can't be found
     * @since 1.0.6
     */
    public static <T> T getAttribute(Map<String, Object> attributes, String attributeName, boolean required) {
        T value = getAttribute(attributes, attributeName, null);
        if (required && value == null) {
            throw new IllegalStateException("The attribute['" + attributeName + "] is required!");
        }
        return value;
    }

    /**
     * Get the attribute value with default value
     *
     * @param attributes    {@link Map the annotation attributes} or {@link AnnotationAttributes}
     * @param attributeName the name of attribute
     * @param defaultValue  the default value of attribute
     * @param <T>           the type of attribute value
     * @return the attribute value if found
     * @since 1.0.6
     */
    public static <T> T getAttribute(Map<String, Object> attributes, String attributeName, T defaultValue) {
        T value = (T) attributes.get(attributeName);
        return value == null ? defaultValue : value;
    }

    /**
     * Get the required attribute value
     *
     * @param attributes    {@link Map the annotation attributes} or {@link AnnotationAttributes}
     * @param attributeName the name of attribute
     * @param <T>           the type of attribute value
     * @return the attribute value if found
     * @throws IllegalStateException if attribute value can't be found
     * @since 1.0.6
     */
    public static <T> T getRequiredAttribute(Map<String, Object> attributes, String attributeName) {
        return getAttribute(attributes, attributeName, true);
    }

    /**
     * Get the {@link AnnotationAttributes}
     *
     * @param annotation           specified {@link Annotation}
     * @param ignoreDefaultValue   whether ignore default value or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return non-null
     * @see #getAnnotationAttributes(Annotation, PropertyResolver, boolean, String...)
     * @since 1.0.3
     */
    public static AnnotationAttributes getAnnotationAttributes(Annotation annotation, boolean ignoreDefaultValue,
                                                               String... ignoreAttributeNames) {
        return getAnnotationAttributes(annotation, null, ignoreDefaultValue, ignoreAttributeNames);
    }

    /**
     * Get the {@link AnnotationAttributes}
     *
     * @param annotation           specified {@link Annotation}
     * @param propertyResolver     {@link PropertyResolver} instance, e.g {@link Environment}
     * @param ignoreDefaultValue   whether ignore default value or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return non-null
     * @see #getAttributes(Annotation, PropertyResolver, boolean, String...)
     * @see #getAnnotationAttributes(AnnotatedElement, Class, PropertyResolver, boolean, String...)
     * @since 1.0.3
     */
    public static AnnotationAttributes getAnnotationAttributes(Annotation annotation, PropertyResolver propertyResolver,
                                                               boolean ignoreDefaultValue, String... ignoreAttributeNames) {
        return fromMap(getAttributes(annotation, propertyResolver, ignoreDefaultValue, ignoreAttributeNames));
    }

    /**
     * Get the {@link AnnotationAttributes}
     *
     * @param annotatedElement     {@link AnnotatedElement the annotated element}
     * @param annotationType       the {@link Class tyoe} pf {@link Annotation annotation}
     * @param propertyResolver     {@link PropertyResolver} instance, e.g {@link Environment}
     * @param ignoreDefaultValue   whether ignore default value or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return if <code>annotatedElement</code> can't be found in <code>annotatedElement</code>, return <code>null</code>
     * @since 1.0.3
     */
    public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement annotatedElement,
                                                               Class<? extends Annotation> annotationType,
                                                               PropertyResolver propertyResolver,
                                                               boolean ignoreDefaultValue,
                                                               String... ignoreAttributeNames) {
        Annotation annotation = annotatedElement.getAnnotation(annotationType);
        return annotation == null ? null : getAnnotationAttributes(annotation, propertyResolver, ignoreDefaultValue, ignoreAttributeNames);
    }

    /**
     * Get the {@link AnnotationAttributes}, if the argument <code>tryMergedAnnotation</code> is <code>true</code>,
     * the {@link AnnotationAttributes} will be got from
     * {@link #tryGetMergedAnnotationAttributes(AnnotatedElement, Class, PropertyResolver, boolean, String...) merged annotation} first,
     * if failed, and then to get from
     * {@link #getAnnotationAttributes(AnnotatedElement, Class, PropertyResolver, boolean, boolean, String...) normal one}
     *
     * @param annotatedElement     {@link AnnotatedElement the annotated element}
     * @param annotationType       the {@link Class tyoe} pf {@link Annotation annotation}
     * @param propertyResolver     {@link PropertyResolver} instance, e.g {@link Environment}
     * @param ignoreDefaultValue   whether ignore default value or not
     * @param tryMergedAnnotation  whether try merged annotation or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return if <code>annotatedElement</code> can't be found in <code>annotatedElement</code>, return <code>null</code>
     * @since 1.0.3
     */
    public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement annotatedElement,
                                                               Class<? extends Annotation> annotationType,
                                                               PropertyResolver propertyResolver,
                                                               boolean ignoreDefaultValue,
                                                               boolean tryMergedAnnotation,
                                                               String... ignoreAttributeNames) {
        AnnotationAttributes attributes = null;

        if (tryMergedAnnotation) {
            attributes = tryGetMergedAnnotationAttributes(annotatedElement, annotationType, propertyResolver, ignoreDefaultValue, ignoreAttributeNames);
        }

        if (attributes == null) {
            attributes = getAnnotationAttributes(annotatedElement, annotationType, propertyResolver, ignoreDefaultValue, ignoreAttributeNames);
        }

        return attributes;
    }

    /**
     * Try to get the merged {@link Annotation annotation}
     *
     * @param annotatedElement {@link AnnotatedElement the annotated element}
     * @param annotationType   the {@link Class tyoe} pf {@link Annotation annotation}
     * @return If current version of Spring Framework is below 4.2, return <code>null</code>
     * @since 1.0.3
     */
    public static Annotation tryGetMergedAnnotation(AnnotatedElement annotatedElement,
                                                    Class<? extends Annotation> annotationType) {

        Annotation mergedAnnotation = null;

        ClassLoader classLoader = annotationType.getClassLoader();

        if (ClassUtils.isPresent(ANNOTATED_ELEMENT_UTILS_CLASS_NAME, classLoader)) {
            Class<?> annotatedElementUtilsClass = resolveClassName(ANNOTATED_ELEMENT_UTILS_CLASS_NAME, classLoader);
            // getMergedAnnotation method appears in the Spring Framework 4.2
            Method getMergedAnnotationMethod = findMethod(annotatedElementUtilsClass, "getMergedAnnotation", AnnotatedElement.class, Class.class);
            if (getMergedAnnotationMethod != null) {
                mergedAnnotation = (Annotation) invokeMethod(getMergedAnnotationMethod, null, annotatedElement, annotationType);
            }
        }

        return mergedAnnotation;
    }

    /**
     * Try to get {@link AnnotationAttributes the annotation attributes} after merging and resolving the placeholders
     *
     * @param annotatedElement     {@link AnnotatedElement the annotated element}
     * @param annotationType       the {@link Class tyoe} pf {@link Annotation annotation}
     * @param propertyResolver     {@link PropertyResolver} instance, e.g {@link Environment}
     * @param ignoreDefaultValue   whether ignore default value or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return If the specified annotation type is not found, return <code>null</code>
     * @since 1.0.3
     */
    public static AnnotationAttributes tryGetMergedAnnotationAttributes(AnnotatedElement annotatedElement,
                                                                        Class<? extends Annotation> annotationType,
                                                                        PropertyResolver propertyResolver,
                                                                        boolean ignoreDefaultValue,
                                                                        String... ignoreAttributeNames) {
        Annotation annotation = tryGetMergedAnnotation(annotatedElement, annotationType);
        return annotation == null ? null : getAnnotationAttributes(annotation, propertyResolver, ignoreDefaultValue, ignoreAttributeNames);
    }


    /**
     * Register Infrastructure Bean
     *
     * @param beanDefinitionRegistry {@link BeanDefinitionRegistry}
     * @param beanType               the type of bean
     * @param beanName               the name of bean
     * @return if it's a first time to register, return <code>true</code>, or <code>false</code>
     */
    public static boolean registerInfrastructureBean(BeanDefinitionRegistry beanDefinitionRegistry,
                                                     String beanName,
                                                     Class<?> beanType) {

        boolean registered = false;

        if (!beanDefinitionRegistry.containsBeanDefinition(beanName)) {
            RootBeanDefinition beanDefinition = new RootBeanDefinition(beanType);
            beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            beanDefinitionRegistry.registerBeanDefinition(beanName, beanDefinition);
            registered = true;

            if (log.isInfoEnabled()) {
                log.info("The Infrastructure bean definition [" + beanDefinition
                    + "with name [" + beanName + "] has been registered.");
            }
        }

        return registered;
    }

    /**
     * Detect the alias is present or not in the given bean name from {@link AliasRegistry}
     *
     * @param registry {@link AliasRegistry}
     * @param beanName the bean name
     * @param alias    alias to test
     * @return if present, return <code>true</code>, or <code>false</code>
     */
    public static boolean hasAlias(AliasRegistry registry, String beanName, String alias) {
        return hasText(beanName) && hasText(alias) && containsElement(registry.getAliases(beanName), alias);
    }


    /**
     * Register the beans from {@link SpringFactoriesLoader#loadFactoryNames(Class, ClassLoader) SpringFactoriesLoader}
     *
     * @param registry       {@link BeanDefinitionRegistry}
     * @param factoryClasses The factory classes to register
     * @return the count of beans that are succeeded to be registered
     * @since 1.0.7
     */
    public static int registerSpringFactoriesBeans(BeanDefinitionRegistry registry, Class<?>... factoryClasses) {
        int count = 0;

        ClassLoader classLoader = registry.getClass().getClassLoader();

        for (int i = 0; i < factoryClasses.length; i++) {
            Class<?> factoryClass = factoryClasses[i];
            List<String> factoryImplClassNames = loadFactoryNames(factoryClass, classLoader);
            for (String factoryImplClassName : factoryImplClassNames) {
                Class<?> factoryImplClass = resolveClassName(factoryImplClassName, classLoader);
                String beanName = decapitalize(getShortName(factoryImplClassName));
                if (registerInfrastructureBean(registry, beanName, factoryImplClass)) {
                    count++;
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn(format("The Factory Class bean[%s] has been registered with bean name[%s]",
                            factoryImplClassName, beanName));
                    }
                }
            }
        }

        return count;
    }


    /**
     * Unwrap {@link BeanFactory} to {@link ConfigurableListableBeanFactory}
     *
     * @param beanFactory {@link ConfigurableListableBeanFactory}
     * @return {@link ConfigurableListableBeanFactory}
     * @throws IllegalArgumentException If <code>beanFactory</code> argument is not an instance of {@link ConfigurableListableBeanFactory}
     */
    public static ConfigurableListableBeanFactory unwrap(BeanFactory beanFactory) throws IllegalArgumentException {
        Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory,
            "The 'beanFactory' argument is not an instance of ConfigurableListableBeanFactory, " +
                "is it running in Spring container?");
        return ConfigurableListableBeanFactory.class.cast(beanFactory);
    }


    /**
     * Unwrap {@link Environment} to {@link ConfigurableEnvironment}
     *
     * @param environment {@link Environment}
     * @return {@link ConfigurableEnvironment}
     * @throws IllegalArgumentException If <code>environment</code> argument is not an instance of {@link ConfigurableEnvironment}
     */
    public static ConfigurableEnvironment unwrap(Environment environment) throws IllegalArgumentException {
        Assert.isInstanceOf(ConfigurableEnvironment.class, environment,
            "The 'environment' argument is not a instance of ConfigurableEnvironment, " +
                "is it running in Spring container?");
        return (ConfigurableEnvironment) environment;
    }
}
