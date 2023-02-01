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


import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.FlowControl;
import org.apache.dubbo.rpc.flowcontrol.collector.CpuUsage;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class AutoConcurrencyLimier implements FlowControl, ScopeModelAware {
    public static final String NAME = "autoConcurrencyLimier";
    private static final Logger logger = LoggerFactory.getLogger(AutoConcurrencyLimier.class);

    public static final double MaxExploreRatio = 0.3;
    public static final double MinExploreRatio = 0.06;
    public static final long SampleWindowSizeMs = 1000;
    public static final long MinSampleCount = 40;
    public static final long MaxSampleCount = 500;

    private CpuUsage cpuUsage;
    //private LinuxCpuUsage cpuUsage;

    private double exploreRatio;
    private double emaFactor;
    private double noLoadLatency;
    private double maxQPS;
    private long halfSampleIntervalMS;
    //
    private long startSampleTimeUs;
    private final AtomicLong lastSamplingTimeUs = new AtomicLong();
    private long resetLatencyUs;
    private long remeasureStartUs;
    private long sampleCount;
    private long totalSampleUs;
    private final AtomicLong totalReqCount = new AtomicLong();
    private final AtomicLong inflight = new AtomicLong();

    private int maxConcurrency;
    private AtomicBoolean onResetSlideWindow = new AtomicBoolean(false);

    ApplicationModel applicationModel;
    private Object object;

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        init();
    }

    public void init(){
        exploreRatio = MaxExploreRatio;
        emaFactor = 0.1;
        noLoadLatency = -1;
        maxQPS = -1;
        maxConcurrency = 40;
        halfSampleIntervalMS = 25000;
        resetLatencyUs = 0;
        remeasureStartUs = NextResetTime(System.nanoTime() / 1000);
        cpuUsage = new CpuUsage();
        cpuUsage.startPeriodAutoUpdate();
    }

    public void updateNoLoadLatency(double latency){
        if(noLoadLatency <= 0){
            noLoadLatency = latency;
        }else if(latency < noLoadLatency){
            noLoadLatency = latency * emaFactor + noLoadLatency * (1 - emaFactor);
        }
    }

    public void updateQPS(double qps){
        double tmpEMAFactor = emaFactor / 10;
        if(maxQPS <= qps){
            maxQPS = qps;
        }else{
            maxQPS = qps * emaFactor + maxQPS * (1 - emaFactor);
        }
    }

    public void updateMaxConcurrency(int maxConcurrency){
        if(this.maxConcurrency <= maxConcurrency){
            this.maxConcurrency = maxConcurrency;
        }else{
            this.maxConcurrency = (int) ((double)maxConcurrency * emaFactor + (double)this.maxConcurrency * (1 - emaFactor));
        }
    }

    public long getInFlight(){
        return this.inflight.get();
    }

    public long getRemaining(){
        return maxConcurrency - getInFlight();
    }

    @Override
    public boolean Begin(){
        if(inflight.incrementAndGet() > maxConcurrency && cpuUsage.getCpuUsage() >= 0.5){
            inflight.decrementAndGet();
            return false;
        }
        return true;
    }

    public void Update(long latency,long samplingTimeUs){
        if(onResetSlideWindow.compareAndSet(false,true) == false){
            return;
        }

        if(resetLatencyUs != 0){
            if(resetLatencyUs > samplingTimeUs){
                onResetSlideWindow.set(false);
                return;
            }
            noLoadLatency = -1;
            resetLatencyUs = 0;
            remeasureStartUs = NextResetTime(samplingTimeUs);
            Reset(samplingTimeUs);
        }

        if(startSampleTimeUs == 0){
            startSampleTimeUs = samplingTimeUs;
        }
        sampleCount ++;
        totalSampleUs += latency;
        logger.debug(String.format("[Auto Concurrency Limiter Test] samplingTimeUs: %d, startSampleTimeUs: %d",samplingTimeUs,startSampleTimeUs));

        if(sampleCount < MinSampleCount){
            if(samplingTimeUs - startSampleTimeUs >= SampleWindowSizeMs * 1000){
                Reset(samplingTimeUs);
            }
            onResetSlideWindow.set(false);
            return;
        }

        logger.debug(String.format("[Auto Concurrency Limiter Test] samplingTimeUs: %d, startSampleTimeUs: %d",samplingTimeUs,startSampleTimeUs));
        if(samplingTimeUs - startSampleTimeUs < SampleWindowSizeMs * 1000 && sampleCount < MaxSampleCount){
            onResetSlideWindow.set(false);
            return;
        }

        if(sampleCount > 0){
            double qps = (double)totalReqCount.get() * 1000000.0 /(double) (samplingTimeUs - startSampleTimeUs);
            updateQPS(qps);

            long avgLatency = totalSampleUs / sampleCount;
            updateNoLoadLatency(avgLatency);

            int nextMaxConcurrency = 0;
            if(remeasureStartUs <= samplingTimeUs){
                Reset(samplingTimeUs);
                resetLatencyUs = samplingTimeUs + 2 * avgLatency;
                nextMaxConcurrency = (int) (Math.ceil(maxQPS * noLoadLatency * 0.9 / 1000000.0));
            }else {
                if((double)avgLatency <= noLoadLatency*(1.0 + MinExploreRatio) || qps >= maxQPS * (1.0 + MinExploreRatio)){
                    exploreRatio = Math.min(MaxExploreRatio,exploreRatio + 0.02);
                }else{
                    exploreRatio = Math.max(MinExploreRatio,exploreRatio - 0.02);
                }
                nextMaxConcurrency = (int) (Math.ceil(noLoadLatency * maxQPS * (1 + exploreRatio) / 1000000.0));
            }

            maxConcurrency = (int) nextMaxConcurrency;
        }else{
            maxConcurrency /= 2;
        }

        if (maxConcurrency <= 0){
            maxConcurrency = 1;
        }


        logger.debug(String.format("[Auto Concurrency Limiter] Qps: %f, NoLoadLatency: %f, MaxConcurrency: %d",maxQPS,noLoadLatency,maxConcurrency));
        Reset(samplingTimeUs);
        onResetSlideWindow.set(false);
    }


    public void End(long latency){
        long lastSamplingTimeUs = this.lastSamplingTimeUs.get();
        long now = System.nanoTime() / 1000;

        if(lastSamplingTimeUs == 0 || now - lastSamplingTimeUs >= 100){
            boolean sample = this.lastSamplingTimeUs.compareAndSet(lastSamplingTimeUs,now);
            if(sample){
                logger.debug(String.format("[Auto Concurrency Updater] sample, %d, %d",resetLatencyUs,remeasureStartUs));
                Update(latency,now);
            }
        }
        this.inflight.decrementAndGet();
    }

    public void Reset(long startTimeUs){
        startSampleTimeUs = startTimeUs;
        sampleCount = 0;
        totalSampleUs = 0;
        totalReqCount.set(0);
    }
    public long NextResetTime(long samplingTimeUs){
        return samplingTimeUs + (halfSampleIntervalMS + ThreadLocalRandom.current().nextLong(halfSampleIntervalMS))*1000;
    }


    @Override
    public int getMaxConcurrency() {
        return maxConcurrency;
    }
}
