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
package org.apache.dubbo.config;

import org.apache.dubbo.common.utils.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A extension class of {@link AbstractConfig Dubbo base config} with parameters
 *
 * @see AbstractConfig
 * @since 2.7.7
 */
public abstract class AbstractParameterizedConfig extends AbstractConfig {

    /**
     * The customized parameters
     */
    private Map<String, String> parameters;

    public Map<String, String> getParameters() {
        if (parameters == null) {
            parameters = new LinkedHashMap<>();
        }
        return parameters;
    }

    public String getParameter(String name) {
        return getParameter(name, null);
    }

    public String getParameter(String name, String defaultValue) {
        return getParameters().getOrDefault(name, defaultValue);
    }

    public AbstractParameterizedConfig setParameter(String name, String value) {
        getParameters().put(name, value);
        return this;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public void updateParameters(Map<String, String> parameters) {
        if (CollectionUtils.isEmptyMap(parameters)) {
            return;
        }
        if (this.parameters == null) {
            this.parameters = parameters;
        } else {
            this.parameters.putAll(parameters);
        }
    }
}
