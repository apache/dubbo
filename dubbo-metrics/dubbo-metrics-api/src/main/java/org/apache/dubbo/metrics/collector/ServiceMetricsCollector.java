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
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.rpc.Invocation;

/**
 * Service-level collector.
 * registration center, configuration center and other scenarios
 */
public interface ServiceMetricsCollector<E extends TimeCounterEvent> extends MetricsCollector<E> {

    void increment(String serviceKey, MetricsKeyWrapper wrapper, int size);

    void setNum(MetricsKeyWrapper metricsKey, String serviceKey, int num);

    void addServiceRt(String serviceKey, String registryOpType, Long responseTime);

    void addServiceRt(Invocation invocation, String registryOpType, Long responseTime);
}
