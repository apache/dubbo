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

import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
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

    private final ConcurrentMap<String, AdaptiveMetrics> metricsStatistics = new ConcurrentHashMap<>();

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

    public double getLoad(String idKey, int weight, int timeout) {
        AdaptiveMetrics metrics = getStatus(idKey);

        //If the time more than 2 times, mandatory selected
        if (System.currentTimeMillis() - metrics.pickTime > timeout * 2) {
            return 0;
        }

        if (metrics.currentTime > 0) {
            long multiple = (System.currentTimeMillis() - metrics.currentTime) / timeout + 1;
            if (multiple > 0) {
                if (metrics.currentProviderTime == metrics.currentTime) {
                    //penalty value
                    metrics.lastLatency = timeout * 2L;
                } else {
                    metrics.lastLatency = metrics.lastLatency >> multiple;
                }
                metrics.ewma = metrics.beta * metrics.ewma + (1 - metrics.beta) * metrics.lastLatency;
                metrics.currentTime = System.currentTimeMillis();
            }
        }

        long inflight = metrics.consumerReq.get() - metrics.consumerSuccess.get() - metrics.errorReq.get();
        return metrics.providerCPULoad * (Math.sqrt(metrics.ewma) + 1) * (inflight + 1) / ((((double) metrics.consumerSuccess.get() / (double) (metrics.consumerReq.get() + 1)) * weight) + 1);
    }

    public AdaptiveMetrics getStatus(String idKey) {
        return ConcurrentHashMapUtils.computeIfAbsent(metricsStatistics, idKey, k -> new AdaptiveMetrics());
    }

    public void addConsumerReq(String idKey) {
        AdaptiveMetrics metrics = getStatus(idKey);
        metrics.consumerReq.incrementAndGet();
    }

    public void addConsumerSuccess(String idKey) {
        AdaptiveMetrics metrics = getStatus(idKey);
        metrics.consumerSuccess.incrementAndGet();
    }

    public void addErrorReq(String idKey) {
        AdaptiveMetrics metrics = getStatus(idKey);
        metrics.errorReq.incrementAndGet();
    }

    public void setPickTime(String idKey, long time) {
        AdaptiveMetrics metrics = getStatus(idKey);
        metrics.pickTime = time;
    }


    public void setProviderMetrics(String idKey, Map<String, String> metricsMap) {

        AdaptiveMetrics metrics = getStatus(idKey);

        long serviceTime = Long.parseLong(Optional.ofNullable(metricsMap.get("curTime")).filter(v -> StringUtils.isNumeric(v, false)).orElse("0"));
        //If server time is less than the current time, discard
        if (metrics.currentProviderTime > serviceTime) {
            return;
        }

        metrics.currentProviderTime = serviceTime;
        metrics.currentTime = serviceTime;
        metrics.providerCPULoad = Double.parseDouble(Optional.ofNullable(metricsMap.get("load")).filter(v -> StringUtils.isNumeric(v, true)).orElse("0"));
        metrics.lastLatency = Long.parseLong((Optional.ofNullable(metricsMap.get("rt")).filter(v -> StringUtils.isNumeric(v, false)).orElse("0")));

        metrics.beta = 0.5;
        //Vt =  β * Vt-1 + (1 -  β ) * θt
        metrics.ewma = metrics.beta * metrics.ewma + (1 - metrics.beta) * metrics.lastLatency;

    }
}

