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

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IMetricManager {

    /**
     * Create a {@link Counter} metric in given group, and name.
     * if not exist, an instance will be created.
     *
     * @param group the group of MetricRegistry
     * @param name the name of the metric
     * @return an instance of counter
     */
    Counter getCounter(String group, MetricName name);

    /**
     * Create a {@link BucketCounter} metric in given group, and name.
     * if not exist, an instance will be created.
     *
     * @param group the group of MetricRegistry
     * @param name the name of the metric
     * @return an instance of {@link BucketCounter}
     */
    BucketCounter getBucketCounter(String group, MetricName name);

    /**
     * Create a {@link Compass} metric in give group, name, and type
     * if not exist, an instance will be created.
     * @param group the group of MetricRegistry
     * @param name the name of the metric
     * @return an instance of {@link Compass}
     */
    Compass getCompass(String group, MetricName name);

    /**
     * Register a customized metric to specified group.
     * @param group: the group name of MetricRegistry
     * @param metric the metric to register
     */
    void register(String group, MetricName name, Metric metric);

    /**
     * Get a list of group in current MetricManager
     * @return a list of group name
     */
    List<String> listMetricGroups();

    /**
     * list all metric names by group
     * @return a map of metric name set, keyed by group name
     */
    Map<String, Set<MetricName>> listMetricNamesByGroup();

    /**
     * Get metric registry by group name,
     * if not found, null will be returned
     * @param group the group name to query
     * @return the MetricRegistry that is correspondent to the group
     */
    MetricRegistry getMetricRegistryByGroup(String group);

    /**
     * Get all the counters by the specific group and filter
     * @param group the given group
     * @param filter the given filter
     * @return the MetricName to Counter map
     */
    Map<MetricName, Counter> getCounters(String group, MetricFilter filter);

    /**
     * Get all the compasses by the specific group and filter
     * @param group the given group
     * @param filter the given filter
     * @return the MetricName to Compass map
     */
    Map<MetricName, Compass> getCompasses(String group, MetricFilter filter);

    /**
     * A map of metric names to metrics.
     *
     * @return all the metrics
     */
    Map<MetricName, Metric> getMetrics(String group);

}
