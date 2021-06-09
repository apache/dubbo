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
package org.apache.dubbo.common.status;

/**
 * Status
 */
public class Status {

    private final Level level;
    private final String message;
    private final String description;

    public Status(Level level) {
        this(level, null, null);
    }

    public Status(Level level, String message) {
        this(level, message, null);
    }

    public Status(Level level, String message, String description) {
        this.level = level;
        this.message = message;
        this.description = description;
    }

    public Level getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Level
     */
    public enum Level {
        /**
         * OK
         */
        OK,

        /**
         * WARN
         */
        WARN,

        /**
         * ERROR
         */
        ERROR,

        /**
         * UNKNOWN
         */
        UNKNOWN
    }

}