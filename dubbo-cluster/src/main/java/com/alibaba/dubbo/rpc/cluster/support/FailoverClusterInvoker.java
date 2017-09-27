/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.cluster.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * 失败转移，当出现失败，重试其它服务器，通常用于读操作，但重试会带来更长延迟。
 * <p/>
 * <a href="http://en.wikipedia.org/wiki/Failover">Failover</a>
 *
 * @author william.liangf
 * @author chao.liuc
 */
public class FailoverClusterInvoker<T> extends AbstractClusterInvoker<T> {
    private static final Logger logger = LoggerFactory.getLogger(FailoverClusterInvoker.class);

    private static final Executor SAME_EXECUTOR = MoreExecutors.sameThreadExecutor();

    public FailoverClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    protected int getRetries(URL url, String methodName) {
        return url.getMethodParameter(methodName, Constants.RETRIES_KEY, Constants.DEFAULT_RETRIES);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Result doInvoke(Invocation invocation, final List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        checkInvokers(invokers, invocation);
        int totalRetries = getRetries(getUrl(), invocation.getMethodName()) + 1;
        if (totalRetries <= 0) {
            totalRetries = 1;
        }
        List<Invoker<T>> invoked = new ArrayList<Invoker<T>>(totalRetries);
        Set<String> providers = new HashSet<String>(totalRetries);
        Holder<RpcException> lastRpcException = new Holder<RpcException>();
        Holder<Invoker<T>> lastInvoked = new Holder<Invoker<T>>();

        Result result = doInvoke(invokers, invoked, lastRpcException, providers, invocation, loadbalance, totalRetries, totalRetries, lastInvoked);
        if (isAsync(result)) {
            return asyncFailoverInvoke(result, invokers, invoked, providers, invocation, loadbalance, totalRetries, totalRetries, lastInvoked);
        }
        recordLog(invokers, lastRpcException.value, providers, invocation, lastInvoked.value);
        return result;
    }

    private Result doInvoke(List<Invoker<T>> invokers,
                            final List<Invoker<T>> invoked,
                            Holder<RpcException> lastException,
                            final Set<String> providers,
                            final Invocation invocation,
                            final LoadBalance loadbalance,
                            final int totalRetries,
                            int retries,
                            Holder<Invoker<T>> lastInvoked) throws RpcException {
        if (retries < totalRetries) {
            checkWhetherDestroyed();
            invokers = list(invocation);
            checkInvokers(invokers, invocation);
        }

        final Invoker<T> invoker = select(loadbalance, invocation, invokers, invoked);
        invoked.add(invoker);
        lastInvoked.value = invoker;
        RpcContext.getContext().setInvokers((List) invoked);

        try {
            return invoker.invoke(invocation);
        } catch (RpcException e) {
            //业务异常不重试
            if (e.isBiz()) {
                throw e;
            }
            lastException.value = e;
        } catch (Throwable e) {
            lastException.value = new RpcException(e.getMessage(), e);
        } finally {
            providers.add(invoker.getUrl().getAddress());
        }

        if (--retries == 0) {
            throw populateException(invokers, lastException.value, providers, invocation, totalRetries);
        }

        return doInvoke(invokers, invoked, lastException, providers, invocation, loadbalance, totalRetries, retries, lastInvoked);
    }

    private Result asyncFailoverInvoke(final Result lastResult,
                                       final List<Invoker<T>> copyInvokers,
                                       final List<Invoker<T>> invoked,
                                       final Set<String> providers,
                                       final Invocation invocation,
                                       final LoadBalance loadbalance,
                                       final int totalRetries,
                                       final int retries,
                                       final Holder<Invoker<T>> invoker) {
        if (retries - 1 == 0) return lastResult;

        final FailoverAsyncResult result = new FailoverAsyncResult();
        doAsyncFailoverInvoke(result, lastResult, copyInvokers, invoked, new Holder<RpcException>(), providers, invocation, loadbalance, totalRetries, retries, invoker);
        return result;
    }

    private void doAsyncFailoverInvoke(final FailoverAsyncResult result,
                                       final Result lastResult,
                                       final List<Invoker<T>> copyInvokers,
                                       final List<Invoker<T>> invoked,
                                       final Holder<RpcException> lastException,
                                       final Set<String> providers,
                                       final Invocation invocation,
                                       final LoadBalance loadBalance,
                                       final int totalRetries,
                                       final int retries,
                                       final Holder<Invoker<T>> invoker) {
        final ListenableFuture future = (ListenableFuture) lastResult;
        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    Object o = future.get();
                    recordLog(copyInvokers, lastException.value, providers, invocation, invoker.value);
                    result.setResult(o);
                    return;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RpcException) {
                        RpcException te = (RpcException) cause;
                        //业务异常不重试
                        if (te.isBiz()) {
                            result.setResult(te);
                            return;
                        }
                        lastException.value = te;
                    } else {
                        //不是RpcException必定是业务异常
                        result.setResult(cause);
                        return;
                    }
                } catch (Throwable e) {
                    lastException.value = new RpcException(e.getMessage(), e);
                }

