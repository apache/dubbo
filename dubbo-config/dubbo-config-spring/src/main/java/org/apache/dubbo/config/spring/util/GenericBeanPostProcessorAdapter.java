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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ClassUtils;

/**
 * Generic {@link BeanPostProcessor} Adapter
 *
 * @see BeanPostProcessor
 */
@SuppressWarnings("unchecked")
public abstract class GenericBeanPostProcessorAdapter<T> implements BeanPostProcessor {

    private final Class<T> beanType;

    public GenericBeanPostProcessorAdapter() {
        ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        this.beanType = (Class<T>) actualTypeArguments[0];
    }

    @Override
    public final Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (ClassUtils.isAssignableValue(beanType, bean)) {
            return doPostProcessBeforeInitialization((T) bean, beanName);
        }
        return bean;
    }

    @Override
    public final Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (ClassUtils.isAssignableValue(beanType, bean)) {
            return doPostProcessAfterInitialization((T) bean, beanName);
        }
        return bean;
    }

    /**
     * Bean Type
     *
     * @return Bean Type
     */
    public final Class<T> getBeanType() {
        return beanType;
    }

    /**
     * Adapter BeanPostProcessor#postProcessBeforeInitialization(Object, String) method , sub-type
     * could override this method.
     *
     * @param bean     Bean Object
     * @param beanName Bean Name
     * @return Bean Object
     * @see BeanPostProcessor#postProcessBeforeInitialization(Object, String)
     */
    protected T doPostProcessBeforeInitialization(T bean, String beanName) throws BeansException {

        processBeforeInitialization(bean, beanName);

        return bean;
    }

    /**
     * Adapter BeanPostProcessor#postProcessAfterInitialization(Object, String) method , sub-type
     * could override this method.
     *
     * @param bean     Bean Object
     * @param beanName Bean Name
     * @return Bean Object
     * @see BeanPostProcessor#postProcessAfterInitialization(Object, String)
     */
    protected T doPostProcessAfterInitialization(T bean, String beanName) throws BeansException {

        processAfterInitialization(bean, beanName);

        return bean;
    }

    /**
     * Process {@link T Bean} with name without return value before initialization,
     * <p>
     * This method will be invoked by BeanPostProcessor#postProcessBeforeInitialization(Object, String)
     *
     * @param bean     Bean Object
     * @param beanName Bean Name
     * @throws BeansException  in case of errors
     */
    protected void processBeforeInitialization(T bean, String beanName) throws BeansException {}

    /**
     * Process {@link T Bean} with name without return value after initialization,
     * <p>
     * This method will be invoked by BeanPostProcessor#postProcessAfterInitialization(Object, String)
     *
     * @param bean     Bean Object
     * @param beanName Bean Name
     * @throws BeansException  in case of errors
     */
    protected void processAfterInitialization(T bean, String beanName) throws BeansException {}
}
