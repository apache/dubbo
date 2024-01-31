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
package org.apache.dubbo.metrics.model;

import org.apache.dubbo.common.Version;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.MetricsConstants.*;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHost;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHostName;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METADATA_GIT_COMMITID_METRIC;
import static org.junit.jupiter.api.Assertions.*;

class ApplicationMetricTest {

    @Test
    void getApplicationModel() {
        ApplicationMetric applicationMetric = new ApplicationMetric(ApplicationModel.defaultModel());
        Assertions.assertNotNull(applicationMetric.getApplicationModel());
    }

    @Test
    void getApplicationName() {
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        String mockMetrics = "MockMetrics";
        applicationModel
                .getApplicationConfigManager()
                .setApplication(new org.apache.dubbo.config.ApplicationConfig(mockMetrics));
        ApplicationMetric applicationMetric = new ApplicationMetric(applicationModel);
        Assertions.assertNotNull(applicationMetric);
        Assertions.assertEquals(mockMetrics, applicationMetric.getApplicationName());
    }

    @Test
    void getTags() {
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        String mockMetrics = "MockMetrics";
        applicationModel
                .getApplicationConfigManager()
                .setApplication(new org.apache.dubbo.config.ApplicationConfig(mockMetrics));
        ApplicationMetric applicationMetric = new ApplicationMetric(applicationModel);
        Map<String, String> tags = applicationMetric.getTags();
        Assertions.assertEquals(tags.get(TAG_IP), getLocalHost());
        Assertions.assertEquals(tags.get(TAG_HOSTNAME), getLocalHostName());
        Assertions.assertEquals(tags.get(TAG_APPLICATION_NAME), applicationModel.getApplicationName());
        Assertions.assertEquals(tags.get(METADATA_GIT_COMMITID_METRIC.getName()), Version.getLastCommitId());
    }

    @Test
    void gitTags() {
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        String mockMetrics = "MockMetrics";
        applicationModel
                .getApplicationConfigManager()
                .setApplication(new org.apache.dubbo.config.ApplicationConfig(mockMetrics));
        ApplicationMetric applicationMetric = new ApplicationMetric(applicationModel);
        Map<String, String> tags = applicationMetric.getTags();
        Assertions.assertEquals(tags.get(METADATA_GIT_COMMITID_METRIC.getName()), Version.getLastCommitId());
    }

    @Test
    void hostTags() {
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        String mockMetrics = "MockMetrics";
        applicationModel
                .getApplicationConfigManager()
                .setApplication(new org.apache.dubbo.config.ApplicationConfig(mockMetrics));
        ApplicationMetric applicationMetric = new ApplicationMetric(applicationModel);
        Map<String, String> tags = applicationMetric.getTags();
        Assertions.assertEquals(tags.get(TAG_IP), getLocalHost());
        Assertions.assertEquals(tags.get(TAG_HOSTNAME), getLocalHostName());
    }

    @Test
    void getExtraInfo() {}

    @Test
    void setExtraInfo() {}

    @Test
    void testEquals() {}

    @Test
    void testHashCode() {}
}
