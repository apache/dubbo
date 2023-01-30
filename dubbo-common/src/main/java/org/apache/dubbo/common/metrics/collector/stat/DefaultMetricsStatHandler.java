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
package org.apache.dubbo.common.metrics.collector.stat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import org.apache.dubbo.common.metrics.model.MethodMetric;


public class DefaultMetricsStatHandler implements MetricsStatHandler {

    private final Map<MethodMetric, AtomicLong> counts = new ConcurrentHashMap<>();

    public DefaultMetricsStatHandler() {
    }

    @Override
    public void increase(String applicationName, String interfaceName, String methodName, String group, String version) {
        this.doIncrExecute(applicationName, interfaceName,methodName,group,version);
    }

    public void decrease(String applicationName, String interfaceName, String methodName, String group, String version){
        this.doDecrExecute(applicationName, interfaceName,methodName,group,version);
    }

    protected void doExecute(String applicationName, String interfaceName, String methodName, String version,
                             BiConsumer<MethodMetric,Map<MethodMetric, AtomicLong>> execute, String group){
        MethodMetric metric = new MethodMetric(applicationName, interfaceName, methodName, group, version);
        execute.accept(metric,counts);

        this.doNotify(metric);
    }

    protected void doIncrExecute(String applicationName, String interfaceName, String methodName, String group, String version){
        this.doExecute(applicationName, interfaceName,methodName, version, (metric, counts)->{
            AtomicLong count = counts.computeIfAbsent(metric, k -> new AtomicLong(0L));
            count.incrementAndGet();
        }, group);
    }

    protected void doDecrExecute(String applicationName, String interfaceName, String methodName, String group, String version){
        this.doExecute(applicationName, interfaceName,methodName, version, (metric, counts)->{
            AtomicLong count = counts.computeIfAbsent(metric, k -> new AtomicLong(0L));
            count.decrementAndGet();
        }, group);
    }

    @Override
    public Map<MethodMetric, AtomicLong> get() {
        return counts;
    }

    public void doNotify(MethodMetric metric){}
}
