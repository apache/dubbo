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

package org.apache.dubbo.metrics.registry.stat;

import org.apache.dubbo.metrics.collector.MetricsCollector;
import org.apache.dubbo.metrics.data.ApplicationStatComposite;
import org.apache.dubbo.metrics.data.RtStatComposite;
import org.apache.dubbo.metrics.data.ServiceStatComposite;
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.container.LongContainer;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.registry.RegistryConstants;
import org.apache.dubbo.metrics.report.MetricsExport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.metrics.registry.RegistryConstants.OP_TYPE_NOTIFY;
import static org.apache.dubbo.metrics.registry.RegistryConstants.OP_TYPE_REGISTER;
import static org.apache.dubbo.metrics.registry.RegistryConstants.OP_TYPE_REGISTER_SERVICE;
import static org.apache.dubbo.metrics.registry.RegistryConstants.OP_TYPE_SUBSCRIBE;
import static org.apache.dubbo.metrics.registry.RegistryConstants.OP_TYPE_SUBSCRIBE_SERVICE;

/**
 * As a data aggregator, use internal data containers calculates and classifies
 * the registry data collected by {@link MetricsCollector MetricsCollector}, and
 * provides an {@link MetricsExport MetricsExport} interface for exporting standard output formats.
 */
public class RegistryStatComposite implements MetricsExport {

    private final ApplicationStatComposite applicationStatComposite = new ApplicationStatComposite();
    private final ServiceStatComposite serviceStatComposite = new ServiceStatComposite();
    private final RtStatComposite rtStatComposite = new RtStatComposite();

    public RegistryStatComposite() {

        applicationStatComposite.init(RegistryConstants.appKeys);
        serviceStatComposite.init(RegistryConstants.serviceKeys);
        // App-level
        rtStatComposite.init(OP_TYPE_REGISTER, OP_TYPE_SUBSCRIBE, OP_TYPE_NOTIFY, OP_TYPE_REGISTER_SERVICE, OP_TYPE_SUBSCRIBE_SERVICE);
    }


    public void calcApplicationRt(String applicationName, String registryOpType, Long responseTime) {
        rtStatComposite.calcApplicationRt(applicationName, registryOpType, responseTime);
    }

    public void calcServiceKeyRt(String applicationName, String serviceKey, String registryOpType, Long responseTime) {
        rtStatComposite.calcServiceKeyRt(applicationName, serviceKey, registryOpType, responseTime);
    }

    @Override
    @SuppressWarnings({"rawtypes"})
    public List<GaugeMetricSample> exportNumMetrics() {
        return applicationStatComposite.export(MetricsCategory.REGISTRY);
    }

    @Override
    @SuppressWarnings({"rawtypes"})
    public List<GaugeMetricSample> exportRtMetrics() {
        return rtStatComposite.export();
    }


    @SuppressWarnings({"rawtypes"})
    public List<GaugeMetricSample> exportSkMetrics() {
        return serviceStatComposite.export(MetricsCategory.REGISTRY);
    }


}
