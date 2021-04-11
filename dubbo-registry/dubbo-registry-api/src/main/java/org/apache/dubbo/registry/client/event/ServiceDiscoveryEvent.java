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
import org.apache.dubbo.registry.client.ServiceDiscovery;

/**
 * An abstract {@link Event} class for {@link ServiceDiscovery}
 *
 * @see Event
 * @see ServiceDiscovery
 * @since 2.7.5
 */
public abstract class ServiceDiscoveryEvent extends Event {

    private final ServiceDiscovery original;

    /**
     * Constructs a prototypical Event.
     *
     * @param source   The object on which the Event initially occurred.
     * @param original The original {@link ServiceDiscovery}
     * @throws IllegalArgumentException if source is null.
     */
    public ServiceDiscoveryEvent(ServiceDiscovery source, ServiceDiscovery original) {
        super(source);
        this.original = original;
    }

    @Override
    public ServiceDiscovery getSource() {
        return (ServiceDiscovery) super.getSource();
    }

    /**
     * Get the {@link ServiceDiscovery} on which the Event initially occurred.
     *
     * @return {@link ServiceDiscovery} instance
     */
    public final ServiceDiscovery getServiceDiscovery() {
        return getSource();
    }

    /**
     * Get the original {@link ServiceDiscovery}
     *
     * @return the original {@link ServiceDiscovery}
     */
    public final ServiceDiscovery getOriginal() {
        return original;
    }
}
