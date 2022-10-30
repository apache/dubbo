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
package org.apache.dubbo.registry.eureka;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.event.EventDispatcher;
import org.apache.dubbo.registry.client.AbstractServiceDiscovery;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.config.ConfigurationManager;
import com.netflix.discovery.CacheRefreshedEvent;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.EurekaEvent;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.apache.dubbo.common.constants.RegistryConstants.SUBSCRIBED_SERVICE_NAMES_KEY;
import static org.apache.dubbo.event.EventDispatcher.getDefaultExtension;
import static org.apache.dubbo.registry.client.ServiceDiscoveryRegistry.parseServices;

/**
 * Eureka {@link ServiceDiscovery} implementation based on Eureka API
 */
public class EurekaServiceDiscovery extends AbstractServiceDiscovery {

    private final EventDispatcher eventDispatcher = getDefaultExtension();

    private ApplicationInfoManager applicationInfoManager;

    private EurekaClient eurekaClient;

    private Set<String> subscribedServices;

    /**
     * last apps hash code is used to identify the {@link Applications} is changed or not
     */
    private String lastAppsHashCode;

    @Override
    public void initialize(URL registryURL) throws Exception {
        Properties eurekaConfigProperties = buildEurekaConfigProperties(registryURL);
        initConfigurationManager(eurekaConfigProperties);
        initSubscribedServices(registryURL);
    }

    /**
     * Build the Properties whose {@link java.util.Map.Entry entries} are retrieved from
     * {@link URL#getParameters() the parameters of the specified URL}, which will be used in the Eureka's {@link ConfigurationManager}
     *
     * @param registryURL the {@link URL url} to connect Eureka
     * @return non-null
     */
    private Properties buildEurekaConfigProperties(URL registryURL) {
        Properties properties = new Properties();
        Map<String, String> parameters = registryURL.getParameters();
        setDefaultProperties(registryURL, properties);
        parameters.entrySet().stream()
                .filter(this::filterEurekaProperty)
                .forEach(propertyEntry -> {
                    properties.setProperty(propertyEntry.getKey(), propertyEntry.getValue());
                });
        return properties;
    }

    /**
     * Initialize {@link #subscribedServices} property
     *
     * @param registryURL the {@link URL url} to connect Eureka
     */
    private void initSubscribedServices(URL registryURL) {
        this.subscribedServices = parseServices(registryURL.getParameter(SUBSCRIBED_SERVICE_NAMES_KEY));
        ;
    }

    private boolean filterEurekaProperty(Map.Entry<String, String> propertyEntry) {
        String propertyName = propertyEntry.getKey();
        return propertyName.startsWith("eureka.");
    }

    private void setDefaultProperties(URL registryURL, Properties properties) {
        setDefaultServiceURL(registryURL, properties);
        setDefaultInitialInstanceInfoReplicationIntervalSeconds(properties);
    }

    private void setDefaultServiceURL(URL registryURL, Properties properties) {
        StringBuilder defaultServiceURLBuilder = new StringBuilder("http://")
                .append(registryURL.getHost())
                .append(":")
                .append(registryURL.getPort())
                .append("/eureka");
        properties.setProperty("eureka.serviceUrl.default", defaultServiceURLBuilder.toString());
    }

    /**
     * Set the default property for {@link EurekaClientConfig#getInitialInstanceInfoReplicationIntervalSeconds()}
     * which means do register immediately
     *
     * @param properties {@link Properties}
     */
    private void setDefaultInitialInstanceInfoReplicationIntervalSeconds(Properties properties) {
        properties.setProperty("eureka.appinfo.initial.replicate.time", "0");
    }

    /**
     * Initialize {@link ConfigurationManager}
     *
     * @param eurekaConfigProperties the Eureka's {@link ConfigurationManager}
     */
    private void initConfigurationManager(Properties eurekaConfigProperties) {
        ConfigurationManager.loadProperties(eurekaConfigProperties);
    }

    private void initApplicationInfoManager(ServiceInstance serviceInstance) {
        EurekaInstanceConfig eurekaInstanceConfig = buildEurekaInstanceConfig(serviceInstance);
        this.applicationInfoManager = new ApplicationInfoManager(eurekaInstanceConfig, (ApplicationInfoManager.OptionalArgs) null);
    }

