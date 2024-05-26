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
package org.apache.dubbo.config.deploy;

import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.metrics.utils.MetricsSupportUtil;

import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_PROMETHEUS;

class DefaultApplicationDeployerTest {

    @Test
    void isSupportPrometheus() {
        boolean supportPrometheus = MetricsSupportUtil.isSupportPrometheus();
        Assert.assertTrue(supportPrometheus, "MetricsSupportUtil.isSupportPrometheus() should return true");
    }

    @Test
    void isImportPrometheus() {
        MetricsConfig metricsConfig = new MetricsConfig();
        metricsConfig.setProtocol("prometheus");
        boolean importPrometheus =
                PROTOCOL_PROMETHEUS.equals(metricsConfig.getProtocol()) && !MetricsSupportUtil.isSupportPrometheus();
        Assert.assertTrue(!importPrometheus, " should return false");
    }
}
