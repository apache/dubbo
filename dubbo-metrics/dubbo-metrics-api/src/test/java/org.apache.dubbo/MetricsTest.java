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
package org.apache.dubbo;

import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.metrices.MethodMetrics;
import org.apache.dubbo.metrices.Metrics;
import org.apache.dubbo.service.MetricsService;
import org.apache.dubbo.service.MetricsServiceImpl;
import org.junit.jupiter.api.Test;



public class MetricsTest {

    @Test
    public void testMetricsServiceImpl() {
        MetricsService metricsService = new MetricsServiceImpl(true, 6, 120);
        for (int i = 0; i < 10; i++) {
            try {
                Metrics metrics = metricsService.start("Interface", "method");
                Thread.sleep(i * 100);
                metrics.success();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        MethodMetrics methodMetrics = metricsService.getMethodMetrics("Interface", "method");
        System.out.println(JsonUtils.getJson().toJson(methodMetrics));
    }

}
