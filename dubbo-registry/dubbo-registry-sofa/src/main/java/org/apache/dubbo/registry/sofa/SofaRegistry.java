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
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.support.FailbackRegistry;

import com.alipay.sofa.registry.client.api.RegistryClient;
import com.alipay.sofa.registry.client.api.RegistryClientConfig;
import com.alipay.sofa.registry.client.api.Subscriber;
import com.alipay.sofa.registry.client.api.model.RegistryType;
import com.alipay.sofa.registry.client.api.model.UserData;
import com.alipay.sofa.registry.client.api.registration.PublisherRegistration;
import com.alipay.sofa.registry.client.api.registration.SubscriberRegistration;
import com.alipay.sofa.registry.client.provider.DefaultRegistryClient;
import com.alipay.sofa.registry.client.provider.DefaultRegistryClientConfigBuilder;
import com.alipay.sofa.registry.core.model.ScopeEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.registry.Constants.CONSUMER_PROTOCOL;
import static org.apache.dubbo.registry.Constants.PROVIDER_PROTOCOL;
import static org.apache.dubbo.registry.Constants.REGISTER_KEY;
import static org.apache.dubbo.registry.Constants.SUBSCRIBE_KEY;
import static org.apache.dubbo.registry.sofa.SofaRegistryConstants.ADDRESS_WAIT_TIME_KEY;
import static org.apache.dubbo.registry.sofa.SofaRegistryConstants.DEFAULT_GROUP;
import static org.apache.dubbo.registry.sofa.SofaRegistryConstants.LOCAL_DATA_CENTER;
import static org.apache.dubbo.registry.sofa.SofaRegistryConstants.LOCAL_REGION;

/**
 * The Sofa registry.
 *
 * @since 2.7.2
 */
public class SofaRegistry extends FailbackRegistry {

    /**
     * Cache subscriber by dataId
     */
    private final Map<String, Subscriber> subscribers = new ConcurrentHashMap<>();

    /**
     * Direct registry client
     */
    private RegistryClient registryClient;
    /**
     * wait address from registry
     */
    private int waitAddressTimeout;

    /**
     * Instantiates a new Sofa registry.
     *
     * @param url the url
     */
    public SofaRegistry(URL url) {
        super(url);
        if (logger.isInfoEnabled()) {
            logger.info("Build sofa registry by url:" + url);
        }
        this.registryClient = buildClient(url);
        this.waitAddressTimeout = Integer.parseInt(ConfigUtils.getProperty(ADDRESS_WAIT_TIME_KEY, "5000"));
    }

