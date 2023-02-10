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

/**
 * BaseMetricsEvent.
 */
public abstract class MetricsEvent<S> {

    /**
     * Metric context. (eg. {@link org.apache.dubbo.rpc.model.ApplicationModel})
     */
    protected transient S source;

    public MetricsEvent(S source) {
        if (source == null) {
            throw new IllegalArgumentException("null source");
        }

        this.source = source;
    }

    public S getSource() {
        return source;
    }

    public String toString() {
        return getClass().getName() + "[source=" + source + "]";
    }

    public enum Type {
        TOTAL,
        SUCCEED,
        BUSINESS_FAILED,
        REQUEST_TIMEOUT,
        REQUEST_LIMIT,
        PROCESSING,
        UNKNOWN_FAILED,
        TOTAL_FAILED,
        APPLICATION_INFO
    }
}
