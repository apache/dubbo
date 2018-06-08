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
package com.alibaba.dubbo.config.spring.context.properties;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

/**
 * Abstract {@link DubboConfigBinder} implementation
 */
public abstract class AbstractDubboConfigBinder implements DubboConfigBinder {

    private Iterable<PropertySource<?>> propertySources;

    private boolean ignoreUnknownFields = true;

    private boolean ignoreInvalidFields = false;

    /**
     * Get multiple {@link PropertySource propertySources}
     *
     * @return multiple {@link PropertySource propertySources}
     */
    protected Iterable<PropertySource<?>> getPropertySources() {
        return propertySources;
    }

    public boolean isIgnoreUnknownFields() {
        return ignoreUnknownFields;
    }

    @Override
    public void setIgnoreUnknownFields(boolean ignoreUnknownFields) {
        this.ignoreUnknownFields = ignoreUnknownFields;
    }

    public boolean isIgnoreInvalidFields() {
        return ignoreInvalidFields;
    }

    @Override
    public void setIgnoreInvalidFields(boolean ignoreInvalidFields) {
        this.ignoreInvalidFields = ignoreInvalidFields;
    }

    @Override
    public final void setEnvironment(Environment environment) {

        if (environment instanceof ConfigurableEnvironment) {
            this.propertySources = ((ConfigurableEnvironment) environment).getPropertySources();
        }

    }
}
