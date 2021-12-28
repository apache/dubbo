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
package org.apache.dubbo.rpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.rpc.Constants.ASYNC_KEY;
import static org.apache.dubbo.rpc.Constants.RETURN_KEY;

public class RpcServiceContext extends RpcContext {

    protected RpcServiceContext() {
    }

    // RPC service context updated before each service call.
    private URL consumerUrl;

    private List<URL> urls;

    private URL url;

    private String methodName;

    private Class<?>[] parameterTypes;

    private Object[] arguments;

    private InetSocketAddress localAddress;

    private InetSocketAddress remoteAddress;

    private String remoteApplicationName;

    @Deprecated
    private List<Invoker<?>> invokers;
    @Deprecated
    private Invoker<?> invoker;
    @Deprecated
    private Invocation invocation;

    // now we don't use the 'values' map to hold these objects
    // we want these objects to be as generic as possible
    private Object request;
    private Object response;
    private AsyncContext asyncContext;

    private boolean needPrintRouterSnapshot;

    /**
     * Get the request object of the underlying RPC protocol, e.g. HttpServletRequest
     *
     * @return null if the underlying protocol doesn't provide support for getting request
     */
    @Override
    public Object getRequest() {
        return request;
    }

    @Override
    public void setRequest(Object request) {
        this.request = request;
    }

    /**
     * Get the request object of the underlying RPC protocol, e.g. HttpServletRequest
     *
     * @return null if the underlying protocol doesn't provide support for getting request or the request is not of the specified type
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getRequest(Class<T> clazz) {
        return (request != null && clazz.isAssignableFrom(request.getClass())) ? (T) request : null;
    }

    /**
     * Get the response object of the underlying RPC protocol, e.g. HttpServletResponse
     *
     * @return null if the underlying protocol doesn't provide support for getting response
     */
    @Override
    public Object getResponse() {
        return response;
    }

    @Override
    public void setResponse(Object response) {
        this.response = response;
    }

    /**
     * Get the response object of the underlying RPC protocol, e.g. HttpServletResponse
     *
     * @return null if the underlying protocol doesn't provide support for getting response or the response is not of the specified type
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getResponse(Class<T> clazz) {
        return (response != null && clazz.isAssignableFrom(response.getClass())) ? (T) response : null;
    }

    /**
     * is provider side.
     *
     * @return provider side.
     */
    @Override
    public boolean isProviderSide() {
        return !isConsumerSide();
    }

    /**
     * is consumer side.
     *
     * @return consumer side.
     */
    @Override
    public boolean isConsumerSide() {
        return getUrl().getSide(PROVIDER_SIDE).equals(CONSUMER_SIDE);
    }

    /**
     * get CompletableFuture.
     *
     * @param <T>
     * @return future
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> getCompletableFuture() {
        return FutureContext.getContext().getCompletableFuture();
    }

    /**
     * get future.
     *
     * @param <T>
     * @return future
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> Future<T> getFuture() {
        return FutureContext.getContext().getCompletableFuture();
    }

    /**
     * set future.
     *
     * @param future
     */
    @Override
    public void setFuture(CompletableFuture<?> future) {
        FutureContext.getContext().setFuture(future);
    }

    @Override
    public List<URL> getUrls() {
        return urls == null && url != null ? Arrays.asList(url) : urls;
    }

