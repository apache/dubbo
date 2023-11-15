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
package org.apache.dubbo.metrics.metrics.model.sample;

import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.ReflectionUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.collector.sample.ErrorCodeMetricsListenRegister;
import org.apache.dubbo.metrics.collector.sample.ErrorCodeSampler;
import org.apache.dubbo.metrics.model.sample.CounterMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

public class ErrorCodeSampleTest {

    @Test
    void testErrorCodeMetric() {
        FrameworkModel frameworkModel = FrameworkModel.defaultModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("MyApplication1");

        applicationModel.getApplicationConfigManager().setApplication(applicationConfig);

        DefaultMetricsCollector defaultMetricsCollector = new DefaultMetricsCollector(applicationModel);
        defaultMetricsCollector.setCollectEnabled(true);

        ErrorCodeSampler sampler =
                (ErrorCodeSampler) ReflectionUtils.getField(defaultMetricsCollector, "errorCodeSampler");

        ErrorCodeMetricsListenRegister register =
                (ErrorCodeMetricsListenRegister) ReflectionUtils.getField(sampler, "register");

        register.onMessage("0-1", null);
        register.onMessage("0-1", null);
        register.onMessage("0-2", null);
        register.onMessage("0-2", null);
        register.onMessage("1-2", null);
        register.onMessage("1-2", null);
        register.onMessage("1-3", null);
        register.onMessage("1-3", null);

        List<MetricSample> samples = defaultMetricsCollector.collect();

        Assert.assertTrue(samples.size() == 4, "Wrong number of samples.");
        samples.forEach(metricSample -> Assert.assertTrue(
                ((AtomicLong) ((CounterMetricSample<?>) metricSample).getValue()).get() == 2L, "Sample count error."));

        System.out.println(samples);
    }
}
