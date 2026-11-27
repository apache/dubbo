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
package org.apache.dubbo.xds.security;

import org.apache.dubbo.xds.bootstrap.Bootstrapper.BootstrapInfo;
import org.apache.dubbo.xds.client.XdsClient.ResourceWatcher;
import org.apache.dubbo.xds.client.XdsResourceType;
import org.apache.dubbo.xds.client.XdsRuntimePool;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.grpc.xds.shaded.io.envoyproxy.envoy.config.listener.v3.Listener;

/** A reserved native port is fail-closed until LDS arrives, and stays closed after its last owner leaves. */
public final class XdsServerBindings {
    private static final Map<Integer, Binding> PORTS = new ConcurrentHashMap<>();

    private XdsServerBindings() {}

    public static ServerLease acquire(BootstrapInfo bootstrap, String address, int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Native xDS requires an explicit provider port");
        }
        String name = bootstrap.serverListenerName(address);
        synchronized (XdsServerBindings.class) {
            Binding binding = PORTS.get(port);
            if (binding == null || binding.closed.get()) {
                binding = new Binding(bootstrap, name, port);
                PORTS.put(port, binding);
            } else if (!binding.listenerName.equals(name) || !binding.fingerprint.equals(bootstrap.getFingerprint())) {
                throw new IllegalStateException("Conflicting xDS inbound ownership for port " + port);
            }
            binding.references++;
            return new ServerLease(binding);
        }
    }

    static Binding binding(int port) {
        return PORTS.get(port);
    }

    public static boolean isManaged(int port) {
        return PORTS.containsKey(port);
    }

    // Called only after the owning protocol has closed its servers. Old guarded streams retain their Binding.
    static synchronized void discardRetired(int port) {
        Binding binding = PORTS.get(port);
        if (binding != null && binding.closed.get()) {
            PORTS.remove(port, binding);
        }
    }

    static final class Binding {
        final String fingerprint;
        final String listenerName;
        final int port;
        final XdsRuntimePool.Lease runtime;
        final CompletableFuture<Void> initial = new CompletableFuture<>();
        final AtomicBoolean closed = new AtomicBoolean();
        final AutoCloseable watch;
        volatile InboundPolicy policy;
        volatile String lastError;
        int references;

        Binding(BootstrapInfo bootstrap, String listenerName, int port) {
            fingerprint = bootstrap.getFingerprint();
            this.listenerName = listenerName;
            this.port = port;
            runtime = XdsRuntimePool.acquire(bootstrap);
            watch = runtime.getClient().watch(XdsResourceType.LDS, listenerName, new ResourceWatcher<Listener>() {
                @Override
                public void onChanged(Listener listener) {
                    if (closed.get()) {
                        return;
                    }
                    try {
                        InboundPolicy next = InboundPolicy.parse(listener, bootstrap);
                        if (next.getPort() != port) {
                            throw new IllegalArgumentException("Inbound LDS does not match the reserved port");
                        }
                        policy = next;
                        lastError = null;
                        initial.complete(null);
                    } catch (RuntimeException error) {
                        lastError = error.getClass().getSimpleName();
                        // Resource validation ordinarily catches this before ACK; never apply a partial policy.
                    }
                }

                @Override
                public void onAbsent() {
                    policy = null;
                    lastError = "InboundListenerAbsent";
                    initial.completeExceptionally(new IllegalStateException("Inbound LDS listener is absent"));
                }

                @Override
                public void onError(Throwable error) {
                    lastError = error.getClass().getSimpleName();
                }
            });
        }

        NativeCertificate credentials() {
            InboundPolicy current = policy;
            if (closed.get() || current == null) {
                throw new IllegalStateException("Native inbound LDS is not ready: " + lastError);
            }
            return current.credentials(runtime.getSecurity());
        }

        void close() {
            if (closed.compareAndSet(false, true)) {
                policy = null;
                initial.completeExceptionally(new IllegalStateException("Native inbound binding is closed"));
                try {
                    watch.close();
                } catch (Exception ignored) {
                    /* The pool also closes all watches with its last lease. */
                }
                runtime.close();
            }
        }
    }

    public static final class ServerLease implements AutoCloseable {
        private final Binding binding;
        private final AtomicBoolean closed = new AtomicBoolean();

        private ServerLease(Binding binding) {
            this.binding = binding;
        }

        public int getPort() {
            return binding.port;
        }

        public void awaitInitial(long waitMillis) {
            if (waitMillis < 1 || waitMillis > 60000) {
                throw new IllegalArgumentException("Inbound LDS wait must be 1..60000 milliseconds");
            }
            try {
                long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(waitMillis);
                if (binding.policy == null) {
                    binding.initial.get(waitMillis, TimeUnit.MILLISECONDS);
                }
                // Wait on the exporting thread, never a transport event loop; agent files may lag the first LDS.
                while (true) {
                    try {
                        binding.credentials();
                        return;
                    } catch (IllegalStateException notReady) {
                        long remaining = deadline - System.nanoTime();
                        if (binding.closed.get() || binding.policy == null || remaining <= 0) {
                            throw notReady;
                        }
                        TimeUnit.NANOSECONDS.sleep(Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(100)));
                    }
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted waiting for inbound LDS", interrupted);
            } catch (Exception error) {
                throw new IllegalStateException("Unable to activate inbound LDS for port " + binding.port, error);
            }
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                synchronized (XdsServerBindings.class) {
                    if (--binding.references == 0) {
                        binding.close();
                    }
                }
            }
        }
    }
}
