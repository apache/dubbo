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
package org.apache.dubbo.registry.consul;

import org.apache.dubbo.common.URL;

import static org.apache.dubbo.common.utils.StringUtils.isBlank;

/**
 * The enumeration for the Consul's parameters on the {@link URL}
 *
 * @see URL#getParameters()
 * @since 2.7.8
 */
public enum ConsulParameter {

    ACL_TOKEN,

    TAGS,

    INSTANCE_ZONE,

    DEFAULT_ZONE_METADATA_NAME("zone"),

    INSTANCE_GROUP,

    CONSISTENCY_MODE,

    ;

    private final String name;

    private final String defaultValue;

    ConsulParameter() {
        this(null);
    }

    ConsulParameter(String defaultValue) {
        this(null, defaultValue);
    }

    ConsulParameter(String name, String defaultValue) {
        this.name = isBlank(name) ? defaultName() : name;
        this.defaultValue = defaultValue;
    }

    private String defaultName() {
        return name().toLowerCase().replace('_', '-');
    }

    /**
     * The parameter value from the specified registry {@link URL}
     *
     * @param registryURL the specified registry {@link URL}
     * @return <code>defaultValue</code> if not found
     */
    public String getValue(URL registryURL) {
        return registryURL.getParameter(name, defaultValue);
    }

    /**
     * The parameter value from the specified registry {@link URL}
     *
     * @param registryURL  the specified registry {@link URL}
     * @param valueType    the type of parameter value
     * @param defaultValue the default value if parameter is absent
     * @return <code>defaultValue</code> if not found
     */
    public <T> T getValue(URL registryURL, Class<T> valueType, T defaultValue) {
        return registryURL.getParameter(name, valueType, defaultValue);
    }
}
