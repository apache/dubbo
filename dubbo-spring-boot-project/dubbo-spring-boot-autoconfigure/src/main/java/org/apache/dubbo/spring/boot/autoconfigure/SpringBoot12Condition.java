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
package org.apache.dubbo.spring.boot.autoconfigure;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that matches when running on Spring Boot 1.x or 2.x (pre-Spring Boot 3).
 *
 * <p>This condition is used to enable Spring Boot 2.x specific auto-configuration
 * that uses Java EE APIs (javax.servlet.*) instead of Jakarta EE APIs (jakarta.servlet.*).
 *
 * <p>This is the inverse of {@link SpringBoot3Condition}.
 *
 * @since 3.2.0
 * @see SpringBoot3Condition
 */
public class SpringBoot12Condition implements Condition {

    /**
     * Cached result indicating if we're running on Spring Boot 1.x or 2.x.
     * This is simply the inverse of {@link SpringBoot3Condition#IS_SPRING_BOOT_3}.
     */
    public static final boolean IS_SPRING_BOOT_12 = !SpringBoot3Condition.IS_SPRING_BOOT_3;

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return IS_SPRING_BOOT_12;
    }
}
