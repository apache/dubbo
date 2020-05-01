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

package com.alibaba.dubbo.rpc;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.FutureContext;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.protocol.dubbo.FutureAdapter;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Deprecated
public class RpcContext {

    public static RpcContext getContext() {
        return new RpcContext(org.apache.dubbo.rpc.RpcContext.getContext());
    }

    public static RpcContext getServerContext() {
        return new RpcContext(org.apache.dubbo.rpc.RpcContext.getServerContext());
    }

    public static void removeServerContext() {
        org.apache.dubbo.rpc.RpcContext.removeServerContext();
    }

    public static void removeContext() {
        org.apache.dubbo.rpc.RpcContext.removeContext();
    }

    private org.apache.dubbo.rpc.RpcContext newRpcContext;

    public RpcContext(org.apache.dubbo.rpc.RpcContext newRpcContext) {
        this.newRpcContext = newRpcContext;
    }

    public Object getRequest() {
        return newRpcContext.getRequest();
    }

    public <T> T getRequest(Class<T> clazz) {
        return newRpcContext.getRequest(clazz);
    }


    public void setRequest(Object request) {
        newRpcContext.setRequest(request);
    }

    /**
     * Get the response object of the underlying RPC protocol, e.g. HttpServletResponse
     *
     * @return null if the underlying protocol doesn't provide support for getting response
     */
    public Object getResponse() {
        return newRpcContext.getResponse();
    }

    /**
     * Get the response object of the underlying RPC protocol, e.g. HttpServletResponse
     *
     * @return null if the underlying protocol doesn't provide support for getting response or the response is not of the specified type
     */
    @SuppressWarnings("unchecked")
    public <T> T getResponse(Class<T> clazz) {
        return newRpcContext.getResponse(clazz);
    }

    public void setResponse(Object response) {
        newRpcContext.setResponse(response);
    }

    /**
     * is provider side.
     *
     * @return provider side.
     */
    public boolean isProviderSide() {
        return newRpcContext.isProviderSide();
    }

    /**
     * is consumer side.
     *
     * @return consumer side.
     */
    public boolean isConsumerSide() {
        return newRpcContext.isConsumerSide();
    }

    public List<URL> getUrls() {
        List<org.apache.dubbo.common.URL> newUrls = newRpcContext.getUrls();
        if (CollectionUtils.isNotEmpty(newUrls)) {
            List<URL> urls = new ArrayList<>(newUrls.size());
            for (org.apache.dubbo.common.URL newUrl : newUrls) {
                urls.add(new URL(newUrl));
            }
            return urls;
        }
        return Collections.emptyList();
    }

    public void setUrls(List<URL> urls) {
        if (CollectionUtils.isNotEmpty(urls)) {
            List<org.apache.dubbo.common.URL> newUrls = new ArrayList<>(urls.size());
            for (URL url : urls) {
                newUrls.add(url.getOriginalURL());
            }
            newRpcContext.setUrls(newUrls);
        }
    }

    public URL getUrl() {
        return new URL(newRpcContext.getUrl());
    }

    public void setUrl(URL url) {
        newRpcContext.setUrl(url.getOriginalURL());
    }

    public String getMethodName() {
        return newRpcContext.getMethodName();
    }

    public void setMethodName(String methodName) {
        newRpcContext.setMethodName(methodName);
    }

    public Class<?>[] getParameterTypes() {
        return newRpcContext.getParameterTypes();
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        newRpcContext.setParameterTypes(parameterTypes);
    }

    public Object[] getArguments() {
        return newRpcContext.getArguments();
    }

    public void setArguments(Object[] arguments) {
        newRpcContext.setArguments(arguments);
    }

    public RpcContext setLocalAddress(String host, int port) {
        newRpcContext.setLocalAddress(host, port);
        return this;
    }

    /**
     * get local address.
     *
     * @return local address
     */
    public InetSocketAddress getLocalAddress() {
        return newRpcContext.getLocalAddress();
    }

    public RpcContext setLocalAddress(InetSocketAddress address) {
        newRpcContext.setLocalAddress(address);
        return this;
    }

    public String getLocalAddressString() {
        return newRpcContext.getLocalAddressString();
    }

    public String getLocalHostName() {
        return newRpcContext.getLocalHostName();
    }

