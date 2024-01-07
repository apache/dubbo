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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta;

import org.apache.dubbo.common.utils.StringUtils;

import java.util.Arrays;

public class NamedValueMeta {

    private final String name;
    private final boolean required;
    private final String defaultValue;
    private Class<?> type;
    private Class<?>[] nestedTypes;
    private ParameterMeta parameterMeta;

    public NamedValueMeta(String name, boolean required, String defaultValue) {
        this.name = name;
        this.required = required;
        this.defaultValue = defaultValue;
    }

    public String name() {
        return StringUtils.isEmpty(name) ? parameterMeta.getName() : name;
    }

    public boolean required() {
        return required;
    }

    public String defaultValue() {
        return defaultValue;
    }

    public Class<?> type() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public Class<?> nestedType() {
        return nestedTypes == null ? null : nestedTypes[0];
    }

    public Class<?> nestedType(int index) {
        return nestedTypes == null || nestedTypes.length <= index ? null : nestedTypes[index];
    }

    public Class<?>[] nestedTypes() {
        return nestedTypes;
    }

    public void setNestedTypes(Class<?>[] nestedTypes) {
        this.nestedTypes = nestedTypes;
    }

    public ParameterMeta parameterMeta() {
        return parameterMeta;
    }

    public void setParameterMeta(ParameterMeta parameterMeta) {
        this.parameterMeta = parameterMeta;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("NamedValueMeta{name='");
        sb.append(name).append('\'');
        if (required) {
            sb.append(", required=true");
        }
        if (defaultValue != null) {
            sb.append(", defaultValue='").append(defaultValue).append('\'');
        }
        if (type != null) {
            sb.append(", type=").append(type);
        }
        if (nestedTypes != null) {
            sb.append(", nestedTypes=").append(Arrays.toString(nestedTypes));
        }
        sb.append('}');
        return sb.toString();
    }
}
