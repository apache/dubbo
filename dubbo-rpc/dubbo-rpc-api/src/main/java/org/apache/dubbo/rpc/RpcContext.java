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

import org.apache.dubbo.common.Experimental;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.threadlocal.InternalThreadLocal;
import org.apache.dubbo.common.utils.StringUtils;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;


/**
 * Thread local context. (API, ThreadLocal, ThreadSafe)
 * <p>
 * Note: RpcContext is a temporary state holder. States in RpcContext changes every time when request is sent or received.
 * <p/>
 * There are four kinds of RpcContext, which are ServerContext, ClientAttachment, ServerAttachment and ServiceContext.
 * <p/>
 * ServiceContext: Using to pass environment parameters in the whole invocation. For example, `remotingApplicationName`,
 * `remoteAddress`, etc. {@link RpcServiceContext}
 * ClientAttachment, ServerAttachment and ServiceContext are using to transfer attachments.
 * Imaging a situation like this, A is calling B, and B will call C, after that, B wants to return some attachments back to A.
 * ClientAttachment is using to pass attachments to next hop as a consumer. ( A --> B , in A side)
 * ServerAttachment is using to fetch attachments from previous hop as a provider. ( A --> B , in B side)
 * ServerContext is using to return some attachments back to client as a provider. ( A <-- B , in B side)
 * The reason why using `ServiceContext` is to make API compatible with previous.
 *
 * @export
 * @see org.apache.dubbo.rpc.filter.ContextFilter
 */
public class RpcContext {

    private static final RpcContext AGENT = new RpcContext();

    /**
     * use internal thread local to improve performance
     */

    private static final InternalThreadLocal<RpcContextAttachment> CLIENT_RESPONSE_LOCAL = new InternalThreadLocal<RpcContextAttachment>() {
        @Override
        protected RpcContextAttachment initialValue() {
            return new RpcContextAttachment();
        }
    };

    private static final InternalThreadLocal<RpcContextAttachment> SERVER_RESPONSE_LOCAL = new InternalThreadLocal<RpcContextAttachment>() {
        @Override
        protected RpcContextAttachment initialValue() {
            return new RpcContextAttachment();
        }
    };

    private static final InternalThreadLocal<RpcContextAttachment> CLIENT_ATTACHMENT = new InternalThreadLocal<RpcContextAttachment>() {
        @Override
        protected RpcContextAttachment initialValue() {
            return new RpcContextAttachment();
        }
    };

    private static final InternalThreadLocal<RpcContextAttachment> SERVER_ATTACHMENT = new InternalThreadLocal<RpcContextAttachment>() {
        @Override
        protected RpcContextAttachment initialValue() {
            return new RpcContextAttachment();
        }
    };

    private static final InternalThreadLocal<RpcServiceContext> SERVICE_CONTEXT = new InternalThreadLocal<RpcServiceContext>() {
        @Override
        protected RpcServiceContext initialValue() {
            return new RpcServiceContext();
        }
    };

    /**
     * use by cancel call
     */
    private static final InternalThreadLocal<CancellationContext> CANCELLATION_CONTEXT = new InternalThreadLocal<CancellationContext>() {
        @Override
        protected CancellationContext initialValue() {
            return new CancellationContext();
        }
    };


    public static CancellationContext getCancellationContext() {
        return CANCELLATION_CONTEXT.get();
    }

    public static void removeCancellationContext() {
        CANCELLATION_CONTEXT.remove();
    }

    public static void restoreCancellationContext(CancellationContext oldContext) {
        CANCELLATION_CONTEXT.set(oldContext);
    }

    private boolean remove = true;

    protected RpcContext() {
    }

    /**
     * get server side context. ( A <-- B , in B side)
     *
     * @return server context
     */
    public static RpcContextAttachment getServerContext() {
        return new RpcServerContextAttachment();
    }

