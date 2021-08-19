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
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardMethodMetadata;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Spring Compatibility Utils for spring 3.x/4.x/5.x
 */
public class SpringCompatUtils {

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
