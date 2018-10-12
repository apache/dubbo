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


import java.util.Map;
import java.util.Set;

/**
 * A registry of metric instances.
 */
public abstract class MetricRegistry implements MetricSet {

    /**
     * Given a {@link Metric}, registers it under the given name.
     *
     * @param name   the name of the metric
     * @param metric the metric
     * @param <T>    the type of the metric
     * @return {@code metric}
     * @throws IllegalArgumentException if the name is already registered
     */
    public abstract <T extends Metric> T register(String name, T metric) throws IllegalArgumentException;

    /**
     * Given a {@link Metric}, registers it under the given name.
     *
     * @param name   the name of the metric
     * @param metric the metric
     * @param <T>    the type of the metric
     * @return {@code metric}
     * @throws IllegalArgumentException if the name is already registered
     */
    public abstract <T extends Metric> T register(MetricName name, T metric) throws IllegalArgumentException;

    /**
     * Given a metric set, registers them.
     *
     * @param metrics    a set of metrics
     * @throws IllegalArgumentException if any of the names are already registered
     */
    public abstract void registerAll(MetricSet metrics) throws IllegalArgumentException;

    /**
     * Creates a new {@link Counter} and registers it under the given name.
     *
     * @param name the name of the metric
     * @return a new {@link Counter}
     */
    public abstract Counter counter(String name);

    /**
     * Return the {@link Counter} registered under this name; or create and register
     * a new {@link Counter} if none is registered.
     *
     * @param name the name of the metric
     * @return a new or pre-existing {@link Counter}
     */
    public abstract Counter counter(MetricName name);

    /**
     * Create a FastCompass with given name
     * @param name the name of the metric
     * @return a FastCompass instance
     */
    public abstract Compass compass(MetricName name);

    /**
     * Removes the metric with the given name.
     *
     * @param name the name of the metric
     * @return whether or not the metric was removed
     */
    public abstract boolean remove(MetricName name);

    /**
     * Removes all metrics which match the given filter.
     *
     * @param filter a filter
     */
    public abstract void removeMatching(MetricFilter filter);

    /**
     * Returns a set of the names of all the metrics in the registry.
     *
     * @return the names of all the metrics
     */
    public abstract Set<MetricName> getNames();

    /**
     * Returns a map of all the gauges in the registry and their names.
     *
     * @return all the gauges in the registry
     */
    public abstract Map<MetricName, Gauge> getGauges();

    /**
     * Returns a map of all the gauges in the registry and their names which match the given filter.
     *
     * @param filter    the metric filter to match
     * @return all the gauges in the registry
     */
    public abstract Map<MetricName, Gauge> getGauges(MetricFilter filter);

    /**
     * Returns a map of all the counters in the registry and their names.
     *
     * @return all the counters in the registry
     */
    public abstract Map<MetricName, Counter> getCounters();

    /**
     * Returns a map of all the counters in the registry and their names which match the given
     * filter.
     *
     * @param filter    the metric filter to match
     * @return all the counters in the registry
     */
    public abstract Map<MetricName, Counter> getCounters(MetricFilter filter);

    /**
     * Returns a map of all the compasses in the registry and their names.
     *
     * @return all the compasses in the registry
     */
    public abstract Map<MetricName, Compass> getCompasses();

    /**
     * Returns a map of all the compasses in the registry and their names which match the given filter.
     *
     * @param filter    the metric filter to match
     * @return all the compasses in the registry
     */
    public abstract Map<MetricName, Compass> getCompasses(MetricFilter filter);

    /**
     * Returns a map of all the metrics in the registry and their names which match the given filter
     * @param filter the metric filter to match
     * @return all the metrics in the registry
     */
    public abstract Map<MetricName, Metric> getMetrics(MetricFilter filter);


}
