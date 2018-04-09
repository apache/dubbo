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
package com.alibaba.dubbo.flowcontrol;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.StringUtils;

import java.util.Map;
public class CircuitBreakerUtils {

    private CircuitBreakerUtils(){}

    public static void mergeUrl(URL consumerUrl, Map<String, String> queryMap) {
        Map<String, String> consumerMap = consumerUrl.getParameters();
        if (consumerMap != null && consumerMap.size() > 0) {
            String circuitBreakerSwitch = consumerMap.get(Constants.CIRCUIT_BREAKER_SWITCH);

            if (StringUtils.isNotEmpty(circuitBreakerSwitch)) {
                queryMap.put(Constants.CIRCUIT_BREAKER_SWITCH, circuitBreakerSwitch);
            }
            String requestVolumeThreshold = consumerMap.get(Constants.CIRCUIT_BREAKER_REQUEST_VOLUME_THRESHOLD);
            if (StringUtils.isNotEmpty(requestVolumeThreshold) ) {
                queryMap.put(Constants.CIRCUIT_BREAKER_REQUEST_VOLUME_THRESHOLD, requestVolumeThreshold);
            }
            String sleepWindowinMilliseconds = consumerMap.get(Constants.CIRCUIT_BREAKER_SLEEP_WINDOWIN_MILLISECONDS);
            if (StringUtils.isNotEmpty(sleepWindowinMilliseconds)) {
                queryMap.put(Constants.CIRCUIT_BREAKER_SLEEP_WINDOWIN_MILLISECONDS, sleepWindowinMilliseconds);
            }
            String errorThresholdRercentage = consumerMap.get(Constants.CIRCUIT_BREAKER_ERROR_THRESHOLD_PERCENTAGE);
            if (StringUtils.isNotEmpty(errorThresholdRercentage)) {
                queryMap.put(Constants.CIRCUIT_BREAKER_ERROR_THRESHOLD_PERCENTAGE, errorThresholdRercentage);
            }
            String openWeightKey = consumerMap.get(Constants.CRICUIT_BREAKER_OPEN_WEIGHT_KEY);
            if (StringUtils.isNotEmpty(openWeightKey) ) {
                queryMap.put(Constants.CRICUIT_BREAKER_OPEN_WEIGHT_KEY, openWeightKey);
            }
            String closeWeightKey = consumerMap.get(Constants.CRICUIT_BREAKER_CLOSE_WEIGHT_KEY);
            if (StringUtils.isNotEmpty(closeWeightKey)) {
                queryMap.put(Constants.CRICUIT_BREAKER_CLOSE_WEIGHT_KEY, closeWeightKey);
            }
        }
        return ;
    }


}
