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
package org.apache.dubbo.config.spring.context.config;

import org.apache.dubbo.config.AbstractConfig;

import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.apache.dubbo.config.spring.util.ObjectUtils.of;
import static org.springframework.beans.BeanUtils.getPropertyDescriptor;

/**
 * {@link DubboConfigBeanCustomizer} for the default value for the "name" property that will be taken bean name
 * if absent.
 *
 * @since 2.6.6
 */
public class NamePropertyDefaultValueDubboConfigBeanCustomizer implements DubboConfigBeanCustomizer {

    /**
     * The bean name of {@link NamePropertyDefaultValueDubboConfigBeanCustomizer}
     *
     * @since 2.7.1
     */
    public static final String BEAN_NAME = "namePropertyDefaultValueDubboConfigBeanCustomizer";

    /**
     * The name of property that is "name" maybe is absent in target class
     */
    private static final String PROPERTY_NAME = "name";

    @Override
    public void customize(String beanName, AbstractConfig dubboConfigBean) {

        PropertyDescriptor propertyDescriptor = getPropertyDescriptor(dubboConfigBean.getClass(), PROPERTY_NAME);

        if (propertyDescriptor != null) { // "name" property is present

            Method getNameMethod = propertyDescriptor.getReadMethod();

            if (getNameMethod == null) { // if "getName" method is absent
                return;
            }

            Object propertyValue = ReflectionUtils.invokeMethod(getNameMethod, dubboConfigBean);

            if (propertyValue != null) { // If The return value of "getName" method is not null
                return;
            }

            Method setNameMethod = propertyDescriptor.getWriteMethod();
            if (setNameMethod != null) { // "setName" and "getName" methods are present
                if (Arrays.equals(of(String.class), setNameMethod.getParameterTypes())) { // the param type is String
                    // set bean name to the value of the "name" property
                    ReflectionUtils.invokeMethod(setNameMethod, dubboConfigBean, beanName);
                }
            }
        }
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