    @Override
    public void setUrls(List<URL> urls) {
        this.urls = urls;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void setUrl(URL url) {
        this.url = url;
    }

    /**
     * get method name.
     *
     * @return method name.
     */
    @Override
    public String getMethodName() {
        return methodName;
    }

    @Override
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    /**
     * get parameter types.
     *
     * @serial
     */
    @Override
    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    /**
     * get arguments.
     *
     * @return arguments.
     */
    @Override
    public Object[] getArguments() {
        return arguments;
    }

    @Override
    public void setArguments(Object[] arguments) {
        this.arguments = arguments;
    }

    /**
     * set local address.
     *
     * @param host
     * @param port
     * @return context
     */
    @Override
    public RpcServiceContext setLocalAddress(String host, int port) {
        if (port < 0) {
            port = 0;
        }
        this.localAddress = InetSocketAddress.createUnresolved(host, port);
        return this;
    }

    /**
     * get local address.
     *
     * @return local address
     */
    @Override
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    /**
     * set local address.
     *
     * @param address
     * @return context
     */
    @Override
    public RpcServiceContext setLocalAddress(InetSocketAddress address) {
        this.localAddress = address;
        return this;
    }

    @Override
    public String getLocalAddressString() {
        return getLocalHost() + ":" + getLocalPort();
    }

    /**
     * get local host name.
     *
     * @return local host name
     */
    @Override
    public String getLocalHostName() {
        String host = localAddress == null ? null : localAddress.getHostName();
        if (StringUtils.isEmpty(host)) {
            return getLocalHost();
        }
        return host;
    }

    /**
     * set remote address.
     *
     * @param host
     * @param port
     * @return context
     */
    @Override
    public RpcServiceContext setRemoteAddress(String host, int port) {
        if (port < 0) {
            port = 0;
        }
        this.remoteAddress = InetSocketAddress.createUnresolved(host, port);
        return this;
    }

    /**
     * get remote address.
     *
     * @return remote address
     */
    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * set remote address.
     *
     * @param address
     * @return context
     */
    @Override
    public RpcServiceContext setRemoteAddress(InetSocketAddress address) {
        this.remoteAddress = address;
        return this;
    }

    @Override
    public String getRemoteApplicationName() {
        return remoteApplicationName;
    }

    @Override
    public RpcServiceContext setRemoteApplicationName(String remoteApplicationName) {
        this.remoteApplicationName = remoteApplicationName;
        return this;
    }

    /**
     * get remote address string.
     *
     * @return remote address string.
     */
    @Override
    public String getRemoteAddressString() {
        return getRemoteHost() + ":" + getRemotePort();
    }

    /**
     * get remote host name.
     *
     * @return remote host name
     */
    @Override
    public String getRemoteHostName() {
        return remoteAddress == null ? null : remoteAddress.getHostName();
    }

    /**
     * get local host.
     *
     * @return local host
     */
    @Override
    public String getLocalHost() {
        String host = localAddress == null ? null :
                localAddress.getAddress() == null ? localAddress.getHostName()
                        : NetUtils.filterLocalHost(localAddress.getAddress().getHostAddress());
        if (host == null || host.length() == 0) {
            return NetUtils.getLocalHost();
        }
        return host;
    }

    /**
     * get local port.
     *
     * @return port
     */
    @Override
    public int getLocalPort() {
        return localAddress == null ? 0 : localAddress.getPort();
    }

    /**
     * get remote host.
     *
     * @return remote host
     */
    @Override
    public String getRemoteHost() {
        return remoteAddress == null ? null :
                remoteAddress.getAddress() == null ? remoteAddress.getHostName()
                        : NetUtils.filterLocalHost(remoteAddress.getAddress().getHostAddress());
    }

    /**
     * get remote port.
     *
     * @return remote port
     */
    @Override
    public int getRemotePort() {
        return remoteAddress == null ? 0 : remoteAddress.getPort();
    }

    /**
     * @deprecated Replace to isProviderSide()
     */
    @Override
    @Deprecated
    public boolean isServerSide() {
        return isProviderSide();
    }

    /**
     * @deprecated Replace to isConsumerSide()
     */
    @Override
    @Deprecated
    public boolean isClientSide() {
        return isConsumerSide();
    }

    /**
     * @deprecated Replace to getUrls()
     */
    @Override
    @Deprecated
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Invoker<?>> getInvokers() {
        return invokers == null && invoker != null ? (List) Arrays.asList(invoker) : invokers;
    }

    @Override
    public RpcServiceContext setInvokers(List<Invoker<?>> invokers) {
        this.invokers = invokers;
        if (CollectionUtils.isNotEmpty(invokers)) {
            List<URL> urls = new ArrayList<URL>(invokers.size());
            for (Invoker<?> invoker : invokers) {
                urls.add(invoker.getUrl());
            }
            setUrls(urls);
        }
        return this;
    }

    /**
     * @deprecated Replace to getUrl()
     */
    @Override
    @Deprecated
    public Invoker<?> getInvoker() {
        return invoker;
    }

    @Override
    public RpcServiceContext setInvoker(Invoker<?> invoker) {
        this.invoker = invoker;
        if (invoker != null) {
            setUrl(invoker.getUrl());
        }
        return this;
    }

    /**
     * @deprecated Replace to getMethodName(), getParameterTypes(), getArguments()
     */
    @Override
    @Deprecated
    public Invocation getInvocation() {
        return invocation;
    }

    @Override
    public RpcServiceContext setInvocation(Invocation invocation) {
        this.invocation = invocation;
        if (invocation != null) {
            setMethodName(invocation.getMethodName());
            setParameterTypes(invocation.getParameterTypes());
            setArguments(invocation.getArguments());
        }
        return this;
    }

    /**
     * Async invocation. Timeout will be handled even if <code>Future.get()</code> is not called.
     *
     * @param callable
     * @return get the return result from <code>future.get()</code>
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> asyncCall(Callable<T> callable) {
        try {
            try {
                setAttachment(ASYNC_KEY, Boolean.TRUE.toString());
                final T o = callable.call();
                //local invoke will return directly
                if (o != null) {
                    if (o instanceof CompletableFuture) {
                        return (CompletableFuture<T>) o;
                    }
                    return CompletableFuture.completedFuture(o);
                } else {
                    // The service has a normal sync method signature, should get future from RpcContext.
                }
            } catch (Exception e) {
                throw new RpcException(e);
            } finally {
                removeAttachment(ASYNC_KEY);
            }
        } catch (final RpcException e) {
            CompletableFuture<T> exceptionFuture = new CompletableFuture<>();
            exceptionFuture.completeExceptionally(e);
            return exceptionFuture;
        }
        return ((CompletableFuture<T>) getServiceContext().getFuture());
    }

    /**
     * one way async call, send request only, and result is not required
     *
     * @param runnable
     */
    @Override
    public void asyncCall(Runnable runnable) {
        try {
            setAttachment(RETURN_KEY, Boolean.FALSE.toString());
            runnable.run();
        } catch (Throwable e) {
            // FIXME should put exception in future?
            throw new RpcException("oneway call error ." + e.getMessage(), e);
        } finally {
            removeAttachment(RETURN_KEY);
        }
    }

    /**
     * @return
     * @throws IllegalStateException
     */
    @SuppressWarnings("unchecked")
    public static AsyncContext startAsync() throws IllegalStateException {
        RpcServiceContext currentContext = getServiceContext();
        if (currentContext.asyncContext == null) {
            currentContext.asyncContext = new AsyncContextImpl();
        }
        currentContext.asyncContext.start();
        return currentContext.asyncContext;
    }

    @Override
    protected void setAsyncContext(AsyncContext asyncContext) {
        this.asyncContext = asyncContext;
    }

    @Override
    public boolean isAsyncStarted() {
        if (this.asyncContext == null) {
            return false;
        }
        return asyncContext.isAsyncStarted();
    }

    @Override
    public boolean stopAsync() {
        return asyncContext.stop();
    }

    @Override
    public AsyncContext getAsyncContext() {
        return asyncContext;
    }

    @Override
    public String getGroup() {
        if (consumerUrl == null) {
            return null;
        }
        return consumerUrl.getGroup();
    }

    @Override
    public String getVersion() {
        if (consumerUrl == null) {
            return null;
        }
        return consumerUrl.getVersion();
    }

    @Override
    public String getInterfaceName() {
        if (consumerUrl == null) {
            return null;
        }
        return consumerUrl.getServiceInterface();
    }

    @Override
    public String getProtocol() {
        if (consumerUrl == null) {
            return null;
        }
        return consumerUrl.getParameter(PROTOCOL_KEY, DUBBO);
    }

    @Override
    public String getServiceKey() {
        if (consumerUrl == null) {
            return null;
        }
        return consumerUrl.getServiceKey();
    }

    @Override
    public String getProtocolServiceKey() {
        if (consumerUrl == null) {
            return null;
        }
        return consumerUrl.getProtocolServiceKey();
    }

    @Override
    public URL getConsumerUrl() {
        return consumerUrl;
    }

    @Override
    public void setConsumerUrl(URL consumerUrl) {
        this.consumerUrl = consumerUrl;
    }

    public static void setRpcContext(URL url) {
        RpcServiceContext rpcContext = RpcContext.getServiceContext();
        rpcContext.setConsumerUrl(url);
    }

    public boolean isNeedPrintRouterSnapshot() {
        return needPrintRouterSnapshot;
    }

    public void setNeedPrintRouterSnapshot(boolean needPrintRouterSnapshot) {
        this.needPrintRouterSnapshot = needPrintRouterSnapshot;
    }

    /**
     * Only part of the properties are copied, the others are either not used currently or can be got from invocation.
     * Also see {@link RpcContextAttachment#copyOf(boolean)}
     *
     * @param needCopy
     * @return a shallow copy of RpcServiceContext
     */
    public RpcServiceContext copyOf(boolean needCopy) {
        if (needCopy) {
            RpcServiceContext copy = new RpcServiceContext();
            copy.consumerUrl = this.consumerUrl;
            copy.localAddress = this.localAddress;
            copy.remoteAddress = this.remoteAddress;
            copy.invocation = this.invocation;
            copy.asyncContext = this.asyncContext;
            return copy;
        } else {
            return this;
        }
    }

}
