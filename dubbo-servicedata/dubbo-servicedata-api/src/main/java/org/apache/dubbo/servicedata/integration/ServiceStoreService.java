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
package org.apache.dubbo.servicedata.integration;

import com.alibaba.fastjson.JSON;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.servicedata.metadata.ServiceDescriptor;
import org.apache.dubbo.servicedata.metadata.builder.ServiceDescriptorBuilder;
import org.apache.dubbo.servicedata.store.ServiceStore;
import org.apache.dubbo.servicedata.store.ServiceStoreFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.apache.dubbo.common.Constants.SERVICE_DESCIPTOR_KEY;


public class ServiceStoreService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private static final int ONE_DAY_IN_MIll = 60 * 24 * 60 * 1000;
    private static final int FOUR_HOURS_IN_MIll = 60 * 4 * 60 * 1000;

    private static ServiceStoreService serviceStoreService;
    private static Object lock = new Object();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(0, new NamedThreadFactory("DubboRegistryFailedRetryTimer", true));
    private ServiceStoreFactory serviceStoreFactory = ExtensionLoader.getExtensionLoader(ServiceStoreFactory.class).getAdaptiveExtension();
    final Set<URL> providerURLs = new ConcurrentHashSet<>();
    final Set<URL> consumerURLs = new ConcurrentHashSet<URL>();
    ServiceStore serviceStore;
    URL serviceStoreUrl;



    ServiceStoreService(URL serviceStoreURL) {
        if (Constants.SERVICE_STORE_KEY.equals(serviceStoreURL.getProtocol())) {
            String protocol = serviceStoreURL.getParameter(Constants.SERVICE_STORE_KEY, Constants.DEFAULT_DIRECTORY);
            serviceStoreURL = serviceStoreURL.setProtocol(protocol).removeParameter(Constants.SERVICE_STORE_KEY);
        }
        this.serviceStoreUrl = serviceStoreURL;
        serviceStore = serviceStoreFactory.getServiceStore(this.serviceStoreUrl);
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                publishAll();
            }
        }, calculateStartTime(), ONE_DAY_IN_MIll, TimeUnit.MILLISECONDS);
    }


    public static ServiceStoreService instance(Supplier<URL> loadServiceStoreUrl) {
        if (serviceStoreService == null) {
            synchronized (lock) {
                if (serviceStoreService == null) {
                    URL serviceStoreURL = loadServiceStoreUrl.get();
                    if (serviceStoreURL == null) {
                        return null;
                    }
                    serviceStoreService = new ServiceStoreService(serviceStoreURL);
                }
            }
        }
        return serviceStoreService;
    }

    public void publishProvider(URL providerUrl) throws RpcException {
        //first add into the list
        providerURLs.add(providerUrl);
        try {
            String interfaceName = providerUrl.getParameter(Constants.INTERFACE_KEY);
            if (StringUtils.isNotEmpty(interfaceName)) {
                Class interfaceClass = Class.forName(interfaceName);
                ServiceDescriptor serviceDescriptor = ServiceDescriptorBuilder.build(interfaceClass);
                providerUrl = providerUrl.addParameter(SERVICE_DESCIPTOR_KEY, JSON.toJSONString(serviceDescriptor));
            }
        } catch (ClassNotFoundException e) {
            //ignore error
            logger.error("Servicestore getServiceDescriptor error. providerUrl: " + providerUrl.toFullString(), e);
        }
        serviceStore.put(providerUrl);
    }

    public void publishConsumer(URL consumerURL) throws RpcException {
        consumerURLs.add(consumerURL);
        serviceStore.put(consumerURL);
    }

    void publishAll() {
        for (URL url : providerURLs) {
            publishProvider(url);
        }
        for (URL url : consumerURLs) {
            publishConsumer(url);
        }
    }

    long calculateStartTime() {
        Date now = new Date();
        long nowMill = now.getTime();
        long today0 = DateUtils.truncate(now, Calendar.DAY_OF_MONTH).getTime();
        long subtract = today0 + ONE_DAY_IN_MIll - nowMill;
        Random r = new Random();
        return subtract + (FOUR_HOURS_IN_MIll / 2) + r.nextInt(FOUR_HOURS_IN_MIll);
    }

}
