
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


import org.apache.dubbo.common.URL;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


public class ServerMetricsCollector {
    public static int defaultBucketNum = 10;
    public static int defaultTimeWindowSeconds = 1;
    private final Long INF = 9223372036854775807L;
    private final Long[] ringBufferCount;
    private final Long[] ringBufferSucceedElapsed;
    private final Long[] ringBufferMaxLatency;
    private final Long[] ringBufferMinLatency;
    private final Double[] ringBufferMaxQPS;
    private final Long[] bucketStartTimeMillis;

    private int currentBucket;
    private long lastRotateTimestampMillis;
    private final long durationBetweenRotatesMillis;

    private int bucketNum;
    private int timeWindowSeconds;

    String uri;
    String methodName;

    public ServerMetricsCollector(int bucketNum,int timeWindowSeconds){
        this.bucketNum = bucketNum;
        this.timeWindowSeconds = timeWindowSeconds;

        this.ringBufferCount = new Long[bucketNum];
        this.ringBufferMaxLatency = new Long[bucketNum];
        this.ringBufferMinLatency = new Long[bucketNum];
        this.ringBufferSucceedElapsed = new Long[bucketNum];
        this.ringBufferMaxQPS = new Double[bucketNum];
        this.bucketStartTimeMillis = new Long[bucketNum];


        for (int i = 0; i < bucketNum; i++) {
            this.ringBufferCount[i] = 0L;
            this.ringBufferMaxLatency[i] = 0L;
            this.ringBufferMinLatency[i] = INF;
            this.bucketStartTimeMillis[i] = System.currentTimeMillis();
            this.ringBufferSucceedElapsed[i] = 0L;
            this.ringBufferMaxQPS[i] = 0.0;
        }

        this.currentBucket = 0;
        this.lastRotateTimestampMillis = System.currentTimeMillis();
        this.durationBetweenRotatesMillis = TimeUnit.SECONDS.toMillis(timeWindowSeconds) / bucketNum;
    }
    public ServerMetricsCollector(int bucketNum, int timeWindowSeconds,String uri,String methodName) {
        this(bucketNum,timeWindowSeconds);
        this.uri = uri;
        this.methodName = methodName;
    }

    public void reset(int currentBucket){
        ringBufferCount[currentBucket] = 0L;
        ringBufferMaxLatency[currentBucket] = 0L;
        ringBufferMinLatency[currentBucket] = INF;
        bucketStartTimeMillis[currentBucket] = System.currentTimeMillis();
        ringBufferSucceedElapsed[currentBucket] = 0L;
        ringBufferMaxQPS[currentBucket] = 0.0;
    }

    public void end(Long elapsed){
        if(elapsed == 0L)
            return;
        updateCount(1L);
        updateMaxLatency(elapsed);
        updateMinLatency(elapsed);
        updateSucceedElapsed(elapsed);
        double QPS = 0;
        if(1.0*getSucceedElapsed()/1000.0 > 0.0){
            QPS = 1.0 * getCount() / (1.0 * getSucceedElapsed() / 1000.0);
        }else{
            QPS = 0.0;
        }
        updateMaxQPS(Double.valueOf(QPS));
    }

    public synchronized Long getCount() {
        rotate();
        return ringBufferCount[currentBucket];
    }

    public synchronized Long getMaxLatency(){
        rotate();
        return ringBufferMaxLatency[currentBucket];
    }

    public synchronized Long getMinLatency(){
        rotate();
        if(ringBufferMinLatency[currentBucket] >= INF){
            return 0L;
        }
        return ringBufferMinLatency[currentBucket];
    }

    public synchronized Long getSucceedElapsed(){
        rotate();
        return ringBufferSucceedElapsed[currentBucket];
    }

    public synchronized Double getMaxQPS(){
        rotate();
        return ringBufferMaxQPS[currentBucket];
    }

    public long bucketLivedSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - bucketStartTimeMillis[currentBucket]);
    }

    public synchronized void updateCount(Long step){
        rotate();
        for (int i = 0; i < ringBufferCount.length; i++){
            ringBufferCount[i] = ringBufferCount[i] + step;
        }
    }

    public synchronized void updateMaxLatency(Long value){
        rotate();
        for (int i = 0; i < ringBufferMaxLatency.length; i++){
            ringBufferMaxLatency[i] = Math.max(ringBufferMaxLatency[i],value);
        }
    }

    public synchronized void updateMinLatency(Long value){
        rotate();
        for(int i = 0; i < ringBufferMinLatency.length; i++){
            ringBufferMinLatency[i] = Math.min(ringBufferMinLatency[i],value);
        }
    }

    public synchronized void updateSucceedElapsed(Long value){
        rotate();
        for(int i = 0; i < ringBufferSucceedElapsed.length; i++){
            ringBufferSucceedElapsed[i] = ringBufferSucceedElapsed[i] + value;
        }
    }

    public synchronized void updateMaxQPS(Double value){
        rotate();
        for(int i = 0; i < ringBufferMaxQPS.length; i++){
            ringBufferMaxQPS[i] = Math.max(ringBufferMaxQPS[i],value);
        }
    }

    private synchronized void rotate() {
        long timeSinceLastRotateMillis = System.currentTimeMillis() - lastRotateTimestampMillis;
        while (timeSinceLastRotateMillis > durationBetweenRotatesMillis) {
            reset(currentBucket);
            bucketStartTimeMillis[currentBucket] = lastRotateTimestampMillis + durationBetweenRotatesMillis;
            if (++currentBucket >= bucketNum) {
                currentBucket = 0;
            }
            timeSinceLastRotateMillis -= durationBetweenRotatesMillis;
            lastRotateTimestampMillis += durationBetweenRotatesMillis;
        }
    }

    public String getUri(){
        return uri;
    }

    public String getMethodName(){
        return methodName;
    }

    public void setUri(String uri){
        this.uri = uri;
    }

    public void setMethodName(String methodName){
        this.methodName = methodName;
    }

}
