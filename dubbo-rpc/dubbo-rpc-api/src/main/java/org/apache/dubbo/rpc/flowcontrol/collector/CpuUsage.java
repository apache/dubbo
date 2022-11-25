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
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class CpuUsage implements ScopeModelAware {
    private HardwareMetricsCollector hardwareMetricsCollector = new HardwareMetricsCollector();
    private static final Logger logger = LoggerFactory.getLogger(CpuUsage.class);

    private ApplicationModel applicationModel;
    private double aConstant = 0.05;
    private volatile double value;

    private final long defaultUpdateInterval = 500;
    private ScheduledExecutorService scheduledExecutorService;

    private ScheduledFuture<?> sendFuture;

    @Override
    public void setApplicationModel(ApplicationModel applicationModel){
        this.applicationModel = applicationModel;
        scheduledExecutorService = applicationModel.getFrameworkModel().getBeanFactory().getBean(FrameworkExecutorRepository.class).getSharedScheduledExecutor();
    }

    public CpuUsage() {
        init();
    }

    public void startPeriodAutoUpdate(){
        sendFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                // collect data
                update();
            } catch (Throwable t) {
                t.printStackTrace();
                destroyPeriodAutoUpdate();
            }
        }, defaultUpdateInterval, defaultUpdateInterval, TimeUnit.MILLISECONDS);
    }

    public void destroyPeriodAutoUpdate(){
        try {
            ExecutorUtil.cancelScheduledFuture(sendFuture);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void setAConstant(double aConstant){
        this.aConstant = aConstant;
    }

    public void init(){
        Double tmp = Double.NaN;
        while(tmp.equals(Double.NaN))
            tmp = hardwareMetricsCollector.systemCpuUsage();

        value = tmp;
    }

    synchronized public void update(){
        Double yt = Double.NaN;
        while(yt.equals(Double.NaN)){
            yt = hardwareMetricsCollector.processCpuUsage();
            //double yt = hardwareMetricsCollector.systemLoadAverage1m();
        }
        value = aConstant * yt + (1 - aConstant) * value;
        logger.debug(String.format("current cpu usage is %f ",value));
    }


    public double getCpuUsage(){
        //return hardwareMetricsCollector.systemLoadAverage1m();
        //update();
        return value;
    }
}
