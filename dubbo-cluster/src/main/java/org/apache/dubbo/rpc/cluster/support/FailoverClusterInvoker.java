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
package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.protocol.dubbo.FutureAdapter;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_RETRIES;
import static org.apache.dubbo.common.constants.CommonConstants.RETRIES_KEY;

/**
 * When invoke fails, log the initial error and retry other invokers (retry n times, which means at most n different invokers will be invoked)
 * Note that retry causes latency.
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Failover">Failover</a>
 *
 */
public class FailoverClusterInvoker<T> extends AbstractClusterInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(FailoverClusterInvoker.class);

    public FailoverClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    @Override
    public Result doInvoke(Invocation invocation, final List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        String methodName = RpcUtils.getMethodName(invocation);
        int maxInvokeCount = getUrl().getMethodParameter(methodName, RETRIES_KEY, DEFAULT_RETRIES) + 1;
        if (maxInvokeCount <= 0) {
            maxInvokeCount = 1;
        }

        FailoverInvoker failoverInvoker = new FailoverInvoker(invocation, loadbalance, maxInvokeCount);
        AsyncRpcResult result = failoverInvoker.invoke(invokers);
        CompletableFuture<AppResponse> responseFuture = result.getResponseFuture();
        FutureContext.getContext().setCompatibleFuture(responseFuture);
        RpcContext.getContext().setFuture(new FutureAdapter(responseFuture));
        if (isSyncInvocation(failoverInvoker.getLastInvoker(), invocation) && responseFuture.isCompletedExceptionally()) {
            try {
                responseFuture.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RpcException(e);
            } catch (ExecutionException e) {
                throw getRpcException(e.getCause());
            }
        }
        return result;
    }

    private class FailoverInvoker {

        private final Invocation invocation;

        private final LoadBalance loadbalance;

        private final int maxInvokeCount;

        private final Set<String> providers;

        private final AsyncRpcResult returnResult;

        private final CompletableFuture<AppResponse> responseFuture;

        private final List<Invoker<T>> invoked = new ArrayList<>();

        private volatile Invoker<T> lastInvoker;

        private RpcException lastException = null;

        public FailoverInvoker(Invocation invocation, LoadBalance loadbalance, int maxInvokeCount) {
            this.invocation = invocation;
            this.loadbalance = loadbalance;
            this.maxInvokeCount = maxInvokeCount;
            this.providers = new HashSet<>(maxInvokeCount);
            this.responseFuture = new CompletableFuture<>();
            this.returnResult = new AsyncRpcResult(responseFuture, invocation);
        }

        public AsyncRpcResult invoke(List<Invoker<T>> invokers) {
            doInvoke(invokers, maxInvokeCount);
            return returnResult;
        }

        public Invoker<T> getLastInvoker() {
            return lastInvoker;
        }

        private void resumeContext(AsyncRpcResult result) {
            result.setStoredContext(RpcContext.getContext());
            result.setStoredServerContext(RpcContext.getServerContext());
        }

        private void setResult(Result result) {
            resumeContext(returnResult);
            AppResponse appResponse;
            if (result instanceof AppResponse) {
                appResponse = (AppResponse) result;
            } else {
                appResponse = new AppResponse();
                appResponse.setValue(result.getValue());
                appResponse.setException(result.getException());
                if (result.getObjectAttachments() != null) {
                    appResponse.setObjectAttachments(result.getObjectAttachments());
                }
            }
            responseFuture.complete(appResponse);
        }

        private void setException(RpcException e) {
            resumeContext(returnResult);
            responseFuture.completeExceptionally(e);
        }

        @SuppressWarnings({"unchecked"})
        private void doInvoke(List<Invoker<T>> invokers, int remainingCount) {
            Invoker<T> invoker;
            try {
                checkInvokers(invokers, invocation);
                invoker = select(loadbalance, invocation, invokers, invoked);
            } catch (RpcException e) {
                setException(e);
                return;
            } catch (Throwable t) {
                setException(new RpcException(t.getMessage(), t));
                return;
            }

            invoked.add(invoker);
            lastInvoker = invoker;
            RpcContext.getContext().setInvokers((List) invoked);

            Result result = realInvoke(invoker, invocation);
            result.whenCompleteWithContext(new BiConsumer<Result, Throwable>() {
                @Override
                public void accept(Result result, Throwable throwable) {
                    if (throwable == null) {
                        setResult(result);
                        recordFailover(invoker, invokers);
                        return;
                    }

                    RpcException exception = getRpcException(throwable);
                    if (exception.isBiz()) {
                        setException(exception);
                        return;
                    }

                    lastException = exception;

                    if (remainingCount > 1) {
                        List<Invoker<T>> nextInvokers;
                        try {
                            checkWhetherDestroyed();
                            nextInvokers = list(invocation);
                        } catch (RpcException e) {
                            setException(e);
                            return;
                        } catch (Throwable t) {
                            setException(new RpcException(t.getMessage(), t));
                            return;
                        }

                        doInvoke(nextInvokers, remainingCount - 1);
                    } else {
                        setException(generateFailException(invokers));
                    }
                }
            });
        }

        private RpcException generateFailException(List<Invoker<T>> invokers) {
            return new RpcException(lastException.getCode(), "Failed to invoke the method "
                    + RpcUtils.getMethodName(invocation) + " in the service " + getInterface().getName()
                    + ". Tried " + maxInvokeCount + " times of the providers " + providers
                    + " (" + providers.size() + "/" + invokers.size()
                    + ") from the registry " + directory.getUrl().getAddress()
                    + " on the consumer " + NetUtils.getLocalHost() + " using the dubbo version "
                    + Version.getVersion() + ". Last error is: "
                    + lastException.getMessage(), lastException.getCause() != null ? lastException.getCause() : lastException);
        }

        private void recordFailover(Invoker<T> invoker, List<Invoker<T>> invokers) {
            if (lastException != null && logger.isWarnEnabled()) {
                logger.warn("Although retry the method " + RpcUtils.getMethodName(invocation)
                        + " in the service " + getInterface().getName()
                        + " was successful by the provider " + invoker.getUrl().getAddress()
                        + ", but there have been failed providers " + providers
                        + " (" + providers.size() + "/" + invokers.size()
                        + ") from the registry " + directory.getUrl().getAddress()
                        + " on the consumer " + NetUtils.getLocalHost()
                        + " using the dubbo version " + Version.getVersion() + ". Last error is: "
                        + lastException.getMessage(), lastException);
            }
        }

        private Result realInvoke(Invoker<T> invoker, Invocation invocation) {
            try {
                return invoker.invoke(invocation);
            } catch (Throwable e) {
                CompletableFuture<AppResponse> future = new CompletableFuture<>();
                AsyncRpcResult result = new AsyncRpcResult(future, invocation);
                future.completeExceptionally(e);
                return result;
            } finally {
                providers.add(invoker.getUrl().getAddress());
            }
        }
    }

    private boolean isSyncInvocation(Invoker<T> invoker, Invocation invocation) {
        if (invoker == null) {
            return true;
        } else {
            return InvokeMode.SYNC == RpcUtils.getInvokeMode(invoker.getUrl(), invocation);
        }
    }

    private RpcException getRpcException(Throwable throwable) {
        if (throwable instanceof RpcException) {
            return (RpcException) throwable;
        } else {
            return new RpcException(throwable.getMessage(), throwable);
        }
    }
}
