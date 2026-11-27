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
package org.apache.dubbo.xds.traffic;

import org.apache.dubbo.remoting.api.connection.ConnectionStateProvider;
import org.apache.dubbo.remoting.api.connection.ConnectionStateProvider.State;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.xds.registry.XdsEndpoint;
import org.apache.dubbo.xds.security.NativeTlsAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

/** Per-reference priority history. Transport notifications never execute an RPC or application callback. */
final class XdsPriorityState implements AutoCloseable {
    private final Map<String, Map<Long, Child>> scopes = new HashMap<>();
    private final LongSupplier clock;
    private final long grace;
    private boolean closed;

    XdsPriorityState() {
        this(System::nanoTime, TimeUnit.SECONDS.toNanos(10));
    }

    XdsPriorityState(LongSupplier clock, long grace) {
        this.clock = clock;
        this.grace = grace;
    }

    /** Returns null while the selected priority is connecting; never waits for transport availability. */
    synchronized <T> List<Invoker<T>> ready(String scope, List<Invoker<T>> candidates) {
        return ready(scope, candidates, false);
    }

    synchronized <T> List<Invoker<T>> ready(String scope, List<Invoker<T>> candidates, boolean ring) {
        if (closed) throw new IllegalStateException("Native xDS priority state closed");
        Map<Long, List<Invoker<T>>> groups = new TreeMap<>();
        for (Invoker<T> invoker : candidates) {
            long priority = Integer.toUnsignedLong(
                    NativeTlsAdapter.endpoint(invoker.getUrl()).getLocality().getPriority());
            groups.computeIfAbsent(priority, unused -> new ArrayList<>()).add(invoker);
        }
        Map<Long, Child> children = scopes.computeIfAbsent(scope, unused -> new LinkedHashMap<>());
        for (Long priority : new ArrayList<>(children.keySet())) {
            if (!groups.containsKey(priority)) children.remove(priority).close();
        }
        for (Map.Entry<Long, List<Invoker<T>>> group : groups.entrySet()) {
            children.computeIfAbsent(group.getKey(), unused -> new Child()).refresh(group.getValue(), ring);
        }
        boolean connecting = false;
        for (Map.Entry<Long, List<Invoker<T>>> group : groups.entrySet()) {
            Child child = children.get(group.getKey());
            History history = child.history.get();
            if (history.state == State.READY) {
                if (ring) return new ArrayList<>(group.getValue());
                List<Invoker<T>> ready = new ArrayList<>();
                for (Invoker<T> invoker : group.getValue()) {
                    if (state(invoker) == State.READY && invoker.isAvailable()) ready.add(invoker);
                }
                if (!ready.isEmpty()) return ready;
                // The transport can publish a new state between aggregate observation and selection.
                // Reconcile it before treating an empty READY group as a failed priority.
                child.observe();
                history = child.history.get();
            }
            if (history.state == State.IDLE) return null;
            if (history.state == State.CONNECTING) {
                connecting = true;
                if (history.until != 0 && clock.getAsLong() - history.until < 0) return null;
            }
        }
        // After all grace timers expired, a remaining CONNECTING child still buffers instead of failing.
        return connecting ? null : new ArrayList<>();
    }

    static State state(Invoker<?> invoker) {
        Object provider = invoker.getUrl().getAttribute(ConnectionStateProvider.ATTRIBUTE);
        if (provider instanceof ConnectionStateProvider) {
            State state = ((ConnectionStateProvider) provider).getConnectivityState();
            if (state != null) return state;
        }
        return invoker.isAvailable() ? State.READY : State.TRANSIENT_FAILURE;
    }

    @Override
    public synchronized void close() {
        closed = true;
        scopes.values().forEach(children -> children.values().forEach(Child::close));
        scopes.clear();
    }

    private final class Child {
        private volatile boolean refreshing;
        private volatile boolean ring;
        final Map<String, Watch> watches = new ConcurrentHashMap<>();
        final AtomicReference<History> history =
                new AtomicReference<>(new History(State.CONNECTING, false, clock.getAsLong() + grace));

