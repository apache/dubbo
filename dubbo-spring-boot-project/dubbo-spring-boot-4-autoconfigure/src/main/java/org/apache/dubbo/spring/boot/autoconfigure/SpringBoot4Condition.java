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

public class SpringBoot4Condition implements Condition {

    public static boolean IS_SPRING_BOOT_4 = getSpringBootMajorVersion() >= 4;

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return IS_SPRING_BOOT_4;
    }

    private static int getSpringBootMajorVersion() {
        String version = SpringBootVersion.getVersion();
        if (version == null || version.isEmpty()) {
            return -1;
        }
        int separator = version.indexOf('.');
        String majorVersion = separator < 0 ? version : version.substring(0, separator);
        try {
            return Integer.parseInt(majorVersion);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
