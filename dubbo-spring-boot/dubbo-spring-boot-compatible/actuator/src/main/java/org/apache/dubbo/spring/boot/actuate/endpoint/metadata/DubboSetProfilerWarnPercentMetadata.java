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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.profiler.ProfilerSwitch;

import java.util.Collections;
import java.util.Map;

import org.springframework.stereotype.Component;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.QOS_PROFILER_WARN_PERCENT;

/**
 * Dubbo Set Profiler Warn Percent
 *
 * @since 3.3.0
 */
@Component
public class DubboSetProfilerWarnPercentMetadata {

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(DubboSetProfilerWarnPercentMetadata.class);

    public Map<String, Object> SetProfilerWarnPercent(String percent) {
        if (percent == null || percent.isEmpty()) {
            return Collections.singletonMap("Error", "args error. example: setProfilerWarnPercent 0.75");
        }
        ProfilerSwitch.setWarnPercent(Double.parseDouble(percent));
        logger.warn(
                QOS_PROFILER_WARN_PERCENT, "", "", "Dubbo Invocation Profiler warn percent has been set to " + percent);
        return Collections.singletonMap(
                "Current Dubbo Invocation Profiler warn percent", ProfilerSwitch.getWarnPercent());
    }
}
