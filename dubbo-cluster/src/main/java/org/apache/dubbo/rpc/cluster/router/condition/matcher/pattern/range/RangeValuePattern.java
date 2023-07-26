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
package org.apache.dubbo.rpc.cluster.router.condition.matcher.pattern.range;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.cluster.router.condition.matcher.pattern.ValuePattern;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CLUSTER_FAILED_EXEC_CONDITION_ROUTER;

/**
 * Matches with patterns like 'key=1~100', 'key=~100' or 'key=1~'
 */
@Activate(order = 100)
public class RangeValuePattern implements ValuePattern {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RangeValuePattern.class);

    @Override
    public boolean shouldMatch(String pattern) {
        return pattern.contains("~");
    }

    @Override
    public boolean match(String pattern, String value, URL url, Invocation invocation, boolean isWhenCondition) {
        boolean defaultValue = !isWhenCondition;
        try {
            int intValue = StringUtils.parseInteger(value);

            String[] arr = pattern.split("~");
            if (arr.length < 2) {
                logger.error(CLUSTER_FAILED_EXEC_CONDITION_ROUTER, "", "", "Invalid condition rule " + pattern + " or value " + value + ", will ignore.");
                return defaultValue;
            }

            String rawStart = arr[0];
            String rawEnd = arr[1];

            if (StringUtils.isEmpty(rawStart) && StringUtils.isEmpty(rawEnd)) {
                return defaultValue;
            }

            if (StringUtils.isEmpty(rawStart)) {
                int end = StringUtils.parseInteger(rawEnd);
                if (intValue > end) {
                    return false;
                }
            } else if (StringUtils.isEmpty(rawEnd)) {
                int start = StringUtils.parseInteger(rawStart);
                if (intValue < start) {
                    return false;
                }
            } else {
                int start = StringUtils.parseInteger(rawStart);
                int end = StringUtils.parseInteger(rawEnd);
                if (intValue < start || intValue > end) {
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error(CLUSTER_FAILED_EXEC_CONDITION_ROUTER, "Parse integer error", "", "Invalid condition rule " + pattern + " or value " + value + ", will ignore.", e);
            return defaultValue;
        }

        return true;
    }
}