    /**
     * remove server side context.
     *
     * @see org.apache.dubbo.rpc.filter.ContextFilter
     */
    public static RpcContextAttachment getClientResponseContext() {
        return CLIENT_RESPONSE_LOCAL.get();
    }

    public static RpcContextAttachment getServerResponseContext() {
        return SERVER_RESPONSE_LOCAL.get();
    }

    public static void removeClientResponseContext() {
        CLIENT_RESPONSE_LOCAL.remove();
    }

    public static void removeServerResponseContext() {
        SERVER_RESPONSE_LOCAL.remove();
    }

    /**
     * get context.
     *
     * @return context
     */
    @Deprecated
    public static RpcContext getContext() {
        return AGENT;
    }

    /**
     * get consumer side attachment ( A --> B , in A side)
     *
     * @return context
     */
    public static RpcContextAttachment getClientAttachment() {
        return CLIENT_ATTACHMENT.get();
    }

    /**
     * get provider side attachment from consumer ( A --> B , in B side)
     *
     * @return context
     */
    public static RpcContextAttachment getServerAttachment() {
        return SERVER_ATTACHMENT.get();
    }

    public static void removeServerContext() {
        RpcContextAttachment rpcContextAttachment = RpcContext.getServerContext();
        for(String key : rpcContextAttachment.attachments.keySet()) {
            rpcContextAttachment.remove(key);
        }
    }

    public boolean canRemove() {
        return remove;
    }

    public void clearAfterEachInvoke(boolean remove) {
        this.remove = remove;
    }

    /**
     * Using to pass environment parameters in the whole invocation. For example, `remotingApplicationName`,
     * `remoteAddress`, etc. {@link RpcServiceContext}
     *
     * @return context
     */
    public static RpcServiceContext getServiceContext() {
        return SERVICE_CONTEXT.get();
    }

    public static RpcServiceContext getCurrentServiceContext() {
        return SERVICE_CONTEXT.getWithoutInitialize();
    }

    public static void removeServiceContext() {
        SERVICE_CONTEXT.remove();
    }

    public static void removeClientAttachment() {
        if (CLIENT_ATTACHMENT.get().canRemove()) {
            CLIENT_ATTACHMENT.remove();
        }
    }

    public static void removeServerAttachment() {
        if (SERVER_ATTACHMENT.get().canRemove()) {
            SERVER_ATTACHMENT.remove();
        }
    }

    /**
     * customized for internal use.
     */
    public static void removeContext() {
        if (CLIENT_ATTACHMENT.get().canRemove()) {
            CLIENT_ATTACHMENT.remove();
        }
        if (SERVER_ATTACHMENT.get().canRemove()) {
            SERVER_ATTACHMENT.remove();
        }
        CLIENT_RESPONSE_LOCAL.remove();
        SERVER_RESPONSE_LOCAL.remove();
        SERVICE_CONTEXT.remove();
        CANCELLATION_CONTEXT.remove();
    }

    /**
     * Get the request object of the underlying RPC protocol, e.g. HttpServletRequest
     *
     * @return null if the underlying protocol doesn't provide support for getting request
     */
    public Object getRequest() {
        return SERVICE_CONTEXT.get().getRequest();
    }

    public void setRequest(Object request) {
        SERVICE_CONTEXT.get().setRequest(request);
    }

    /**
     * Get the request object of the underlying RPC protocol, e.g. HttpServletRequest
     *
     * @return null if the underlying protocol doesn't provide support for getting request or the request is not of the specified type
     */
    @SuppressWarnings("unchecked")
    public <T> T getRequest(Class<T> clazz) {
        return SERVICE_CONTEXT.get().getRequest(clazz);
    }

    /**
     * Get the response object of the underlying RPC protocol, e.g. HttpServletResponse
     *
     * @return null if the underlying protocol doesn't provide support for getting response
     */
    public Object getResponse() {
        return SERVICE_CONTEXT.get().getResponse();
    }

    public void setResponse(Object response) {
        SERVICE_CONTEXT.get().setResponse(response);
    }

