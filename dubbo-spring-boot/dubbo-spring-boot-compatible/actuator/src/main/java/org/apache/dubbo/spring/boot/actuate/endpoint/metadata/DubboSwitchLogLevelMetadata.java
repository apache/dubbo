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

import org.apache.dubbo.common.logger.Level;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Dubbo Switch Log Level
 *
 * @since 3.3.0
 */
@Component
public class DubboSwitchLogLevelMetadata {

    public Map<String, Object> switchLogLevel(String level) {
        Map<String, Object> logLevelInfo = new LinkedHashMap<>();
        Level newLevel;
        switch (level) {
            case "0":
                newLevel = Level.ALL;
                break;
            case "1":
                newLevel = Level.TRACE;
                break;
            case "2":
                newLevel = Level.DEBUG;
                break;
            case "3":
                newLevel = Level.INFO;
                break;
            case "4":
                newLevel = Level.WARN;
                break;
            case "5":
                newLevel = Level.ERROR;
                break;
            case "6":
                newLevel = Level.OFF;
                break;
            default:
                newLevel = Level.valueOf(level.toUpperCase(Locale.ROOT));
                break;
        }
        LoggerFactory.setLevel(newLevel);
        logLevelInfo.put("Current Log Level", newLevel);
        return logLevelInfo;
    }
}