    /**
     * Build client registry client.
     *
     * @param url the url
     * @return the registry client
     */
    protected RegistryClient buildClient(URL url) {
        RegistryClientConfig config = DefaultRegistryClientConfigBuilder.start()
                .setDataCenter(LOCAL_DATA_CENTER)
                .setZone(LOCAL_REGION)
                .setRegistryEndpoint(url.getHost())
                .setRegistryEndpointPort(url.getPort()).build();

        DefaultRegistryClient registryClient = new DefaultRegistryClient(config);
        registryClient.init();
        return registryClient;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void doRegister(URL url) {
        if (!url.getParameter(REGISTER_KEY, true)
                || CONSUMER_PROTOCOL.equals(url.getProtocol())) {
            return;
        }

        String serviceName = buildServiceName(url);
        String serviceData = url.toFullString();

        PublisherRegistration registration = new PublisherRegistration(serviceName);
        addAttributesForPub(registration);

        registryClient.register(registration, serviceData);
    }

    /**
     * Add attributes for pub.
     *
     * @param publisherRegistration the publisher registration
     */
    protected void addAttributesForPub(PublisherRegistration publisherRegistration) {
        publisherRegistration.setGroup(DEFAULT_GROUP);
    }

    @Override
    public void doUnregister(URL url) {
        if (!url.getParameter(REGISTER_KEY, true)
                || CONSUMER_PROTOCOL.equals(url.getProtocol())) {
            return;
        }
        String serviceName = buildServiceName(url);
        registryClient.unregister(serviceName, DEFAULT_GROUP, RegistryType.PUBLISHER);
    }

    @Override
    public void doSubscribe(URL url, final NotifyListener listener) {
        if (!url.getParameter(SUBSCRIBE_KEY, true)
                || PROVIDER_PROTOCOL.equals(url.getProtocol())) {
            return;
        }

        String serviceName = buildServiceName(url);
        // com.alipay.test.TestService:1.0:group@dubbo
        Subscriber listSubscriber = subscribers.get(serviceName);

        if (listSubscriber != null) {
            logger.warn("Service name [" + serviceName + "] have bean registered in SOFARegistry.");

            CountDownLatch countDownLatch = new CountDownLatch(1);
            handleRegistryData(listSubscriber.peekData(), listener, countDownLatch);
            waitAddress(serviceName, countDownLatch);
            return;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        SubscriberRegistration subscriberRegistration = new SubscriberRegistration(serviceName,
                (dataId, data) -> {
                    //record change
                    printAddressData(dataId, data);
                    handleRegistryData(data, listener, latch);
                });

        addAttributesForSub(subscriberRegistration);
        listSubscriber = registryClient.register(subscriberRegistration);

        subscribers.put(serviceName, listSubscriber);

        waitAddress(serviceName, latch);
    }

    private void waitAddress(String serviceName, CountDownLatch countDownLatch) {
        try {
            if (!countDownLatch.await(waitAddressTimeout, TimeUnit.MILLISECONDS)) {
                logger.warn("Subscribe data failed by dataId " + serviceName);
            }
        } catch (Exception e) {
            logger.error("Error when wait Address!", e);
        }
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        if (!url.getParameter(SUBSCRIBE_KEY, true)
                || PROVIDER_PROTOCOL.equals(url.getProtocol())) {
            return;
        }
        String serviceName = buildServiceName(url);

        registryClient.unregister(serviceName, DEFAULT_GROUP, RegistryType.SUBSCRIBER);
    }

    private void handleRegistryData(UserData data, NotifyListener notifyListener,
                                    CountDownLatch latch) {
        try {
            List<URL> urls = new ArrayList<>();
            if (null != data) {

                List<String> datas = flatUserData(data);
                for (String serviceUrl : datas) {
                    URL url = URL.valueOf(serviceUrl);
                    String serverApplication = url.getParameter(APPLICATION_KEY);
                    if (StringUtils.isNotEmpty(serverApplication)) {
                        url = url.addParameter("dstApp", serverApplication);
                    }
                    urls.add(url);
                }
            }
            notifyListener.notify(urls);
        } finally {
            latch.countDown();
        }
    }

    private String buildServiceName(URL url) {
        // return url.getServiceKey();
        StringBuilder buf = new StringBuilder();
        buf.append(url.getServiceInterface());
        String version = url.getParameter(VERSION_KEY);
        if (StringUtils.isNotEmpty(version)) {
            buf.append(":").append(version);
        }
        String group = url.getParameter(GROUP_KEY);
        if (StringUtils.isNotEmpty(group)) {
            buf.append(":").append(group);
        }
        buf.append("@").append(DUBBO);
        return buf.toString();
    }

    /**
     * Print address data.
     *
     * @param dataId   the data id
     * @param userData the user data
     */
    protected void printAddressData(String dataId, UserData userData) {

        List<String> datas;
        if (userData == null) {
            datas = new ArrayList<>(0);
        } else {
            datas = flatUserData(userData);
        }

        StringBuilder sb = new StringBuilder();
        for (String provider : datas) {
            sb.append("  >>> ").append(provider).append("\n");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Receive updated RPC service addresses: service[" + dataId
                    + "]\n  .Available target addresses size [" + datas.size() + "]\n"
                    + sb.toString());
        }
    }

    /**
     * Add attributes for sub.
     *
     * @param subscriberRegistration the subscriber registration
     */
    protected void addAttributesForSub(SubscriberRegistration subscriberRegistration) {
        subscriberRegistration.setGroup(DEFAULT_GROUP);
        subscriberRegistration.setScopeEnum(ScopeEnum.global);
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
}
