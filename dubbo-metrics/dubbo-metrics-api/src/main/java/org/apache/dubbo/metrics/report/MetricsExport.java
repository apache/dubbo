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

package org.apache.dubbo.metrics.report;

import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.sample.MetricSample;

import java.util.List;

/**
 * Metrics data export.
 * Export data in a unified format for external collection(e.g. Prometheus).
 */
public interface MetricsExport {

    /**
     * export all.
     */
    List<MetricSample> export(MetricsCategory category);

    /**
     * Check if samples have been changed.
     * Note that this method will reset the changed flag to false using CAS.
     *
     * @return true if samples have been changed
     */
    boolean calSamplesChanged();
}