                int nextRetries = retries - 1;
                if (nextRetries == 0) {
                    result.setResult(populateException(copyInvokers, lastException.value, providers, invocation, totalRetries));
                } else {
                    invocation.getAttachments().put(Constants.ASYNC_KEY, "true");
                    Result nextResult = doInvoke(copyInvokers, invoked, lastException, providers, invocation, loadBalance, totalRetries, nextRetries, invoker);
                    doAsyncFailoverInvoke(result, nextResult, copyInvokers, invoked, lastException, providers, invocation, loadBalance, totalRetries, nextRetries, invoker);
                }
            }
        }, SAME_EXECUTOR);
    }

    private boolean isAsync(Result result) {
        return result != null && result instanceof ListenableFuture;
    }

    private RpcException populateException(List<Invoker<T>> copyInvokers, RpcException lastException, Set<String> providers, Invocation invocation, int totalRetries) {
        return new RpcException(lastException != null ? lastException.getCode() : 0, "Failed to invoke the method "
                + invocation.getMethodName() + " in the service " + getInterface().getName()
                + ". Tried " + totalRetries + " times of the providers " + providers
                + " (" + providers.size() + "/" + copyInvokers.size()
                + ") from the registry " + directory.getUrl().getAddress()
                + " on the consumer " + NetUtils.getLocalHost() + " using the dubbo version "
                + Version.getVersion() + ". Last error is: "
                + (lastException != null ? lastException.getMessage() : ""), lastException != null && lastException.getCause() != null ? lastException.getCause() : lastException);
    }

    private void recordLog(List<Invoker<T>> copyInvokers,
                           RpcException lastException,
                           Set<String> providers,
                           Invocation invocation,
                           Invoker<T> successInvoker) {
        if (lastException == null || !logger.isWarnEnabled()) return;
        logger.warn("Although retry the method " + invocation.getMethodName()
                + " in the service " + getInterface().getName()
                + " was successful by the provider " + successInvoker.getUrl().getAddress()
                + ", but there have been failed providers " + providers
                + " (" + providers.size() + "/" + copyInvokers.size()
                + ") from the registry " + directory.getUrl().getAddress()
                + " on the consumer " + NetUtils.getLocalHost()
                + " using the dubbo version " + Version.getVersion() + ". Last error is: "
                + lastException.getMessage(), lastException);
    }

    private static class FailoverAsyncResult extends AbstractFuture implements AsyncResult {

        private Object value;

        private Throwable throwable;


        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public Throwable getException() {
            return throwable;
        }

        @Override
        public boolean hasException() {
            return throwable != null;
        }

        @Override
        public Object recreate() throws Throwable {
            return null;
        }

        @Override
        public Object getResult() {
            return null;
        }

        @Override
        public Map<String, String> getAttachments() {
            return null;
        }

        @Override
        public String getAttachment(String key) {
            return null;
        }

        @Override
        public String getAttachment(String key, String defaultValue) {
            return null;
        }

        public void setResult(Object value) {
            if (value instanceof Throwable) {
                throwable = (Throwable) value;
                setException(throwable);
            } else {
                this.value = value;
                set(value);
            }
        }
    }

    private static class Holder<T> {
        public volatile T value;
    }

}