    /**
     * Get the response object of the underlying RPC protocol, e.g. HttpServletResponse
     *
     * @return null if the underlying protocol doesn't provide support for getting response or the response is not of the specified type
     */
    @SuppressWarnings("unchecked")
    public <T> T getResponse(Class<T> clazz) {
        return SERVICE_CONTEXT.get().getResponse(clazz);
    }

    /**
     * is provider side.
     *
     * @return provider side.
     */
    public boolean isProviderSide() {
        return SERVICE_CONTEXT.get().isProviderSide();
    }

    /**
     * is consumer side.
     *
     * @return consumer side.
     */
    public boolean isConsumerSide() {
        return SERVICE_CONTEXT.get().isConsumerSide();
    }

    /**
     * get CompletableFuture.
     *
     * @param <T>
     * @return future
     */
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> getCompletableFuture() {
        return SERVICE_CONTEXT.get().getCompletableFuture();
    }

    /**
     * get future.
     *
     * @param <T>
     * @return future
     */
    @SuppressWarnings("unchecked")
    public <T> Future<T> getFuture() {
        return SERVICE_CONTEXT.get().getFuture();
    }

    /**
     * set future.
     *
     * @param future
     */
    public void setFuture(CompletableFuture<?> future) {
        SERVICE_CONTEXT.get().setFuture(future);
    }

    public List<URL> getUrls() {
        return SERVICE_CONTEXT.get().getUrls();
    }

    public void setUrls(List<URL> urls) {
        SERVICE_CONTEXT.get().setUrls(urls);
    }

    public URL getUrl() {
        return SERVICE_CONTEXT.get().getUrl();
    }

    public void setUrl(URL url) {
        SERVICE_CONTEXT.get().setUrl(url);
    }

    /**
     * get method name.
     *
     * @return method name.
     */
    public String getMethodName() {
        return SERVICE_CONTEXT.get().getMethodName();
    }

    public void setMethodName(String methodName) {
        SERVICE_CONTEXT.get().setMethodName(methodName);
    }

    /**
     * get parameter types.
     *
     * @serial
     */
    public Class<?>[] getParameterTypes() {
        return SERVICE_CONTEXT.get().getParameterTypes();
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        SERVICE_CONTEXT.get().setParameterTypes(parameterTypes);
    }

    /**
     * get arguments.
     *
     * @return arguments.
     */
    public Object[] getArguments() {
        return SERVICE_CONTEXT.get().getArguments();
    }

    public void setArguments(Object[] arguments) {
        SERVICE_CONTEXT.get().setArguments(arguments);
    }

    /**
     * set local address.
     *
     * @param host
     * @param port
     * @return context
     */
    public RpcContext setLocalAddress(String host, int port) {
        return SERVICE_CONTEXT.get().setLocalAddress(host, port);
    }

    /**
     * get local address.
     *
     * @return local address
     */
    public InetSocketAddress getLocalAddress() {
        return SERVICE_CONTEXT.get().getLocalAddress();
    }

    /**
     * set local address.
     *
     * @param address
     * @return context
     */
    public RpcContext setLocalAddress(InetSocketAddress address) {
        return SERVICE_CONTEXT.get().setLocalAddress(address);
    }

    public String getLocalAddressString() {
        return SERVICE_CONTEXT.get().getLocalAddressString();
    }

    /**
     * get local host name.
     *
     * @return local host name
     */
    public String getLocalHostName() {
        return SERVICE_CONTEXT.get().getLocalHostName();
    }

    /**
     * set remote address.
     *
     * @param host
     * @param port
     * @return context
     */
    public RpcContext setRemoteAddress(String host, int port) {
        return SERVICE_CONTEXT.get().setRemoteAddress(host, port);
    }

    /**
     * get remote address.
     *
     * @return remote address
     */
    public InetSocketAddress getRemoteAddress() {
        return SERVICE_CONTEXT.get().getRemoteAddress();
    }

