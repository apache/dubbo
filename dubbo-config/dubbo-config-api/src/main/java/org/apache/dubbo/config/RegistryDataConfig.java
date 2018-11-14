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

    private boolean simpleProviderUrl;
    private String extraProviderUrlParamKeys;

    private boolean simpleConsumerUrl;
    private String extraConsumerUrlParamKeys;

    public Map<String, String> transferToMap() {
        Map<String, String> map = new HashMap<String, String>(4);
        if (simpleProviderUrl) {
            map.put(Constants.SIMPLE_PROVIDER_URL_KEY, Boolean.TRUE.toString());
            if (StringUtils.isNotEmpty(extraProviderUrlParamKeys)) {
                map.put(Constants.EXTRA_PROVIDER_URL_PARAM_KEYS_KEY, extraProviderUrlParamKeys.trim());
            }
        }
        if (simpleConsumerUrl) {
            map.put(Constants.SIMPLE_CONSUMER_URL_KEY, Boolean.TRUE.toString());
            if (StringUtils.isNotEmpty(extraConsumerUrlParamKeys)) {
                map.put(Constants.EXTRA_CONSUMER_URL_PARAM_KEYS_KEY, extraConsumerUrlParamKeys.trim());
            }
        }

        return map;
    }


    public boolean isSimpleProviderUrl() {
        return simpleProviderUrl;
    }

    public void setSimpleProviderUrl(boolean simpleProviderUrl) {
        this.simpleProviderUrl = simpleProviderUrl;
    }

    public String getExtraProviderUrlParamKeys() {
        return extraProviderUrlParamKeys;
    }

    public void setExtraProviderUrlParamKeys(String extraProviderUrlParamKeys) {
        this.extraProviderUrlParamKeys = extraProviderUrlParamKeys;
    }

    public boolean isSimpleConsumerUrl() {
        return simpleConsumerUrl;
    }

    public void setSimpleConsumerUrl(boolean simpleConsumerUrl) {
        this.simpleConsumerUrl = simpleConsumerUrl;
    }

    public String getExtraConsumerUrlParamKeys() {
        return extraConsumerUrlParamKeys;
    }

    public void setExtraConsumerUrlParamKeys(String extraConsumerUrlParamKeys) {
        this.extraConsumerUrlParamKeys = extraConsumerUrlParamKeys;
    }
}
