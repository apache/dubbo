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
package org.apache.dubbo.rpc.cluster.loadbalance.statistics;

import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.RpcException;

import com.alibaba.metrics.Gauge;
import com.alibaba.metrics.MetricName;
import com.alibaba.metrics.os.linux.CpuUsageGaugeSet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class CpuUsageServiceImpl implements CpuUsageService {
    private final Map<String, CpuUsageListener> listeners = new ConcurrentHashMap<>();
    private CpuUsageGaugeSet cpuUsage;
    private static final Logger logger = LoggerFactory.getLogger(CpuUsageService.class);

    public CpuUsageServiceImpl() {
        long dataTimeToLive = Long.parseLong(ConfigurationUtils.getProperty("time.to.live"));
        long collectCpuUsageInMill = Long.parseLong(ConfigurationUtils.getProperty("mill.to.collect.cpu.usage"));

        cpuUsage = new CpuUsageGaugeSet(dataTimeToLive, TimeUnit.MILLISECONDS);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(this.getClass().getSimpleName(), true));
        executor.scheduleAtFixedRate(this::collectCpuUsage, collectCpuUsageInMill, collectCpuUsageInMill, TimeUnit.MILLISECONDS);
    }

    @Override
    public void addListener(String key, CpuUsageListener cpuListener) {
        listeners.put(key, cpuListener);
    }

    @Override
    public void removeListener(String key, CpuUsageListener cpuListener) {
       listeners.remove(key, cpuListener);
    }

    private void collectCpuUsage() {
        try {
            Gauge<Float> user = (Gauge) cpuUsage.getMetrics().get(MetricName.build("cpu.user"));
            listeners.forEach((key, value) -> value.cpuChanged(NetUtils.getLocalHost(), user.getValue()));
        } catch(RpcException e) {
            logger.warn(e.getMessage(), e);
        }
    }
}
