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

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.registry.client.ServiceDiscoveryRegistryDirectory;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.FutureContext;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.InvokeMode;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;
import org.apache.dubbo.rpc.cluster.support.wrapper.AbstractCluster;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.dubbo.FutureAdapter;
import org.apache.dubbo.rpc.protocol.tri.TripleInvoker;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

/** Keeps xDS call policies outside Dubbo's default Failover loop. */
public final class XdsCluster extends AbstractCluster {
    @Override
    protected <T> AbstractClusterInvoker<T> doJoin(Directory<T> directory) {
        return new NativeInvoker<>(directory);
    }

    private static final class NativeInvoker<T> extends AbstractClusterInvoker<T> {
        private final XdsLoadBalance loadBalance = new XdsLoadBalance();
        private final long referenceId = ThreadLocalRandom.current().nextLong();
        private XdsRetryExecutor retries;
        private XdsFaultExecutor faults;
        private final Set<CancellationContext> streams = ConcurrentHashMap.newKeySet();
        private final Set<CompletableFuture<AppResponse>> unaryCalls = ConcurrentHashMap.newKeySet();
        private boolean closing;

        NativeInvoker(Directory<T> directory) {
            super(directory);
        }

        @Override
        public Result invoke(Invocation invocation) {
            invocation.put(XdsHashPolicy.REFERENCE_ID_ATTRIBUTE, referenceId);
            XdsCallBudget.begin(getUrl(), invocation);
            ((RpcInvocation) invocation)
                    .addObjectAttachmentsIfAbsent(
                            RpcContext.getClientAttachment().getObjectAttachments());
            invocation.getObjectAttachments().remove(CommonConstants.TIME_COUNTDOWN_KEY);
            try {
                Result result = super.invoke(invocation);
                XdsRouteSelection selected = (XdsRouteSelection) invocation.get(XdsRouteSelection.ATTRIBUTE);
                CancellationContext stream =
                        (CancellationContext) invocation.get(TripleInvoker.STREAM_CANCELLATION_ATTRIBUTE);
                result.whenCompleteWithContext((response, error) -> {
                    if (stream == null) {
                        if (selected != null) selected.close();
                    } else if (error != null || (response != null && response.hasException())) {
                        stream.cancel(error == null ? response.getException() : error);
                    }
                });
                return result;
            } catch (RuntimeException | Error error) {
                CancellationContext stream =
                        (CancellationContext) invocation.get(TripleInvoker.STREAM_CANCELLATION_ATTRIBUTE);
                if (stream != null) stream.cancel(error);
                XdsRouteSelection selected = (XdsRouteSelection) invocation.get(XdsRouteSelection.ATTRIBUTE);
                if (selected != null) selected.close();
                throw error;
            }
        }

        @Override
        protected List<Invoker<T>> list(Invocation invocation) {
            try {
                return super.list(invocation);
            } catch (RpcException error) {
                if (error.isForbidden()
                        && getDirectory() instanceof ServiceDiscoveryRegistryDirectory
                        && getDirectory().isEmpty()) {
                    throw TriRpcStatus.UNAVAILABLE
                            .withDescription("Native xDS has no discovered providers")
                            .withCause(error)
                            .asException();
                }
                throw error;
            }
        }

        @Override
        protected void checkInvokers(List<Invoker<T>> invokers, Invocation invocation) {
            // A matched cluster may temporarily have no endpoints; its retry policy still applies.
            if (invokers.isEmpty() && invocation.get(XdsRouteSelection.ATTRIBUTE) == null) {
                throw TriRpcStatus.UNAVAILABLE
                        .withDescription("Native xDS has no routed providers")
                        .asException();
            } else if (invocation.get(XdsRouteSelection.ATTRIBUTE) == null) {
                super.checkInvokers(invokers, invocation);
            }
        }

        private synchronized XdsRetryExecutor retryExecutor() {
            checkWhetherDestroyed();
            if (retries == null) {
                retries = new XdsRetryExecutor();
            }
            return retries;
        }

        @Override
        public synchronized void destroy() {
            closing = true;
            if (faults != null) faults.close();
            if (retries != null) {
                retries.close();
            }
            unaryCalls.forEach(call -> call.cancel(false));
            streams.forEach(stream -> stream.cancel(TriRpcStatus.CANCELLED
                    .withDescription("Native xDS reference destroyed")
                    .asException()));
            loadBalance.close();
            super.destroy();
        }

        private synchronized CancellationContext streamContext(XdsRouteSelection route) {
            checkWhetherDestroyed();
            CancellationContext context = new CancellationContext();
            streams.add(context);
            context.addListener(ignored -> {
                streams.remove(context);
                route.close();
            });
            return context;
        }

        @Override
        protected LoadBalance initLoadBalance(List<Invoker<T>> invokers, Invocation invocation) {
            return loadBalance;
        }

