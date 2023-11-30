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
package org.apache.dubbo.metrics;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.metrics.collector.MetricsCollector;
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.metrics.service.DefaultMetricsService;
import org.apache.dubbo.metrics.service.MetricsEntity;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

@SuppressWarnings("rawtypes")
public class DefaultMetricsServiceTest {

    private MetricsCollector metricsCollector;

    private DefaultMetricsService defaultMetricsService;

    @BeforeEach
    public void setUp() {
        ApplicationModel applicationModel = Mockito.mock(ApplicationModel.class);
        ScopeBeanFactory beanFactory = Mockito.mock(ScopeBeanFactory.class);
        metricsCollector = Mockito.mock(MetricsCollector.class);

        when(applicationModel.getBeanFactory()).thenReturn(beanFactory);
        when(beanFactory.getBeansOfType(MetricsCollector.class))
                .thenReturn(Collections.singletonList(metricsCollector));

        defaultMetricsService = new DefaultMetricsService(applicationModel);
    }

    @Test
    public void testGetMetricsByCategories() {
        MetricSample sample = new GaugeMetricSample<>(
                "testMetric", "testDescription", null, MetricsCategory.REQUESTS, 42, value -> 42.0);
        when(metricsCollector.collect()).thenReturn(Collections.singletonList(sample));
        List<MetricsCategory> categories = Collections.singletonList(MetricsCategory.REQUESTS);

        Map<MetricsCategory, List<MetricsEntity>> result = defaultMetricsService.getMetricsByCategories(categories);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        List<MetricsEntity> entities = result.get(MetricsCategory.REQUESTS);
        Assertions.assertNotNull(entities);
        Assertions.assertEquals(1, entities.size());

        MetricsEntity entity = entities.get(0);
        Assertions.assertEquals("testMetric", entity.getName());
        Assertions.assertEquals(42.0, entity.getValue());
        Assertions.assertEquals(MetricsCategory.REQUESTS, entity.getCategory());
    }
}
