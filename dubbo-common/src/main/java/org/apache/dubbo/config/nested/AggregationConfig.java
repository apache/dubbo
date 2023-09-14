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
package org.apache.dubbo.config.nested;

import java.io.Serializable;

public class AggregationConfig implements Serializable {

    /**
     * Enable local aggregation or not
     */
    private Boolean enabled;

    private Boolean enableQps;

    private Boolean enableRtPxx;

    private Boolean enableRt;

    private Boolean enableRequest;

    /**
     * Bucket num for time window quantile
     */
    private Integer bucketNum;

    /**
     * Time window seconds for time window quantile
     */
    private Integer timeWindowSeconds;

    /**
     * Time window mill seconds for qps
     */
    private Integer qpsTimeWindowMillSeconds;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getBucketNum() {
        return bucketNum;
    }

    public void setBucketNum(Integer bucketNum) {
        this.bucketNum = bucketNum;
    }

    public Integer getTimeWindowSeconds() {
        return timeWindowSeconds;
    }

    public void setTimeWindowSeconds(Integer timeWindowSeconds) {
        this.timeWindowSeconds = timeWindowSeconds;
    }

    public Boolean getEnableQps() {
        return enableQps;
    }

    public void setEnableQps(Boolean enableQps) {
        this.enableQps = enableQps;
    }

    public Boolean getEnableRtPxx() {
        return enableRtPxx;
    }

    public void setEnableRtPxx(Boolean enableRtPxx) {
        this.enableRtPxx = enableRtPxx;
    }

    public Boolean getEnableRt() {
        return enableRt;
    }

    public void setEnableRt(Boolean enableRt) {
        this.enableRt = enableRt;
    }

    public Boolean getEnableRequest() {
        return enableRequest;
    }

    public void setEnableRequest(Boolean enableRequest) {
        this.enableRequest = enableRequest;
    }

    public Integer getQpsTimeWindowMillSeconds() {
        return qpsTimeWindowMillSeconds;
    }

    public void setQpsTimeWindowMillSeconds(Integer qpsTimeWindowMillSeconds) {
        this.qpsTimeWindowMillSeconds = qpsTimeWindowMillSeconds;
    }
}
