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
package org.apache.dubbo.rpc.model;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.invoker.ProviderInvokerWrapper;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Represent a application which is using Dubbo and store basic metadata info for using
 * during the processing of RPC invoking.
 *
 * ApplicationModel includes many ProviderModel which is about published services
 * and many Consumer Model which is about subscribed services.
 *
 * adjust project structure in order to fully utilize the methods introduced here.
 */
public class ApplicationModel {
    protected static final Logger LOGGER = LoggerFactory.getLogger(ApplicationModel.class);

    /**
     * serviceKey -> exported service
     */
    private static final ConcurrentMap<String, ProviderModel> PROVIDED_SERVICES = new ConcurrentHashMap<>();
    /**
     * serviceKey -> referred service
     */
    private static final ConcurrentMap<String, ConsumerModel> CONSUMED_SERVICES = new ConcurrentHashMap<>();

    private static String application;

    public static Collection<ConsumerModel> allConsumerModels() {
        return CONSUMED_SERVICES.values();
    }

    public static Collection<ProviderModel> allProviderModels() {
        return PROVIDED_SERVICES.values();
    }

    public static ProviderModel getProviderModel(String serviceKey) {
        return PROVIDED_SERVICES.get(serviceKey);
    }

    public static ConsumerModel getConsumerModel(String serviceKey) {
        return CONSUMED_SERVICES.get(serviceKey);
    }

    public static void initConsumerModel(String serviceName, ConsumerModel consumerModel) {
        if (CONSUMED_SERVICES.putIfAbsent(serviceName, consumerModel) != null) {
            LOGGER.warn("Already register the same consumer:" + serviceName);
        }
    }

    public static void initProviderModel(String serviceName, ProviderModel providerModel) {
        if (PROVIDED_SERVICES.putIfAbsent(serviceName, providerModel) != null) {
            LOGGER.warn("Already register the same:" + serviceName);
        }
    }

    public static String getApplication() {
        return application;
    }

    public static void setApplication(String application) {
        ApplicationModel.application = application;
    }

    public static <T> ProviderInvokerWrapper<T> registerProviderInvoker(Invoker<T> invoker, URL registryUrl, URL providerUrl) {
        ProviderInvokerWrapper<T> wrapperInvoker = new ProviderInvokerWrapper<>(invoker, registryUrl, providerUrl);
        ProviderModel providerModel = getProviderModel(providerUrl.getServiceKey());
        providerModel.addInvoker(wrapperInvoker);
        return wrapperInvoker;
    }

    public static Collection<ProviderInvokerWrapper> getProviderInvokers(String serviceKey) {
        ProviderModel providerModel = getProviderModel(serviceKey);
        if (providerModel == null) {
            return Collections.emptySet();
        }
        return providerModel.getInvokers();
    }

    public static <T> ProviderInvokerWrapper<T> getProviderInvoker(String serviceKey, Invoker<T> invoker) {
        ProviderModel providerModel = getProviderModel(serviceKey);
        return providerModel.getInvoker(invoker.getUrl().getProtocol());
    }

    public static boolean isRegistered(String serviceKey) {
        return getProviderInvokers(serviceKey).stream().anyMatch(ProviderInvokerWrapper::isReg);
    }

    public static void registerConsumerInvoker(Invoker invoker, String serviceKey) {
        ConsumerModel consumerModel = getConsumerModel(serviceKey);
        consumerModel.setInvoker(invoker);
    }

    public static <T> Invoker<T> getConsumerInvoker(String serviceKey) {
        return (Invoker<T>) getConsumerModel(serviceKey).getInvoker();
    }

    /**
     * For unit test
     */
    public static void reset() {
        PROVIDED_SERVICES.clear();
        CONSUMED_SERVICES.clear();
    }
}