    /**
     * set remote address.
     *
     * @param address
     * @return context
     */
    public RpcContext setRemoteAddress(InetSocketAddress address) {
        return SERVICE_CONTEXT.get().setRemoteAddress(address);
    }

    public String getRemoteApplicationName() {
        return SERVICE_CONTEXT.get().getRemoteApplicationName();
    }

    public RpcContext setRemoteApplicationName(String remoteApplicationName) {
        return SERVICE_CONTEXT.get().setRemoteApplicationName(remoteApplicationName);
    }

    /**
     * get remote address string.
     *
     * @return remote address string.
     */
    public String getRemoteAddressString() {
        return SERVICE_CONTEXT.get().getRemoteAddressString();
    }

    /**
     * get remote host name.
     *
     * @return remote host name
     */
    public String getRemoteHostName() {
        return SERVICE_CONTEXT.get().getRemoteHostName();
    }

    /**
     * get local host.
     *
     * @return local host
     */
    public String getLocalHost() {
        return SERVICE_CONTEXT.get().getLocalHost();
    }

    /**
     * get local port.
     *
     * @return port
     */
    public int getLocalPort() {
        return SERVICE_CONTEXT.get().getLocalPort();
    }

    /**
     * get remote host.
     *
     * @return remote host
     */
    public String getRemoteHost() {
        return SERVICE_CONTEXT.get().getRemoteHost();
    }

    /**
     * get remote port.
     *
     * @return remote port
     */
    public int getRemotePort() {
        return SERVICE_CONTEXT.get().getRemotePort();
    }

    /**
     * also see {@link #getObjectAttachment(String)}.
     *
     * @param key
     * @return attachment
     */
    public String getAttachment(String key) {
        String client = CLIENT_ATTACHMENT.get().getAttachment(key);
        if (StringUtils.isEmpty(client)) {
            return SERVER_ATTACHMENT.get().getAttachment(key);
        }
        return client;
    }

    /**
     * get attachment.
     *
     * @param key
     * @return attachment
     */
    @Experimental("Experiment api for supporting Object transmission")
    public Object getObjectAttachment(String key) {
        Object client = CLIENT_ATTACHMENT.get().getObjectAttachment(key);
        if (client == null) {
            return SERVER_ATTACHMENT.get().getObjectAttachment(key);
        }
        return client;
    }

    /**
     * set attachment.
     *
     * @param key
     * @param value
     * @return context
     */
    public RpcContext setAttachment(String key, String value) {
        return setObjectAttachment(key, value);
    }

    public RpcContext setAttachment(String key, Object value) {
        return setObjectAttachment(key, value);
    }

    @Experimental("Experiment api for supporting Object transmission")
    public RpcContext setObjectAttachment(String key, Object value) {
        // TODO compatible with previous
        CLIENT_ATTACHMENT.get().setObjectAttachment(key, value);
        return this;
    }

    /**
     * remove attachment.
     *
     * @param key
     * @return context
     */
    public RpcContext removeAttachment(String key) {
        CLIENT_ATTACHMENT.get().removeAttachment(key);
        return this;
    }

    /**
     * get attachments.
     *
     * @return attachments
     */
    @Deprecated
    public Map<String, String> getAttachments() {
        return new AttachmentsAdapter.ObjectToStringMap(this.getObjectAttachments());
    }

    /**
     * get attachments.
     *
     * @return attachments
     */
    @Experimental("Experiment api for supporting Object transmission")
    public Map<String, Object> getObjectAttachments() {
        Map<String, Object> result = new HashMap<>((int) ((CLIENT_ATTACHMENT.get().attachments.size() + SERVER_ATTACHMENT.get().attachments.size()) / .75) + 1);
        result.putAll(SERVER_ATTACHMENT.get().attachments);
        result.putAll(CLIENT_ATTACHMENT.get().attachments);
        return result;
    }

