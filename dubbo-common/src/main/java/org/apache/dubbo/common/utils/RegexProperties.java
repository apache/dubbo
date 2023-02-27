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
package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.constants.CommonConstants;

import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Regex matching of keys is supported.
 */
public class RegexProperties extends Properties {

    @Override
    public String getProperty(String key) {
        String value = super.getProperty(key);
        if (value != null) {
            return value;
        }

        // Sort the keys to solve the problem of matching priority.
        List<String> sortedKeyList = keySet()
                .stream()
                .map(k -> (String) k)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        String keyPattern = sortedKeyList
                .stream()
                .filter(k -> {
                    String matchingKey = k;
                    if(matchingKey.startsWith(CommonConstants.ANY_VALUE)){
                        matchingKey = CommonConstants.HIDE_KEY_PREFIX + matchingKey;
                    }
                    return Pattern.matches(matchingKey, key);
                })
                .findFirst()
                .orElse(null);

        return keyPattern == null ? null : super.getProperty(keyPattern);
    }
}
