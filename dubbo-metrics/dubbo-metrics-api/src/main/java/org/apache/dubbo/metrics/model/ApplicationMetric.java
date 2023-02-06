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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.apache.dubbo.common.constants.MetricsConstants.TAG_APPLICATION_NAME;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_HOSTNAME;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_IP;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHost;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHostName;

/**
 * Metric class for method.
 */
public class ApplicationMetric {
    private final String applicationName;

    public ApplicationMetric(String applicationName) {
        this.applicationName = applicationName;
    }

    public static Map<String, String> getTags(String applicationName) {
        Map<String, String> tags = new HashMap<>();
        tags.put(TAG_IP, getLocalHost());
        tags.put(TAG_HOSTNAME, getLocalHostName());
        tags.put(TAG_APPLICATION_NAME, applicationName);
        return tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApplicationMetric that = (ApplicationMetric) o;
        return Objects.equals(applicationName, that.applicationName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationName);
    }
}
