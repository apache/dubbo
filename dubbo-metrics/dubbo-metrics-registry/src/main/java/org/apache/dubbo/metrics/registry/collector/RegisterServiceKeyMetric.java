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

package org.apache.dubbo.metrics.registry.collector;

import org.apache.dubbo.common.constants.RegistryConstants;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;
import java.util.Objects;

/**
 * Metric class for service.
 */
public class RegisterServiceKeyMetric extends RegisterAppKeyMetric {
    private final String interfaceName;

    public RegisterServiceKeyMetric(ApplicationModel applicationModel, String registryClusterName, String interfaceName) {
        super(applicationModel, registryClusterName);
        this.interfaceName = interfaceName;
    }

    @Override
    public Map<String, String> getTags() {
        Map<String, String> map = super.getTags();
        map.put(RegistryConstants.REGISTRY_CLUSTER_KEY, interfaceName);
        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RegisterServiceKeyMetric that = (RegisterServiceKeyMetric) o;
        return interfaceName.equals(that.interfaceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), interfaceName);
    }
}
