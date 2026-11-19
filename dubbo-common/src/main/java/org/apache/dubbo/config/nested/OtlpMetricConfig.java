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
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OtlpMetricConfig implements Serializable {

    /**
     * URI of the OLTP server.
     */
    private String endpoint;

    /**
     * Monitored resource's attributes.
     */
    private Map<String, String> resourceAttributes;

    /**
     * Headers for the exported metrics.
     */
    private Map<String, String> headers;

    /**
     * Time unit for exported metrics.
     */
    private TimeUnit baseTimeUnit = TimeUnit.MILLISECONDS;

    /**
     * Intervals for pushing metrics
     */
    private Duration step;

    public String getEndpoint() {
        return this.endpoint;
    }

    public void setUrl(String endpoint) {
        this.endpoint = endpoint;
    }

    public Map<String, String> getResourceAttributes() {
        return this.resourceAttributes;
    }

    public void setResourceAttributes(Map<String, String> resourceAttributes) {
        this.resourceAttributes = resourceAttributes;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public TimeUnit getBaseTimeUnit() {
        return this.baseTimeUnit;
    }

    public void setBaseTimeUnit(TimeUnit baseTimeUnit) {
        this.baseTimeUnit = baseTimeUnit;
    }

    public Duration getStep() {
        return this.step;
    }

    public void setStep(Duration step) {
        this.step = step;
    }
}
