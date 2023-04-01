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

import static org.apache.dubbo.common.constants.MetricsConstants.TAG_APPLICATION_NAME;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_HOSTNAME;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_INTERFACE_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_IP;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHost;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHostName;

/**
 * Metric class for service.
 */
public class ServiceKeyMetric implements Metric {
    private final String applicationName;
    private final String serviceKey;

    public ServiceKeyMetric(String applicationName, String serviceKey) {
        this.applicationName = applicationName;
        this.serviceKey = serviceKey;
    }

    @Override
    public Map<String, String> getTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put(TAG_IP, getLocalHost());
        tags.put(TAG_HOSTNAME, getLocalHostName());
        tags.put(TAG_APPLICATION_NAME, applicationName);
        tags.put(TAG_INTERFACE_KEY, serviceKey);
        return tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ServiceKeyMetric that = (ServiceKeyMetric) o;

        if (!applicationName.equals(that.applicationName)) {
            return false;
        }
        return serviceKey.equals(that.serviceKey);
    }

    @Override
    public int hashCode() {
        int result = applicationName.hashCode();
        result = 31 * result + serviceKey.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ServiceKeyMetric{" +
            "applicationName='" + applicationName + '\'' +
            ", serviceKey='" + serviceKey + '\'' +
            '}';
    }
}
