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
package org.apache.dubbo.common.event;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class DefaultDubboEventMulticaster implements DubboLifecycleEventMulticaster {

    /**
     * All registered subscribers, indexed by event type.
     *
     * <p>The {@link CopyOnWriteArraySet} values make it easy and relatively lightweight to get an
     * immutable snapshot of all current subscribers to an event without any locking.
     */
    private final Set<DubboListener<?>> listeners = new CopyOnWriteArraySet<>();

    final Map<Class<?>, Set<DubboListener<?>>> listenerCache = new ConcurrentHashMap<>(64);

    /**
     * Registers all subscriber methods on the given listener object.
     *
     * @param listener object whose subscriber methods should be registered.
     */
    @Override
    public void addListener(DubboListener<?> listener) {
        synchronized (this.listeners) {
            this.listeners.add(listener);
            listenerCache.clear();
        }
    }

    /**
     * Registers all subscriber methods on the given listener object.
     *
     * @param listener object whose subscriber methods should be registered.
     */
    @Override
    public void removeListener(DubboListener<?> listener) {
        synchronized (this.listeners) {
            this.listeners.remove(listener);
            listenerCache.clear();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void publishEvent(DubboEvent event) {
        for (DubboListener listener : getListeners(event.getClass())) {
            listener.onEvent(event);
        }
    }

    /**
     * Return the listeners for the given event type.
     *
     * @param eventType the type of event to find listeners for.
     * @return a live {@link Collection} of listeners for the given event type. If no listeners are
     *     found, this method returns an empty collection.
     */
    public Collection<DubboListener<?>> getListeners(Class<? extends DubboEvent> eventType) {
        return listenerCache.computeIfAbsent(eventType, key -> {
            Set<DubboListener<?>> listeners = new CopyOnWriteArraySet<>();
            for (DubboListener<?> listener : this.listeners) {
                if (listener.support(eventType)) {
                    listeners.add(listener);
                }
            }
            return listeners;
        });
    }

    /**
     * Remove a listener from the notification list.
     *
     * @param listener the listener to remove
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void publishBeforeEvent(DubboEvent event) {
        for (DubboListener<?> listener : getListeners(event.getClass())) {
            if (listener instanceof DubboLifecycleListener) {
                ((DubboLifecycleListener) listener).onEventBefore(event);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void publishErrorEvent(DubboEvent event) {
        for (DubboListener<?> listener : getListeners(event.getClass())) {
            if (listener instanceof DubboLifecycleListener) {
                ((DubboLifecycleListener) listener).onEventError(event);
            }
        }
    }
}
