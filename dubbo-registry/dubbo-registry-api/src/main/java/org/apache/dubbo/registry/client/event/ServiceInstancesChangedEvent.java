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
package org.apache.dubbo.registry.client.event;

import org.apache.dubbo.event.Event;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;

import java.util.List;

import static java.util.Collections.unmodifiableList;

/**
 * An event raised after the {@link ServiceInstance instances} of one service has been changed.
 *
 * @see ServiceInstancesChangedListener
 * @since 2.7.5
 */
public class ServiceInstancesChangedEvent extends Event {

    private final String serviceName;

    private final List<ServiceInstance> serviceInstances;

    /**
     * @param serviceName      The name of service that was changed
     * @param serviceInstances all {@link ServiceInstance service instances}
     * @throws IllegalArgumentException if source is null.
     */
    public ServiceInstancesChangedEvent(String serviceName, List<ServiceInstance> serviceInstances) {
        super(serviceName);
        this.serviceName = serviceName;
        this.serviceInstances = unmodifiableList(serviceInstances);
    }

    /**
     * @return The name of service that was changed
     */
    public String getServiceName() {
        return serviceName;
    }


    /**
     * @return all {@link ServiceInstance service instances}
     */
    public List<ServiceInstance> getServiceInstances() {
        return serviceInstances;
    }

}