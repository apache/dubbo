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

package org.apache.dubbo.rpc.cluster.router.circuitBreaker.model;


import static org.apache.dubbo.common.constants.CommonConstants.ANY_VALUE;

public class CircuitBreakerValueMatch {
//    private String target;

    private String timeThreshold;

    private String errorThresholdPercentage;

    private String sleepWindowThreshold;

//    public String getTarget() {
//        return target;
//    }
//
//    public void setTarget(String target) {
//        this.target = target;
//    }

    public String getTimeThreshold() {
        return timeThreshold;
    }

    public void setTimeThreshold(String timeThreshold) {
        this.timeThreshold = timeThreshold;
    }

    public String getErrorThresholdPercentage() {
        return errorThresholdPercentage;
    }

    public void setErrorThresholdPercentage(String errorThresholdPercentage) {
        this.errorThresholdPercentage = errorThresholdPercentage;
    }

    public String getSleepWindowThreshold() {
        return sleepWindowThreshold;
    }

    public void setSleepWindowThreshold(String sleepWindowThreshold) {
        this.sleepWindowThreshold = sleepWindowThreshold;
    }

    public boolean isMatch(String input) {
         if (getTimeThreshold() != null && input != null) {
            return input.startsWith(getTimeThreshold());
        } else if (getErrorThresholdPercentage() != null && input != null) {
            return input.matches(getErrorThresholdPercentage());
        } else if (getSleepWindowThreshold() != null && input != null) {
            // only supports "*"
            return input.equals(getSleepWindowThreshold()) || ANY_VALUE.equals(getSleepWindowThreshold());
        } else {
            return false;
        }
    }

}
