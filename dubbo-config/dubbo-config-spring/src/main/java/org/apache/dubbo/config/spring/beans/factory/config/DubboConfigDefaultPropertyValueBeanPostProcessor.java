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
package org.apache.dubbo.config.spring.beans.factory.config;

import org.apache.dubbo.common.utils.ObjectUtils;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.Constants;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.ClassUtils;

import javax.annotation.PostConstruct;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.springframework.aop.support.AopUtils.getTargetClass;
import static org.springframework.beans.BeanUtils.getPropertyDescriptor;
import static org.springframework.util.ReflectionUtils.invokeMethod;

/**
 * The {@link BeanPostProcessor} class for the default property value of {@link AbstractConfig Dubbo's Config Beans}
 *
 * @since 2.7.6
 */
public class DubboConfigDefaultPropertyValueBeanPostProcessor implements BeanPostProcessor, PriorityOrdered {

    /**
     * The bean name of {@link DubboConfigDefaultPropertyValueBeanPostProcessor}
     */
    public static final String BEAN_NAME = "dubboConfigDefaultPropertyValueBeanPostProcessor";

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (ClassUtils.isAssignableValue(AbstractConfig.class, bean)) {
            // ignore auto generate bean name
            if (!beanName.contains("#")) {
                // [Feature] https://github.com/apache/dubbo/issues/5721
                setPropertyIfAbsent(bean, Constants.ID, beanName);
                // beanName should not be used as config name, fix https://github.com/apache/dubbo/pull/7624
                //setPropertyIfAbsent(dubboConfigBean, "name", beanName);
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    protected void setPropertyIfAbsent(Object bean, String propertyName, String beanName) {

        Class<?> beanClass = getTargetClass(bean);

        PropertyDescriptor propertyDescriptor = getPropertyDescriptor(beanClass, propertyName);

        if (propertyDescriptor != null) { // the property is present

            Method getterMethod = propertyDescriptor.getReadMethod();

            if (getterMethod == null) { // if The getter method is absent
                return;
            }

            Object propertyValue = invokeMethod(getterMethod, bean);

            if (propertyValue != null) { // If The return value of "getId" method is not null
                return;
            }

            Method setterMethod = propertyDescriptor.getWriteMethod();
            if (setterMethod != null) { // the getter and setter methods are present
                if (Arrays.equals(ObjectUtils.of(String.class), setterMethod.getParameterTypes())) { // the param type is String
                    // set bean name to the value of the property
                    invokeMethod(setterMethod, bean, beanName);
                }
            }
        }

    }

    /**
     * @return Higher than {@link InitDestroyAnnotationBeanPostProcessor#getOrder()}
     * @see InitDestroyAnnotationBeanPostProcessor
     * @see CommonAnnotationBeanPostProcessor
     * @see PostConstruct
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE + 1;
    }
}
