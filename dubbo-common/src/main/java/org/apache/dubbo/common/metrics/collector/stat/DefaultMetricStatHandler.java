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


public class DefaultMetricStatHandler implements MetricsStatHandler {

    private final String applicationName;
    private final Map<MethodMetric, AtomicLong> counts = new ConcurrentHashMap<>();

    public DefaultMetricStatHandler(String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public void increase(String interfaceName, String methodName, String group, String version) {
        this.doIncrExecute(interfaceName,methodName,group,version);
    }

    public void decrease(String interfaceName, String methodName, String group, String version){
        this.doDecrExecute(interfaceName,methodName,group,version);
    }

    protected void doExecute(String interfaceName, String methodName, String group, String version, BiConsumer<MethodMetric,Map<MethodMetric, AtomicLong>> execute){
        MethodMetric metric = new MethodMetric(applicationName, interfaceName, methodName, group, version);
        execute.accept(metric,counts);

        this.doNotify(metric);
    }

    protected void doIncrExecute(String interfaceName, String methodName, String group, String version){
        this.doExecute(interfaceName,methodName,group,version,(metric,counts)->{
            AtomicLong count = counts.computeIfAbsent(metric, k -> new AtomicLong(0L));
            count.incrementAndGet();

        });
    }

    protected void doDecrExecute(String interfaceName, String methodName, String group, String version){
        this.doExecute(interfaceName,methodName,group,version,(metric,counts)->{
            AtomicLong count = counts.computeIfAbsent(metric, k -> new AtomicLong(0L));
            count.decrementAndGet();
        });
    }

    @Override
    public Map<MethodMetric, AtomicLong> get() {
        return counts;
    }

    public  void doNotify(MethodMetric metric){}

}
