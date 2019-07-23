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
package org.apache.dubbo.configcenter.mock;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.configcenter.ConfigurationListener;
import org.apache.dubbo.configcenter.DynamicConfiguration;

/**
 *
 */
public class MockDynamicConfiguration implements DynamicConfiguration {
    public MockDynamicConfiguration(URL url) {
    }

    @Override
    public Object getInternalProperty(String key) {
        return null;
    }

    @Override
    public void addListener(String key, String group, ConfigurationListener listener) {

    }

    @Override
    public void removeListener(String key, String group, ConfigurationListener listener) {

    }

    @Override
    public String getRule(String key, String group, long timeout) throws IllegalStateException {
        return null;
    }

    @Override
    public String getProperties(String key, String group, long timeout) throws IllegalStateException {
        return null;
    }
}
