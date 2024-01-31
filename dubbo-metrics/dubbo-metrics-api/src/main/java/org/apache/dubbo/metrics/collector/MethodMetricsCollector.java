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
package org.apache.dubbo.metrics.collector;

import org.apache.dubbo.metrics.event.TimeCounterEvent;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.rpc.Invocation;

/**
 * Method-level metrics collection for rpc invocation scenarios
 */
public interface MethodMetricsCollector<E extends TimeCounterEvent> extends MetricsCollector<E> {

    void increment(MethodMetric methodMetric, MetricsKeyWrapper wrapper, int size);

    void addMethodRt(Invocation invocation, String registryOpType, Long responseTime);

    void init(Invocation invocation, MetricsKeyWrapper wrapper);
}
