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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.AbstractApplicationContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * {@link BeanFactory} Utilities class
 *
 * @see BeanFactory
 * @see ConfigurableBeanFactory
 * @see org.springframework.beans.factory.BeanFactoryUtils
 * @since 2.5.7
 */
public class BeanFactoryUtils {

    public static boolean addApplicationListener(ApplicationContext applicationContext, ApplicationListener listener) {
        try {
            // backward compatibility to spring 2.0.1
            Method method = applicationContext.getClass().getMethod("addApplicationListener", ApplicationListener.class);
            method.invoke(applicationContext, listener);
            return true;
        } catch (Throwable t) {
            if (applicationContext instanceof AbstractApplicationContext) {
                try {
                    // backward compatibility to spring 2.0.1
                    Method method = AbstractApplicationContext.class.getDeclaredMethod("addListener", ApplicationListener.class);
                    if (!method.isAccessible()) {
                        method.setAccessible(true);
                    }
                    method.invoke(applicationContext, listener);
                    return true;
                } catch (Throwable t2) {
                    // ignore
                }
            }
        }
        return false;
    }

    /**
     * Get nullable Bean
     *
     * @param beanFactory {@link ListableBeanFactory}
     * @param beanName    the name of Bean
     * @param beanType    the {@link Class type} of Bean
     * @param <T>         the {@link Class type} of Bean
     * @return A bean if present , or <code>null</code>
     */
    public static <T> T getNullableBean(ListableBeanFactory beanFactory, String beanName, Class<T> beanType) {
        T bean = null;
        try {
            bean = beanFactory.getBean(beanName, beanType);
        } catch (Throwable ignored) {
            // Any exception will be ignored to handle
        }
        return bean;
    }


    /**
     * Gets name-matched Beans from {@link ListableBeanFactory BeanFactory}
     *
     * @param beanFactory {@link ListableBeanFactory BeanFactory}
     * @param beanNames   the names of Bean
     * @param beanType    the {@link Class type} of Bean
     * @param <T>         the {@link Class type} of Bean
     * @return the read-only and non-null {@link List} of Bean names
     */
    public static <T> List<T> getBeans(ListableBeanFactory beanFactory, String[] beanNames, Class<T> beanType) {

        if (isEmpty(beanNames)) {
            return emptyList();
        }

        List<T> beans = new ArrayList<T>(beanNames.length);

        for (String beanName : beanNames) {
            T bean = getNullableBean(beanFactory, beanName, beanType);
            if (bean != null) {
                beans.add(bean);
            }
        }

        return Collections.unmodifiableList(beans);
    }
}
