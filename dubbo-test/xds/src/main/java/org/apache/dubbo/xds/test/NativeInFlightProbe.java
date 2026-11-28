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

import org.apache.dubbo.common.stream.ClientCallStreamObserver;
import org.apache.dubbo.common.stream.ClientResponseObserver;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.TriRpcStatus.Code;
import org.apache.dubbo.xds.test.stream.NativeStreamMessage;
import org.apache.dubbo.xds.test.stream.NativeStreams;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson2.JSON;

/** Test-only holds with provider dispatch evidence and nonblocking consumer observations. */
final class NativeInFlightProbe {
    private static final Map<String, Call> CALLS = new ConcurrentHashMap<>();
    private final Path directory;
    private final String identity;

    NativeInFlightProbe(Path runtime, String identity) {
        this.directory = runtime.resolve("holds");
        this.identity = identity;
    }

    CompletableFuture<String> unary(String message) {
        Hold hold = begin(message, "unary");
        return CompletableFuture.supplyAsync(() -> release(hold));
    }

    void stream(String message, StreamObserver<String> observer) {
        Hold hold = begin(message, "stream");
        observer.onNext(identity + ":hold:" + hold.token + ":0");
        CompletableFuture.runAsync(() -> {
            try {
                String value = release(hold);
                observer.onNext(value + ":1");
                observer.onNext(value + ":2");
                observer.onCompleted();
            } catch (Throwable error) {
                observer.onError(error);
            }
        });
    }

    StreamObserver<NativeStreamMessage> requests(StreamObserver<NativeStreamMessage> observer, boolean bidi) {
        CancellationContext context = RpcContext.getCancellationContext();
        return new StreamObserver<NativeStreamMessage>() {
            private Hold hold;

            public synchronized void onNext(NativeStreamMessage request) {
                if (hold == null) {
                    String value = request.getValue();
                    if (!value.startsWith("hold:"))
                        throw TriRpcStatus.fromCode(Code.INVALID_ARGUMENT).asException();
                    hold = begin(value, bidi ? "bidi" : "client-stream", context);
                    Hold active = hold;
                    receive(request);
                    CompletableFuture.runAsync(() -> {
                        try {
                            String valueOut = release(active);
                            if (active.requests.size() != 2)
                                throw TriRpcStatus.fromCode(Code.INVALID_ARGUMENT)
                                        .asException();
                            if (!bidi) observer.onNext(message(valueOut + ":collected=2"));
                            observer.onCompleted();
                        } catch (Throwable error) {
                            observer.onError(error);
                        }
                    });
                } else receive(request);
            }

            private void receive(NativeStreamMessage request) {
                int index = hold.requests.size();
                String expected = index == 0 ? "hold:" + hold.token : hold.token + ":1";
                if (hold.halfClosed || index >= 2 || !expected.equals(request.getValue()))
                    throw TriRpcStatus.fromCode(Code.INVALID_ARGUMENT).asException();
                hold.requests.add(request.getValue());
                publish(hold, "ACTIVE");
                if (bidi) observer.onNext(message(identity + ":hold:" + hold.token + ":" + index));
            }

            public void onError(Throwable error) {
                // Triple cancels the captured call context; the release worker observes it.
            }

            public synchronized void onCompleted() {
                if (hold == null || hold.halfClosed)
                    throw TriRpcStatus.fromCode(Code.INVALID_ARGUMENT).asException();
                synchronized (NativeInFlightProbe.this) {
                    hold.halfClosed = true;
                    publish(hold, "ACTIVE");
                }
            }
        };
    }

    private static NativeStreamMessage message(String value) {
        return NativeStreamMessage.newBuilder().setValue(value).build();
    }

    private Hold begin(String message, String kind) {
        return begin(message, kind, RpcContext.getCancellationContext());
    }

