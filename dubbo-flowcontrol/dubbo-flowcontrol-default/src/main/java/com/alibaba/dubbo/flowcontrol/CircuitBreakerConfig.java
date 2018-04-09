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

public class CircuitBreakerConfig {
    /**
     * half-open状态下成功次数阈值
     */
    private int consecutiveSuccThreshold = 5;

    private volatile int circuitBreakerRequestVolumeThreshold = Constants.CIRCUIT_BREAKER_REQUEST_VOLUME_THRESHOLD_DEFAULT;
    private volatile int circuitBreakerSleepWindowInMilliseconds =Constants.CIRCUIT_BREAKER_SLEEP_WINDOWIN_MILLISECONDS_DEFAULT;
    private volatile int circuitBreakerErrorThresholdPercentage=Constants.CIRCUIT_BREAKER_ERROR_THRESHOLD_PERCENTAGE_DEFAULT;

    private CircuitBreakerConfig(){

    }

    public static CircuitBreakerConfig newDefault(){
        CircuitBreakerConfig config = new CircuitBreakerConfig();
        return config;
    }

    public int getCircuitBreakerRequestVolumeThreshold() {
        return circuitBreakerRequestVolumeThreshold;
    }

    public void setCircuitBreakerRequestVolumeThreshold(int circuitBreakerRequestVolumeThreshold) {
        this.circuitBreakerRequestVolumeThreshold = circuitBreakerRequestVolumeThreshold;
    }

    public int getCircuitBreakerSleepWindowInMilliseconds() {
        return circuitBreakerSleepWindowInMilliseconds;
    }

    public void setCircuitBreakerSleepWindowInMilliseconds(int circuitBreakerSleepWindowInMilliseconds) {
        this.circuitBreakerSleepWindowInMilliseconds = circuitBreakerSleepWindowInMilliseconds;
    }

    public int getCircuitBreakerErrorThresholdPercentage() {
        return circuitBreakerErrorThresholdPercentage;
    }

    public void setCircuitBreakerErrorThresholdPercentage(int circuitBreakerErrorThresholdPercentage) {
        this.circuitBreakerErrorThresholdPercentage = circuitBreakerErrorThresholdPercentage;
    }


    public int getConsecutiveSuccThreshold() {
        return consecutiveSuccThreshold;
    }

    public void setConsecutiveSuccThreshold(int consecutiveSuccThreshold) {
        this.consecutiveSuccThreshold = consecutiveSuccThreshold;
    }

    public void parmEqual(int a1,int a2,int a3) {
        if(a1!=circuitBreakerRequestVolumeThreshold){
            circuitBreakerRequestVolumeThreshold=a1;
        }
        if(a2!=circuitBreakerSleepWindowInMilliseconds){
            circuitBreakerSleepWindowInMilliseconds=a2;
        }
        if(a3!=circuitBreakerErrorThresholdPercentage){
            circuitBreakerErrorThresholdPercentage=a3;
        }
    }

    @Override
    public String toString() {
        return "CircuitBreakerConfig{" +
                "consecutiveSuccThreshold=" + consecutiveSuccThreshold +
                ", circuitBreakerRequestVolumeThreshold=" + circuitBreakerRequestVolumeThreshold +
                ", circuitBreakerSleepWindowInMilliseconds=" + circuitBreakerSleepWindowInMilliseconds +
                ", circuitBreakerErrorThresholdPercentage=" + circuitBreakerErrorThresholdPercentage +
                '}';
    }
}
