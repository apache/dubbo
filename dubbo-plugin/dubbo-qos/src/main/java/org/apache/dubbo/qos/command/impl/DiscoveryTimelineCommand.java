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
package org.apache.dubbo.qos.command.impl;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.qos.api.BaseCommand;
import org.apache.dubbo.qos.api.Cmd;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.support.RegistryManager;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.FrameworkServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_PARAMETER_FORMAT_ERROR;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_FETCH_INSTANCE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_LOAD_METADATA;

@Cmd(
        name = "discovery-timeline",
        summary = "Show service discovery timeline",
        example = {
            "discovery-timeline",
            "discovery-timeline service=com.example.Service",
            "discovery-timeline registry=zookeeper://localhost:2181",
            "discovery-timeline page=2",
            "discovery-timeline limit=5"
        })
public class DiscoveryTimelineCommand implements BaseCommand {

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(DiscoveryTimelineCommand.class);
    private final FrameworkModel frameworkModel;
    private static final int DEFAULT_LIMIT = 10;
    private static final Map<String, Long> globalProviderRegistrationTimes = new HashMap<>();
    private static final String TIMESTAMP_KEY = "timestamp";

    public DiscoveryTimelineCommand(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        logger.debug("DiscoveryTimelineCommand started");

        try {
            ApplicationModel applicationModel = frameworkModel.defaultApplication();
            if (applicationModel == null) {
                throw new IllegalStateException("No ApplicationModel available");
            }

            RegistryManager registryManager = applicationModel.getBeanFactory().getBean(RegistryManager.class);
            if (registryManager == null) {
                logger.warn(REGISTRY_FAILED_FETCH_INSTANCE, "", "", "RegistryManager not available");
                return "Error: RegistryManager not available. Check configuration.";
            }

            List<ServiceDiscovery> serviceDiscoveries = registryManager.getServiceDiscoveries();
            if (serviceDiscoveries == null || serviceDiscoveries.isEmpty()) {
                logger.warn(REGISTRY_FAILED_LOAD_METADATA, "", "", "No ServiceDiscovery found");
                return "Error: No ServiceDiscovery instances found.";
            }

            String filterServiceName = null;
            String filterRegistry = null;
            int page = 1;
            int limit = DEFAULT_LIMIT;
            if (args != null && args.length > 0) {
                for (String arg : args) {
                    if (arg == null) continue;
                    if (arg.startsWith("service=")) {
                        filterServiceName = arg.substring("service=".length());
                    } else if (arg.startsWith("registry=")) {
                        filterRegistry = arg.substring("registry=".length());
                    } else if (arg.startsWith("page=")) {
                        try {
                            page = Math.max(1, Integer.parseInt(arg.substring("page=".length())));
                        } catch (NumberFormatException e) {
                            logger.warn(CONFIG_PARAMETER_FORMAT_ERROR, "", "", "Invalid page number: " + arg, e);
                        }
                    } else if (arg.startsWith("limit=")) {
                        try {
                            limit = Math.max(1, Integer.parseInt(arg.substring("limit=".length())));
                        } catch (NumberFormatException e) {
                            logger.warn(CONFIG_PARAMETER_FORMAT_ERROR, "", "", "Invalid limit number: " + arg, e);
                        }
                    }
                }
            }

            final String finalFilterServiceName = filterServiceName;

            Map<ServiceDiscovery, CustomServiceInstancesChangedListener> listeners = new HashMap<>();
            Map<String, Long> providerRegistrationTimes = new HashMap<>(globalProviderRegistrationTimes);
            Map<ServiceDiscovery, Set<String>> registryServices = new HashMap<>();

            FrameworkServiceRepository serviceRepository = frameworkModel.getServiceRepository();
            Set<ProviderModel> uniqueProviderModels = new HashSet<>(serviceRepository.allProviderModels());
            List<ProviderModel> providerModels = uniqueProviderModels.stream()
                    .filter(model -> {
                        String serviceName = model.getServiceKey();
                        return serviceName != null
                                && (finalFilterServiceName == null || serviceName.contains(finalFilterServiceName));
                    })
                    .sorted((m1, m2) -> {
                        String key1 = m1.getServiceKey();
                        String key2 = m2.getServiceKey();
                        String numStr1 = key1.replaceAll("[^0-9]", "");
                        String numStr2 = key2.replaceAll("[^0-9]", "");
                        if (StringUtils.isEmpty(numStr1) || StringUtils.isEmpty(numStr2)) {
                            return key1.compareTo(key2);
                        }
                        try {
                            int num1 = Integer.parseInt(numStr1);
                            int num2 = Integer.parseInt(numStr2);
                            return Integer.compare(num1, num2);
                        } catch (NumberFormatException e) {
                            logger.warn(
                                    CONFIG_PARAMETER_FORMAT_ERROR,
                                    "",
                                    "",
                                    "Failed to parse numbers for sorting: " + numStr1 + " vs " + numStr2,
                                    e);
                            return key1.compareTo(key2);
                        }
                    })
                    .collect(Collectors.toList());
            logger.debug(
                    "Filtered and sorted provider models: {}",
                    providerModels.stream().map(ProviderModel::getServiceKey).collect(Collectors.toList()));

            boolean hasServices = false;
            for (ServiceDiscovery serviceDiscovery : serviceDiscoveries) {
                if (filterRegistry != null
                        && (serviceDiscovery.getUrl() == null
                                || !serviceDiscovery.getUrl().getAddress().contains(filterRegistry))) {
                    continue;
                }

                Set<String> serviceNames = new HashSet<>();
                ServiceInstance instance = serviceDiscovery.getLocalInstance();
                if (instance == null) {
                    logger.warn(
                            REGISTRY_FAILED_FETCH_INSTANCE,
                            "",
                            "",
                            "No local instance found for registry: " + serviceDiscovery.getUrl());
                    continue;
                }

                Map<String, String> metadata = instance.getMetadata();
                if (metadata == null || metadata.isEmpty()) {
                    logger.warn(
                            REGISTRY_FAILED_LOAD_METADATA,
                            "",
                            "",
                            "No metadata found for instance in registry: " + serviceDiscovery.getUrl());
                    metadata = new HashMap<>();
                }

                for (ProviderModel providerModel : providerModels) {
                    String serviceName = providerModel.getServiceKey();
                    if (serviceName != null) {
                        serviceNames.add(serviceName);
                        if (!providerRegistrationTimes.containsKey(serviceName)) {
                            String timestampStr =
                                    metadata.getOrDefault(TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
                            try {
                                providerRegistrationTimes.put(serviceName, Long.parseLong(timestampStr));
                                logger.debug("Set timestamp for {}: {}", serviceName, timestampStr);
                            } catch (NumberFormatException e) {
                                logger.warn(
                                        CONFIG_PARAMETER_FORMAT_ERROR,
                                        "",
                                        "",
                                        "Invalid timestamp format for service: " + serviceName + ", timestamp: "
                                                + timestampStr,
                                        e);
                                providerRegistrationTimes.put(serviceName, System.currentTimeMillis());
                            }
                        }
                    }
                }
                registryServices.put(serviceDiscovery, serviceNames);
                CustomServiceInstancesChangedListener listener = new CustomServiceInstancesChangedListener(
                        serviceNames, serviceDiscovery, providerRegistrationTimes);
                serviceDiscovery.addServiceInstancesChangedListener(listener);
                listeners.put(serviceDiscovery, listener);
                hasServices = true;
                logger.debug("Registered services for discovery {}: {}", serviceDiscovery.getUrl(), serviceNames);
            }

            for (Map.Entry<String, Long> entry : providerRegistrationTimes.entrySet()) {
                globalProviderRegistrationTimes.putIfAbsent(entry.getKey(), entry.getValue());
            }

            if (!hasServices) {
                return "Error: No services discovered.";
            }

            StringBuilder timeline = new StringBuilder("Discovery Timeline\n");
            timeline.append("------------------------------------------------------------\n");
            timeline.append(String.format("%-30s|%-30s%n", "Registry", "Last Refresh"));
            timeline.append("------------------------------------------------------------\n");

            for (ServiceDiscovery sd : serviceDiscoveries) {
                if (filterRegistry != null
                        && (sd.getUrl() == null || !sd.getUrl().getAddress().contains(filterRegistry))) {
                    continue;
                }
                String refreshTimeStr = "Unknown";
                ServiceInstance instance = sd.getLocalInstance();
                if (instance != null
                        && instance.getMetadata() != null
                        && instance.getMetadata().containsKey(TIMESTAMP_KEY)) {
                    String timestampStr = instance.getMetadata()
                            .getOrDefault(TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
                    try {
                        long registryTimestamp = Long.parseLong(timestampStr);
                        refreshTimeStr = new Date(registryTimestamp).toString();
                    } catch (NumberFormatException e) {
                        logger.warn(
                                CONFIG_PARAMETER_FORMAT_ERROR,
                                "",
                                "",
                                "Invalid timestamp format for registry: "
                                        + sd.getUrl().getAddress() + ", timestamp: " + timestampStr,
                                e);
                        refreshTimeStr = new Date(System.currentTimeMillis()).toString();
                    }
                }
                timeline.append(String.format("%-30s|%-30s%n", sd.getUrl().getAddress(), refreshTimeStr));
            }

            timeline.append("------------------------------------------------------------\n");
            timeline.append("Provider Services\n");

            int providerStart = (page - 1) * limit;
            int providerEnd = Math.min(providerStart + limit, providerModels.size());
            logger.debug(
                    "Pagination: page={}, limit={}, start={}, end={}, total providers={}",
                    page,
                    limit,
                    providerStart,
                    providerEnd,
                    providerModels.size());
            List<ProviderModel> paginatedProviders = providerModels.subList(
                    Math.min(providerStart, providerModels.size()), Math.min(providerEnd, providerModels.size()));
            logger.debug(
                    "Paginated providers: {}",
                    paginatedProviders.stream()
                            .map(ProviderModel::getServiceKey)
                            .collect(Collectors.toList()));

            for (ProviderModel providerModel : paginatedProviders) {
                String serviceName = providerModel.getServiceKey();
                Long lastRegistrationTime = providerRegistrationTimes.get(serviceName);
                String refreshTimeStr =
                        lastRegistrationTime != null ? new Date(lastRegistrationTime).toString() : "Unknown";
                timeline.append(String.format("%-30s|%-30s%n", "Discovered: " + serviceName, refreshTimeStr));
            }

            timeline.append("------------------------------------------------------------\n");
            String result = timeline.toString();
            logger.debug("Final output:\n{}", result);
            return result;

        } catch (Exception e) {
            logger.error(INTERNAL_ERROR, "", "", "Failed to generate discovery timeline", e);
            return "Error: " + e.getMessage();
        }
    }

    private static class CustomServiceInstancesChangedListener extends ServiceInstancesChangedListener {
        private final Set<String> serviceNames;
        private final Map<String, Long> serviceRegistrationTimes;
        private Set<String> previousInstances = new HashSet<>();

        public CustomServiceInstancesChangedListener(
                Set<String> serviceNames,
                ServiceDiscovery serviceDiscovery,
                Map<String, Long> serviceRegistrationTimes) {
            super(serviceNames, serviceDiscovery);
            this.serviceNames = serviceNames;
            this.serviceRegistrationTimes = serviceRegistrationTimes;
            initializeRegistrationTimes();
        }

        private void initializeRegistrationTimes() {
            long currentTime = System.currentTimeMillis();
            for (String serviceName : serviceNames) {
                if (!serviceRegistrationTimes.containsKey(serviceName)) {
                    serviceRegistrationTimes.put(serviceName, currentTime);
                    logger.debug(
                            "Initialized registration time for {}: {}", serviceName, new Date(currentTime).toString());
                }
            }
        }

        @Override
        public void onEvent(ServiceInstancesChangedEvent event) {
            super.onEvent(event);
            String serviceName = event.getServiceName();
            Set<String> currentInstances = event.getServiceInstances().stream()
                    .map(ServiceInstance::getServiceName)
                    .filter(name -> name != null)
                    .collect(Collectors.toSet());

            if (previousInstances.isEmpty() || !previousInstances.equals(currentInstances)) {
                serviceNames.add(serviceName);
                for (ServiceInstance instance : event.getServiceInstances()) {
                    String timestampStr = instance.getMetadata() != null
                            ? instance.getMetadata()
                                    .getOrDefault(TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()))
                            : String.valueOf(System.currentTimeMillis());
                    if (!serviceRegistrationTimes.containsKey(serviceName)) {
                        try {
                            serviceRegistrationTimes.put(serviceName, Long.parseLong(timestampStr));
                            logger.debug(
                                    "Updated timestamp for {}: {}",
                                    serviceName,
                                    new Date(Long.parseLong(timestampStr)).toString());
                        } catch (NumberFormatException e) {
                            logger.warn(
                                    CONFIG_PARAMETER_FORMAT_ERROR,
                                    "",
                                    "",
                                    "Invalid timestamp format for service: " + serviceName + ", timestamp: "
                                            + timestampStr,
                                    e);
                            serviceRegistrationTimes.put(serviceName, System.currentTimeMillis());
                        }
                    }
                }
                previousInstances = new HashSet<>(currentInstances);
            }
            logger.debug(
                    "Event received for service: {}, current instances: {}, registration times: {}",
                    serviceName,
                    currentInstances,
                    serviceRegistrationTimes);
        }
    }
}
