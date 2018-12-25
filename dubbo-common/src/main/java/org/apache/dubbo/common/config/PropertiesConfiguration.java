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
package org.apache.dubbo.common.config;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConfigUtils;

/**
 * Configuration from system properties and dubbo.properties
 */
public class PropertiesConfiguration extends AbstractPrefixConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesConfiguration.class);

    public PropertiesConfiguration(String prefix, String id) {
        super(prefix, id);
    }

    public PropertiesConfiguration() {
        this(null, null);
    }

    @Override
    protected Object getInternalProperty(String key) {
        return ConfigUtils.getProperty(key);
    }
}
