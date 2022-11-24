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

package org.apache.dubbo.metrics.prometheus;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.metrics.MetricsReporter;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PrometheusMetricsReporterFactoryTest {

    @Test
    void test() {
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        PrometheusMetricsReporterFactory factory = new PrometheusMetricsReporterFactory(applicationModel);
        MetricsReporter reporter = factory.createMetricsReporter(URL.valueOf("prometheus://localhost:9090/"));

        Assertions.assertTrue(reporter instanceof PrometheusMetricsReporter);
    }
}
