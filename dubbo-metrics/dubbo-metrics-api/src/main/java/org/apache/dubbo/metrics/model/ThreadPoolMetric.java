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

package org.apache.dubbo.metrics.model;

import org.apache.dubbo.common.utils.ConfigUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;


import static org.apache.dubbo.common.constants.MetricsConstants.TAG_IP;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_PID;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_HOSTNAME;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_APPLICATION_NAME;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_THREAD_NAME;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHost;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHostName;

public class ThreadPoolMetric implements Metric{

    private String applicationName;

    private String threadPoolName;

    private ThreadPoolExecutor threadPoolExecutor;
    
    public ThreadPoolMetric(String applicationName, String threadPoolName, ThreadPoolExecutor threadPoolExecutor) {
        this.applicationName = applicationName;
        this.threadPoolExecutor = threadPoolExecutor;
        this.threadPoolName = threadPoolName;
    }

    public String getThreadPoolName() {
        return threadPoolName;
    }

    public void setThreadPoolName(String threadPoolName) {
        this.threadPoolName = threadPoolName;
    }

    public ThreadPoolExecutor getThreadPoolExecutor() {
        return threadPoolExecutor;
    }

    public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThreadPoolMetric that = (ThreadPoolMetric) o;
        return Objects.equals(applicationName, that.applicationName) &&
            Objects.equals(threadPoolName, that.threadPoolName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationName, threadPoolName);
    }

    public Map<String, String> getTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put(TAG_IP, getLocalHost());
        tags.put(TAG_PID, ConfigUtils.getPid()+"");
        tags.put(TAG_HOSTNAME, getLocalHostName());
        tags.put(TAG_APPLICATION_NAME, applicationName);
        tags.put(TAG_THREAD_NAME, threadPoolName);
        return tags;
    }

    public double getCorePoolSize() {
        return threadPoolExecutor.getCorePoolSize();
    }

    public double getLargestPoolSize() {
        return threadPoolExecutor.getLargestPoolSize();
    }

    public double getMaximumPoolSize() {
        return threadPoolExecutor.getMaximumPoolSize();
    }

    public double getActiveCount() {
        return threadPoolExecutor.getActiveCount();
    }

    public double getPoolSize(){
        return threadPoolExecutor.getPoolSize();
    }

    public double getQueueSize(){
        return threadPoolExecutor.getQueue().size();
    }

}
