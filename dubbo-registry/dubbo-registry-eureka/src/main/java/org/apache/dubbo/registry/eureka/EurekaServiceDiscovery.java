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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.config.ConfigurationManager;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Eureka {@link ServiceDiscovery} implementation based on Eureka API
 */
public class EurekaServiceDiscovery implements ServiceDiscovery {

    /**
     * The namespace for {@link EurekaInstanceConfig} is compatible with Spring Cloud
     */
    public static final String EUREKA_INSTANCE_NAMESPACE = "eureka.instance.";

    /**
     * The namespace for {@link EurekaClient} is compatible with Spring Cloud
     */
    public static final String EUREKA_CLIENT_NAMESPACE = "eureka.client.";

    private EurekaInstanceConfig eurekaInstanceConfig;

    private ApplicationInfoManager applicationInfoManager;

    private EurekaClientConfig eurekaClientConfig;

    private EurekaClient eurekaClient;

    @Override
    public void initialize(URL registryURL) throws Exception {
        Properties eurekaConfigProperties = buildEurekaConfigProperties(registryURL);
        initConfigurationManager(eurekaConfigProperties);
        initEurekaInstanceConfig();
        initApplicationInfoManager();
        initEurekaClientConfig();
        initEurekaClient();
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
                    String rawPropertyName = propertyEntry.getKey();
                    String propertyValue = propertyEntry.getValue();
                    properties.setProperty(normalizePropertyName(rawPropertyName), propertyValue);
                });
        return properties;
    }

    private void setDefaultProperties(URL registryURL, Properties properties) {
        setDefaultServiceURL(registryURL, properties);
    }

    private void setDefaultServiceURL(URL registryURL, Properties properties) {
        StringBuilder defaultServiceURLBuilder = new StringBuilder("http://")
                .append(registryURL.getHost())
                .append(":")
                .append(registryURL.getPort())
                .append("/")
                .append(registryURL.getPath());
        properties.setProperty(EUREKA_CLIENT_NAMESPACE + "serviceUrl.default", defaultServiceURLBuilder.toString());
    }

    private boolean filterEurekaProperty(Map.Entry<String, String> propertyEntry) {
        String propertyName = propertyEntry.getKey();
        return propertyName.startsWith(EUREKA_INSTANCE_NAMESPACE) ||
                propertyName.startsWith(EUREKA_CLIENT_NAMESPACE);
    }

    private String normalizePropertyName(String rawPropertyName) {
        return StringUtils.replace(rawPropertyName, "-", ".");
    }

    /**
     * Initialize {@link ConfigurationManager}
     *
     * @param eurekaConfigProperties the Eureka's {@link ConfigurationManager}
     */
    private void initConfigurationManager(Properties eurekaConfigProperties) {
        ConfigurationManager.loadProperties(eurekaConfigProperties);
    }

    /**
     * Initialize {@link #eurekaInstanceConfig} property
     */
    private void initEurekaInstanceConfig() {
        this.eurekaInstanceConfig = new MyDataCenterInstanceConfig(EUREKA_INSTANCE_NAMESPACE);
    }

    /**
     * Initialize {@link #applicationInfoManager} property
     */
    private void initApplicationInfoManager() {
        this.applicationInfoManager = new ApplicationInfoManager(eurekaInstanceConfig, (ApplicationInfoManager.OptionalArgs) null);
    }

    /**
     * Initialize {@link #eurekaClient} property
     */
    private void initEurekaClientConfig() {
        this.eurekaClientConfig = new DefaultEurekaClientConfig(EUREKA_CLIENT_NAMESPACE);
    }

    /**
     * Initialize {@link #eurekaClient} property
     */
    private void initEurekaClient() {
        this.eurekaClient = new DiscoveryClient(applicationInfoManager, eurekaClientConfig);
    }

    @Override
    public void destroy() throws Exception {
        this.eurekaInstanceConfig = null;
        this.applicationInfoManager = null;
        this.eurekaClientConfig = null;
        this.eurekaClient = null;
    }

    @Override
    public void register(ServiceInstance serviceInstance) throws RuntimeException {
        EurekaInstanceConfig eurekaInstanceConfig = buildEurekaInstanceConfig(serviceInstance);
    }

    @Override
    public void update(ServiceInstance serviceInstance) throws RuntimeException {

    }

    @Override
    public void unregister(ServiceInstance serviceInstance) throws RuntimeException {

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
        List<InstanceInfo> infos = this.eurekaClient.getInstancesByVipAddress(serviceName, false);
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
        MutableEurekaInstanceConfig eurekaInstanceConfig = new MutableEurekaInstanceConfig();
        eurekaInstanceConfig.setInstanceId(serviceInstance.getId());
        eurekaInstanceConfig.setAppname(serviceInstance.getServiceName());
        eurekaInstanceConfig.setIpAddress(serviceInstance.getHost());
        eurekaInstanceConfig.setNonSecurePort(serviceInstance.getPort());
        eurekaInstanceConfig.setInitialStatus(serviceInstance.isHealthy() ? InstanceInfo.InstanceStatus.UP :
                InstanceInfo.InstanceStatus.UNKNOWN);
        return eurekaInstanceConfig;
    }
}
