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

package org.apache.dubbo.common.metrics.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.apache.dubbo.common.constants.MetricsConstants.TAG_GROUP_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_INTERFACE_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_METHOD_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_PARAMETER_TYPES_DESC;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_VERSION_KEY;

public class MethodMetric {
    private String interfaceName;
    private String methodName;
    private String parameterTypesDesc;
    private String group;
    private String version;

    public MethodMetric() {

    }

    public MethodMetric(String interfaceName, String methodName, String parameterTypesDesc, String group, String version) {
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.parameterTypesDesc = parameterTypesDesc;
        this.group = group;
        this.version = version;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getParameterTypesDesc() {
        return parameterTypesDesc;
    }

    public void setParameterTypesDesc(String parameterTypesDesc) {
        this.parameterTypesDesc = parameterTypesDesc;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put(TAG_INTERFACE_KEY, interfaceName);
        tags.put(TAG_METHOD_KEY, methodName);
        tags.put(TAG_PARAMETER_TYPES_DESC, parameterTypesDesc);
        tags.put(TAG_GROUP_KEY, group);
        tags.put(TAG_VERSION_KEY, version);
        return tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodMetric that = (MethodMetric) o;
        return Objects.equals(interfaceName, that.interfaceName) && Objects.equals(methodName, that.methodName)
            && Objects.equals(parameterTypesDesc, that.parameterTypesDesc) && Objects.equals(group, that.group) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interfaceName, methodName, parameterTypesDesc, group, version);
    }

    @Override
    public String toString() {
        return "MethodMetric{" +
            "interfaceName='" + interfaceName + '\'' +
            ", methodName='" + methodName + '\'' +
            ", parameterTypesDesc='" + parameterTypesDesc + '\'' +
            ", group='" + group + '\'' +
            ", version='" + version + '\'' +
            '}';
    }
}