    public RpcContext setRemoteAddress(String host, int port) {
        newRpcContext.setRemoteAddress(host, port);
        return this;
    }

    public InetSocketAddress getRemoteAddress() {
        return newRpcContext.getRemoteAddress();
    }

    public RpcContext setRemoteAddress(InetSocketAddress address) {
        newRpcContext.setRemoteAddress(address);
        return this;
    }

    public String getRemoteAddressString() {
        return newRpcContext.getRemoteAddressString();
    }

    public String getRemoteHostName() {
        return newRpcContext.getRemoteHostName();
    }

    public String getLocalHost() {
        return newRpcContext.getLocalHost();
    }

    public int getLocalPort() {
        return newRpcContext.getLocalPort();
    }

    public String getRemoteHost() {
        return newRpcContext.getRemoteHost();
    }

    public int getRemotePort() {
        return newRpcContext.getRemotePort();
    }

    public String getAttachment(String key) {
        return newRpcContext.getAttachment(key);
    }

    public RpcContext setAttachment(String key, String value) {
        newRpcContext.setAttachment(key, value);
        return this;
    }

    public RpcContext removeAttachment(String key) {
        newRpcContext.removeAttachment(key);
        return this;
    }

    public Map<String, String> getAttachments() {
        return newRpcContext.getAttachments();
    }

    public RpcContext setAttachments(Map<String, String> attachment) {
        newRpcContext.setAttachments(attachment);
        return this;
    }

    public void clearAttachments() {
        newRpcContext.clearAttachments();
    }

    /**
     * get values.
     *
     * @return values
     */
    public Map<String, Object> get() {
        return newRpcContext.get();
    }

    /**
     * set value.
     *
     * @param key
     * @param value
     * @return context
     */
    public RpcContext set(String key, Object value) {
        newRpcContext.set(key, value);
        return this;
    }

    public RpcContext remove(String key) {
        newRpcContext.remove(key);
        return this;
    }

    public Object get(String key) {
        return newRpcContext.get(key);
    }

    @Deprecated
    public boolean isServerSide() {
        return isProviderSide();
    }

    @Deprecated
    public boolean isClientSide() {
        return isConsumerSide();
    }

    /**
     * Async invocation. Timeout will be handled even if <code>Future.get()</code> is not called.
     *
     * @param callable
     * @return get the return result from <code>future.get()</code>
     */
    @SuppressWarnings("unchecked")
    public <T> Future<T> asyncCall(Callable<T> callable) {
        try {
            try {
                setAttachment(Constants.ASYNC_KEY, Boolean.TRUE.toString());
                final T o = callable.call();
                //local invoke will return directly
                if (o != null) {
                    FutureTask<T> f = new FutureTask<T>(new Callable<T>() {
                        @Override
                        public T call() throws Exception {
                            return o;
                        }
                    });
                    f.run();
                    return f;
                } else {

                }
            } catch (Exception e) {
                throw new RpcException(e);
            } finally {
                removeAttachment(Constants.ASYNC_KEY);
            }
        } catch (final RpcException e) {
            return new Future<T>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public boolean isDone() {
                    return true;
                }

                @Override
                public T get() throws InterruptedException, ExecutionException {
                    throw new ExecutionException(e.getCause());
                }

                @Override
                public T get(long timeout, TimeUnit unit)
                        throws InterruptedException, ExecutionException,
                        TimeoutException {
                    return get();
                }
            };
        }
        return ((Future<T>) getContext().getFuture());
    }

    /**
     * one way async call, send request only, and result is not required
     *
     * @param runnable
     */
    public void asyncCall(Runnable runnable) {
        try {
            setAttachment(Constants.RETURN_KEY, Boolean.FALSE.toString());
            runnable.run();
        } catch (Throwable e) {
            // FIXME should put exception in future?
            throw new RpcException("oneway call error ." + e.getMessage(), e);
        } finally {
            removeAttachment(Constants.RETURN_KEY);
        }
    }

    public <T> Future<T> getFuture() {
        CompletableFuture completableFuture = FutureContext.getContext().getCompatibleCompletableFuture();
        if (completableFuture == null) {
            return null;
        }
        return new FutureAdapter(completableFuture);
    }

    public void setFuture(CompletableFuture<?> future) {
        FutureContext.getContext().setCompatibleFuture(future);
    }
}
