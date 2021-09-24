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
package org.apache.dubbo.registry.client.event.listener;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.event.Event;
import org.apache.dubbo.event.GenericEventListener;
import org.apache.dubbo.registry.client.event.ServiceDiscoveryDestroyedEvent;
import org.apache.dubbo.registry.client.event.ServiceDiscoveryDestroyingEvent;
import org.apache.dubbo.registry.client.event.ServiceDiscoveryInitializedEvent;
import org.apache.dubbo.registry.client.event.ServiceDiscoveryInitializingEvent;
import org.apache.dubbo.registry.client.event.ServiceInstancePreRegisteredEvent;
import org.apache.dubbo.registry.client.event.ServiceInstancePreUnregisteredEvent;
import org.apache.dubbo.registry.client.event.ServiceInstanceRegisteredEvent;
import org.apache.dubbo.registry.client.event.ServiceInstanceUnregisteredEvent;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;

import static java.lang.String.format;

/**
 * A listener for logging the {@link Event Dubbo event}
 *
 * @since 2.7.5
 */
public class LoggingEventListener extends GenericEventListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void onEvent(ServiceDiscoveryInitializingEvent event) {
        info("%s is initializing...", event.getServiceDiscovery());
    }

    public void onEvent(ServiceDiscoveryInitializedEvent event) {
        info("%s is initialized.", event.getServiceDiscovery());
    }

    public void onEvent(ServiceInstancePreRegisteredEvent event) {
        info("%s is registering into %s...", event.getServiceInstance(), event.getSource());
    }

    public void onEvent(ServiceInstanceRegisteredEvent event) {
        info("%s has been registered into %s.", event.getServiceInstance(), event.getSource());
    }

    public void onEvent(ServiceInstancesChangedEvent event) {
        info("The service[name : %s] instances[size : %s] has been changed.", event.getServiceName(), event.getServiceInstances().size());
    }

    public void onEvent(ServiceInstancePreUnregisteredEvent event) {
        info("%s is unregistering from %s...", event.getServiceInstance(), event.getSource());
    }

    public void onEvent(ServiceInstanceUnregisteredEvent event) {
        info("%s has been unregistered from %s.", event.getServiceInstance(), event.getSource());
    }

    public void onEvent(ServiceDiscoveryDestroyingEvent event) {
        info("%s is stopping...", event.getServiceDiscovery());
    }

    public void onEvent(ServiceDiscoveryDestroyedEvent event) {
        info("%s is stopped.", event.getServiceDiscovery());
    }

    private void info(String pattern, Object... args) {
        if (logger.isInfoEnabled()) {
            logger.info(format(pattern, args));
        }
    }
}
