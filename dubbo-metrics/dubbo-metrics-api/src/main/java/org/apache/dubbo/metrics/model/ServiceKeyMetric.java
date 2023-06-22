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
import java.util.Objects;

/**
 * Metric class for service.
 */
public class ServiceKeyMetric extends ApplicationMetric {
    private final String interfaceName;

    public ServiceKeyMetric(ApplicationModel applicationModel, String serviceKey) {
        super(applicationModel);
        this.interfaceName = serviceKey;
    }

    @Override
    public Map<String, String> getTags() {
        return MetricsSupport.serviceTags(getApplicationModel(), interfaceName, getExtraInfo());
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceKeyMetric)) return false;
        ServiceKeyMetric that = (ServiceKeyMetric) o;
        return interfaceName.equals(that.interfaceName) && Objects.equals(extraInfo, that.extraInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interfaceName, extraInfo);
    }
}