    /**
     * set attachments
     *
     * @param attachment
     * @return context
     */
    public RpcContext setAttachments(Map<String, String> attachment) {
        CLIENT_ATTACHMENT.get().attachments.clear();
        if (attachment != null && attachment.size() > 0) {
            CLIENT_ATTACHMENT.get().attachments.putAll(attachment);
        }
        return this;
    }

    /**
     * set attachments
     *
     * @param attachment
     * @return context
     */
    @Experimental("Experiment api for supporting Object transmission")
    public RpcContext setObjectAttachments(Map<String, Object> attachment) {
        CLIENT_ATTACHMENT.get().attachments.clear();
        if (attachment != null && attachment.size() > 0) {
            CLIENT_ATTACHMENT.get().attachments.putAll(attachment);
        }
        return this;
    }

    public void clearAttachments() {
        CLIENT_ATTACHMENT.get().attachments.clear();
    }

    /**
     * get values.
     *
     * @return values
     */
    @Deprecated
    public Map<String, Object> get() {
        return CLIENT_ATTACHMENT.get().get();
    }

    /**
     * set value.
     *
     * @param key
     * @param value
     * @return context
     */
    @Deprecated
    public RpcContext set(String key, Object value) {
        CLIENT_ATTACHMENT.get().set(key, value);
        return this;
    }

    /**
     * remove value.
     *
     * @param key
     * @return value
     */
    @Deprecated
    public RpcContext remove(String key) {
        CLIENT_ATTACHMENT.get().remove(key);
        return this;
    }

    /**
     * get value.
     *
     * @param key
     * @return value
     */
    @Deprecated
    public Object get(String key) {
        return CLIENT_ATTACHMENT.get().get(key);
    }

    /**
     * @deprecated Replace to isProviderSide()
     */
    @Deprecated
    public boolean isServerSide() {
        return SERVICE_CONTEXT.get().isServerSide();
    }

    /**
     * @deprecated Replace to isConsumerSide()
     */
    @Deprecated
    public boolean isClientSide() {
        return SERVICE_CONTEXT.get().isClientSide();
    }

    /**
     * @deprecated Replace to getUrls()
     */
    @Deprecated
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Invoker<?>> getInvokers() {
        return SERVICE_CONTEXT.get().getInvokers();
    }

    public RpcContext setInvokers(List<Invoker<?>> invokers) {
        return SERVICE_CONTEXT.get().setInvokers(invokers);
    }

    /**
     * @deprecated Replace to getUrl()
     */
    @Deprecated
    public Invoker<?> getInvoker() {
        return SERVICE_CONTEXT.get().getInvoker();
    }

    public RpcContext setInvoker(Invoker<?> invoker) {
        return SERVICE_CONTEXT.get().setInvoker(invoker);
    }

    /**
     * @deprecated Replace to getMethodName(), getParameterTypes(), getArguments()
     */
    @Deprecated
    public Invocation getInvocation() {
        return SERVICE_CONTEXT.get().getInvocation();
    }

    public RpcContext setInvocation(Invocation invocation) {
        return SERVICE_CONTEXT.get().setInvocation(invocation);
    }

    /**
     * Async invocation. Timeout will be handled even if <code>Future.get()</code> is not called.
     *
     * @param callable
     * @return get the return result from <code>future.get()</code>
     */
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> asyncCall(Callable<T> callable) {
        return SERVICE_CONTEXT.get().asyncCall(callable);
    }

    /**
     * one way async call, send request only, and result is not required
     *
     * @param runnable
     */
    public void asyncCall(Runnable runnable) {
        SERVICE_CONTEXT.get().asyncCall(runnable);
    }

    /**
     * @return
     * @throws IllegalStateException
     */
    @SuppressWarnings("unchecked")
    public static AsyncContext startAsync() throws IllegalStateException {
        return RpcContextAttachment.startAsync();
    }

    protected void setAsyncContext(AsyncContext asyncContext) {
        SERVER_ATTACHMENT.get().setAsyncContext(asyncContext);
    }

    public boolean isAsyncStarted() {
        return SERVER_ATTACHMENT.get().isAsyncStarted();
    }

