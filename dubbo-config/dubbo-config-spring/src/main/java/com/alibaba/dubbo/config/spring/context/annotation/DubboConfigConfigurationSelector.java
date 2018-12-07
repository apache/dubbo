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
package com.alibaba.dubbo.config.spring.context.annotation;

import com.alibaba.dubbo.config.AbstractConfig;

import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Dubbo {@link AbstractConfig Config} {@link ImportSelector} implementation, which order can be configured
 *
 * @see EnableDubboConfig
 * @see DubboConfigConfiguration
 * @see Ordered
 * @since 2.5.8
 */
public class DubboConfigConfigurationSelector implements ImportSelector, EnvironmentAware, Ordered {

    /**
     * The property name of {@link EnableDubboConfig}'s {@link Ordered order}
     */
    public static final String DUBBO_CONFIG_ORDER_PROPERTY_NAME = "dubbo.config.order";

    private int order;

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {

        AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                importingClassMetadata.getAnnotationAttributes(EnableDubboConfig.class.getName()));

        boolean multiple = attributes.getBoolean("multiple");

        if (multiple) {
            return of(DubboConfigConfiguration.Multiple.class.getName());
        } else {
            return of(DubboConfigConfiguration.Single.class.getName());
        }
    }

    /**
     * Set {@link Ordered order}, it may be changed in in the future.
     *
     * @param order {@link Ordered order}
     */
    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.order = environment.getProperty(DUBBO_CONFIG_ORDER_PROPERTY_NAME, int.class, LOWEST_PRECEDENCE);
    }

    private static <T> T[] of(T... values) {
        return values;
    }
}