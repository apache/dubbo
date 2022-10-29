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
package org.apache.dubbo.registry.sofa;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.DefaultPage;
import org.apache.dubbo.common.utils.Page;
import org.apache.dubbo.registry.client.AbstractServiceDiscovery;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.rpc.RpcException;

import com.alipay.sofa.registry.client.api.Publisher;
import com.alipay.sofa.registry.client.api.RegistryClientConfig;
import com.alipay.sofa.registry.client.api.Subscriber;
import com.alipay.sofa.registry.client.api.model.RegistryType;
import com.alipay.sofa.registry.client.api.model.UserData;
import com.alipay.sofa.registry.client.api.registration.PublisherRegistration;
import com.alipay.sofa.registry.client.api.registration.SubscriberRegistration;
import com.alipay.sofa.registry.client.provider.DefaultRegistryClient;
import com.alipay.sofa.registry.client.provider.DefaultRegistryClientConfigBuilder;
import com.alipay.sofa.registry.core.model.ScopeEnum;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.registry.sofa.SofaRegistryConstants.ADDRESS_WAIT_TIME_KEY;
import static org.apache.dubbo.registry.sofa.SofaRegistryConstants.LOCAL_DATA_CENTER;
import static org.apache.dubbo.registry.sofa.SofaRegistryConstants.LOCAL_REGION;