    public boolean stopAsync() {
        return SERVER_ATTACHMENT.get().stopAsync();
    }

    public AsyncContext getAsyncContext() {
        return SERVER_ATTACHMENT.get().getAsyncContext();
    }

    public String getGroup() {
        return SERVICE_CONTEXT.get().getGroup();
    }

    public String getVersion() {
        return SERVICE_CONTEXT.get().getVersion();
    }

    public String getInterfaceName() {
        return SERVICE_CONTEXT.get().getInterfaceName();
    }

    public String getProtocol() {
        return SERVICE_CONTEXT.get().getProtocol();
    }

    public String getServiceKey() {
        return SERVICE_CONTEXT.get().getServiceKey();
    }

    public String getProtocolServiceKey() {
        return SERVICE_CONTEXT.get().getProtocolServiceKey();
    }

    public URL getConsumerUrl() {
        return SERVICE_CONTEXT.get().getConsumerUrl();
    }

    public void setConsumerUrl(URL consumerUrl) {
        SERVICE_CONTEXT.get().setConsumerUrl(consumerUrl);
    }

    @Deprecated
    public static void setRpcContext(URL url) {
        RpcServiceContext.getServiceContext().setConsumerUrl(url);
    }

    protected static RestoreContext clearAndStoreContext() {
        RestoreContext restoreContext = new RestoreContext();
        RpcContext.removeContext();
        return restoreContext;
    }

    protected static RestoreContext storeContext() {
        return new RestoreContext();
    }

    public static RestoreServiceContext storeServiceContext() {
        return new RestoreServiceContext();
    }

    public static void restoreServiceContext(RestoreServiceContext restoreServiceContext) {
        if (restoreServiceContext != null) {
            restoreServiceContext.restore();
        }
    }

    protected static void restoreContext(RestoreContext restoreContext) {
        if (restoreContext != null) {
            restoreContext.restore();
        }
    }

    /**
     * Used to temporarily store and restore all kinds of contexts of current thread.
     */
    public static class RestoreContext {
        private final RpcServiceContext serviceContext;
        private final RpcContextAttachment clientAttachment;
        private final RpcContextAttachment serverAttachment;
        private final RpcContextAttachment clientResponseLocal;
        private final RpcContextAttachment serverResponseLocal;

        public RestoreContext() {
            serviceContext = getServiceContext().copyOf(false);
            clientAttachment = getClientAttachment().copyOf(false);
            serverAttachment = getServerAttachment().copyOf(false);
            clientResponseLocal = getClientResponseContext().copyOf(false);
            serverResponseLocal = getServerResponseContext().copyOf(false);
        }

        public void restore() {
            if (serviceContext != null) {
                SERVICE_CONTEXT.set(serviceContext);
            } else {
                removeServiceContext();
            }
            if (clientAttachment != null) {
                CLIENT_ATTACHMENT.set(clientAttachment);
            } else {
                removeClientAttachment();
            }
            if (serverAttachment != null) {
                SERVER_ATTACHMENT.set(serverAttachment);
            } else {
                removeServerAttachment();
            }
            if (clientResponseLocal != null) {
                CLIENT_RESPONSE_LOCAL.set(clientResponseLocal);
            } else {
                removeClientResponseContext();
            }
            if (serverResponseLocal != null) {
                SERVER_RESPONSE_LOCAL.set(serverResponseLocal);
            } else {
                removeServerResponseContext();
            }
        }
    }

    public static class RestoreServiceContext {
        private final RpcServiceContext serviceContext;

        public RestoreServiceContext() {
            RpcServiceContext originContext = getCurrentServiceContext();
            if (originContext == null) {
                this.serviceContext = null;
            } else {
                this.serviceContext = originContext.copyOf(true);
            }
        }

        protected void restore() {
            if (serviceContext != null) {
                SERVICE_CONTEXT.set(serviceContext);
            } else {
                removeServiceContext();
            }
        }
    }
}
