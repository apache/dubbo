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
package com.alibaba.dubbo.registry.common.util;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * MessageSource
 *
 */
public class MessageSource {

    private static final Logger logger = LoggerFactory.getLogger(MessageSource.class);

    private final ResourceBundle resourceBundle;

    private final String errorPrefix;

    public MessageSource(ResourceBundle resourceBundle) {
        this(resourceBundle, null);
    }

    public MessageSource(ResourceBundle resourceBundle, String errorPrefix) {
        this.resourceBundle = resourceBundle;
        this.errorPrefix = errorPrefix == null ? "" : errorPrefix + " ";
    }

    public String getString(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (Throwable t) {
            logger.warn(errorPrefix + t.getMessage(), t);
            return key;
        }
    }

    public String getString(String key, Object... args) {
        try {
            return new MessageFormat(resourceBundle.getString(key)).format(args);
        } catch (Throwable t) {
            logger.warn(errorPrefix + t.getMessage(), t);
            return key;
        }
    }

}
