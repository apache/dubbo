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

package org.apache.dubbo.metrics.data;

import org.apache.dubbo.metrics.collector.MetricsCollector;
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;

import org.apache.dubbo.metrics.report.MetricsExport;

import java.util.ArrayList;
import java.util.List;


/**
 * As a data aggregator, use internal data containers calculates and classifies
 * the registry data collected by {@link MetricsCollector MetricsCollector}, and
 * provides an {@link MetricsExport MetricsExport} interface for exporting standard output formats.
 */
public abstract class BaseStatComposite implements MetricsExport {

    private final ApplicationStatComposite applicationStatComposite = new ApplicationStatComposite();
    private final ServiceStatComposite serviceStatComposite = new ServiceStatComposite();
    private final RtStatComposite rtStatComposite = new RtStatComposite();


    public BaseStatComposite() {
        init(applicationStatComposite, serviceStatComposite, rtStatComposite);
    }

    protected abstract void init(ApplicationStatComposite applicationStatComposite, ServiceStatComposite serviceStatComposite, RtStatComposite rtStatComposite);

    public void calcApplicationRt(String applicationName, String registryOpType, Long responseTime) {
        rtStatComposite.calcApplicationRt(applicationName, registryOpType, responseTime);
    }

    public void calcServiceKeyRt(String applicationName, String serviceKey, String registryOpType, Long responseTime) {
        rtStatComposite.calcServiceKeyRt(applicationName, serviceKey, registryOpType, responseTime);
    }

    public void setServiceKey(MetricsKey metricsKey, String applicationName, String serviceKey, int num) {
        serviceStatComposite.setServiceKey(metricsKey, applicationName, serviceKey, num);
    }

    public void setApplicationKey(MetricsKey metricsKey, String applicationName, int num) {
        applicationStatComposite.setApplicationKey(metricsKey, applicationName, num);
    }

    public void incrementApp(MetricsKey metricsKey, String applicationName, int size) {
        applicationStatComposite.incrementSize(metricsKey, applicationName, size);
    }

    public void incrementServiceKey(MetricsKey metricsKey, String applicationName, String attServiceKey, int size) {
        serviceStatComposite.incrementServiceKey(metricsKey, applicationName, attServiceKey, size);
    }

    @Override
    @SuppressWarnings({"rawtypes"})
    public List<GaugeMetricSample> export(MetricsCategory category) {
        List<GaugeMetricSample> list = new ArrayList<>();
        list.addAll(applicationStatComposite.export(category));
        list.addAll(rtStatComposite.export(category));
        list.addAll(serviceStatComposite.export(category));
        return list;
    }

    public ApplicationStatComposite getApplicationStatComposite() {
        return applicationStatComposite;
    }

    public RtStatComposite getRtStatComposite() {
        return rtStatComposite;
    }
}
