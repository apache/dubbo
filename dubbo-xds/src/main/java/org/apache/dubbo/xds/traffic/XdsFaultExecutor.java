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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.FutureContext;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.InvokeMode;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TimeoutCountDown;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.MethodDescriptor.RpcType;
import org.apache.dubbo.rpc.protocol.dubbo.FutureAdapter;
import org.apache.dubbo.rpc.protocol.tri.TripleInvoker;
import org.apache.dubbo.rpc.support.RpcUtils;
import org.apache.dubbo.xds.traffic.XdsFaultPolicy.Decision;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/** Bounded fault and endpoint-readiness gate. No timer thread runs application callbacks. */
public final class XdsFaultExecutor implements AutoCloseable {
    static final String RESUME_ATTRIBUTE = "dubbo.xds.fault-resume";
    private final ScheduledThreadPoolExecutor timer =
            new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("dubbo-xds-fault-timer", true));
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(
            2,
            4,
            30,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1024),
            new NamedThreadFactory("dubbo-xds-fault", true));
    private final Semaphore slots = new Semaphore(1024);
    private final Set<Call> calls = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    public XdsFaultExecutor() {
        timer.setRemoveOnCancelPolicy(true);
    }

    public Result invoke(
            URL url,
            Invocation invocation,
            Collection<XdsFaultPolicy> policies,
            Map<String, String> headers,
            Function<RpcInvocation, Result> action) {
        if (closed || !slots.tryAcquire())
            throw TriRpcStatus.fromCode(TriRpcStatus.Code.RESOURCE_EXHAUSTED)
                    .withDescription("Native xDS fault executor is closed or saturated")
                    .asException();
        if (invocation instanceof RpcInvocation) {
            ((RpcInvocation) invocation).setInvokeMode(RpcUtils.getInvokeMode(url, invocation));
        }
        Call call;
        try {
            call = new Call(url, invocation, policies, headers, action);
        } catch (RuntimeException | Error error) {
            slots.release();
            throw error;
        }
        synchronized (this) {
            if (closed) {
                slots.release();
                throw TriRpcStatus.CANCELLED
                        .withDescription("Native xDS fault executor has been closed")
                        .asException();
            }
            calls.add(call);
        }
        try {
            call.prepare();
            call.attachLifecycle();
            call.start();
        } catch (Throwable error) {
            call.fail(TriRpcStatus.getStatus(error));
        }
        if (closed) call.fail(TriRpcStatus.CANCELLED.withDescription("Native xDS reference destroyed"));
        if (call.stream != null) {
            return new AsyncRpcResult(
                    CompletableFuture.completedFuture(
                            new AppResponse(call.type == RpcType.SERVER_STREAM ? null : call.stream.writer)),
                    invocation);
        }
        AsyncRpcResult result = new AsyncRpcResult(call, invocation);
        FutureContext.getContext().setCompatibleFuture(call);
        RpcContext.getServiceContext().setFuture(new FutureAdapter<>(call));
        if (invocation instanceof RpcInvocation && ((RpcInvocation) invocation).getInvokeMode() == InvokeMode.SYNC) {
            try {
                result.get();
            } catch (InterruptedException error) {
                call.cancel(true);
                Thread.currentThread().interrupt();
                throw TriRpcStatus.CANCELLED.withCause(error).asException();
            } catch (ExecutionException error) {
                throw TriRpcStatus.getStatus(error.getCause()).asException();
            } catch (CancellationException error) {
                throw TriRpcStatus.CANCELLED.withCause(error).asException();
            }
        }
        return result;
    }

    @Override
    public void close() {
        synchronized (this) {
            closed = true;
        }
        calls.forEach(call -> call.fail(TriRpcStatus.CANCELLED.withDescription("Native xDS reference destroyed")));
        timer.shutdownNow();
        shutdownIfIdle();
    }

    private synchronized void shutdownIfIdle() {
        // A call may still be inside beforeStart or delivering its terminal callback.
        // Keep the bounded workers alive until every registered call has released its slot.
        if (closed && calls.isEmpty()) workers.shutdown();
    }

    private final class Call extends CompletableFuture<AppResponse> {
        private final AtomicBoolean finished = new AtomicBoolean();
        private final AtomicBoolean released = new AtomicBoolean();
        private final RpcInvocation invocation;
        private final RpcType type;
        private final TimeoutCountDown deadline;
        private final Iterator<XdsFaultPolicy> policies;
        private final Map<String, String> headers;
        private final Function<RpcInvocation, Result> action;
        private final CancellationContext lifecycle;
        private final XdsDeferredStream stream;
        private Decision decision;
        private ScheduledFuture<?> expiry;
        private ScheduledFuture<?> delayed;
        private FutureTask<Void> queued;
        private CompletableFuture<AppResponse> active;
        private boolean prepared;
        private TriRpcStatus pendingFailure;

        Call(
                URL url,
                Invocation original,
                Collection<XdsFaultPolicy> policies,
                Map<String, String> headers,
                Function<RpcInvocation, Result> action) {
            this.policies = policies.iterator();
            this.headers = headers;
            this.action = action;
            type = TripleInvoker.resolveMethodDescriptor(url, original).getRpcType();
            deadline = (TimeoutCountDown) original.get(TimeoutCountDown.CALL_ATTRIBUTE);
            invocation = new RpcInvocation(
                    original.getTargetServiceUniqueName(),
                    original.getServiceModel(),
                    original.getMethodName(),
                    original.getServiceName(),
                    original.getProtocolServiceKey(),
                    original.getParameterTypes(),
                    original.getArguments().clone(),
                    original.copyObjectAttachments(),
                    original.getInvoker(),
                    new HashMap<>(original.getAttributes()),
                    InvokeMode.ASYNC);
            if (original instanceof RpcInvocation) invocation.setReturnType(((RpcInvocation) original).getReturnType());
            invocation.put(RESUME_ATTRIBUTE, Boolean.TRUE);
            if (type == RpcType.UNARY) {
                lifecycle = null;
                stream = null;
            } else {
                lifecycle = (CancellationContext) original.get(TripleInvoker.STREAM_CANCELLATION_ATTRIBUTE);
                if (lifecycle == null) throw new IllegalArgumentException("Fault streaming call has no lifecycle");
                Object[] args = invocation.getArguments();
                stream = new XdsDeferredStream((StreamObserver<?>) args[args.length - 1], lifecycle);
                args[args.length - 1] = stream;
            }
            prepared = stream == null;
        }

        void prepare() {
            if (stream == null) return;
            try {
                stream.prepare();
            } finally {
                TriRpcStatus pending;
                synchronized (this) {
                    prepared = true;
                    pending = pendingFailure;
                    pendingFailure = null;
                }
                if (pending != null) fail(pending);
            }
        }

        void attachLifecycle() {
            if (lifecycle != null)
                lifecycle.addListener(ignored -> {
                    if (!finished.get())
                        fail(
                                lifecycle.getCancellationCause() == null
                                        ? TriRpcStatus.CANCELLED
                                        : TriRpcStatus.getStatus(lifecycle.getCancellationCause()));
                });
        }

        synchronized void start() {
            if (finished.get()) return;
            expiry = timer.schedule(
                    () -> fail(TriRpcStatus.DEADLINE_EXCEEDED.withDescription(
                            "Call deadline exceeded during xDS readiness or fault wait")),
                    Math.max(0, deadline.timeRemaining(TimeUnit.NANOSECONDS)),
                    TimeUnit.NANOSECONDS);
            enqueue(this::step);
        }

        private synchronized void enqueue(Runnable task) {
            if (finished.get()) return;
            queued = new FutureTask<>(() -> {
                task.run();
                return null;
            });
            try {
                workers.execute(queued);
            } catch (RejectedExecutionException error) {
                fail(TriRpcStatus.UNAVAILABLE.withCause(error));
            }
        }

        private void step() {
            try {
                synchronized (this) {
                    if (finished.get()) return;
                    if (deadline.isExpired()) {
                        fail(TriRpcStatus.DEADLINE_EXCEEDED);
                        return;
                    }
                    while (policies.hasNext()) {
                        decision = policies.next().decide(headers);
                        if (decision.isInjected()) {
                            if (decision.getDelayNanos() > 0) {
                                delayed = timer.schedule(
                                        () -> enqueue(this::afterDelay),
                                        decision.getDelayNanos(),
                                        TimeUnit.NANOSECONDS);
                                return;
                            }
                            if (decision.getAbort() != null) {
                                fail(decision.getAbort());
                                return;
                            }
                        }
                        decision.close();
                        decision = null;
                    }
                }
                dispatch();
            } catch (Throwable error) {
                fail(TriRpcStatus.getStatus(error));
            }
        }

        private void afterDelay() {
            synchronized (this) {
                if (finished.get()) return;
                if (deadline.isExpired()) {
                    fail(TriRpcStatus.DEADLINE_EXCEEDED);
                    return;
                }
                if (decision.getAbort() != null) {
                    fail(decision.getAbort());
                    return;
                }
                decision.close();
                decision = null;
            }
            step();
        }

        private void dispatch() {
            try {
                synchronized (this) {
                    if (finished.get() || deadline.isExpired()) {
                        if (!finished.get()) fail(TriRpcStatus.DEADLINE_EXCEEDED);
                        return;
                    }
                }
                Result result = action.apply(invocation);
                if (result == null) {
                    synchronized (this) {
                        if (!finished.get())
                            delayed = timer.schedule(() -> enqueue(this::dispatch), 25, TimeUnit.MILLISECONDS);
                    }
                    return;
                }
                CompletableFuture<AppResponse> source = result instanceof AsyncRpcResult
                        ? ((AsyncRpcResult) result).getResponseFuture()
                        : CompletableFuture.completedFuture(asResponse(result));
                synchronized (this) {
                    if (finished.get()) {
                        source.cancel(false);
                        return;
                    }
                    active = source;
                }
                source.whenComplete((response, error) -> {
                    if (error != null || (stream != null && response != null && response.hasException())) {
                        Throwable cause = error == null ? response.getException() : error;
                        fail(cause);
                    } else success(response);
                });
            } catch (Throwable error) {
                fail(error);
            } finally {
                RpcContext.removeCancellationContext();
                RpcContext.removeContext();
                FutureContext.getContext().setCompatibleFuture(null);
                FutureContext.getContext().setFuture(null);
            }
        }

        private synchronized void success(AppResponse response) {
            if (!finished.compareAndSet(false, true)) return;
            cleanup(false);
            release();
            complete(response);
        }

        private void fail(Throwable error) {
            Throwable cause = unwrap(error);
            fail(TriRpcStatus.getStatus(cause), cause);
        }

        private void fail(TriRpcStatus status) {
            fail(status, null);
        }

        private void fail(TriRpcStatus status, Throwable cause) {
            Decision faultPermit;
            synchronized (this) {
                if (!prepared) {
                    if (pendingFailure == null) pendingFailure = status;
                    lifecycle.cancel(status.code == TriRpcStatus.Code.OK ? null : status.asException());
                    return;
                }
                if (!finished.compareAndSet(false, true)) return;
                faultPermit = decision;
                decision = null;
                cleanup(true);
            }
            if (lifecycle != null) lifecycle.cancel(status.code == TriRpcStatus.Code.OK ? null : status.asException());
            Runnable notification = () -> {
                try {
                    if (stream == null) {
                        TriRpcStatus failure = status.code == TriRpcStatus.Code.OK
                                ? TriRpcStatus.INTERNAL.withDescription(
                                        "Fault-aborted unary RPC returned no response message")
                                : status;
                        // Preserve dispatch failures so the retry layer can distinguish a local drop.
                        complete(new AppResponse(cause == null ? failure.asException() : cause));
                    } else {
                        if (status.code == TriRpcStatus.Code.OK && type != RpcType.CLIENT_STREAM) stream.onCompleted();
                        else
                            stream.onError((status.code == TriRpcStatus.Code.OK
                                            ? TriRpcStatus.INTERNAL.withDescription(
                                                    "Fault-aborted client stream returned no response message")
                                            : status)
                                    .asException());
                        complete(new AppResponse());
                    }
                } finally {
                    if (faultPermit != null) faultPermit.close();
                    release();
                    clearContexts();
                }
            };
            // The slot bounds the number of dispatch tasks and terminal notifications together.
            // Registered calls prevent worker shutdown, including the beforeStart/close race.
            workers.execute(notification);
        }

        private void cleanup(boolean cancelActive) {
            if (expiry != null) expiry.cancel(false);
            if (delayed != null) delayed.cancel(false);
            if (queued != null) {
                queued.cancel(false);
                workers.remove(queued);
            }
            if (cancelActive && active != null) active.cancel(false);
            if (decision != null) {
                decision.close();
                decision = null;
            }
        }

        private void release() {
            if (released.compareAndSet(false, true)) {
                calls.remove(this);
                slots.release();
                shutdownIfIdle();
            }
        }

        @Override
        public boolean cancel(boolean interrupt) {
            if (stream != null) {
                if (finished.get()) return false;
                fail(TriRpcStatus.CANCELLED.withDescription("Native xDS fault call cancelled"));
                return super.cancel(interrupt);
            }
            synchronized (this) {
                if (!finished.compareAndSet(false, true)) return false;
                cleanup(true);
                release();
            }
            return super.cancel(interrupt);
        }
    }

    private static void clearContexts() {
        RpcContext.removeCancellationContext();
        RpcContext.removeContext();
        FutureContext.getContext().setCompatibleFuture(null);
        FutureContext.getContext().setFuture(null);
    }

    private static AppResponse asResponse(Result result) {
        AppResponse response = new AppResponse(result.getValue());
        response.setException(result.getException());
        response.setObjectAttachments(result.getObjectAttachments());
        return response;
    }

    private static Throwable unwrap(Throwable error) {
        while ((error instanceof CompletionException || error instanceof ExecutionException)
                && error.getCause() != null) error = error.getCause();
        return error;
    }
}
