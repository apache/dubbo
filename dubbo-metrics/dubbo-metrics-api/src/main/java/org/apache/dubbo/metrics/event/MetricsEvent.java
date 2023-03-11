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

package org.apache.dubbo.metrics.event;

import org.apache.dubbo.metrics.model.MethodMetric;

/**
 * BaseMetricsEvent.
 */
public abstract class MetricsEvent {

    /**
     * Metric object. (eg. {@link MethodMetric})
     */
    protected transient Object source;

    public MetricsEvent(Object source) {
        if (source == null) {
            throw new IllegalArgumentException("null source");
        }

        this.source = source;
    }

    public Object getSource() {
        return source;
    }

    public String toString() {
        return getClass().getName() + "[source=" + source + "]";
    }

    public enum Type {
        TOTAL("TOTAL_%s"),
        SUCCEED("SUCCEED_%s"),
        BUSINESS_FAILED("BUSINESS_FAILED_%s"),
        REQUEST_TIMEOUT("REQUEST_TIMEOUT_%s"),
        REQUEST_LIMIT("REQUEST_LIMIT_%s"),
        PROCESSING("PROCESSING_%s"),
        UNKNOWN_FAILED("UNKNOWN_FAILED_%s"),
        TOTAL_FAILED("TOTAL_FAILED_%s"),
        APPLICATION_INFO("APPLICATION_INFO_%s"),
        NETWORK_EXCEPTION("NETWORK_EXCEPTION_%s"),
        SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE_%s"),
        CODEC_EXCEPTION("CODEC_EXCEPTION_%s"),;

        private String name;

        public final String getName() {
            return this.name;
        }

        public final String getNameByType(String type) {
            return String.format(name, type);
        }


        Type(String name) {
            this.name = name;
        }
    }
}
