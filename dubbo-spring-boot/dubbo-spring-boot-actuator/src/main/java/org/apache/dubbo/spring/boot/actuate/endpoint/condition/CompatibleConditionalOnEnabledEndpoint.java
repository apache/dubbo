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

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointExtension;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link Conditional} that checks whether or not an endpoint is enabled, which is compatible with
 * org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint ([2.0.x, 2.2.x])
 * org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint
 *
 * @see CompatibleOnEnabledEndpointCondition
 * @since 2.7.7
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Conditional(CompatibleOnEnabledEndpointCondition.class)
public @interface CompatibleConditionalOnEnabledEndpoint {

    /**
     * The endpoint type that should be checked. Inferred when the return type of the
     * {@code @Bean} method is either an {@link Endpoint @Endpoint} or an
     * {@link EndpointExtension @EndpointExtension}.
     *
     * @return the endpoint type to check
     */
    Class<?> endpoint() default Void.class;
}
