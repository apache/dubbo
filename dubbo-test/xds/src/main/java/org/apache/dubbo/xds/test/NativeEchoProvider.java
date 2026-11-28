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
package org.apache.dubbo.xds.test;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.TriRpcStatus.Code;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.fastjson2.JSON;

import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;

/** Bounded, secured business fixture for native deadline/retry/fault acceptance. */
final class NativeEchoProvider implements NativeEcho {
    private final String identity;
    private final NativeInFlightProbe held;
    private final Map<String, Counters> counters = new ConcurrentHashMap<>();

    NativeEchoProvider(String identity, Path runtime) {
        this.identity = identity;
        this.held = new NativeInFlightProbe(runtime, identity);
    }

    public String echo(String message) {
        if (message.startsWith("stats:")) return stats(token(message.substring(6)));
        if (message.startsWith("reset:")) {
            String key = token(message.substring(6));
            synchronized (counters) {
                Counters previous = counters.get(key);
                if (previous != null && previous.active.get() != 0)
                    throw TriRpcStatus.fromCode(Code.FAILED_PRECONDITION).asException();
                counters.remove(key);
            }
            return identity + ":reset";
        }
        Probe probe = record(message);
        return probe == null ? ordinary(message) : respond(message, probe);
    }

    public CompletableFuture<String> asyncEcho(String message) {
        if (message.startsWith("hold:")) return held.unary(message);
        if (!message.startsWith("probe:")) return CompletableFuture.completedFuture(echo(message));
        Probe probe = record(message);
        return CompletableFuture.supplyAsync(() -> respond(message, probe));
    }

    public void watch(String message, StreamObserver<String> observer) {
        if (message.startsWith("hold:")) {
            held.stream(message, observer);
            return;
        }
        Probe probe = record(message);
        Runnable response = () -> {
            try {
                String value = probe == null ? ordinary(message) : respond(message, probe);
                if (probe != null && probe.context.isCancelled()) return;
                for (int i = 0; i < 3; i++) observer.onNext(value + ":" + i);
                observer.onCompleted();
            } catch (Throwable error) {
                if (probe == null || !probe.context.isCancelled()) observer.onError(error);
            }
        };
        if (probe == null) response.run();
        else CompletableFuture.runAsync(response);
    }

    private String ordinary(String message) {
        if (message.startsWith("unavailable:")) throw TriRpcStatus.UNAVAILABLE.asException();
        return identity + ":" + message;
    }

    private Probe record(String message) {
        if (!message.startsWith("probe:")) return null;
        String[] fields = message.split(":", -1);
        if (fields.length != 4)
            throw TriRpcStatus.fromCode(Code.INVALID_ARGUMENT).asException();
        String key = token(fields[1]);
        int delay = Integer.parseInt(fields[2]);
        int failures = Integer.parseInt(fields[3]);
        if (delay < 0 || delay > 5000 || failures < 0 || failures > 10)
            throw TriRpcStatus.fromCode(Code.INVALID_ARGUMENT).asException();
        Counters state;
        synchronized (counters) {
            state = counters.get(key);
            if (state == null) {
                if (counters.size() >= 256)
                    throw TriRpcStatus.fromCode(Code.RESOURCE_EXHAUSTED).asException();
                state = new Counters();
                counters.put(key, state);
            }
            state.active.incrementAndGet();
        }
        int attempt = state.attempts.incrementAndGet();
        if (attempt <= 32) {
            Object timeout = RpcContext.getServiceContext().getInvocation().get(TIMEOUT_KEY);
            state.timeouts.add(timeout instanceof Number ? ((Number) timeout).longValue() : -1L);
            String previous =
                    RpcContext.getServiceContext().getInvocation().getAttachment("grpc-previous-rpc-attempts");
            state.previous.add(previous == null ? "" : previous);
        }
        Counters recorded = state;
        CancellationContext context = RpcContext.getCancellationContext();
        Probe probe = new Probe(state, context, attempt, delay, failures);
        context.addListener(ignored -> {
            if (context.getCancellationCause() != null) {
                recorded.cancelled.incrementAndGet();
                if (!probe.finished.get()) recorded.cancelledWhileRunning.incrementAndGet();
            }
        });
        return probe;
    }

    private String respond(String message, Probe probe) {
        try {
            Thread.sleep(probe.delay);
            if (probe.attempt <= probe.failures) throw TriRpcStatus.UNAVAILABLE.asException();
            return identity + ":" + message + ":attempt=" + probe.attempt;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw TriRpcStatus.CANCELLED.asException();
        } finally {
            probe.finished.set(true);
            probe.counters.active.decrementAndGet();
        }
    }

    private String stats(String key) {
        Counters state = counters.get(key);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attempts", state == null ? 0 : state.attempts.get());
        result.put("active", state == null ? 0 : state.active.get());
        result.put("cancelled", state == null ? 0 : state.cancelled.get());
        result.put("cancelled_while_running", state == null ? 0 : state.cancelledWhileRunning.get());
        result.put("received_timeout_ms", state == null ? new ArrayList<>() : new ArrayList<>(state.timeouts));
        result.put("previous_attempts", state == null ? new ArrayList<>() : new ArrayList<>(state.previous));
        return identity + ":" + JSON.toJSONString(result);
    }

    private static String token(String value) {
        if (!value.matches("[A-Za-z0-9_-]{1,64}"))
            throw TriRpcStatus.fromCode(Code.INVALID_ARGUMENT).asException();
        return value;
    }

    private static final class Counters {
        final AtomicInteger attempts = new AtomicInteger();
        final AtomicInteger active = new AtomicInteger();
        final AtomicInteger cancelled = new AtomicInteger();
        final AtomicInteger cancelledWhileRunning = new AtomicInteger();
        final List<Long> timeouts = new CopyOnWriteArrayList<>();
        final List<String> previous = new CopyOnWriteArrayList<>();
    }

    private static final class Probe {
        final Counters counters;
        final CancellationContext context;
        final int attempt;
        final int delay;
        final int failures;
        final AtomicBoolean finished = new AtomicBoolean();

        Probe(Counters counters, CancellationContext context, int attempt, int delay, int failures) {
            this.counters = counters;
            this.context = context;
            this.attempt = attempt;
            this.delay = delay;
            this.failures = failures;
        }
    }
}
