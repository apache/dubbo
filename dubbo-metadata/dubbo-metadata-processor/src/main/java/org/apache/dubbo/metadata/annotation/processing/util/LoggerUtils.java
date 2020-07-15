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
package org.apache.dubbo.metadata.annotation.processing.util;


import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import static java.lang.String.format;

/**
 * Logger Utils
 *
 * @since 2.7.6
 */
public interface LoggerUtils {

    Logger LOGGER = LoggerFactory.getLogger("dubbo-metadata-processor");

    static void info(String format, Object... args) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(format(format, args));
        }
    }

    static void warn(String format, Object... args) {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn(format(format, args));
        }
    }
}
