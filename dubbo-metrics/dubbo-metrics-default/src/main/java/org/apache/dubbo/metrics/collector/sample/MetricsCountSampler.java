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

package org.apache.dubbo.metrics.collector.sample;

import org.apache.dubbo.metrics.model.Metric;

import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;

public interface MetricsCountSampler<S, K,M extends Metric> {

    void inc(S source, K metricName);

    void dec(S source, K metricName);

    void incOnEvent(S source, K metricName);

    void decOnEvent(S source, K metricName);

    void addRT(S source, Long rt);

    Optional<ConcurrentMap<M, AtomicLong>> getCount(K metricName);

    ConcurrentMap<M, AtomicLong> getLastRT();

    ConcurrentMap<M, LongAccumulator> getMinRT();

    ConcurrentMap<M, LongAccumulator> getMaxRT();

    ConcurrentMap<M, AtomicLong> getAvgRT();

    ConcurrentMap<M, AtomicLong> getTotalRT();

    ConcurrentMap<M, AtomicLong> getRtCount();

}
