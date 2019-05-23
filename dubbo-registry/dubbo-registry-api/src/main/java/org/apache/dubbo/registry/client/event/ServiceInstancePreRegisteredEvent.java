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

import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.ServiceRegistry;


/**
 * The before-{@link ServiceRegistry#register(ServiceInstance) register} event for {@link ServiceInstance}
 *
 * @since 2.7.2
 */
public class ServiceInstancePreRegisteredEvent extends ServiceInstanceEvent {

    public ServiceInstancePreRegisteredEvent(Object source, ServiceInstance serviceInstance) {
        super(source, serviceInstance);
    }
}
