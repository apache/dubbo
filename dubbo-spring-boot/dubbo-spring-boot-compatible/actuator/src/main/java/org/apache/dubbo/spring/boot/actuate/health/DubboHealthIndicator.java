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
package org.apache.dubbo.spring.boot.actuate.health;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.status.StatusChecker;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;

/**
 * Dubbo {@link HealthIndicator}
 *
 * @see HealthIndicator
 * @since 2.7.0
 */
public class DubboHealthIndicator extends AbstractHealthIndicator {

    @Autowired
    private DubboHealthIndicatorProperties dubboHealthIndicatorProperties;

    @Autowired(required = false)
    private Map<String, ProtocolConfig> protocolConfigs = Collections.emptyMap();

    @Autowired(required = false)
    private Map<String, ProviderConfig> providerConfigs = Collections.emptyMap();

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {

        ExtensionLoader<StatusChecker> extensionLoader = getExtensionLoader(StatusChecker.class);

        Map<String, String> statusCheckerNamesMap = resolveStatusCheckerNamesMap();

        boolean hasError = false;

        boolean hasUnknown = false;

        // Up first
        builder.up();

        for (Map.Entry<String, String> entry : statusCheckerNamesMap.entrySet()) {

            String statusCheckerName = entry.getKey();

            String source = entry.getValue();

            StatusChecker checker = extensionLoader.getExtension(statusCheckerName);

            org.apache.dubbo.common.status.Status status = checker.check();

            org.apache.dubbo.common.status.Status.Level level = status.getLevel();

            if (!hasError && level.equals(org.apache.dubbo.common.status.Status.Level.ERROR)) {
                hasError = true;
                builder.down();
            }

            if (!hasError && !hasUnknown && level.equals(org.apache.dubbo.common.status.Status.Level.UNKNOWN)) {
                hasUnknown = true;
                builder.unknown();
            }

            Map<String, Object> detail = new LinkedHashMap<>();

            detail.put("source", source);
            detail.put("status", status);

            builder.withDetail(statusCheckerName, detail);

        }


    }

    /**
     * Resolves the map of {@link StatusChecker}'s name and its' source.
     *
     * @return non-null {@link Map}
     */
    protected Map<String, String> resolveStatusCheckerNamesMap() {

        Map<String, String> statusCheckerNamesMap = new LinkedHashMap<>();

        statusCheckerNamesMap.putAll(resolveStatusCheckerNamesMapFromDubboHealthIndicatorProperties());

        statusCheckerNamesMap.putAll(resolveStatusCheckerNamesMapFromProtocolConfigs());

        statusCheckerNamesMap.putAll(resolveStatusCheckerNamesMapFromProviderConfig());

        return statusCheckerNamesMap;

    }

    private Map<String, String> resolveStatusCheckerNamesMapFromDubboHealthIndicatorProperties() {

        DubboHealthIndicatorProperties.Status status =
                dubboHealthIndicatorProperties.getStatus();

        Map<String, String> statusCheckerNamesMap = new LinkedHashMap<>();

        for (String statusName : status.getDefaults()) {

            statusCheckerNamesMap.put(statusName, DubboHealthIndicatorProperties.PREFIX + ".status.defaults");

        }

        for (String statusName : status.getExtras()) {

            statusCheckerNamesMap.put(statusName, DubboHealthIndicatorProperties.PREFIX + ".status.extras");

        }

        return statusCheckerNamesMap;

    }


    private Map<String, String> resolveStatusCheckerNamesMapFromProtocolConfigs() {

        Map<String, String> statusCheckerNamesMap = new LinkedHashMap<>();

        for (Map.Entry<String, ProtocolConfig> entry : protocolConfigs.entrySet()) {

            String beanName = entry.getKey();

            ProtocolConfig protocolConfig = entry.getValue();

            Set<String> statusCheckerNames = getStatusCheckerNames(protocolConfig);

            for (String statusCheckerName : statusCheckerNames) {

                String source = buildSource(beanName, protocolConfig);

                statusCheckerNamesMap.put(statusCheckerName, source);

            }

        }

        return statusCheckerNamesMap;

    }

    private Map<String, String> resolveStatusCheckerNamesMapFromProviderConfig() {

        Map<String, String> statusCheckerNamesMap = new LinkedHashMap<>();

        for (Map.Entry<String, ProviderConfig> entry : providerConfigs.entrySet()) {

            String beanName = entry.getKey();

            ProviderConfig providerConfig = entry.getValue();

            Set<String> statusCheckerNames = getStatusCheckerNames(providerConfig);

            for (String statusCheckerName : statusCheckerNames) {

                String source = buildSource(beanName, providerConfig);

                statusCheckerNamesMap.put(statusCheckerName, source);

            }

        }

        return statusCheckerNamesMap;

    }

    private Set<String> getStatusCheckerNames(ProtocolConfig protocolConfig) {
        String status = protocolConfig.getStatus();
        return StringUtils.commaDelimitedListToSet(status);
    }

    private Set<String> getStatusCheckerNames(ProviderConfig providerConfig) {
        String status = providerConfig.getStatus();
        return StringUtils.commaDelimitedListToSet(status);
    }

    private String buildSource(String beanName, Object bean) {
        return beanName + "@" + bean.getClass().getSimpleName() + ".getStatus()";
    }

}
