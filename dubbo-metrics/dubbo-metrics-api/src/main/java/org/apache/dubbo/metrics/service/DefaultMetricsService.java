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

package org.apache.dubbo.metrics.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.dubbo.common.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.common.metrics.collector.MetricsCollector;
import org.apache.dubbo.common.metrics.model.MetricsCategory;
import org.apache.dubbo.common.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.common.metrics.model.sample.MetricSample;
import org.apache.dubbo.common.metrics.service.MetricsEntity;
import org.apache.dubbo.common.metrics.service.MetricsService;
import org.apache.dubbo.metrics.collector.AggregateMetricsCollector;
import org.apache.dubbo.rpc.model.ApplicationModel;

/**
 * Default implementation of {@link MetricsService}
 */
public class DefaultMetricsService implements MetricsService {

    protected final List<MetricsCollector> collectors = new ArrayList<>();

    private final ApplicationModel applicationModel;

    public DefaultMetricsService(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        collectors.add(applicationModel.getBeanFactory().getBean(DefaultMetricsCollector.class));
        collectors.add(applicationModel.getBeanFactory().getBean(AggregateMetricsCollector.class));
    }

    @Override
    public Map<MetricsCategory, List<MetricsEntity>> getMetricsByCategories(List<MetricsCategory> categories) {
        return getMetricsByCategories(null, categories);
    }

    @Override
    public Map<MetricsCategory, List<MetricsEntity>> getMetricsByCategories(String serviceUniqueName, List<MetricsCategory> categories) {
        return getMetricsByCategories(serviceUniqueName, null, null, categories);
    }

    @Override
    public Map<MetricsCategory, List<MetricsEntity>> getMetricsByCategories(String serviceUniqueName, String methodName, Class<?>[] parameterTypes, List<MetricsCategory> categories) {
        Map<MetricsCategory, List<MetricsEntity>> result = new HashMap<>();
        for (MetricsCollector collector : collectors) {
            List<MetricSample> samples = collector.collect();
            for (MetricSample sample : samples) {
                if (categories.contains(sample.getCategory())) {
                    List<MetricsEntity> entities = result.computeIfAbsent(sample.getCategory(), k -> new ArrayList<>());
                    entities.add(sampleToEntity(sample));
                }
            }
        }

        return result;
    }

    private MetricsEntity sampleToEntity(MetricSample sample) {
        MetricsEntity entity = new MetricsEntity();

        entity.setName(sample.getName());
        entity.setTags(sample.getTags());
        entity.setCategory(sample.getCategory());
        switch (sample.getType()) {
            case GAUGE:
                GaugeMetricSample gaugeSample = (GaugeMetricSample) sample;
                entity.setValue(gaugeSample.getSupplier().get());
                break;
            case COUNTER:
            case LONG_TASK_TIMER:
            case TIMER:
            case DISTRIBUTION_SUMMARY:
            default:
                break;
        }

        return entity;
    }
}
