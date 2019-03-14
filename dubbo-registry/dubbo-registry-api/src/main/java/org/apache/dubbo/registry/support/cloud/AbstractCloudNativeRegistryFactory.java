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
package org.apache.dubbo.registry.support.cloud;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;

import java.util.concurrent.ScheduledExecutorService;

import static java.lang.System.getProperty;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

/**
 * Abstract {@link CloudNativeRegistry} Factory
 *
 * @param <S> The subclass of {@link ServiceInstance}
 * @since 2.7.1
 */
public abstract class AbstractCloudNativeRegistryFactory<S extends ServiceInstance> extends AbstractRegistryFactory {

    private static String SERVICES_LOOKUP_SCHEDULER_THREAD_NAME_PREFIX =
            getProperty("dubbo.services.lookup.scheduler.thread.name.prefix ", "dubbo-services-lookup-");

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final ScheduledExecutorService servicesLookupScheduler;

    public AbstractCloudNativeRegistryFactory() {
        this.servicesLookupScheduler = newSingleThreadScheduledExecutor(
                new NamedThreadFactory(SERVICES_LOOKUP_SCHEDULER_THREAD_NAME_PREFIX));
    }

    @Override
    protected final Registry createRegistry(URL url) {
        return new CloudNativeRegistry(url,
                createCloudServiceRegistry(url),
                createCloudServiceDiscovery(url),
                createServiceInstanceFactory(url),
                servicesLookupScheduler
        );
    }

    /**
     * The subclass implement this method to create {@link CloudServiceRegistry}
     *
     * @param url The {@link URL} of Dubbo Registry
     * @return non-null
     */
    protected abstract CloudServiceRegistry<S> createCloudServiceRegistry(URL url);

    /**
     * The subclass implement this method to create {@link CloudServiceDiscovery}
     *
     * @param url The {@link URL} of Dubbo Registry
     * @return non-null
     */
    protected abstract CloudServiceDiscovery<S> createCloudServiceDiscovery(URL url);

    /**
     * The subclass implement this method to create {@link ServiceInstanceFactory}
     *
     * @param url The {@link URL} of Dubbo Registry
     * @return non-null
     */
    protected abstract ServiceInstanceFactory<S> createServiceInstanceFactory(URL url);
}
