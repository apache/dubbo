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
package org.apache.dubbo.xds.cluster;

import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;
import org.apache.dubbo.rpc.support.RpcUtils;
import org.apache.dubbo.xds.resource.route.RetryPolicy;
import org.apache.dubbo.xds.resource.route.RouteAction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import io.grpc.Status;

public class XdsClusterInvoker<T> extends AbstractClusterInvoker<T> {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(XdsClusterInvoker.class);

    public XdsClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    @Override
    protected Result doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance)
            throws RpcException {
        RouteAction routeAction = (RouteAction) invocation.get("xds.route.action");

        if (routeAction != null && routeAction.getRetryPolicy() != null) {
            logger.debug("[XDS] Using xDS retry policy for invocation: {}", routeAction.getRetryPolicy());
            return doInvokeWithXdsRetry(invocation, invokers, loadbalance, routeAction.getRetryPolicy());
        } else {
            logger.debug("[XDS] Using standard Dubbo retry mechanism");
            return doSingleInvoke(invocation, invokers, loadbalance);
        }
    }

    private Result doInvokeWithXdsRetry(
            Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance, RetryPolicy retryPolicy)
            throws RpcException {
        String methodName = RpcUtils.getMethodName(invocation);
        int maxAttempts = retryPolicy.getMaxAttempts();
        long initialBackoffMs = com.google.protobuf.util.Durations.toMillis(retryPolicy.getInitialBackoff());
        long maxBackoffMs = com.google.protobuf.util.Durations.toMillis(retryPolicy.getMaxBackoff());

        logger.info(
                "[XDS-TEST] Starting XDS retry logic: maxAttempts={}, initialBackoff={}ms, maxBackoff={}ms, retryableCodes={}",
                maxAttempts,
                initialBackoffMs,
                maxBackoffMs,
                retryPolicy.getRetryableStatusCodes());

        RpcException lastException = null;
        List<Invoker<T>> invoked = new ArrayList<>(invokers.size());
        Set<String> providers = new HashSet<>(maxAttempts);

        for (int i = 0; i < maxAttempts; i++) {
            Invoker<T> invoker = select(loadbalance, invocation, invokers, invoked);
            invoked.add(invoker);

            try {
                logger.info(
                        "[XDS-RETRY] Attempt {}/{}: invoking {}",
                        i + 1,
                        maxAttempts,
                        invoker.getUrl().getAddress());
                Result result = invoker.invoke(invocation);

                // 检查异步结果中的异常
                if (result.hasException()) {
                    Throwable exception = result.getException();
                    logger.info(
                            "[XDS-RETRY] Attempt {}/{} has exception in result: {}",
                            i + 1,
                            maxAttempts,
                            exception.getMessage());

                    if (exception instanceof RpcException) {
                        throw (RpcException) exception;
                    } else {
                        throw new RpcException(exception.getMessage(), exception);
                    }
                }

                return result;
            } catch (RpcException e) {
                logger.info(
                        "[XDS-RETRY] Attempt {}/{} failed: code={}, message={}, isBiz={}",
                        i + 1,
                        maxAttempts,
                        e.getCode(),
                        e.getMessage(),
                        e.isBiz());

                if (e.isBiz()) {
                    logger.info("[XDS-RETRY] Business exception, not retrying");
                    throw e;
                }

                boolean isRetryable = isRetryableStatusCode(e, retryPolicy);
                logger.info("[XDS-RETRY] Exception is retryable: {}", isRetryable);

                if (!isRetryable) {
                    logger.info("[XDS-RETRY] Exception not retryable, throwing");
                    throw e;
                }

                lastException = e;
                providers.add(invoker.getUrl().getAddress());

                if (i < maxAttempts - 1) {
                    long backoffMs = calculateBackoff(i, initialBackoffMs, maxBackoffMs);
                    logger.info("[XDS-RETRY] Waiting {}ms before retry {}/{}", backoffMs, i + 2, maxAttempts);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RpcException("Interrupted during retry backoff", ie);
                    }
                } else {
                    logger.info("[XDS-RETRY] Max attempts reached, no more retries");
                }

            } catch (Throwable t) {
                logger.info("[XDS-RETRY] Attempt {}/{} failed with Throwable: {}", i + 1, maxAttempts, t.getMessage());

                RpcException rpcException;
                if (t instanceof RpcException) {
                    rpcException = (RpcException) t;
                } else {
                    rpcException = new RpcException(t.getMessage(), t);
                }

                if (!isRetryableStatusCode(rpcException, retryPolicy)) {
                    logger.info("[XDS-RETRY] Throwable not retryable, breaking");
                    lastException = rpcException;
                    providers.add(invoker.getUrl().getAddress());
                    break;
                }

                lastException = rpcException;
                providers.add(invoker.getUrl().getAddress());

                if (i < maxAttempts - 1) {
                    long backoffMs = calculateBackoff(i, initialBackoffMs, maxBackoffMs);
                    logger.info(
                            "[XDS-RETRY] Waiting {}ms before retry {}/{} (from Throwable)",
                            backoffMs,
                            i + 2,
                            maxAttempts);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RpcException("Interrupted during retry backoff", ie);
                    }
                } else {
                    logger.info("[XDS-RETRY] Max attempts reached for Throwable, no more retries");
                }
            }
        }

        throw new RpcException(
                lastException != null ? lastException.getCode() : 0,
                "Failed to invoke method " + methodName + " after " + maxAttempts + " attempts. "
                        + "Tried providers: " + providers + " (" + providers.size() + "/" + invokers.size() + ") "
                        + "from registry " + directory.getUrl().getAddress() + " on consumer " + NetUtils.getLocalHost()
                        + " using dubbo version " + Version.getVersion() + ". Last error: "
                        + (lastException != null ? lastException.getMessage() : "Unknown error"),
                lastException != null ? lastException.getCause() : null);
    }

    private Result doSingleInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance)
            throws RpcException {
        Invoker<T> invoker = select(loadbalance, invocation, invokers, null);
        try {
            return invokeWithContext(invoker, invocation);
        } catch (Throwable e) {
            if (e instanceof RpcException && ((RpcException) e).isBiz()) {
                throw (RpcException) e;
            }
            throw new RpcException(
                    e instanceof RpcException ? ((RpcException) e).getCode() : 0,
                    "XDS invoke failed on provider " + invoker.getUrl() + " "
                            + loadbalance.getClass().getSimpleName()
                            + " for service " + getInterface().getName()
                            + " method " + RpcUtils.getMethodName(invocation) + " on consumer "
                            + NetUtils.getLocalHost()
                            + " using dubbo version " + Version.getVersion()
                            + ". Error: " + e.getMessage(),
                    e.getCause() != null ? e.getCause() : e);
        }
    }

    private boolean isRetryableStatusCode(RpcException e, RetryPolicy retryPolicy) {
        if (retryPolicy.getRetryableStatusCodes().isEmpty()) {
            return true;
        }

        for (Status.Code code : retryPolicy.getRetryableStatusCodes()) {
            if (isMatchingStatusCode(e, code)) {
                return true;
            }
        }

        return false;
    }

    private boolean isMatchingStatusCode(RpcException e, Status.Code code) {
        boolean matches = false;
        switch (code) {
            case CANCELLED:
                matches =
                        e.getCode() == RpcException.TIMEOUT_EXCEPTION || e.getCode() == RpcException.UNKNOWN_EXCEPTION;
                break;
            case DEADLINE_EXCEEDED:
                matches = e.getCode() == RpcException.TIMEOUT_EXCEPTION;
                break;
            case INTERNAL:
                matches = e.getCode() == RpcException.UNKNOWN_EXCEPTION
                        || e.getCode() == RpcException.SERIALIZATION_EXCEPTION;
                break;
            case RESOURCE_EXHAUSTED:
                matches = e.getCode() == RpcException.LIMIT_EXCEEDED_EXCEPTION
                        || e.getCode() == RpcException.NETWORK_EXCEPTION;
                break;
            case UNAVAILABLE:
                matches = e.getCode() == RpcException.NETWORK_EXCEPTION
                        || e.getCode() == RpcException.FORBIDDEN_EXCEPTION;
                break;
            default:
                matches = false;
        }

        logger.debug("[XDS-RETRY] Status code mapping: gRPC {} -> Dubbo {} -> matches: {}", code, e.getCode(), matches);
        return matches;
    }

    private long calculateBackoff(int attempt, long initialBackoffMs, long maxBackoffMs) {
        long backoff = Math.min((long) (initialBackoffMs * Math.pow(2, attempt)), maxBackoffMs);

        long jitter = ThreadLocalRandom.current().nextLong(0, backoff / 10 + 1);
        return backoff + jitter;
    }

    @Override
    public boolean isAvailable() {
        List<Invoker<T>> invokers = directory.list(null);
        return invokers != null && !invokers.isEmpty() && invokers.stream().anyMatch(Invoker::isAvailable);
    }
}
