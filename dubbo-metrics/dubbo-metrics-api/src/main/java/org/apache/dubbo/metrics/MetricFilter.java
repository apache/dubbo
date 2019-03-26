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
package org.apache.dubbo.metrics;

/**
 * A filter used to determine whether or not a metric should be reported, among other things.
 */
public interface MetricFilter {

    /**
     * Matches all metrics, regardless of type or name.
     */
    MetricFilter ALL = new MetricFilter() {
        @Override
        public boolean matches(MetricName name, Metric metric) {
            return true;
        }
    };

    /**
     * Returns {@code true} if the metric matches the filter; {@code false} otherwise.
     *
     * @param name      the metric's name
     * @param metric    the metric
     * @return {@code true} if the metric matches the filter
     */
    boolean matches(MetricName name, Metric metric);
}
