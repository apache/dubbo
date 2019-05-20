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
package org.apache.dubbo.registry.client;

import org.apache.dubbo.event.Event;
import org.apache.dubbo.event.EventDispatcher;
import org.apache.dubbo.registry.client.event.ServiceInstancePreRegisteredEvent;
import org.apache.dubbo.registry.client.event.ServiceInstanceRegisteredEvent;

import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * The abstract {@link ServiceRegistry} implementation publishes the {@link Event Dubbo event} on methods executing.
 *
 * @since 2.7.2
 */
public abstract class EventPublishingServiceRegistry implements ServiceRegistry {

    private final EventDispatcher eventDispatcher = EventDispatcher.getDefaultExtension();

    @Override
    public final void register(ServiceInstance serviceInstance) throws RuntimeException {
        executeWithEvents(
                of(new ServiceInstancePreRegisteredEvent(this, serviceInstance)),
                () -> doRegister(serviceInstance),
                of(new ServiceInstanceRegisteredEvent(this, serviceInstance))
        );
    }

    @Override
    public final void update(ServiceInstance serviceInstance) throws RuntimeException {
        // TODO publish event
        executeWithEvents(
                empty(),
                () -> doUpdate(serviceInstance),
                empty()
        );
    }

    @Override
    public final void unregister(ServiceInstance serviceInstance) throws RuntimeException {
        // TODO publish event
        executeWithEvents(
                empty(),
                () -> doUnregister(serviceInstance),
                empty()
        );
    }

    @Override
    public final void start() {
        // TODO publish event
        executeWithEvents(
                empty(),
                this::doStart,
                empty()
        );
    }

    @Override
    public final void stop() {
        // TODO publish event
        executeWithEvents(
                empty(),
                this::doStop,
                empty()
        );
    }

    protected final void executeWithEvents(Optional<? extends Event> beforeEvent,
                                           Runnable action,
                                           Optional<? extends Event> afterEvent) {
        beforeEvent.ifPresent(eventDispatcher::dispatch);
        action.run();
        afterEvent.ifPresent(eventDispatcher::dispatch);
    }

    /**
     * Registers an instance of {@link ServiceInstance}.
     *
     * @param serviceInstance an instance of {@link ServiceInstance} to be registered
     * @throws RuntimeException if failed
     */
    protected abstract void doRegister(ServiceInstance serviceInstance) throws RuntimeException;

    /**
     * Updates the registered {@link ServiceInstance}.
     *
     * @param serviceInstance the registered {@link ServiceInstance}
     * @throws RuntimeException if failed
     */
    protected abstract void doUpdate(ServiceInstance serviceInstance) throws RuntimeException;

    /**
     * Unregisters an instance of {@link ServiceInstance}.
     *
     * @param serviceInstance an instance of {@link ServiceInstance} to be deregistered
     * @throws RuntimeException if failed
     */
    protected abstract void doUnregister(ServiceInstance serviceInstance) throws RuntimeException;

    /**
     * Starts the ServiceRegistry. This is a lifecycle method.
     */
    protected abstract void doStart();

    /**
     * Stops the ServiceRegistry. This is a lifecycle method.
     */
    protected abstract void doStop();
}
