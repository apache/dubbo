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
package org.apache.dubbo.spring.boot.actuate.endpoint.metadata;

import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Dubbo loggerInfo
 *
 * @since 3.3.0
 */
@Component
public class DubboLoggerInfoMetadata {

    public Map<String, Object> loggerInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("Available logger adapters", LoggerFactory.getAvailableAdapter());
        info.put("Current Adapter", LoggerFactory.getCurrentAdapter());
        info.put("Log level", LoggerFactory.getLevel());
        return info;
    }
}
