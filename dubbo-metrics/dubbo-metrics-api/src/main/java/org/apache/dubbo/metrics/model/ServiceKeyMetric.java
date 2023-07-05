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

import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;

/**
 * Metric class for service.
 */
public class ServiceKeyMetric extends ApplicationMetric {
    private final String serviceKey;

    public ServiceKeyMetric(ApplicationModel applicationModel, String serviceKey) {
        super(applicationModel);
        this.serviceKey = serviceKey;
    }

    @Override
    public Map<String, String> getTags() {
        return MetricsSupport.serviceTags(getApplicationModel(), serviceKey);
    }

    public String getServiceKey() {
        return serviceKey;
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

        if (!getApplicationName().equals(that.getApplicationName())) {
            return false;
        }
        return serviceKey.equals(that.serviceKey);
    }

    @Override
    public int hashCode() {
        int result = getApplicationName().hashCode();
        result = 31 * result + serviceKey.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ServiceKeyMetric{" +
                "applicationName='" + getApplicationName() + '\'' +
                ", serviceKey='" + serviceKey + '\'' +
                '}';
    }
}
