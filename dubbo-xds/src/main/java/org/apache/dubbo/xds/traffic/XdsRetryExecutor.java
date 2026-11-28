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

import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.FutureContext;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.InvokeMode;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TimeoutCountDown;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.UnaryRetryState;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/** Bounded attempt execution, a single total deadline and cancellation, owned by the native Cluster. */
final class XdsRetryExecutor implements AutoCloseable {
    private final ScheduledThreadPoolExecutor timer =
            new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("dubbo-xds-retry-timer", true));
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(
            2,
            4,
            30,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1024),
            new NamedThreadFactory("dubbo-xds-retry", true));
    private final Set<Call> calls = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    XdsRetryExecutor() {
        timer.setRemoveOnCancelPolicy(true);
    }

    Call newCall(Invocation invocation, XdsRetryPolicy policy, Function<RpcInvocation, Result> attempt) {
        Call call = new Call(invocation, policy, attempt);
        calls.add(call);
        if (closed) {
            call.cancel(false);
        }
        return call;
    }

    @Override
    public void close() {
        closed = true;
        calls.forEach(call -> call.cancel(false));
        timer.shutdownNow();
        workers.shutdownNow();
    }

    final class Call extends CompletableFuture<AppResponse> {
        private final AtomicBoolean finished = new AtomicBoolean();
        private final Invocation invocation;
        private final XdsRetryPolicy policy;
        private final Function<RpcInvocation, Result> invoke;
        private final TimeoutCountDown deadline;
        private final UnaryRetryState request = new UnaryRetryState();
        private CompletableFuture<AppResponse> active;
        private ScheduledFuture<?> expiry;
        private ScheduledFuture<?> backoff;
        private Future<?> queued;
        private int attempts;
        private long nextBackoff;

        Call(Invocation invocation, XdsRetryPolicy policy, Function<RpcInvocation, Result> invoke) {
            this.invocation = invocation;
            this.policy = policy;
            this.invoke = invoke;
            deadline = (TimeoutCountDown) invocation.get(TimeoutCountDown.CALL_ATTRIBUTE);
            nextBackoff = policy.getInitialBackoff();
        }

        synchronized void start() {
            if (finished.get()) {
                return;
            }
            try {
                expiry = timer.schedule(
                        this::expired, Math.max(0, deadline.timeRemaining(TimeUnit.NANOSECONDS)), TimeUnit.NANOSECONDS);
                dispatch();
            } catch (RejectedExecutionException error) {
                finish(
                        new AppResponse(TriRpcStatus.UNAVAILABLE
                                .withDescription("xDS retry executor is closed or saturated")
                                .withCause(error)
                                .asException()),
                        null,
                        true);
            }
        }

        private synchronized void dispatch() {
            if (finished.get()) {
                return;
            }
            try {
                queued = workers.submit(this::attempt);
            } catch (RejectedExecutionException error) {
                finish(
                        new AppResponse(TriRpcStatus.fromCode(TriRpcStatus.Code.RESOURCE_EXHAUSTED)
                                .withDescription("xDS retry executor is saturated")
                                .withCause(error)
                                .asException()),
                        null,
                        true);
            }
        }

        private void attempt() {
            UnaryRetryState.Attempt state = new UnaryRetryState.Attempt(request);
            RpcInvocation copy;
            synchronized (this) {
                if (finished.get()) {
                    return;
                }
                if (deadline.isExpired()) {
                    expired();
                    return;
                }
                attempts++;
                copy = new RpcInvocation(
                        invocation.getTargetServiceUniqueName(),
                        invocation.getServiceModel(),
                        invocation.getMethodName(),
                        invocation.getServiceName(),
                        invocation.getProtocolServiceKey(),
                        invocation.getParameterTypes(),
                        invocation.getArguments(),
                        invocation.copyObjectAttachments(),
                        invocation.getInvoker(),
                        new HashMap<>(invocation.getAttributes()),
                        InvokeMode.ASYNC);
                if (invocation instanceof RpcInvocation) {
                    copy.setReturnType(((RpcInvocation) invocation).getReturnType());
                }
                copy.put(UnaryRetryState.ATTRIBUTE, state);
                copy.getObjectAttachments().remove("grpc-previous-rpc-attempts");
                if (attempts > 1) {
                    copy.setAttachment("grpc-previous-rpc-attempts", String.valueOf(attempts - 1));
                }
            }
            try {
                Result result = invoke.apply(copy);
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
                source.whenComplete((response, error) -> received(state, response, error));
            } catch (Throwable error) {
                received(state, null, error);
            } finally {
                RpcContext.removeCancellationContext();
                RpcContext.removeContext();
                FutureContext.getContext().setCompatibleFuture(null);
                FutureContext.getContext().setFuture(null);
            }
        }

        private synchronized void received(UnaryRetryState.Attempt state, AppResponse response, Throwable error) {
            if (finished.get()) {
                return;
            }
            active = null;
            if (deadline.isExpired()) {
                expired();
                return;
            }
            Throwable failure = error == null ? (response == null ? null : response.getException()) : unwrap(error);
            if (failure == null
                    || failure instanceof XdsCircuitBreaker.DropException
                    || state.isCommitted()
                    || !request.isReplayable()
                    || attempts >= policy.getMaxAttempts()
                    || (failure instanceof RpcException && ((RpcException) failure).isBiz())
                    || !policy.permits(TriRpcStatus.getStatus(failure).code)) {
                finish(response, error == null ? null : unwrap(error), false);
                return;
            }
            long delay;
            if (state.getPushback() != null) {
                long millis;
                try {
                    millis = Long.parseLong(state.getPushback());
                } catch (NumberFormatException invalid) {
                    millis = -1;
                }
                if (millis < 0) {
                    finish(response, error == null ? null : unwrap(error), false);
                    return;
                }
                delay = TimeUnit.MILLISECONDS.toNanos(millis);
                nextBackoff = policy.getInitialBackoff();
            } else {
                delay = policy.jitter(nextBackoff);
                nextBackoff = policy.nextBackoff(nextBackoff);
            }
            try {
                backoff = timer.schedule(this::dispatch, delay, TimeUnit.NANOSECONDS);
            } catch (RejectedExecutionException rejected) {
                finish(response, error == null ? null : unwrap(error), false);
            }
        }

        private void expired() {
            finish(
                    new AppResponse(TriRpcStatus.DEADLINE_EXCEEDED
                            .withDescription("xDS call deadline exceeded across attempts/backoff")
                            .asException()),
                    null,
                    true);
        }

        private synchronized void finish(AppResponse response, Throwable error, boolean cancelActive) {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            cleanup(cancelActive);
            if (error == null) {
                complete(response);
            } else {
                completeExceptionally(error);
            }
        }

        private void cleanup(boolean cancelActive) {
            if (expiry != null) {
                expiry.cancel(false);
            }
            if (backoff != null) {
                backoff.cancel(false);
            }
            if (queued != null) {
                queued.cancel(false);
            }
            if (cancelActive && active != null) {
                active.cancel(false);
            }
            request.close();
            calls.remove(this);
        }

        @Override
        public synchronized boolean cancel(boolean mayInterruptIfRunning) {
            if (!finished.compareAndSet(false, true)) {
                return false;
            }
            cleanup(true);
            return super.cancel(mayInterruptIfRunning);
        }
    }

    private static AppResponse asResponse(Result result) {
        AppResponse response = new AppResponse(result.getValue());
        response.setException(result.getException());
        response.setObjectAttachments(result.getObjectAttachments());
        return response;
    }

    private static Throwable unwrap(Throwable error) {
        while ((error instanceof CompletionException || error instanceof ExecutionException)
                && error.getCause() != null) {
            error = error.getCause();
        }
        return error;
    }
}
