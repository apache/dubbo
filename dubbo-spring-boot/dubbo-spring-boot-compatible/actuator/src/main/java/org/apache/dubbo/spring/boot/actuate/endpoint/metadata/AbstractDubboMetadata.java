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
package org.apache.dubbo.spring.boot.actuate.endpoint.metadata;

import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.beans.factory.BeanFactoryUtils.beansOfTypeIncludingAncestors;
import static org.springframework.util.ClassUtils.isPrimitiveOrWrapper;

/**
 * Abstract Dubbo Meatadata
 *
 * @since 2.7.0
 */
public abstract class AbstractDubboMetadata implements ApplicationContextAware, EnvironmentAware {

    protected ApplicationContext applicationContext;

    protected ConfigurableEnvironment environment;

    private static boolean isSimpleType(Class<?> type) {
        return isPrimitiveOrWrapper(type)
                || type == String.class
                || type == BigDecimal.class
                || type == BigInteger.class
                || type == Date.class
                || type == URL.class
                || type == Class.class
                ;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        if (environment instanceof ConfigurableEnvironment) {
            this.environment = (ConfigurableEnvironment) environment;
        }
    }

    protected Map<String, Object> resolveBeanMetadata(final Object bean) {

        final Map<String, Object> beanMetadata = new LinkedHashMap<>();

        try {

            BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();

            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {

                Method readMethod = propertyDescriptor.getReadMethod();

                if (readMethod != null && isSimpleType(propertyDescriptor.getPropertyType())) {

                    String name = Introspector.decapitalize(propertyDescriptor.getName());
                    Object value = readMethod.invoke(bean);
                    if (value != null) {
                        beanMetadata.put(name, value);
                    }
                }

            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return beanMetadata;

    }

    protected Map<String, ServiceBean> getServiceBeansMap() {
        return beansOfTypeIncludingAncestors(applicationContext, ServiceBean.class);
    }

    protected ReferenceAnnotationBeanPostProcessor getReferenceAnnotationBeanPostProcessor() {
        return DubboBeanUtils.getReferenceAnnotationBeanPostProcessor(applicationContext);
    }

    protected Map<String, ProtocolConfig> getProtocolConfigsBeanMap() {
        return beansOfTypeIncludingAncestors(applicationContext, ProtocolConfig.class);
    }


}
