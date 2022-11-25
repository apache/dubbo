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
package org.apache.dubbo.rpc.flowcontrol;


import org.apache.dubbo.rpc.FlowControl;

import org.apache.dubbo.rpc.flowcontrol.collector.CpuUsage;
import org.apache.dubbo.rpc.flowcontrol.collector.ServerMetricsCollector;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import java.util.concurrent.atomic.AtomicLong;


public class HeuristicSmoothingFlowControl implements FlowControl, ScopeModelAware {
    public static final String NAME = "heuristicSmoothingFlowControl";
    public static final double LowCpuLoad = 0.2;
    public static final double HighCpuLoad = 0.6;
    public static final double Alpha = 0.3;
    public static final int BucketNum = 10;
    public static final int TimeWindowSeconds = 1;
    public static final double ConstantA = 0.5;
    public static final int initialMaxConcurrency = 40;



    private int maxConcurrency;
    private double noLoadLatency;
    private final AtomicLong inflight = new AtomicLong();

    CpuUsage cpuUsage;
    ServerMetricsCollector serverMetricsCollector;
    ApplicationModel applicationModel;

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        noLoadLatency = 0;
        maxConcurrency = initialMaxConcurrency;

        cpuUsage = new CpuUsage();
        cpuUsage.setApplicationModel(applicationModel);
        cpuUsage.startPeriodAutoUpdate();

        serverMetricsCollector = new ServerMetricsCollector(BucketNum,TimeWindowSeconds);
    }

    public double updateNoLoadLatency(){
        double tmpCpuUsage = cpuUsage.getCpuUsage();
        if(noLoadLatency == 0 || tmpCpuUsage <= LowCpuLoad){
            noLoadLatency = serverMetricsCollector.getMinLatency();
        }else if(tmpCpuUsage > LowCpuLoad && tmpCpuUsage <= HighCpuLoad){
            noLoadLatency = ConstantA * serverMetricsCollector.getMinLatency() + (1 - ConstantA) * noLoadLatency;
        }else{
            double tmp = ConstantA * serverMetricsCollector.getMinLatency() + (1 - ConstantA) * noLoadLatency;
            if(tmp <= noLoadLatency)
                noLoadLatency = tmp;
        }
        return noLoadLatency;
    }

    public double getNoLoadLatency(){
        updateNoLoadLatency();
        return noLoadLatency;
    }


    public void updateMaxConcurrency(){
        double noLoadLatency = getNoLoadLatency() / 1000;
        double maxQPS = serverMetricsCollector.getMaxQPS();
        double avg_latency = serverMetricsCollector.getCount() != 0?1.0 * serverMetricsCollector.getSucceedElapsed()/ serverMetricsCollector.getCount():0;
        avg_latency /= 1000;
        if(serverMetricsCollector.getCount() <= 0){
            maxConcurrency /= 2;
        }else{
            maxConcurrency = (int) Math.ceil(maxQPS*((2 + Alpha)*noLoadLatency - avg_latency));
        }

        if(maxConcurrency <= 0)
            maxConcurrency = 1;
    }

    public boolean Begin(){
        if(inflight.incrementAndGet() > maxConcurrency && cpuUsage.getCpuUsage() >= 0.5){
            inflight.decrementAndGet();
            return false;
        }
        return true;
    }

    public void End(long latency){
        serverMetricsCollector.end(latency / 1000);
        updateMaxConcurrency();
        inflight.decrementAndGet();
    }


    @Override
    public int getMaxConcurrency(){
        return maxConcurrency;
    }
}
