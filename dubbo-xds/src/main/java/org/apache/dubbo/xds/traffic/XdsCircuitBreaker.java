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

import org.apache.dubbo.rpc.StatusRpcException;
import org.apache.dubbo.rpc.TriRpcStatus;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.CircuitBreakers;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.core.v3.RoutingPriority;

/** A32 call counters are shared across references and applications in the same workload. */
public final class XdsCircuitBreaker {
    private static final ConcurrentHashMap<Key, Counter> COUNTERS = new ConcurrentHashMap<>();

    private XdsCircuitBreaker() {}

    public static long limit(Cluster cluster) {
        for (CircuitBreakers.Thresholds threshold : cluster.getCircuitBreakers().getThresholdsList()) {
            if (threshold.getPriority() == RoutingPriority.DEFAULT) {
                return threshold.hasMaxRequests()
                        ? Integer.toUnsignedLong(threshold.getMaxRequests().getValue())
                        : 1024;
            }
        }
        return 1024;
    }

    public static Permit acquire(String workload, Cluster cluster) {
        Key key = new Key(workload, cluster);
        Permit[] admitted = new Permit[1];
        long maximum = limit(cluster);
        COUNTERS.compute(key, (ignored, existing) -> {
            Counter counter = existing == null ? new Counter() : existing;
            if (counter.active < maximum) {
                counter.active++;
                admitted[0] = new Permit(key, counter);
            } else {
                counter.dropped++;
            }
            return counter.active == 0 ? null : counter;
        });
        if (admitted[0] == null) {
            throw new DropException(cluster.getName());
        }
        return admitted[0];
    }

    public static long active(String workload, Cluster cluster) {
        Counter counter = COUNTERS.get(new Key(workload, cluster));
        return counter == null ? 0 : counter.active;
    }

    public static long dropped(String workload, Cluster cluster) {
        Counter counter = COUNTERS.get(new Key(workload, cluster));
        return counter == null ? 0 : counter.dropped;
    }

    static int activeCounterCount() {
        return COUNTERS.size();
    }

    private static final class Counter {
        volatile long active;
        volatile long dropped;
    }

    private static final class Key {
        final String workload;
        final String cluster;
        final String eds;

        Key(String workload, Cluster cluster) {
            this.workload = Objects.requireNonNull(workload);
            this.cluster = cluster.getName();
            eds = cluster.getEdsClusterConfig().getServiceName();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Key)) return false;
            Key value = (Key) other;
            return workload.equals(value.workload) && cluster.equals(value.cluster) && eds.equals(value.eds);
        }

        @Override
        public int hashCode() {
            return Objects.hash(workload, cluster, eds);
        }
    }

    public static final class Permit implements AutoCloseable {
        private final Key key;
        private final Counter counter;
        private final AtomicBoolean released = new AtomicBoolean();

        Permit(Key key, Counter counter) {
            this.key = key;
            this.counter = counter;
        }

        @Override
        public void close() {
            if (released.compareAndSet(false, true)) {
                COUNTERS.computeIfPresent(key, (ignored, current) -> {
                    if (current == counter) current.active--;
                    return current.active == 0 ? null : current;
                });
            }
        }
    }

    /** This is a local drop, not an upstream UNAVAILABLE response eligible for retry. */
    public static final class DropException extends StatusRpcException {
        public static DropException eds() {
            return new DropException(
                    TriRpcStatus.UNAVAILABLE.withDescription("xDS EDS drop policy rejected the request"));
        }

        private DropException(TriRpcStatus status) {
            super(status);
        }

        private DropException(String cluster) {
            super(TriRpcStatus.UNAVAILABLE.withDescription("xDS max_requests limit reached for " + cluster));
        }
    }
}
