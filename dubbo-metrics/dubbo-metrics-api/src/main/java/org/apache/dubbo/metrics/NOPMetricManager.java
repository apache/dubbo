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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The empty implementation for IMetricManager
 */
public class NOPMetricManager implements IMetricManager {

    private static final Map emptyMap = new HashMap();
    private static final Set emptySet = new HashSet();

    @Override
    public Counter getCounter(String group, MetricName name) {
        return NOP_COUNTER;
    }

    @Override
    public BucketCounter getBucketCounter(String group, MetricName name) {
        return NOP_BUCKET_COUNTER;
    }

    @Override
    public Compass getCompass(String group, MetricName name) {
        return NOP_COMPASS;
    }

    @Override
    public List<String> listMetricGroups() {
        return Collections.emptyList();
    }

    @Override
    public Map<String, Set<MetricName>> listMetricNamesByGroup() {
        return Collections.emptyMap();
    }

    @Override
    public MetricRegistry getMetricRegistryByGroup(String group) {
        return NOP_REGISTRY;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<MetricName, Counter> getCounters(String group, MetricFilter filter) {
        return emptyMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<MetricName, Compass> getCompasses(String group, MetricFilter filter) {
        return emptyMap;
    }

    @Override
    public void register(String group, MetricName name, Metric metric) {

    }

    static final BucketCounter NOP_BUCKET_COUNTER = new BucketCounter() {
        @Override
        public void update() {

        }

        @Override
        public void update(long n) {

        }

        @Override
        public Map<Long, Long> getBucketCounts() {
            return emptyMap;
        }

        @Override
        public Map<Long, Long> getBucketCounts(long startTime) {
            return emptyMap;
        }

        @Override
        public int getBucketInterval() {
            return 0;
        }

        @Override
        public long lastUpdateTime() {
            return 0;
        }
    };

    static final Counter NOP_COUNTER = new Counter() {
        @Override
        public void inc() {
        }

        @Override
        public void inc(long n) {
        }

        @Override
        public void dec() {
        }

        @Override
        public void dec(long n) {
        }

        @Override
        public long getCount() {
            return 0;
        }

        @Override
        public long lastUpdateTime() {
            return 0;
        }
    };

    static final Compass NOP_COMPASS = new Compass() {
        @Override
        public void record(long duration, String subCategory) {

        }

        @Override
        public Map<String, Map<Long, Long>> getMethodCountPerCategory() {
            return emptyMap;
        }

        @Override
        public Map<String, Map<Long, Long>> getMethodRtPerCategory() {
            return emptyMap;
        }

        @Override
        public Map<String, Map<Long, Long>> getMethodCountPerCategory(long startTime) {
            return emptyMap;
        }

        @Override
        public Map<String, Map<Long, Long>> getMethodRtPerCategory(long startTime) {
            return emptyMap;
        }

        @Override
        public int getBucketInterval() {
            return 0;
        }

        @Override
        public Map<String, Map<Long, Long>> getCountAndRtPerCategory() {
            return emptyMap;
        }

        @Override
        public Map<String, Map<Long, Long>> getCountAndRtPerCategory(long startTime) {
            return emptyMap;
        }

        @Override
        public long lastUpdateTime() {
            return 0;
        }
    };

    private static final MetricRegistry NOP_REGISTRY = new MetricRegistry() {
        @Override
        public <T extends Metric> T register(String name, T metric) throws IllegalArgumentException {
            return metric;
        }

        @Override
        public <T extends Metric> T register(MetricName name, T metric) throws IllegalArgumentException {
            return metric;
        }

        @Override
        public void registerAll(MetricSet metrics) throws IllegalArgumentException {

        }

        @Override
        public Counter counter(String name) {
            return NOP_COUNTER;
        }

        @Override
        public Counter counter(MetricName name) {
            return NOP_COUNTER;
        }

        @Override
        public Compass compass(MetricName name) {
            return NOP_COMPASS;
        }

        @Override
        public boolean remove(MetricName name) {
            return false;
        }

        @Override
        public void removeMatching(MetricFilter filter) {

        }

        @Override
        @SuppressWarnings("unchecked")
        public Set<MetricName> getNames() {
            return emptySet;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Map<MetricName, Gauge> getGauges() {
            return emptyMap;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Map<MetricName, Gauge> getGauges(MetricFilter filter) {
            return emptyMap;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Map<MetricName, Counter> getCounters() {
            return emptyMap;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Map<MetricName, Counter> getCounters(MetricFilter filter) {
            return emptyMap;
        }

        @Override
        public Map<MetricName, Compass> getCompasses() {
            return emptyMap;
        }

        @Override
        public Map<MetricName, Compass> getCompasses(MetricFilter filter) {
            return emptyMap;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Map<MetricName, Metric> getMetrics(MetricFilter filter) {
            return emptyMap;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Map<MetricName, Metric> getMetrics() {
            return emptyMap;
        }

        @Override
        public long lastUpdateTime() {
            return 0;
        }
    };

    @Override
    public Map<MetricName, Metric> getMetrics(String group) {
        return emptyMap;
    }

}
