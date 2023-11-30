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
package org.apache.dubbo.registry.integration;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.EXTRA_KEYS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.registry.Constants.SIMPLIFIED_KEY;
import static org.apache.dubbo.registry.integration.RegistryProtocol.DEFAULT_REGISTER_PROVIDER_KEYS;

public class SimplifiedRegistryParameterCustomizer implements RegistryParameterCustomizer {

    @Override
    public Map<String, String> getExtraParameter(URL providerUrl, URL registryUrl) {
        return null;
    }

    @Override
    public String[] parametersIncluded(URL providerUrl, URL registryUrl) {
        if (!registryUrl.getParameter(SIMPLIFIED_KEY, false)) {
            return new String[0];
        }
        String extraKeys = registryUrl.getParameter(EXTRA_KEYS_KEY, "");
        // if path is not the same as interface name then we should keep INTERFACE_KEY,
        // otherwise, the registry structure of zookeeper would be '/dubbo/path/providers',
        // but what we expect is '/dubbo/interface/providers'
        if (!providerUrl.getPath().equals(providerUrl.getParameter(INTERFACE_KEY))) {
            if (StringUtils.isNotEmpty(extraKeys)) {
                extraKeys += ",";
            }
            extraKeys += INTERFACE_KEY;
        }
        return Stream.concat(
                        Arrays.stream(DEFAULT_REGISTER_PROVIDER_KEYS),
                        Arrays.stream(COMMA_SPLIT_PATTERN.split(extraKeys)))
                .toArray(String[]::new);
    }

    @Override
    public String[] parametersExcluded(URL providerUrl, URL registryUrl) {
        return new String[0];
    }

    @Override
    public String[] prefixesIncluded(URL providerUrl, URL registryUrl) {
        if (!registryUrl.getParameter(SIMPLIFIED_KEY, false)) {
            return new String[0];
        }
        return new String[] {METHODS_KEY};
    }

    @Override
    public String[] prefixesExcluded(URL providerUrl, URL registryUrl) {
        return new String[0];
    }
}
