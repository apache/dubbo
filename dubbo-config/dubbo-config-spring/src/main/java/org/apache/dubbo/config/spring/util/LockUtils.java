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

import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.context.ApplicationContext;

public class LockUtils {

    private static final String DUBBO_SINGLETON_MUTEX_KEY = "DUBBO_SINGLETON_MUTEX";

    /**
     * Get the mutex to lock the singleton in the specified {@link ApplicationContext}
     */
    public static synchronized Object getSingletonMutex(ApplicationContext applicationContext) {
        DefaultSingletonBeanRegistry autowireCapableBeanFactory =
                (DefaultSingletonBeanRegistry) applicationContext.getAutowireCapableBeanFactory();
        try {
            return autowireCapableBeanFactory.getSingletonMutex();
        } catch (Throwable t1) {
            try {
                // try protected
                Method method = DefaultSingletonBeanRegistry.class.getDeclaredMethod("getSingletonMutex");
                method.setAccessible(true);
                return method.invoke(autowireCapableBeanFactory);
            } catch (Throwable t2) {
                // Before Spring 4.2, there is no getSingletonMutex method
                if (!autowireCapableBeanFactory.containsSingleton(DUBBO_SINGLETON_MUTEX_KEY)) {
                    autowireCapableBeanFactory.registerSingleton(DUBBO_SINGLETON_MUTEX_KEY, new Object());
                }
                return autowireCapableBeanFactory.getSingleton(DUBBO_SINGLETON_MUTEX_KEY);
            }
        }
    }
}
