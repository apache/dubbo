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
package org.apache.dubbo.metrices;


public class Metrics {
    /**
     * Metrics status.
     * 0 : processing,
     * 1 : failed,
     * 2 : success.
     */
    private int status;
    private final long startTime;
    private long executeLatency;

    public Metrics() {
        this.status = 0;
        this.startTime = System.currentTimeMillis();
    }

    public int getStatus() {
        return status;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public double getELSeconds() {
        return this.executeLatency / 1000.0;
    }

    public void success() {
        this.status = 2;
        this.executeLatency = System.currentTimeMillis() - this.startTime;
    }

    public void fail() {
        this.status = 1;
        this.executeLatency = System.currentTimeMillis() - this.startTime;
    }
}
