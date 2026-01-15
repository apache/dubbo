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

import org.springframework.boot.SpringBootVersion;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that matches when running on Spring Boot 3.x or higher.
 *
 * <p>This condition is used to enable Spring Boot 3.x specific auto-configuration
 * that requires Jakarta EE APIs (jakarta.servlet.*) instead of Java EE APIs (javax.servlet.*).
 *
 * <p>Compatible with Spring Boot 3.5.x and Spring Cloud 2025.0.0.
 *
 * @since 3.2.0
 */
public class SpringBoot3Condition implements Condition {

    /**
     * Cached result indicating if we're running on Spring Boot 3.x or higher.
     * Uses safe version parsing to handle edge cases.
     */
    public static final boolean IS_SPRING_BOOT_3 = isSpringBoot3OrHigher();

    private static boolean isSpringBoot3OrHigher() {
        try {
            String version = SpringBootVersion.getVersion();
            if (version == null || version.isEmpty()) {
                // Fallback: check for Jakarta Servlet API presence (Spring Boot 3 indicator)
                try {
                    Class.forName("jakarta.servlet.Servlet");
                    return true;
                } catch (ClassNotFoundException e) {
                    return false;
                }
            }
            // Parse major version from version string (e.g., "3.5.9" -> '3')
            char majorVersion = version.charAt(0);
            return majorVersion >= '3';
        } catch (Exception e) {
            // If version detection fails, assume older Spring Boot
            return false;
        }
    }

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return IS_SPRING_BOOT_3;
    }
}