    private Hold begin(String message, String kind, CancellationContext context) {
        String token = token(message.substring(5));
        Hold hold = new Hold(token, kind, context);
        try {
            Files.createDirectories(directory);
            // One dispatch per token. A repeated dispatch is visible and fails the verifier.
            Files.createFile(directory.resolve(token + ".claimed"));
            publish(hold, "ACTIVE");
            return hold;
        } catch (IOException error) {
            throw new IllegalStateException("Could not record held provider dispatch", error);
        }
    }

    private String release(Hold hold) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(hold.budgetMillis);
        String status = "FAILED";
        try {
            while (!Files.exists(directory.resolve(hold.token + ".release"))
                    || (("client-stream".equals(hold.kind) || "bidi".equals(hold.kind)) && !hold.halfClosed)) {
                if (hold.context.isCancelled()) throw TriRpcStatus.CANCELLED.asException();
                if (System.nanoTime() >= deadline) throw TriRpcStatus.DEADLINE_EXCEEDED.asException();
                Thread.sleep(20);
            }
            status = "RELEASED";
            return identity + ":hold:" + hold.token;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw TriRpcStatus.CANCELLED.asException();
        } finally {
            publish(hold, status);
        }
    }

    private synchronized void publish(Hold hold, String status) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("identity", identity);
        value.put("token", hold.token);
        value.put("kind", hold.kind);
        value.put("started_at_ms", hold.startedAt);
        value.put("budget_ms", hold.budgetMillis);
        value.put("elapsed_ms", System.currentTimeMillis() - hold.startedAt);
        value.put("status", status);
        value.put("attempts", 1);
        value.put("cancelled", hold.context.isCancelled());
        value.put("request_messages", new ArrayList<>(hold.requests));
        value.put("half_closed", hold.halfClosed);
        try {
            Path temporary = directory.resolve(hold.token + ".tmp");
            Files.write(temporary, JSON.toJSONString(value).getBytes(StandardCharsets.UTF_8));
            Files.move(temporary, directory.resolve(hold.token + ".json"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) {
            throw new IllegalStateException("Could not publish held provider observation", error);
        }
    }

    static synchronized Map<String, Object> execute(Properties properties, NativeEcho remote, NativeStreams streams) {
        String token = token(properties.getProperty("message", ""));
        String method = properties.getProperty("method");
        if ("hold-start".equals(method)) {
            if (CALLS.size() >= 8 || CALLS.containsKey(token))
                throw new IllegalStateException("Held call capacity/token");
            String kind = properties.getProperty("key", "unary");
            if (!"unary".equals(kind)
                    && !"stream".equals(kind)
                    && !"client-stream".equals(kind)
                    && !"bidi".equals(kind)) throw new IllegalArgumentException("Invalid hold kind");
            if (("client-stream".equals(kind) || "bidi".equals(kind)) && streams == null)
                throw new IllegalArgumentException("Enable the native streaming fixture");
            Call call = new Call(kind);
            CALLS.put(token, call);
            RpcContext.getClientAttachment().setAttachment("timeout", call.budgetMillis);
            try {
                if ("unary".equals(kind)) {
                    call.unary = remote.asyncEcho("hold:" + token);
                    call.unary.whenComplete((value, error) -> {
                        if (error == null) call.next(value);
                        call.end(error);
                    });
                } else if ("stream".equals(kind))
                    remote.watch("hold:" + token, new ClientResponseObserver<String, String>() {
                        public void beforeStart(ClientCallStreamObserver<String> writer) {
                            call.writer = writer;
                        }

                        public void onNext(String value) {
                            call.next(value);
                        }

                        public void onCompleted() {
                            call.end(null);
                        }

                        public void onError(Throwable error) {
                            call.end(error);
                        }
                    });
                else {
                    ClientResponseObserver<NativeStreamMessage, NativeStreamMessage> observer =
                            new ClientResponseObserver<NativeStreamMessage, NativeStreamMessage>() {
                                public void beforeStart(ClientCallStreamObserver<NativeStreamMessage> writer) {
                                    call.writer = writer;
                                }

                                public void onNext(NativeStreamMessage value) {
                                    call.next(value.getValue());
                                }

                                public void onCompleted() {
                                    call.end(null);
                                }

                                public void onError(Throwable error) {
                                    call.end(error);
                                }
                            };
                    call.requests = "bidi".equals(kind) ? streams.chat(observer) : streams.collect(observer);
                    call.requests.onNext(message("hold:" + token));
                }
            } catch (Throwable error) {
                if (!call.done()) call.end(error);
            } finally {
                RpcContext.removeContext();
            }
        }
        Call call = CALLS.get(token);
        if (call == null) throw new IllegalArgumentException("Unknown held token");
        if ("hold-send".equals(method)) {
            if (call.requests == null || call.done() || call.halfClosed)
                throw new IllegalStateException("Cannot send to this held call");
            call.requests.onNext(message(token + ":1"));
            call.requests.onCompleted();
            call.halfClosed = true;
        }
        Map<String, Object> result = call.snapshot();
        if ("hold-forget".equals(method)) {
            if (!call.done()) throw new IllegalStateException("Cannot forget an active call");
            CALLS.remove(token);
        } else if (!"hold-start".equals(method) && !"hold-status".equals(method) && !"hold-send".equals(method)) {
            throw new IllegalArgumentException("Invalid held method");
        }
        return result;
    }

    static synchronized void close() {
        CALLS.values().forEach(call -> {
            if (call.unary != null && !call.unary.isDone()) call.unary.cancel(false);
            if (call.writer != null && !call.done()) call.writer.onError(TriRpcStatus.CANCELLED.asException());
        });
        CALLS.clear();
    }

    private static String token(String value) {
        if (!value.matches("[A-Za-z0-9_-]{1,64}")) throw new IllegalArgumentException("Invalid held token");
        return value;
    }

    private static int fixtureBudgetMillis() {
        String configured = System.getenv("NATIVE_XDS_HOLD_BUDGET_MS");
        int budget = configured == null ? 180000 : Integer.parseInt(configured);
        if (budget < 1000 || budget > 1800000) throw new IllegalArgumentException("Invalid test hold budget");
        return budget;
    }

    private static final class Hold {
        final String token;
        final String kind;
        final CancellationContext context;
        final int budgetMillis = fixtureBudgetMillis();
        final List<String> requests = new CopyOnWriteArrayList<>();
        volatile boolean halfClosed;
        final long startedAt = System.currentTimeMillis();

        Hold(String token, String kind, CancellationContext context) {
            this.token = token;
            this.kind = kind;
            this.context = context;
        }
    }

    private static final class Call {
        final String kind;
        final int budgetMillis = fixtureBudgetMillis();
        final long startedAt = System.currentTimeMillis();
        final long startedNanos = System.nanoTime();
        final List<String> values = new ArrayList<>();
        CompletableFuture<String> unary;
        volatile ClientCallStreamObserver<?> writer;
        StreamObserver<NativeStreamMessage> requests;
        boolean halfClosed;
        int terminals;
        long completedAt;
        long completionElapsedMillis;
        String status = "ACTIVE";

        Call(String kind) {
            this.kind = kind;
        }

        synchronized void next(String value) {
            values.add(value);
        }

        synchronized void end(Throwable error) {
            if (terminals == 0) {
                completedAt = System.currentTimeMillis();
                completionElapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
            }
            terminals++;
            status = error == null ? "OK" : TriRpcStatus.getStatus(error).code.name();
        }

        synchronized boolean done() {
            return terminals > 0;
        }

        synchronized Map<String, Object> snapshot() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("kind", kind);
            result.put("started_at_ms", startedAt);
            result.put("budget_ms", budgetMillis);
            result.put("elapsed_ms", System.currentTimeMillis() - startedAt);
            if (terminals > 0) {
                result.put("completed_at_ms", completedAt);
                result.put("completion_elapsed_ms", completionElapsedMillis);
            }
            result.put("status", status);
            result.put("terminals", terminals);
            result.put("values", new ArrayList<>(values));
            result.put("half_closed", halfClosed);
            return result;
        }
    }
}
