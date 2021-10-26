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
package org.apache.dubbo.spring.boot.actuate.endpoint.condition;

import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;

import java.util.stream.Stream;

/**
 * {@link Conditional} that checks whether or not an endpoint is enabled, which is compatible with
 * org.springframework.boot.actuate.autoconfigure.endpoint.condition.OnEnabledEndpointCondition
 * and org.springframework.boot.actuate.autoconfigure.endpoint.condition.OnAvailableEndpointCondition
 *
 * @see CompatibleConditionalOnEnabledEndpoint
 * @since 2.7.7
 */
class CompatibleOnEnabledEndpointCondition implements Condition {

    static String[] CONDITION_CLASS_NAMES = {
            "org.springframework.boot.actuate.autoconfigure.endpoint.condition.OnAvailableEndpointCondition", // 2.2.0+
            "org.springframework.boot.actuate.autoconfigure.endpoint.condition.OnEnabledEndpointCondition" // [2.0.0 , 2.2.x]
    };


    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        ClassLoader classLoader = context.getClassLoader();

        Condition condition = Stream.of(CONDITION_CLASS_NAMES)                         // Iterate class names
                .filter(className -> ClassUtils.isPresent(className, classLoader))     // Search class existing or not by name
                .findFirst()                                                           // Find the first candidate
                .map(className -> ClassUtils.resolveClassName(className, classLoader)) // Resolve class name to Class
                .filter(Condition.class::isAssignableFrom)                             // Accept the Condition implementation
                .map(BeanUtils::instantiateClass)                                      // Instantiate Class to be instance
                .map(Condition.class::cast)                                            // Cast the instance to be Condition one
                .orElse(NegativeCondition.INSTANCE);                                   // Or else get a negative condition

        return condition.matches(context, metadata);
    }

    private static class NegativeCondition implements Condition {

        static final NegativeCondition INSTANCE = new NegativeCondition();

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return false;
        }
    }
}
