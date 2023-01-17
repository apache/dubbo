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

package org.apache.dubbo.common.threadpool;

import org.apache.dubbo.common.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.common.metrics.model.ThreadPoolMetric;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.concurrent.ThreadPoolExecutor;

public abstract class MetricThreadPool implements ThreadPool {

    private DefaultMetricsCollector collector = null;

    private ApplicationModel applicationModel;

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        collector = applicationModel.getBeanFactory().getBean(DefaultMetricsCollector.class);
    }

    protected void addThreadPoolMetric(ThreadPoolExecutor threadPoolExecutor, String threadPoolName) {
        ThreadPoolMetric metric = new ThreadPoolMetric(this.applicationModel.getApplicationName(), threadPoolName, threadPoolExecutor);
        collector.addThreadPoolMetric(metric);
    }
}
