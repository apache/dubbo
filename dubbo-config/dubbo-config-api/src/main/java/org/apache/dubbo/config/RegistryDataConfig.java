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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 2018/10/31
 */
public class RegistryDataConfig extends AbstractConfig {

    private Boolean simpleProviderConfig;
    private String extraProviderKeys;

    private Boolean simpleConsumerConfig;
    private String extraConsumerKeys;

    Map<String, String> transferToMap() {
        Map<String, String> map = new HashMap<String, String>(4);
        if (simpleProviderConfig != null && simpleProviderConfig) {
            map.put(Constants.SIMPLE_PROVIDER_CONFIG_KEY, Boolean.TRUE.toString());
            if (StringUtils.isNotEmpty(extraProviderKeys)) {
                map.put(Constants.EXTRA_PROVIDER_CONFIG_KEYS_KEY, extraProviderKeys.trim());
            }
        }
        if (simpleConsumerConfig != null && simpleConsumerConfig) {
            map.put(Constants.SIMPLE_CONSUMER_CONFIG_KEY, Boolean.TRUE.toString());
            if (StringUtils.isNotEmpty(extraConsumerKeys)) {
                map.put(Constants.EXTRA_CONSUMER_CONFIG_KEYS_KEY, extraConsumerKeys.trim());
            }
        }

        return map;
    }

    public Boolean getSimpleProviderConfig() {
        return simpleProviderConfig;
    }

    public void setSimpleProviderConfig(Boolean simpleProviderConfig) {
        this.simpleProviderConfig = simpleProviderConfig;
    }

    public Boolean getSimpleConsumerConfig() {
        return simpleConsumerConfig;
    }

    public void setSimpleConsumerConfig(Boolean simpleConsumerConfig) {
        this.simpleConsumerConfig = simpleConsumerConfig;
    }

    public String getExtraProviderKeys() {
        return extraProviderKeys;
    }

    public void setExtraProviderKeys(String extraProviderKeys) {
        this.extraProviderKeys = extraProviderKeys;
    }


    public String getExtraConsumerKeys() {
        return extraConsumerKeys;
    }

    public void setExtraConsumerKeys(String extraConsumerKeys) {
        this.extraConsumerKeys = extraConsumerKeys;
    }
}
