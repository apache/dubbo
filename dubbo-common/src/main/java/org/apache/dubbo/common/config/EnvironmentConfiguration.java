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

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Configuration from system environment
 */
public class EnvironmentConfiguration implements Configuration {

    private final Map<String, String> envMap;

    @Override
    public Object getInternalProperty(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        String value = envMap.get(key);
        if (value != null) {
            return value;
        }
        for (String candidateKey : generateCandidateEnvironmentKeys(key)) {
            value = envMap.get(candidateKey);
            if (value != null) {
                return value;
            }
        }

        String osStyleKey = StringUtils.toOSStyleKey(key);
        value = envMap.get(osStyleKey);
        return value;
    }

    private Set<String> generateCandidateEnvironmentKeys(String originalKey) {
        Set<String> candidates = new LinkedHashSet<>();

        // Dots and hyphens to underscores, uppercase
        String normalizedKey = originalKey
                .replace(CommonConstants.DOT_SEPARATOR, CommonConstants.UNDERLINE_SEPARATOR)
                .replace(CommonConstants.PROPERTIES_CHAR_SEPARATOR, CommonConstants.UNDERLINE_SEPARATOR);
        candidates.add(normalizedKey.toUpperCase(Locale.ROOT));

        // Dots to underscores, hyphens removed, uppercase (Spring Boot style)
        String springLikeNoHyphens = originalKey
                .replace(CommonConstants.DOT_SEPARATOR, CommonConstants.UNDERLINE_SEPARATOR)
                .replace(CommonConstants.PROPERTIES_CHAR_SEPARATOR, "")
                .toUpperCase(Locale.ROOT);
        candidates.add(springLikeNoHyphens);

        // Dots to underscores, hyphens preserved, uppercase
        String dotsToUnderscoresUpper = originalKey
                .replace(CommonConstants.DOT_SEPARATOR, CommonConstants.UNDERLINE_SEPARATOR)
                .toUpperCase(Locale.ROOT);
        candidates.add(dotsToUnderscoresUpper);

        // Dots and hyphens to underscores, lowercase
        candidates.add(normalizedKey);

        return candidates;
    }

    public Map<String, String> getProperties() {
        return getenv();
    }

    // Adapt to System api, design for unit test
    public EnvironmentConfiguration() {
        this.envMap = System.getenv();
    }

    public EnvironmentConfiguration(Map<String, String> externalEnvMap) {
        this.envMap = externalEnvMap;
    }

    protected Map<String, String> getenv() {
        return System.getenv();
    }
}