        void refresh(List<? extends Invoker<?>> invokers, boolean ring) {
            refreshing = true;
            this.ring = ring;
            Map<String, Invoker<?>> desired = new LinkedHashMap<>();
            for (Invoker<?> invoker : invokers) {
                XdsEndpoint endpoint = NativeTlsAdapter.endpoint(invoker.getUrl());
                desired.put(endpoint.identity(), invoker);
            }
            for (String key : new ArrayList<>(watches.keySet())) {
                if (!desired.containsKey(key)) watches.remove(key).close();
            }
            desired.forEach((key, invoker) -> {
                Object provider = invoker.getUrl().getAttribute(ConnectionStateProvider.ATTRIBUTE);
                Watch previous = watches.get(key);
                if (previous != null && previous.provider != provider) {
                    watches.remove(key).close();
                    previous = null;
                }
                if (previous == null) {
                    Watch watch = new Watch(provider, state(invoker));
                    watches.put(key, watch);
                    if (provider instanceof ConnectionStateProvider) {
                        watch.subscription = ((ConnectionStateProvider) provider).addConnectivityListener(next -> {
                            if (watch.active.get()) {
                                watch.state.set(next);
                                if (!refreshing) observe();
                            }
                        });
                    }
                } else if (!(provider instanceof ConnectionStateProvider)) {
                    previous.state.set(state(invoker));
                }
            });
            refreshing = false;
            observe();
        }

        private void observe() {
            History previous;
            History next;
            do {
                State aggregate = State.TRANSIENT_FAILURE;
                int failures = 0;
                for (Watch watch : watches.values()) {
                    State value = watch.current();
                    if (value == State.READY) {
                        aggregate = State.READY;
                        break;
                    }
                    if (value == State.CONNECTING) aggregate = State.CONNECTING;
                    else if (value == State.IDLE && aggregate != State.CONNECTING) aggregate = State.IDLE;
                    if (value == State.TRANSIENT_FAILURE) failures++;
                }
                if (ring && aggregate != State.READY) {
                    if (failures >= 2) aggregate = State.TRANSIENT_FAILURE;
                    else if (failures == 1 && watches.size() > 1) aggregate = State.CONNECTING;
                }
                previous = history.get();
                if (aggregate == previous.state) return;
                boolean seen = previous.seenReady;
                long until = previous.until;
                if (aggregate == State.READY || aggregate == State.IDLE) {
                    seen = true;
                    until = 0;
                } else if (aggregate == State.TRANSIENT_FAILURE) {
                    seen = false;
                    until = 0;
                } else if (aggregate == State.CONNECTING && seen) until = clock.getAsLong() + grace;
                next = new History(aggregate, seen, until);
            } while (!history.compareAndSet(previous, next));
        }

        void close() {
            for (Watch watch : watches.values()) watch.close();
            watches.clear();
        }
    }

    private static final class Watch {
        final Object provider;
        final AtomicReference<State> state;
        final AtomicBoolean active = new AtomicBoolean(true);
        AutoCloseable subscription;

        Watch(Object provider, State state) {
            this.provider = provider;
            this.state = new AtomicReference<>(state);
        }

        State current() {
            // State publication precedes listener delivery. Delayed notifications must not override
            // the live transport state used for priority grace/failover decisions.
            if (provider instanceof ConnectionStateProvider) {
                State current = ((ConnectionStateProvider) provider).getConnectivityState();
                if (current != null) return current;
            }
            return state.get();
        }

        void close() {
            active.set(false);
            if (subscription != null) {
                try {
                    subscription.close();
                } catch (Exception error) {
                    throw new IllegalStateException("Cannot release connection state subscription", error);
                }
            }
        }
    }

    private static final class History {
        final State state;
        final boolean seenReady;
        final long until;

        History(State state, boolean seenReady, long until) {
            this.state = state;
            this.seenReady = seenReady;
            this.until = until;
        }
    }
}
