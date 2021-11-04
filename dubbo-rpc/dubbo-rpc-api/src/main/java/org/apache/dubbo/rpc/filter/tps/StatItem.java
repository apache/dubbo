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
package org.apache.dubbo.rpc.filter.tps;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Judge whether a particular invocation of service provider method should be allowed within a configured time interval.
 * As a state it contain name of key ( e.g. method), last invocation time, interval and rate count.
 */
class StatItem {

    private final String name;

    private final AtomicLong lastResetTime;

    private final long interval;

    private final AtomicInteger token;

    private final int rate;

    StatItem(String name, int rate, long interval) {
        this.name = name;
        this.rate = rate;
        this.interval = interval;
        this.lastResetTime = new AtomicLong(System.currentTimeMillis());
        this.token = new AtomicInteger(rate);
    }

    public boolean isAllowable() {
        long now = System.currentTimeMillis();
        if (now > lastResetTime.get() + interval) {
            token.set(rate);
            lastResetTime.set(now);
        }

        return token.decrementAndGet() >= 0;
    }

    public long getInterval() {
        return interval;
    }


    public int getRate() {
        return rate;
    }


    long getLastResetTime() {
        return lastResetTime.get();
    }

    int getToken() {
        return token.get();
    }

    @Override
    public String toString() {
        return "StatItem " +
            "[name=" + name + ", " +
            "rate = " + rate + ", " +
            "interval = " + interval + ']';
    }

}
