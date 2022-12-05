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
package org.apache.dubbo.qos.command.annotation;

import org.apache.dubbo.common.utils.StringUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

/**
 * Command
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Cmd {

    /**
     * Command name
     *
     * @return command name
     */
    String name();

    /**
     * Command description
     *
     * @return command description
     */
    String summary();

    /**
     * Command example
     *
     * @return command example
     */
    String[] example() default {};

    /**
     * Command order in help
     *
     * @return command order in help
     */
    int sort() default 0;

    /**
     * Command required access permission level
     *
     * @return command permission level
     */
    PermissionLevel requiredPermissionLevel() default PermissionLevel.PROTECTED;

    enum PermissionLevel {
        /**
         * the lowest permission level, can access with
         * anonymousAccessPermissionLevel=PUBLIC / anonymousAccessPermissionLevel=1 or higher
         */
        PUBLIC(1),
        /**
         * the middle permission level, default permission for each cmd
         */
        PROTECTED(2),
        /**
         * the highest permission level, suppose only the localhost can access this command
         */
        PRIVATE(3),

        /**
         * It is the reserved default anonymous permission level, can not access any command
         */
        NONE(Integer.MIN_VALUE),

        ;
        private final int level;

        PermissionLevel(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }

        public static PermissionLevel from(String permissionLevel) {
            if (StringUtils.isNumber(permissionLevel)) {
                return Arrays.stream(values())
                    .filter(p -> String.valueOf(p.getLevel()).equals(permissionLevel.trim()))
                    .findFirst()
                    .orElse(NONE);
            }
            return Arrays.stream(values())
                .filter(p -> p.name().equalsIgnoreCase(String.valueOf(permissionLevel).trim()))
                .findFirst()
                .orElse(NONE);
        }
    }

}