    /**
     * Initialize {@link #eurekaClient} property
     *
     * @param serviceInstance {@link ServiceInstance}
     */
    private void initEurekaClient(ServiceInstance serviceInstance) {
        if (eurekaClient != null) {
            return;
        }
        initApplicationInfoManager(serviceInstance);
        EurekaClient eurekaClient = createEurekaClient();
        registerEurekaEventListener(eurekaClient);
        // set eurekaClient
        this.eurekaClient = eurekaClient;
    }

    private void registerEurekaEventListener(EurekaClient eurekaClient) {
        eurekaClient.registerEventListener(this::onEurekaEvent);
    }

    private void onEurekaEvent(EurekaEvent event) {
        if (event instanceof CacheRefreshedEvent) {
            onCacheRefreshedEvent(CacheRefreshedEvent.class.cast(event));
        }
    }

    private void onCacheRefreshedEvent(CacheRefreshedEvent event) {
        synchronized (this) { // Make sure thread-safe in async execution
            Applications applications = eurekaClient.getApplications();
            String appsHashCode = applications.getAppsHashCode();
            if (!Objects.equals(lastAppsHashCode, appsHashCode)) { // Changed
                // Dispatch Events
                dispatchServiceInstancesChangedEvent();
                lastAppsHashCode = appsHashCode; // update current result
            }
        }
    }

    private void dispatchServiceInstancesChangedEvent() {
        subscribedServices.forEach((serviceName) -> {
            eventDispatcher.dispatch(new ServiceInstancesChangedEvent(serviceName, getInstances(serviceName)));
        });
    }

    private EurekaClient createEurekaClient() {
        EurekaClientConfig eurekaClientConfig = new DefaultEurekaClientConfig();
        DiscoveryClient eurekaClient = new DiscoveryClient(applicationInfoManager, eurekaClientConfig);
        return eurekaClient;
    }

    @Override
    public void destroy() throws Exception {
        if (eurekaClient != null) {
            this.eurekaClient.shutdown();
        }
    }

    @Override
    public void doRegister(ServiceInstance serviceInstance) {
        initEurekaClient(serviceInstance);
        setInstanceStatus(InstanceInfo.InstanceStatus.UP);
    }

    private void setInstanceStatus(InstanceInfo.InstanceStatus status) {
        if (applicationInfoManager != null) {
            this.applicationInfoManager.setInstanceStatus(status);
        }
    }

    @Override
    public void doUpdate(ServiceInstance serviceInstance) {
        setInstanceStatus(serviceInstance.isHealthy() ? InstanceInfo.InstanceStatus.UP :
                InstanceInfo.InstanceStatus.UNKNOWN);
    }

    @Override
    public void unregister(ServiceInstance serviceInstance) throws RuntimeException {
        setInstanceStatus(InstanceInfo.InstanceStatus.OUT_OF_SERVICE);
    }

    @Override
    public Set<String> getServices() {
        Applications applications = this.eurekaClient.getApplications();
        if (applications == null) {
            return Collections.emptySet();
        }
        List<Application> registered = applications.getRegisteredApplications();
        Set<String> names = new LinkedHashSet<>();
        for (Application app : registered) {
            if (app.getInstances().isEmpty()) {
                continue;
            }
            names.add(app.getName().toLowerCase());
        }
        return names;
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws NullPointerException {
        Application application = this.eurekaClient.getApplication(serviceName);

        if (application == null) {
            return emptyList();
        }

        List<InstanceInfo> infos = application.getInstances();
        List<ServiceInstance> instances = new ArrayList<>();
        for (InstanceInfo info : infos) {
            instances.add(buildServiceInstance(info));
        }
        return instances;
    }

    private ServiceInstance buildServiceInstance(InstanceInfo instance) {
        DefaultServiceInstance serviceInstance = new DefaultServiceInstance(instance.getId(), instance.getAppName(),
                instance.getHostName(),
                instance.isPortEnabled(InstanceInfo.PortType.SECURE) ? instance.getSecurePort() : instance.getPort());
        serviceInstance.setMetadata(instance.getMetadata());
        return serviceInstance;
    }

    private EurekaInstanceConfig buildEurekaInstanceConfig(ServiceInstance serviceInstance) {
        ConfigurableEurekaInstanceConfig eurekaInstanceConfig = new ConfigurableEurekaInstanceConfig()
                .setInstanceId(serviceInstance.getId())
                .setAppname(serviceInstance.getServiceName())
                .setIpAddress(serviceInstance.getHost())
                .setNonSecurePort(serviceInstance.getPort())
                .setMetadataMap(serviceInstance.getMetadata());
        return eurekaInstanceConfig;
    }
}
