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

package org.apache.dubbo.config;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.support.Nested;
import org.apache.dubbo.config.support.Parameter;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_PROMETHEUS;

/**
 * MetricsConfig
 */
public class MetricsConfig extends AbstractConfig {

    private static final long serialVersionUID = -9089919311611546383L;

    private String protocol;

    /**
     * Metrics collect mode, pull or push
     */
    private String mode;

    /**
     * Metrics collect center address
     */
    private String address;

    /**
     * When using pull method, which port to expose
     */
    private Integer metricsPort;

    /**
     * When using pull mode, which path to expose metrics
     */
    private String metricsPath;

    /**
     * When using push mode, the interval of push behavior
     */
    private Integer pushInterval;

    /**
     * The metrics aggregation config
     */
    @Nested
    private Aggregation aggregation;

    public MetricsConfig() {
        super();
    }

    public URL toUrl() {
        Map<String, String> map = new HashMap<>();
        appendParameters(map, this);
        if (StringUtils.isEmpty(address)) {
            address = ANYHOST_VALUE;
        }
        // use 'prometheus' as the default metrics service.
        if (StringUtils.isEmpty(map.get(PROTOCOL_KEY))) {
            map.put(PROTOCOL_KEY, PROTOCOL_PROMETHEUS);
        }
        return UrlUtils.parseURL(address, map);
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    @Parameter(excluded = true)
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getMetricsPort() {
        return metricsPort;
    }

    public void setMetricsPort(Integer metricsPort) {
        this.metricsPort = metricsPort;
    }

    public String getMetricsPath() {
        return metricsPath;
    }

    public void setMetricsPath(String metricsPath) {
        this.metricsPath = metricsPath;
    }

    public Integer getPushInterval() {
        return pushInterval;
    }

    public void setPushInterval(Integer pushInterval) {
        this.pushInterval = pushInterval;
    }

    public Aggregation getAggregation() {
        return aggregation;
    }

    public void setAggregation(Aggregation aggregation) {
        this.aggregation = aggregation;
    }

    public static class Aggregation {

        /**
         * Enable local aggregation or not
         */
        private Boolean enable;

        /**
         * Bucket num for time window quantile
         */
        private Integer bucketNum;

        /**
         * Time window seconds for time window quantile
         */
        private Integer timeWindowSeconds;

        public Boolean getEnable() {
            return enable;
        }

        public void setEnable(Boolean enable) {
            this.enable = enable;
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
    }
}

