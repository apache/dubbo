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

/**
 * Configuration for the metric aggregation.
 */
public class AggregationConfig implements Serializable {

    private static final long serialVersionUID = 4878693820314125085L;

    /**
     * Enable aggregation or not.
     */
    private Boolean enabled;

    /**
     * Enable QPS (Queries Per Second) aggregation or not.
     */
    private Boolean enableQps;

    /**
     * Enable Response Time Percentile (Pxx) aggregation or not.
     */
    private Boolean enableRtPxx;

    /**
     * Enable Response Time aggregation or not.
     */
    private Boolean enableRt;

    /**
     * Enable Request aggregation or not.
     */
    private Boolean enableRequest;

    /**
     * The number of buckets for time window quantile.
     */
    private Integer bucketNum;

    /**
     * The time window in seconds for time window quantile.
     */
    private Integer timeWindowSeconds;

    /**
     * The time window in milliseconds for QPS (Queries Per Second) aggregation.
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
