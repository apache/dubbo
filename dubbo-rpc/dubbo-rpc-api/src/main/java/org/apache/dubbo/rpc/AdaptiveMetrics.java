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
package org.apache.dubbo.rpc;

import org.apache.dubbo.common.utils.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * adaptive Metrics statistics.
 */
public class AdaptiveMetrics {

    private static final ConcurrentMap<String, AdaptiveMetrics> METRICS_STATISTICS = new ConcurrentHashMap<String,
        AdaptiveMetrics>();

    private long currentProviderTime = 0;
    private double providerCPULoad = 0;
    private long lastLatency = 0;
    private long currentTime = 0;

    //Allow some time disorder
    private long pickTime = System.currentTimeMillis();

    private double beta = 0.5;
    private final AtomicLong consumerReq = new AtomicLong();
    private final AtomicLong consumerSuccess = new AtomicLong();
    private final AtomicLong errorReq = new AtomicLong();
    private double ewma = 0;

    private final long factor = 50;
    private int weight = 100;

    public static double getLoad(String idKey,int weight,int timeout){
        AdaptiveMetrics metrics = getStatus(idKey);

        //If the time more than 2 times, mandatory selected
        if (System.currentTimeMillis() - metrics.pickTime > timeout * 2) {
            return 0;
        }

        if (metrics.currentTime > 0){
            long multiple = (System.currentTimeMillis() - metrics.currentTime) / timeout + 1;
            if (multiple > 0) {
                if (metrics.currentProviderTime == metrics.currentTime) {
                    //penalty value
                    metrics.lastLatency = timeout * 2;
                }else {
                    metrics.lastLatency = metrics.lastLatency >> multiple;
                }
                metrics.ewma = metrics.beta * metrics.ewma + (1 - metrics.beta) * metrics.lastLatency;
                metrics.currentTime = System.currentTimeMillis();
            }
        }

        long inflight = metrics.consumerReq.get() - metrics.consumerSuccess.get() - metrics.errorReq.get();
        return metrics.providerCPULoad * (Math.sqrt(metrics.ewma) + 1) * (inflight + 1) / ((((double)metrics.consumerSuccess.get() / (double)(metrics.consumerReq.get() + 1)) * weight) + 1);
    }

    private AdaptiveMetrics() {
    }

    public static AdaptiveMetrics getStatus(String idKey){
        return METRICS_STATISTICS.computeIfAbsent(idKey, k -> new AdaptiveMetrics());
    }

    public static void addConsumerReq(String idKey){
        AdaptiveMetrics metrics = getStatus(idKey);
        metrics.consumerReq.incrementAndGet();
    }

    public static void addConsumerSuccess(String idKey){
        AdaptiveMetrics metrics = getStatus(idKey);
        metrics.consumerSuccess.incrementAndGet();
    }

    public static void addErrorReq(String idKey){
        AdaptiveMetrics metrics = getStatus(idKey);
        metrics.errorReq.incrementAndGet();
    }

    public static void setPickTime(String idKey,long time){
        AdaptiveMetrics metrics = getStatus(idKey);
        metrics.pickTime = time;
    }



    public static void setProviderMetrics(String idKey,Map<String,String> metricsMap){

        AdaptiveMetrics metrics = getStatus(idKey);

        long currentProviderTime = Long.valueOf(Optional.ofNullable(metricsMap.get("curTime")).filter(v -> StringUtils.isNumeric(v,false)).orElse("0"));
        //If server time is less than the current time, discard
        if (metrics.currentProviderTime > currentProviderTime){
            return;
        }

        metrics.currentProviderTime = currentProviderTime;
        metrics.currentTime = currentProviderTime;
        metrics.providerCPULoad = Double.valueOf(Optional.ofNullable(metricsMap.get("load")).filter(v -> StringUtils.isNumeric(v,true)).orElse("0"));
//        metrics.providerQueue = Long.valueOf(Optional.ofNullable(metricsMap.get("queue")).filter(v -> StringUtils.isNumeric(v,false)).orElse("0"));
        metrics.lastLatency = Long.valueOf(Optional.ofNullable(metricsMap.get("rt")).filter(v -> StringUtils.isNumeric(v,false)).orElse("0"));;

        //metrics.beta = Math.pow(Math.E, -metrics.lastLatency / metrics.factor);
        metrics.beta = 0.5;
        //Vt =  β * Vt-1 + (1 -  β ) * θt
        metrics.ewma = metrics.beta * metrics.ewma + (1 - metrics.beta) * metrics.lastLatency;

    }
}

