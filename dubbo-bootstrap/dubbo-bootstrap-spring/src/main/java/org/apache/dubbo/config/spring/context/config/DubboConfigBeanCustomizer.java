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
import org.apache.dubbo.config.spring.beans.factory.annotation.DubboConfigBindingBeanPostProcessor;
import org.apache.dubbo.config.spring.context.properties.DubboConfigBinder;

import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;

/**
 * The Bean customizer for {@link AbstractConfig Dubbo Config}. Generally, The subclass will be  registered as a Spring
 * Bean that is used to {@link #customize(String, AbstractConfig) customize} {@link AbstractConfig Dubbo Config} bean
 * after {@link DubboConfigBinder#bind(String, AbstractConfig) its binding}.
 * <p>
 * If There are multiple {@link DubboConfigBeanCustomizer} beans in the Spring {@link ApplicationContext context}, they
 * are executed orderly, thus the subclass should be aware to implement the {@link #getOrder()} method.
 *
 * @see DubboConfigBinder#bind(String, AbstractConfig)
 * @see DubboConfigBindingBeanPostProcessor
 * @since 2.6.6
 */
public interface DubboConfigBeanCustomizer extends Ordered {

    /**
     * Customize {@link AbstractConfig Dubbo Config Bean}
     *
     * @param beanName        the name of {@link AbstractConfig Dubbo Config Bean}
     * @param dubboConfigBean the instance of {@link AbstractConfig Dubbo Config Bean}
     */
    void customize(String beanName, AbstractConfig dubboConfigBean);
}