public class SofaRegistryServiceDiscovery extends AbstractServiceDiscovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(SofaRegistryServiceDiscovery.class);

    private static final String DEFAULT_GROUP = "dubbo";

    private URL registryURL;

    private DefaultRegistryClient registryClient;

    private int waitAddressTimeout;

    private RegistryClientConfig registryClientConfig;

    private final Map<String, Publisher> publishers = new ConcurrentHashMap<>();

    private final Map<String, Subscriber> subscribers = new ConcurrentHashMap<>();

    private ServiceInstance serviceInstance;

    private Gson gson = new Gson();

    @Override
    public void initialize(URL registryURL) throws Exception {
        this.registryURL = registryURL;

        this.registryClientConfig = DefaultRegistryClientConfigBuilder.start()
                .setDataCenter(LOCAL_DATA_CENTER)
                .setZone(LOCAL_REGION)
                .setRegistryEndpoint(registryURL.getHost())
                .setRegistryEndpointPort(registryURL.getPort()).build();

        registryClient = new DefaultRegistryClient(this.registryClientConfig);
        registryClient.init();

        this.waitAddressTimeout = Integer.parseInt(ConfigUtils.getProperty(ADDRESS_WAIT_TIME_KEY, "5000"));
    }

    @Override
    public URL getUrl() {
        return registryURL;
    }

    @Override
    public void destroy() throws Exception {
    }

    @Override
    public void doRegister(ServiceInstance serviceInstance) {
        SofaRegistryInstance sofaRegistryInstance = new SofaRegistryInstance(serviceInstance.getId(), serviceInstance.getHost(), serviceInstance.getPort(), serviceInstance.getServiceName(), serviceInstance.getMetadata());
        Publisher publisher = publishers.get(serviceInstance.getServiceName());
        this.serviceInstance = serviceInstance;
        if (null == publisher) {
            PublisherRegistration registration = new PublisherRegistration(serviceInstance.getServiceName());
            registration.setGroup(DEFAULT_GROUP);
            publisher = registryClient.register(registration, gson.toJson(sofaRegistryInstance));

            publishers.put(serviceInstance.getServiceName(), publisher);
        } else {
            publisher.republish(gson.toJson(sofaRegistryInstance));
        }
    }

    @Override
    public void doUpdate(ServiceInstance serviceInstance) {
        register(serviceInstance);
    }

    @Override
    public void unregister(ServiceInstance serviceInstance) throws RuntimeException {
        registryClient.unregister(serviceInstance.getServiceName(), DEFAULT_GROUP, RegistryType.PUBLISHER);
    }

    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws NullPointerException, IllegalArgumentException {
        listener.getServiceNames().forEach(serviceName -> registerServiceWatcher(serviceName, listener));
    }

    protected void registerServiceWatcher(String serviceName, ServiceInstancesChangedListener listener) {
        Subscriber subscriber = subscribers.get(serviceName);

        if (null == subscriber) {
            final CountDownLatch latch = new CountDownLatch(1);
            SubscriberRegistration subscriberRegistration = new SubscriberRegistration(serviceName, (dataId, data) -> {
                handleRegistryData(dataId, data, listener, latch);
            });
            subscriberRegistration.setGroup(DEFAULT_GROUP);
            subscriberRegistration.setScopeEnum(ScopeEnum.global);

            subscriber = registryClient.register(subscriberRegistration);
            subscribers.put(serviceName, subscriber);
            waitAddress(serviceName, latch);
        }
    }

    @Override
    public Page<ServiceInstance> getInstances(String serviceName, int offset, int pageSize, boolean healthyOnly)
            throws NullPointerException, IllegalArgumentException, UnsupportedOperationException {
        Subscriber subscriber = subscribers.get(serviceName);

        if (null != subscriber) {
            List<ServiceInstance> serviceInstanceList = handleRegistryData(serviceName, subscriber.peekData(), null, null);
            return new DefaultPage<>(offset, pageSize, serviceInstanceList, serviceInstanceList.size());
        }

        throw new RpcException("getInstances error!");
    }

    private List<ServiceInstance> handleRegistryData(String dataId, UserData userData, ServiceInstancesChangedListener listener, CountDownLatch latch) {
        try {
            List<String> datas = getUserData(dataId, userData);
            List<ServiceInstance> serviceInstances = new ArrayList<>(datas.size());

            for (String serviceData : datas) {
                SofaRegistryInstance sri = gson.fromJson(serviceData, SofaRegistryInstance.class);

                DefaultServiceInstance serviceInstance = new DefaultServiceInstance(sri.getId(), dataId, sri.getHost(), sri.getPort());
                serviceInstance.setMetadata(sri.getMetadata());
                serviceInstances.add(serviceInstance);
            }

            if (null != listener) {
                listener.onEvent(new ServiceInstancesChangedEvent(dataId, serviceInstances));
            }

            return serviceInstances;
        } finally {
            if (null != latch) {
                latch.countDown();
            }
        }
    }

    private void waitAddress(String serviceName, CountDownLatch countDownLatch) {
        try {
            if (!countDownLatch.await(waitAddressTimeout, TimeUnit.MILLISECONDS)) {
                LOGGER.warn("Subscribe data failed by dataId " + serviceName);
            }
        } catch (Exception e) {
            LOGGER.error("Error when wait Address!", e);
        }
    }

    /**
     * Print address data.
     *
     * @param dataId   the data id
     * @param userData the user data
     */
    protected List<String> getUserData(String dataId, UserData userData) {

        List<String> datas = null;
        if (userData == null) {
            datas = new ArrayList<>(0);
        } else {
            datas = flatUserData(userData);
        }

        StringBuilder sb = new StringBuilder();
        for (String provider : datas) {
            sb.append("  >>> ").append(provider).append("\n");
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Receive updated RPC service addresses: service[" + dataId
                    + "]\n  .Available target addresses size [" + datas.size() + "]\n"
                    + sb.toString());
        }

        return datas;
    }

    /**
     * Flat user data list.
     *
     * @param userData the user data
     * @return the list
     */
    protected List<String> flatUserData(UserData userData) {
        List<String> result = new ArrayList<>();
        Map<String, List<String>> zoneData = userData.getZoneData();

        for (Map.Entry<String, List<String>> entry : zoneData.entrySet()) {
            result.addAll(entry.getValue());
        }

        return result;
    }

    /**
     * @return
     * @TODO 后续确认下
     */
    @Override
    public Set<String> getServices() {
        return subscribers.keySet();
    }

}
