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
package org.apache.dubbo.rpc.cluster.configurator.parser.model;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.PojoUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CLUSTER_FAILED_RECEIVE_RULE;

/**
 *
 */
public class ConfigItem {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ConfigItem.class);

    public static final String GENERAL_TYPE = "general";
    public static final String WEIGHT_TYPE = "weight";
    public static final String BALANCING_TYPE = "balancing";
    public static final String DISABLED_TYPE = "disabled";
    public static final String CONFIG_ITEM_TYPE = "type";
    public static final String ENABLED_KEY = "enabled";
    public static final String ADDRESSES_KEY = "addresses";
    public static final String PROVIDER_ADDRESSES_KEY = "providerAddresses";
    public static final String SERVICES_KEY = "services";
    public static final String APPLICATIONS_KEY = "applications";
    public static final String PARAMETERS_KEY = "parameters";
    public static final String MATCH_KEY = "match";
    public static final String SIDE_KEY = "side";

    private String type;
    private Boolean enabled;
    private List<String> addresses;
    private List<String> providerAddresses;
    private List<String> services;
    private List<String> applications;
    private Map<String, String> parameters;
    private ConditionMatch match;
    private String side;

    @SuppressWarnings("unchecked")
    public static ConfigItem parseFromMap(Map<String, Object> map) {
        ConfigItem configItem = new ConfigItem();
        configItem.setType((String) map.get(CONFIG_ITEM_TYPE));

        Object enabled = map.get(ENABLED_KEY);
        if (enabled != null) {
            configItem.setEnabled(Boolean.parseBoolean(enabled.toString()));
        }

        Object addresses = map.get(ADDRESSES_KEY);
        if (addresses != null && List.class.isAssignableFrom(addresses.getClass())) {
            configItem.setAddresses(((List<Object>) addresses).stream()
                    .map(String::valueOf).collect(Collectors.toList()));
        }

        Object providerAddresses = map.get(PROVIDER_ADDRESSES_KEY);
        if (providerAddresses != null && List.class.isAssignableFrom(providerAddresses.getClass())) {
            configItem.setProviderAddresses(((List<Object>) providerAddresses).stream()
                    .map(String::valueOf).collect(Collectors.toList()));
        }

        Object services = map.get(SERVICES_KEY);
        if (services != null && List.class.isAssignableFrom(services.getClass())) {
            configItem.setServices(((List<Object>) services).stream()
                    .map(String::valueOf).collect(Collectors.toList()));
        }

        Object applications = map.get(APPLICATIONS_KEY);
        if (applications != null && List.class.isAssignableFrom(applications.getClass())) {
            configItem.setApplications(((List<Object>) applications).stream()
                .map(String::valueOf).collect(Collectors.toList()));
        }

        Object parameters = map.get(PARAMETERS_KEY);
        if (parameters != null && Map.class.isAssignableFrom(parameters.getClass())) {
            configItem.setParameters(((Map<String, Object>) parameters).entrySet()
                .stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toString())));
        }

        try {
            Object match = map.get(MATCH_KEY);
            if (match != null && Map.class.isAssignableFrom(match.getClass())) {
                configItem.setMatch(PojoUtils.mapToPojo((Map<String, Object>) match, ConditionMatch.class));
            }
        } catch (Throwable t) {
            logger.error(CLUSTER_FAILED_RECEIVE_RULE, " Failed to parse dynamic configuration rule", String.valueOf(map.get(MATCH_KEY)), "Error occurred when parsing rule component.", t);
        }

        configItem.setSide((String) map.get(SIDE_KEY));
        return configItem;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<String> addresses) {
        this.addresses = addresses;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }

    public List<String> getApplications() {
        return applications;
    }

    public void setApplications(List<String> applications) {
        this.applications = applications;
    }

    public List<String> getProviderAddresses() {
        return providerAddresses;
    }

    public void setProviderAddresses(List<String> providerAddresses) {
        this.providerAddresses = providerAddresses;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public ConditionMatch getMatch() {
        return match;
    }

    public void setMatch(ConditionMatch match) {
        this.match = match;
    }
}
