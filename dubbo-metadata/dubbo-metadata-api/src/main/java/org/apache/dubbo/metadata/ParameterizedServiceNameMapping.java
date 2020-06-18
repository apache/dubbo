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
package org.apache.dubbo.metadata;

import org.apache.dubbo.common.URL;

import java.util.Set;

import static org.apache.dubbo.common.constants.RegistryConstants.SUBSCRIBED_SERVICE_NAMES_KEY;

/**
 * The parameterized implementation of {@link ServiceNameMapping}
 *
 * @see ReadOnlyServiceNameMapping
 * @since 2.7.8
 */
public class ParameterizedServiceNameMapping extends ReadOnlyServiceNameMapping {

    /**
     * The priority of {@link PropertiesFileServiceNameMapping}
     */
    static final int PRIORITY = MAX_PRIORITY + 99;

    @Override
    public Set<String> get(URL subscribedURL) {
        return getValue(subscribedURL.getParameter(SUBSCRIBED_SERVICE_NAMES_KEY));
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }
}
