/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.cluster.loadbalance.statistics;

import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CpuUsageAvailabilityTask {

    private CpuUsageService cpuUsageService;
    private Map<String, Float> cpuUsage;
    private String invokerAddress;

    public CpuUsageAvailabilityTask(String invokerAddress, CpuUsageService cpuUsageService, Map<String, Float> cpuUsage) {
        this.cpuUsageService = cpuUsageService;
        this.cpuUsage = cpuUsage;
        this.invokerAddress = invokerAddress;
        long collectCpuUsageInMill = Long.parseLong(ConfigurationUtils.getProperty("mill.to.check.cpu.usage.availability"));

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(this.getClass().getSimpleName(), true));
        executor.scheduleAtFixedRate(this::doTask, collectCpuUsageInMill, collectCpuUsageInMill, TimeUnit.MILLISECONDS);
    }

    private void doTask() {
        try {
            cpuUsageService.isAvailable();
        } catch (Exception e) {
            cpuUsage.remove(invokerAddress);
        }
    }
}
