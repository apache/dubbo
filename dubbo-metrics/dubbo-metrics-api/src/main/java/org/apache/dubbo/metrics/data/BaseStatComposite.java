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
import org.apache.dubbo.metrics.model.ApplicationMetric;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.ServiceKeyMetric;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.report.MetricsExport;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * As a data aggregator, use internal data containers calculates and classifies
 * the registry data collected by {@link MetricsCollector MetricsCollector}, and
 * provides an {@link MetricsExport MetricsExport} interface for exporting standard output formats.
 */
public abstract class BaseStatComposite implements MetricsExport {

    private ApplicationStatComposite applicationStatComposite;
    private ServiceStatComposite serviceStatComposite;

    private MethodStatComposite methodStatComposite;
    private RtStatComposite rtStatComposite;

    public BaseStatComposite(ApplicationModel applicationModel) {
        init(new ApplicationStatComposite(applicationModel));
        init(new ServiceStatComposite(applicationModel));
        init(new MethodStatComposite(applicationModel));
        init(new RtStatComposite(applicationModel));
    }

    protected void init(ApplicationStatComposite applicationStatComposite) {
        this.applicationStatComposite = applicationStatComposite;
    }

    protected void init(ServiceStatComposite serviceStatComposite) {
        this.serviceStatComposite = serviceStatComposite;
    }

    protected void init(MethodStatComposite methodStatComposite) {
        this.methodStatComposite = methodStatComposite;
    }

    protected void init(RtStatComposite rtStatComposite) {
        this.rtStatComposite = rtStatComposite;
    }

    public void calcApplicationRt(String registryOpType, Long responseTime) {
        rtStatComposite.calcServiceKeyRt(
                registryOpType, responseTime, new ApplicationMetric(rtStatComposite.getApplicationModel()));
    }

    public void calcServiceKeyRt(String serviceKey, String registryOpType, Long responseTime) {
        rtStatComposite.calcServiceKeyRt(
                registryOpType, responseTime, new ServiceKeyMetric(rtStatComposite.getApplicationModel(), serviceKey));
    }

    public void calcServiceKeyRt(Invocation invocation, String registryOpType, Long responseTime) {
        rtStatComposite.calcServiceKeyRt(invocation, registryOpType, responseTime);
    }

    public void calcMethodKeyRt(Invocation invocation, String registryOpType, Long responseTime) {
        rtStatComposite.calcMethodKeyRt(invocation, registryOpType, responseTime);
    }

    public void setServiceKey(MetricsKeyWrapper metricsKey, String serviceKey, int num) {
        serviceStatComposite.setServiceKey(metricsKey, serviceKey, num);
    }

    public void setServiceKey(MetricsKeyWrapper metricsKey, String serviceKey, int num, Map<String, String> extra) {
        serviceStatComposite.setExtraServiceKey(metricsKey, serviceKey, num, extra);
    }

    public void incrementApp(MetricsKey metricsKey, int size) {
        applicationStatComposite.incrementSize(metricsKey, size);
    }

    public void incrementServiceKey(MetricsKeyWrapper metricsKeyWrapper, String attServiceKey, int size) {
        serviceStatComposite.incrementServiceKey(metricsKeyWrapper, attServiceKey, size);
    }

    public void incrementServiceKey(
            MetricsKeyWrapper metricsKeyWrapper, String attServiceKey, Map<String, String> extra, int size) {
        serviceStatComposite.incrementExtraServiceKey(metricsKeyWrapper, attServiceKey, extra, size);
    }

    public void incrementMethodKey(MetricsKeyWrapper metricsKeyWrapper, MethodMetric methodMetric, int size) {
        methodStatComposite.incrementMethodKey(metricsKeyWrapper, methodMetric, size);
    }

    public void initMethodKey(MetricsKeyWrapper metricsKeyWrapper, Invocation invocation) {
        methodStatComposite.initMethodKey(metricsKeyWrapper, invocation);
    }

    @Override
    public List<MetricSample> export(MetricsCategory category) {
        List<MetricSample> list = new ArrayList<>();
        list.addAll(applicationStatComposite.export(category));
        list.addAll(rtStatComposite.export(category));
        list.addAll(serviceStatComposite.export(category));
        list.addAll(methodStatComposite.export(category));
        return list;
    }

    public ApplicationStatComposite getApplicationStatComposite() {
        return applicationStatComposite;
    }

    public RtStatComposite getRtStatComposite() {
        return rtStatComposite;
    }

    @Override
    public boolean calSamplesChanged() {
        // Should ensure that all the composite's samplesChanged have been compareAndSet, and cannot flip the `or` logic
        boolean changed = applicationStatComposite.calSamplesChanged();
        changed = rtStatComposite.calSamplesChanged() || changed;
        changed = serviceStatComposite.calSamplesChanged() || changed;
        changed = methodStatComposite.calSamplesChanged() || changed;
        return changed;
    }
}
