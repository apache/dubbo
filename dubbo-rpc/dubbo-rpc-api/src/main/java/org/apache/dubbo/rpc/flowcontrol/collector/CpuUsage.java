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
package org.apache.dubbo.rpc.flowcontrol.collector;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.resource.GlobalResourcesRepository;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class CpuUsage {
    private HardwareMetricsCollector hardwareMetricsCollector = new HardwareMetricsCollector();
    private static final Logger logger = LoggerFactory.getLogger(CpuUsage.class);

    private double aConstant = 0.5;
    private volatile double value;

    private final long defaultUpdateInterval = 1000;
    private ScheduledExecutorService scheduledExecutorService;


    public CpuUsage() {
        init();
    }

    public void startPeriodAutoUpdate(){
        scheduledExecutorService.scheduleWithFixedDelay(this::update,0,defaultUpdateInterval,TimeUnit.MILLISECONDS);
        GlobalResourcesRepository.registerGlobalDisposable(() -> scheduledExecutorService.shutdown());
    }

    public void setAConstant(double aConstant){
        this.aConstant = aConstant;
    }

    public void init(){
        Double tmp = Double.NaN;
        while(tmp.equals(Double.NaN))
            tmp = hardwareMetricsCollector.systemCpuUsage();
        value = tmp;
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Dubbo-Cpu-Collector"));
    }

    synchronized public void update(){
        Double yt = Double.NaN;
        while(yt.equals(Double.NaN)){
            yt = hardwareMetricsCollector.systemCpuUsage();
            //double yt = hardwareMetricsCollector.systemLoadAverage1m();
        }
        value = aConstant * yt + (1 - aConstant) * value;
        logger.debug(String.format("current cpu usage is %f ",value));
    }


    public double getCpuUsage(){
        return value;
    }
}
