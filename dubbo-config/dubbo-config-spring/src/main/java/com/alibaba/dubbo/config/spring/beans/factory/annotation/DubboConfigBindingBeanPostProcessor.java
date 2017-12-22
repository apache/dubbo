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
package com.alibaba.dubbo.config.spring.beans.factory.annotation;

import com.alibaba.dubbo.common.utils.Assert;
import com.alibaba.dubbo.config.spring.context.annotation.DubboConfigBindingRegistrar;
import com.alibaba.dubbo.config.spring.context.annotation.EnableDubboConfigBinding;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.validation.DataBinder;

import java.util.Arrays;

/**
 * Dubbo Config Binding {@link BeanPostProcessor}
 *
 * @see EnableDubboConfigBinding
 * @see DubboConfigBindingRegistrar
 * @since 2.5.8
 */
public class DubboConfigBindingBeanPostProcessor implements BeanPostProcessor {

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Binding Bean Name
     */
    private final String beanName;

    /**
     * Binding {@link PropertyValues}
     */
    private final PropertyValues propertyValues;


    /**
     * @param beanName       Binding Bean Name
     * @param propertyValues {@link PropertyValues}
     */
    public DubboConfigBindingBeanPostProcessor(String beanName, PropertyValues propertyValues) {
        Assert.notNull(beanName, "The name of bean must not be null");
        Assert.notNull(propertyValues, "The PropertyValues of bean must not be null");
        this.beanName = beanName;
        this.propertyValues = propertyValues;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        if (beanName.equals(this.beanName)) {
            DataBinder dataBinder = new DataBinder(bean);
            // TODO ignore invalid fields by annotation attribute
            dataBinder.setIgnoreInvalidFields(true);
            dataBinder.bind(propertyValues);
            if (log.isInfoEnabled()) {
                log.info("The properties of bean [name : " + beanName + "] have been binding by values : "
                        + Arrays.asList(propertyValues.getPropertyValues()));
            }
        }

        return bean;

    }


    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

}
