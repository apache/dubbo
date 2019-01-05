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

import org.apache.dubbo.common.utils.StringUtils;

/**
 * This is an abstraction specially customized for the sequence Dubbo retrieves properties.
 */
public abstract class AbstractPrefixConfiguration implements Configuration {
    protected String id;
    protected String prefix;

    public AbstractPrefixConfiguration(String prefix, String id) {
        super();
        if (StringUtils.isNotEmpty(prefix) && !prefix.endsWith(".")) {
            this.prefix = prefix + ".";
        } else {
            this.prefix = prefix;
        }
        this.id = id;
    }

    @Override
    public Object getProperty(String key, Object defaultValue) {
        Object value = null;
        if (StringUtils.isNotEmpty(prefix) && StringUtils.isNotEmpty(id)) {
            value = getInternalProperty(prefix + id + "." + key);
        }
        if (value == null && StringUtils.isNotEmpty(prefix)) {
            value = getInternalProperty(prefix + key);
        }

        if (value == null) {
            value = getInternalProperty(key);
        }
        return value != null ? value : defaultValue;
    }
}
