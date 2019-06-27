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
package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.rpc.cluster.loadbalance.statistics.CpuUsageService;
import org.apache.dubbo.rpc.cluster.loadbalance.statistics.CpuUsageServiceImpl;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsLoadBalance extends AbstractLoadBalance {

    private Map<String, Float> cpuUsage = new HashMap<>();

    public StatisticsLoadBalance() {
        long timeToLive = Long.parseLong(ConfigurationUtils.getProperty("time.to.live"));
        long collectCpuUsageInMill = Long.parseLong(ConfigurationUtils.getProperty("mill.to.collect.cpu.usage"));
        CpuUsageService cpuUsageService = new CpuUsageServiceImpl(timeToLive, collectCpuUsageInMill);
        cpuUsageService.addListener("statisticsloadbalance", (ip, cpu) -> cpuUsage.put(ip, cpu));
    }

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        throw new IllegalStateException("Method unimplemented");
    }
}
