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

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Spring Compatibility Utils for spring 3.x/4.x/5.x
 */
public class SpringCompatUtils {

    static final String FACTORY_BEAN_OBJECT_TYPE = "factoryBeanObjectType";

    private static volatile Boolean factoryMethodMetadataEnabled = null;

    private static final Log logger = LogFactory.getLog(SpringCompatUtils.class);

    public static <T> T getPropertyValue(PropertyValues pvs, String propertyName) {
        PropertyValue pv = pvs.getPropertyValue(propertyName);
        Object val = pv != null ? pv.getValue() : null;
        if (val instanceof TypedStringValue) {
            TypedStringValue typedString = (TypedStringValue) val;
            return (T) typedString.getValue();
        }
        return (T) val;
    }

    public static boolean isFactoryMethodMetadataEnabled() {
        if (factoryMethodMetadataEnabled == null) {
            try {
                //check AnnotatedBeanDefinition.getFactoryMethodMetadata() since spring 4.1
                AnnotatedBeanDefinition.class.getMethod("getFactoryMethodMetadata");

                // check MethodMetadata.getReturnTypeName() since spring 4.2
                MethodMetadata.class.getMethod("getReturnTypeName");

                factoryMethodMetadataEnabled = true;
            } catch (NoSuchMethodException e) {
                factoryMethodMetadataEnabled = false;
            }
        }
        return factoryMethodMetadataEnabled;
    }

    public static String getFactoryMethodReturnType(AnnotatedBeanDefinition annotatedBeanDefinition) {
        try {
            if (isFactoryMethodMetadataEnabled()) {
                MethodMetadata factoryMethodMetadata = annotatedBeanDefinition.getFactoryMethodMetadata();
                return factoryMethodMetadata != null ? factoryMethodMetadata.getReturnTypeName() : null;
            } else {
                Object source = annotatedBeanDefinition.getSource();
                if (source instanceof StandardMethodMetadata) {
                    StandardMethodMetadata methodMetadata = (StandardMethodMetadata) source;
                    Method introspectedMethod = methodMetadata.getIntrospectedMethod();
                    if (introspectedMethod != null) {
                        return introspectedMethod.getReturnType().getName();
                    }
                }
            }
        } catch (Throwable e) {
            if (logger.isInfoEnabled()) {
                logger.info("get return type of AnnotatedBeanDefinition failed", e);
            }
        }
        return null;
    }

    public static MethodMetadata getFactoryMethodMetadata(AnnotatedBeanDefinition annotatedBeanDefinition) {
        if (isFactoryMethodMetadataEnabled()) {
            return annotatedBeanDefinition.getFactoryMethodMetadata();
        } else {
            Object source = annotatedBeanDefinition.getSource();
            if (source instanceof StandardMethodMetadata) {
                return (MethodMetadata) source;
            }
            return null;
        }
    }

    /**
     * here we do not use beanFactory#getType(), which may cause early instantiation but get null objectType
     * int the case of static @Bean method or @Bean method in @Component java config bean to build ReferenceBean.
     * we get the return generic type as interface name from the @Bean method at the registry time.
     * when the bean has the same method name, we just get common ancestor of return generic type here
     *
     * @see org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForBeanMethod(org.springframework.context.annotation.BeanMethod)
     * @see org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#getTypeForFactoryBean(java.lang.String, org.springframework.beans.factory.support.RootBeanDefinition, boolean)
     * @see org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#getTypeForFactoryBeanFromMethod(java.lang.Class, java.lang.String)
     *
     * @param beanFactory
     * @param definition
     * @return
     */
    public static Class<?> getTypeForFactoryBeanFromMethod(ConfigurableListableBeanFactory beanFactory, BeanDefinition definition) {
        try {
            if (definition.hasAttribute(FACTORY_BEAN_OBJECT_TYPE)) {
                ResolvableType factoryBeanGeneric = getTypeFromAttribute(definition.getAttribute(FACTORY_BEAN_OBJECT_TYPE));
                if (factoryBeanGeneric != null) {
                    return factoryBeanGeneric.resolve();
                }
            }
            if (definition instanceof AnnotatedBeanDefinition) {
                MethodMetadata factoryMethodMetadata = ((AnnotatedBeanDefinition) definition).getFactoryMethodMetadata();
                if (factoryMethodMetadata instanceof StandardMethodMetadata) {
                    Method method = ((StandardMethodMetadata) factoryMethodMetadata).getIntrospectedMethod();
                    ResolvableType factoryMethodReturnType = ResolvableType.forMethodReturnType(method);
                    ResolvableType generic = factoryMethodReturnType.as(FactoryBean.class).getGeneric();
                    return generic.resolve();
                }
            }
            if (!StringUtils.isBlank(definition.getFactoryMethodName())) {
                Class<?> factoryClass;
                if (!StringUtils.isBlank(definition.getFactoryBeanName())) {
                    // instance @Bean method
                    BeanDefinition factoryDefinition = beanFactory.getBeanDefinition(definition.getFactoryBeanName());
                    factoryClass = ClassUtils.forName(factoryDefinition.getBeanClassName(), beanFactory.getBeanClassLoader());
                } else {
                    // static @Bean method
                    factoryClass = ClassUtils.forName(definition.getBeanClassName(), beanFactory.getBeanClassLoader());
                }
                factoryClass = ClassUtils.getUserClass(factoryClass);
                ResolvableType generic = getTypeForFactoryBeanFromMethod(definition, factoryClass);
                return generic.resolve();
            }
        } catch (Exception e) {
            logger.warn("determineTargetType factory bean error : " + definition, e);
        }
        return null;
    }

