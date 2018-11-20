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
package org.apache.dubbo.configcenter;

import org.apache.dubbo.common.config.AbstractConfiguration;
import org.apache.dubbo.common.config.Configuration;

/**
 *
 */
public class ConfigurationWrapper extends AbstractConfiguration {
    private String application;
    private String service;
    private String method;

    private Configuration delegate;

    public ConfigurationWrapper(String application, String service, String method, Configuration configuration) {
        this.application = application;
        this.service = service;
        this.method = method;
        this.delegate = configuration;
    }

    @Override
    protected Object getInternalProperty(String key) {
        Object value = delegate.getProperty(application + "." + key);
        if (value == null) {
            value = delegate.getProperty(service + "." + key);
        }
        if (value == null) {
            value = delegate.getProperty(service + "." + method + "." + key);
        }
        if (value == null) {
            value = delegate.getProperty(key);
        }
        return value;
    }
}
