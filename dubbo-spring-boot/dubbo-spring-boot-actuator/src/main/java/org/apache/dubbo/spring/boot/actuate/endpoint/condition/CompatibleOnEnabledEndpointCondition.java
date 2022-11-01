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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_CLASS_NOT_FOUND;

/**
 * {@link Conditional} that checks whether or not an endpoint is enabled, which is compatible with
 * org.springframework.boot.actuate.autoconfigure.endpoint.condition.OnEnabledEndpointCondition
 * and org.springframework.boot.actuate.autoconfigure.endpoint.condition.OnAvailableEndpointCondition
 *
 * @see CompatibleConditionalOnEnabledEndpoint
 * @since 2.7.7
 */
class CompatibleOnEnabledEndpointCondition implements Condition {

    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(CompatibleOnEnabledEndpointCondition.class);

    // Spring Boot [2.0.0 , 2.2.x]
    static String CONDITION_CLASS_NAME_OLD =
        "org.springframework.boot.actuate.autoconfigure.endpoint.condition.OnEnabledEndpointCondition";

    // Spring Boot 2.2.0 +
    static String CONDITION_CLASS_NAME_NEW =
        "org.springframework.boot.actuate.autoconfigure.endpoint.condition.OnAvailableEndpointCondition";


    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        ClassLoader classLoader = context.getClassLoader();
        if (ClassUtils.isPresent(CONDITION_CLASS_NAME_OLD, classLoader)) {
            Class<?> cls = ClassUtils.resolveClassName(CONDITION_CLASS_NAME_OLD, classLoader);
            if (Condition.class.isAssignableFrom(cls)) {
                Condition condition = Condition.class.cast(BeanUtils.instantiateClass(cls));
                return condition.matches(context, metadata);
            }
        }
        // Check by org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint
        if (ClassUtils.isPresent(CONDITION_CLASS_NAME_NEW, classLoader)) {
            return true;
        }
        // No condition class found
        LOGGER.warn(COMMON_CLASS_NOT_FOUND, "No condition class found", "", String.format("No condition class found, Dubbo Health Endpoint [%s] will not expose", metadata));
        return false;
    }
}
