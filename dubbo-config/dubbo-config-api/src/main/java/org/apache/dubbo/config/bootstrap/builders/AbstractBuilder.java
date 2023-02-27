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
package org.apache.dubbo.config.bootstrap.builders;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.AbstractConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * AbstractBuilder
 *
 * @param <C> The type of {@link AbstractConfig Config}
 * @param <B> The type of {@link AbstractBuilder Builder}
 * @since 2.7
 */
public abstract class AbstractBuilder<C extends AbstractConfig, B extends AbstractBuilder> {
    /**
     * The config id
     */
    protected String id;

    public B id(String id) {
        this.id = id;
        return getThis();
    }

    protected abstract B getThis();

    protected static Map<String, String> appendParameter(Map<String, String> parameters, String key, String value) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        parameters.put(key, value);
        return parameters;
    }

    protected static Map<String, String> appendParameters(Map<String, String> parameters, Map<String, String> appendParameters) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        parameters.putAll(appendParameters);
        return parameters;
    }

    protected void build(C instance) {
        if (!StringUtils.isEmpty(id)) {
            instance.setId(id);
        }
    }

    /**
     * Build an instance of {@link AbstractConfig config}
     *
     * @return an instance of {@link AbstractConfig config}
     */
    public abstract C build();
}
