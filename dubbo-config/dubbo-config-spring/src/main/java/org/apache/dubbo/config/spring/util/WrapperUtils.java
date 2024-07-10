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
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * The utilities class for wrapper interfaces
 *
 */
public abstract class WrapperUtils {

    /**
     * Unwrap {@link BeanFactory} to {@link ConfigurableListableBeanFactory}
     *
     * @param beanFactory {@link ConfigurableListableBeanFactory}
     * @return {@link ConfigurableListableBeanFactory}
     * @throws IllegalArgumentException If <code>beanFactory</code> argument is not an instance of {@link ConfigurableListableBeanFactory}
     */
    public static ConfigurableListableBeanFactory unwrap(BeanFactory beanFactory) throws IllegalArgumentException {
        Assert.isInstanceOf(
                ConfigurableListableBeanFactory.class,
                beanFactory,
                "The 'beanFactory' argument is not an instance of ConfigurableListableBeanFactory, "
                        + "is it running in Spring container?");
        return ConfigurableListableBeanFactory.class.cast(beanFactory);
    }

    /**
     * Unwrap {@link Environment} to {@link ConfigurableEnvironment}
     *
     * @param environment {@link Environment}
     * @return {@link ConfigurableEnvironment}
     * @throws IllegalArgumentException If <code>environment</code> argument is not an instance of {@link ConfigurableEnvironment}
     */
    public static ConfigurableEnvironment unwrap(Environment environment) throws IllegalArgumentException {
        Assert.isInstanceOf(
                ConfigurableEnvironment.class,
                environment,
                "The 'environment' argument is not a instance of ConfigurableEnvironment, "
                        + "is it running in Spring container?");
        return (ConfigurableEnvironment) environment;
    }
}
