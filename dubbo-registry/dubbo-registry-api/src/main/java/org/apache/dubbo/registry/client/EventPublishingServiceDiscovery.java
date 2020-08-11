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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.function.ThrowableAction;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.Page;
import org.apache.dubbo.event.Event;
import org.apache.dubbo.event.EventDispatcher;
import org.apache.dubbo.registry.client.event.ServiceDiscoveryDestroyedEvent;
import org.apache.dubbo.registry.client.event.ServiceDiscoveryDestroyingEvent;
import org.apache.dubbo.registry.client.event.ServiceDiscoveryExceptionEvent;
import org.apache.dubbo.registry.client.event.ServiceDiscoveryInitializedEvent;
import org.apache.dubbo.registry.client.event.ServiceDiscoveryInitializingEvent;
import org.apache.dubbo.registry.client.event.ServiceInstancePreRegisteredEvent;
import org.apache.dubbo.registry.client.event.ServiceInstancePreUnregisteredEvent;
import org.apache.dubbo.registry.client.event.ServiceInstanceRegisteredEvent;
import org.apache.dubbo.registry.client.event.ServiceInstanceUnregisteredEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * The decorating implementation of {@link ServiceDiscovery} to published the {@link Event Dubbo event} when some actions are
 * executing, including:
 * <ul>
 * <li>Lifecycle actions:</li>
 * <table cellpadding="0" cellspacing="0" border="1">
 * <thead>
 * <tr>
 * <th>Action</th>
 * <th>before</th>
 * <th>After</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>{@link #INITIALIZE_ACTION start}</td>
 * <td>{@link ServiceDiscoveryInitializingEvent}</td>
 * <td>{@link ServiceDiscoveryInitializedEvent}</td>
 * </tr>
 * <tr>
 * <td>{@link #DESTROY_ACTION stop}</td>
 * <td>{@link ServiceDiscoveryDestroyingEvent}</td>
 * <td>{@link ServiceDiscoveryDestroyedEvent}</td>
 * </tr>
 * </tbody>
 * </table>
 * <li>Registration actions:</li>
 * <table cellpadding="0" cellspacing="0" border="1">
 * <thead>
 * <tr>
 * <th>Action</th>
 * <th>before</th>
 * <th>After</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>{@link #REGISTER_ACTION register}</td>
 * <td>{@link ServiceInstancePreRegisteredEvent}</td>
 * <td>{@link ServiceInstanceRegisteredEvent}</td>
 * </tr>
 * <tr>
 * <td>{@link #UPDATE_ACTION update}</td>
 * <td>N/A</td>
 * <td>N/A</td>
 * </tr>
 * <tr>
 * <td>{@link #UNREGISTER_ACTION unregister}</td>
 * <td>N/A</td>
 * <td>N/A</td>
 * </tr>
 * </tbody>
 * </table>
 * </ul>
 *
 * @see ServiceDiscovery
 * @see ServiceDiscoveryInitializingEvent
 * @see ServiceDiscoveryInitializedEvent
 * @see ServiceInstancePreRegisteredEvent
 * @see ServiceInstanceRegisteredEvent
 * @see ServiceDiscoveryDestroyingEvent
 * @see ServiceDiscoveryDestroyedEvent
 * @since 2.7.5
 */
final class EventPublishingServiceDiscovery implements ServiceDiscovery {

    /**
     * @see ServiceInstancePreRegisteredEvent
     * @see ServiceInstanceRegisteredEvent
     */
    protected static final String REGISTER_ACTION = "register";

    protected static final String UPDATE_ACTION = "update";

    protected static final String UNREGISTER_ACTION = "unregister";

    /**
     * @see ServiceDiscoveryInitializingEvent
     * @see ServiceDiscoveryInitializedEvent
     */
    protected static final String INITIALIZE_ACTION = "initialize";

    /**
     * @see ServiceDiscoveryDestroyingEvent
     * @see ServiceDiscoveryDestroyedEvent
     */
    protected static final String DESTROY_ACTION = "destroy";

    protected final EventDispatcher eventDispatcher = EventDispatcher.getDefaultExtension();

    protected final AtomicBoolean initialized = new AtomicBoolean(false);

    protected final AtomicBoolean destroyed = new AtomicBoolean(false);

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final ServiceDiscovery serviceDiscovery;

    protected EventPublishingServiceDiscovery(ServiceDiscovery serviceDiscovery) {
        if (serviceDiscovery == null) {
            throw new NullPointerException("The ServiceDiscovery argument must not be null!");
        }
        this.serviceDiscovery = serviceDiscovery;
    }

    @Override
    public final void register(ServiceInstance serviceInstance) throws RuntimeException {

        assertDestroyed(REGISTER_ACTION);
        assertInitialized(REGISTER_ACTION);

        executeWithEvents(
                of(new ServiceInstancePreRegisteredEvent(serviceDiscovery, serviceInstance)),
                () -> serviceDiscovery.register(serviceInstance),
                of(new ServiceInstanceRegisteredEvent(serviceDiscovery, serviceInstance))
        );
    }

    @Override
    public final void update(ServiceInstance serviceInstance) throws RuntimeException {

        assertDestroyed(UPDATE_ACTION);
        assertInitialized(UPDATE_ACTION);

        executeWithEvents(
                empty(),
                () -> serviceDiscovery.update(serviceInstance),
                empty()
        );
    }

    @Override
    public final void unregister(ServiceInstance serviceInstance) throws RuntimeException {

        assertDestroyed(UNREGISTER_ACTION);
        assertInitialized(UNREGISTER_ACTION);

        executeWithEvents(
                of(new ServiceInstancePreUnregisteredEvent(this, serviceInstance)),
                () -> serviceDiscovery.unregister(serviceInstance),
                of(new ServiceInstanceUnregisteredEvent(this, serviceInstance))
        );
    }

    @Override
    public Set<String> getServices() {
        return serviceDiscovery.getServices();
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws NullPointerException {
        return serviceDiscovery.getInstances(serviceName);
    }

    @Override
    public Page<ServiceInstance> getInstances(String serviceName, int offset, int pageSize) throws NullPointerException, IllegalArgumentException {
        return serviceDiscovery.getInstances(serviceName, offset, pageSize);
    }

    @Override
    public Page<ServiceInstance> getInstances(String serviceName, int offset, int pageSize, boolean healthyOnly) throws NullPointerException, IllegalArgumentException {
        return serviceDiscovery.getInstances(serviceName, offset, pageSize, healthyOnly);
    }

    @Override
    public Map<String, Page<ServiceInstance>> getInstances(Iterable<String> serviceNames, int offset, int requestSize) throws NullPointerException, IllegalArgumentException {
        return serviceDiscovery.getInstances(serviceNames, offset, requestSize);
    }

    @Override
    public String toString() {
        return serviceDiscovery.toString();
    }

    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws NullPointerException, IllegalArgumentException {
        serviceDiscovery.addServiceInstancesChangedListener(listener);
        eventDispatcher.addEventListener(listener);
    }

    @Override
    public void initialize(URL registryURL) {

        assertInitialized(INITIALIZE_ACTION);

        if (isInitialized()) {
            if (logger.isWarnEnabled()) {
                logger.warn("It's ignored to start current ServiceDiscovery, because it has been started.");
            }
            return;
        }

        executeWithEvents(
                of(new ServiceDiscoveryInitializingEvent(this, serviceDiscovery)),
                () -> serviceDiscovery.initialize(registryURL),
                of(new ServiceDiscoveryInitializedEvent(this, serviceDiscovery))
        );

        // doesn't start -> started
        initialized.compareAndSet(false, true);
    }

    @Override
    public void destroy() {

        assertDestroyed(DESTROY_ACTION);

        if (isDestroyed()) {
            if (logger.isWarnEnabled()) {
                logger.warn("It's ignored to stop current ServiceDiscovery, because it has been stopped.");
            }
            return;
        }

        executeWithEvents(
                of(new ServiceDiscoveryDestroyingEvent(this, serviceDiscovery)),
                serviceDiscovery::destroy,
                of(new ServiceDiscoveryDestroyedEvent(this, serviceDiscovery))
        );

        // doesn't stop -> stopped
        destroyed.compareAndSet(false, true);
    }

    protected final void executeWithEvents(Optional<? extends Event> beforeEvent,
                                           ThrowableAction action,
                                           Optional<? extends Event> afterEvent) {
        beforeEvent.ifPresent(this::dispatchEvent);
        try {
            action.execute();
        } catch (Throwable e) {
            dispatchEvent(new ServiceDiscoveryExceptionEvent(this, serviceDiscovery, e));
        }
        afterEvent.ifPresent(this::dispatchEvent);
    }

    private void dispatchEvent(Event event) {
        eventDispatcher.dispatch(event);
    }

    public final boolean isInitialized() {
        return initialized.get();
    }

    public final boolean isDestroyed() {
        return destroyed.get();
    }

    protected void assertDestroyed(String action) throws IllegalStateException {
        if (!isInitialized()) {
            throw new IllegalStateException("The action[" + action + "] is rejected, because the ServiceDiscovery is not initialized yet.");
        }
    }

    protected void assertInitialized(String action) throws IllegalStateException {
        if (isDestroyed()) {
            throw new IllegalStateException("The action[" + action + "] is rejected, because the ServiceDiscovery is destroyed already.");
        }
    }
}
