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
package org.apache.dubbo.spring.boot.observability.autoconfigure;

import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * registry observationHandlers to observationConfig
 */
public class ObservationRegistryPostProcessor implements BeanPostProcessor {
    private final ObjectProvider<ObservationHandlerGrouping> observationHandlerGrouping;
    private final ObjectProvider<ObservationHandler<?>> observationHandlers;

    public ObservationRegistryPostProcessor(ObjectProvider<ObservationHandlerGrouping> observationHandlerGrouping,
                                            ObjectProvider<ObservationHandler<?>> observationHandlers){
        this.observationHandlerGrouping = observationHandlerGrouping;
        this.observationHandlers = observationHandlers;

    }
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if(bean instanceof ObservationRegistry){
            ObservationRegistry observationRegistry = (ObservationRegistry) bean;
            List<ObservationHandler<?>> observationHandlerList =
                observationHandlers.orderedStream().collect(Collectors.toList());
            observationHandlerGrouping.ifAvailable(grouping->{
                grouping.apply(observationHandlerList,
                    observationRegistry.observationConfig());
            });
        }
        return bean;
    }
}
