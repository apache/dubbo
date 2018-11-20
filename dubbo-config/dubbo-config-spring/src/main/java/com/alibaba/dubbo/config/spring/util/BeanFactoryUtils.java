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
package com.alibaba.dubbo.config.spring.util;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.AbstractApplicationContext;

import java.lang.reflect.Method;

/**
 * {@link BeanFactory} Utilities class
 *
 * @see BeanFactory
 * @see ConfigurableBeanFactory
 * @see org.springframework.beans.factory.BeanFactoryUtils
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
}