    /**
     *
     * deal with method override. Here return common ancestor of the return generic type
     * @param definition
     * @param factoryClass
     * @return
     */
    private static ResolvableType getTypeForFactoryBeanFromMethod(BeanDefinition definition, Class<?> factoryClass) {
        ResolvableType result = ResolvableType.NONE;
        for (Method candidate : getCandidateFactoryMethods(definition, factoryClass)) {
            if (candidate.getName().equals(definition.getFactoryMethodName())
                && FactoryBean.class.isAssignableFrom(candidate.getReturnType())
                && !candidate.isBridge() && !candidate.isSynthetic()) {

                ResolvableType returnType = ResolvableType.forMethodReturnType(candidate);
                ResolvableType returnTypeGeneric = returnType.as(FactoryBean.class).getGeneric();
                if (result == ResolvableType.NONE) {
                    result = returnTypeGeneric;
                }
                else {
                    Class<?> resolvedResult = result.resolve();
                    Class<?> commonAncestor = ClassUtils.determineCommonAncestor(returnTypeGeneric.resolve(), resolvedResult);
                    if (!ObjectUtils.nullSafeEquals(resolvedResult, commonAncestor)) {
                        result = ResolvableType.forClass(commonAncestor);
                    }
                }
            }
        }
        return result;
    }

    private static Method[] getCandidateFactoryMethods(BeanDefinition definition, Class<?> factoryClass) {
        return (shouldConsiderNonPublicMethods(definition) ? ReflectionUtils.getAllDeclaredMethods(factoryClass)
            : factoryClass.getMethods());
    }

    private static boolean shouldConsiderNonPublicMethods(BeanDefinition definition) {
        return (definition instanceof AbstractBeanDefinition)
            && ((AbstractBeanDefinition) definition).isNonPublicAccessAllowed();
    }

    private static ResolvableType getTypeFromAttribute(Object attribute) throws ClassNotFoundException, LinkageError {
        if (attribute instanceof Class<?>) {
            return ResolvableType.forClass((Class<?>) attribute);
        }
        if (attribute instanceof String) {
            return ResolvableType.forClass(ClassUtils.forName((String) attribute, null));
        }
        return null;
    }

    /**
     * Get the generic type of return type of the method.
     *
     * <pre>
     *  Source method:
     *  ReferenceBean&lt;DemoService> demoService()
     *
     *  Result: DemoService.class
     * </pre>
     *
     * @param factoryMethodMetadata
     * @return
     */
    public static Class getGenericTypeOfReturnType(MethodMetadata factoryMethodMetadata) {
        if (factoryMethodMetadata instanceof StandardMethodMetadata) {
            Method introspectedMethod = ((StandardMethodMetadata) factoryMethodMetadata).getIntrospectedMethod();
            Type returnType = introspectedMethod.getGenericReturnType();
            if (returnType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) returnType;
                Type actualTypeArgument = parameterizedType.getActualTypeArguments()[0];
                if (actualTypeArgument instanceof Class) {
                    return (Class) actualTypeArgument;
                }
            }
        }
        return null;
    }
}
