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
package org.apache.dubbo.config.spring.extension;

import org.apache.dubbo.common.context.Lifecycle;
import org.apache.dubbo.common.extension.ExtensionAccessor;
import org.apache.dubbo.common.extension.ExtensionInjector;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;

/**
 * SpringExtensionInjector
 */
public class SpringExtensionInjector implements ExtensionInjector, Lifecycle {
    private static final Logger logger = LoggerFactory.getLogger(SpringExtensionInjector.class);

    public static final String NAME = "spring";

    private ApplicationContext context;

    @Deprecated
    public static void addApplicationContext(ApplicationContext context) {
//        CONTEXTS.add(context);
//        if (context instanceof ConfigurableApplicationContext) {
//            ((ConfigurableApplicationContext) context).registerShutdownHook();
//            // see https://github.com/apache/dubbo/issues/7093
//            DubboShutdownHook.getDubboShutdownHook().unregister();
//        }
    }

//    @Deprecated
//    public static Set<ApplicationContext> getContexts() {
//        // return contexts;
//        return Collections.emptySet();
//    }

//    @Deprecated
//    public static void clearContexts() {
//        //contexts.clear();
//    }

    public static SpringExtensionInjector get(ExtensionAccessor extensionAccessor) {
        return (SpringExtensionInjector) extensionAccessor.getExtension(ExtensionInjector.class, NAME);
    }

    public ApplicationContext getContext() {
        return context;
    }

    public void init(ApplicationContext context) {
        this.context = context;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getInstance(Class<T> type, String name) {

        if (context == null) {
            // ignore if spring context is not bound
            return null;
        }

        //check @SPI annotation
        if (type.isInterface() && type.isAnnotationPresent(SPI.class)) {
            return null;
        }

        T bean = getOptionalBean(context, name, type);
        if (bean != null) {
            return bean;
        }

        //logger.warn("No spring extension (bean) named:" + name + ", try to find an extension (bean) of type " + type.getName());
        return null;
    }

    private <T> T getOptionalBean(ListableBeanFactory beanFactory, String name, Class<T> type) {
        if (StringUtils.isEmpty(name)) {
            String[] beanNamesForType = beanFactory.getBeanNamesForType(type, true, false);
            if (beanNamesForType != null) {
                if (beanNamesForType.length == 1) {
                    return beanFactory.getBean(beanNamesForType[0], type);
                } else if (beanNamesForType.length > 1) {
                    throw new IllegalStateException("Expect single but found " + beanNamesForType.length + " beans in spring context: " +
                        Arrays.toString(beanNamesForType));
                }
            }
        } else {
            if (beanFactory.containsBean(name)) {
                return beanFactory.getBean(name, type);
            }
        }
        return null;
    }

    @Override
    public void initialize() throws IllegalStateException {
    }

    @Override
    public void start() throws IllegalStateException {
        // no op
    }

    @Override
    public void destroy() {
    }
}
