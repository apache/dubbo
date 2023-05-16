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

public class HistogramConfig implements Serializable {

    private Boolean enabled;

    private Integer[] bucketsMs;

    private Integer minExpectedMs;

    private Integer maxExpectedMs;

    private Boolean enabledPercentiles;

    private double[] percentiles;

    private Integer distributionStatisticExpiryMin;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer[] getBucketsMs() {
        return bucketsMs;
    }

    public void setBucketsMs(Integer[] bucketsMs) {
        this.bucketsMs = bucketsMs;
    }

    public Integer getMinExpectedMs() {
        return minExpectedMs;
    }

    public void setMinExpectedMs(Integer minExpectedMs) {
        this.minExpectedMs = minExpectedMs;
    }

    public Integer getMaxExpectedMs() {
        return maxExpectedMs;
    }

    public void setMaxExpectedMs(Integer maxExpectedMs) {
        this.maxExpectedMs = maxExpectedMs;
    }

    public Boolean getEnabledPercentiles() {
        return enabledPercentiles;
    }

    public void setEnabledPercentiles(Boolean enabledPercentiles) {
        this.enabledPercentiles = enabledPercentiles;
    }

    public double[] getPercentiles() {
        return percentiles;
    }

    public void setPercentiles(double[] percentiles) {
        this.percentiles = percentiles;
    }

    public Integer getDistributionStatisticExpiryMin() {
        return distributionStatisticExpiryMin;
    }

    public void setDistributionStatisticExpiryMin(Integer distributionStatisticExpiryMin) {
        this.distributionStatisticExpiryMin = distributionStatisticExpiryMin;
    }
}