        @Override
        protected Result doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance ignored) {
            XdsRouteSelection route = (XdsRouteSelection) invocation.get(XdsRouteSelection.ATTRIBUTE);
            XdsCallBudget.begin(getUrl(), invocation)
                    .limit(
                            invocation,
                            route.getRoute().getTimeoutNanos(route.getSnapshot().getDefaultTimeoutNanos()));
            MethodDescriptor.RpcType type =
                    TripleInvoker.resolveMethodDescriptor(getUrl(), invocation).getRpcType();
            if (type != MethodDescriptor.RpcType.UNARY) {
                invocation.put(TripleInvoker.STREAM_CANCELLATION_ATTRIBUTE, streamContext(route));
            }
            Collection<XdsFaultPolicy> policies =
                    route.getRoute().getFaultPolicies(route.getSnapshot(), route.getCluster());
            if (!policies.isEmpty()) {
                return faultExecutor().invoke(getUrl(), invocation, policies, route.getFaultHeaders(), resumed -> {
                    checkWhetherDestroyed();
                    RpcContext.getServiceContext().setConsumerUrl(getDirectory().getConsumerUrl());
                    return invokeReady(resumed, list(resumed), type);
                });
            }
            return invokeReady(invocation, invokers, type);
        }

        private synchronized XdsFaultExecutor faultExecutor() {
            checkWhetherDestroyed();
            if (faults == null) faults = new XdsFaultExecutor();
            return faults;
        }

        private Result invokeReady(Invocation invocation, List<Invoker<T>> invokers, MethodDescriptor.RpcType type) {
            XdsRouteSelection route = (XdsRouteSelection) invocation.get(XdsRouteSelection.ATTRIBUTE);
            XdsRetryPolicy policy = route.getRoute().getRetryPolicy();
            if (policy == null || type != MethodDescriptor.RpcType.UNARY) {
                return selectAndInvoke(invocation, type);
            }
            RpcInvocation rpc = (RpcInvocation) invocation;
            rpc.setInvokeMode(RpcUtils.getInvokeMode(getUrl(), invocation));
            XdsRetryExecutor.Call call = retryExecutor().newCall(invocation, policy, attempt -> {
                checkWhetherDestroyed();
                return selectAndInvoke(attempt, type);
            });
            AsyncRpcResult result = new AsyncRpcResult(call, invocation);
            FutureContext.getContext().setCompatibleFuture(call);
            RpcContext.getServiceContext().setFuture(new FutureAdapter<>(call));
            call.start();
            return waitForCompletion(rpc, result, call);
        }

        private Result selectAndInvoke(Invocation invocation, MethodDescriptor.RpcType type) {
            XdsRouteSelection route = (XdsRouteSelection) invocation.get(XdsRouteSelection.ATTRIBUTE);
            XdsDropPolicy.apply(route.getCurrentSnapshot(), route.getCluster());
            return faultExecutor()
                    .invoke(getUrl(), invocation, Collections.emptyList(), route.getFaultHeaders(), attempt -> {
                        checkWhetherDestroyed();
                        RpcContext.getServiceContext()
                                .setConsumerUrl(getDirectory().getConsumerUrl());
                        Invoker<T> selected = loadBalance.selectReady(list(attempt), getUrl(), attempt);
                        return selected == null ? null : invokeSelected(selected, attempt, type);
                    });
        }

        private Result waitForCompletion(
                RpcInvocation rpc, AsyncRpcResult result, CompletableFuture<AppResponse> call) {
            if (rpc.getInvokeMode() == InvokeMode.SYNC) {
                try {
                    result.get();
                } catch (InterruptedException error) {
                    call.cancel(true);
                    Thread.currentThread().interrupt();
                    throw TriRpcStatus.CANCELLED.withCause(error).asException();
                } catch (ExecutionException error) {
                    Throwable cause = error.getCause();
                    if (cause instanceof RpcException) {
                        throw (RpcException) cause;
                    }
                    throw new RpcException(cause);
                } catch (CancellationException error) {
                    throw TriRpcStatus.CANCELLED.withCause(error).asException();
                }
            }
            return result;
        }

        private Result invokeSelected(Invoker<T> selected, Invocation invocation, MethodDescriptor.RpcType type) {
            XdsLoadBalance.Permit permit = loadBalance.acquire(selected);
            CancellationContext stream = null;
            try {
                if (type != MethodDescriptor.RpcType.UNARY) {
                    stream = (CancellationContext) invocation.get(TripleInvoker.STREAM_CANCELLATION_ATTRIBUTE);
                    stream.addListener(ignored -> permit.close());
                }
                CancellationContext lifecycle = stream;
                Result result = invokeWithContext(selected, invocation);
                result.whenCompleteWithContext((response, error) -> {
                    if (lifecycle == null) permit.close();
                    else if (error != null || (response != null && response.hasException())) {
                        lifecycle.cancel(error == null ? response.getException() : error);
                    }
                });
                if (lifecycle == null && result instanceof AsyncRpcResult) {
                    ownUnary(((AsyncRpcResult) result).getResponseFuture());
                }
                return result;
            } catch (RuntimeException | Error error) {
                if (stream != null) stream.cancel(error);
                permit.close();
                throw error;
            }
        }

        private synchronized void ownUnary(CompletableFuture<AppResponse> call) {
            if (closing) {
                call.cancel(false);
                return;
            }
            unaryCalls.add(call);
            call.whenComplete((response, error) -> unaryCalls.remove(call));
        }
    }
}
