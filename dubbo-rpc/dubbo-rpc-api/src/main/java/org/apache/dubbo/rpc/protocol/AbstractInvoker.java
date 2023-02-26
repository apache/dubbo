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
package org.apache.dubbo.rpc.protocol;

import org.apache.dubbo.common.Node;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.ThreadlessExecutor;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.TimeoutException;
import org.apache.dubbo.remoting.utils.UrlUtils;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.InvokeMode;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.dubbo.FutureAdapter;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_VERSION;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_REQUEST;
import static org.apache.dubbo.rpc.Constants.SERIALIZATION_ID_KEY;

/**
 * This Invoker works on Consumer side.
 */
public abstract class AbstractInvoker<T> implements Invoker<T> {

    protected static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AbstractInvoker.class);

    /**
     * Service interface type
     */
    private final Class<T> type;

    /**
     * {@link Node} url
     */
    private final URL url;

    /**
     * {@link Invoker} default attachment
     */
    private final Map<String, Object> attachment;

    protected final String version;

    /**
     * {@link Node} available
     */
    private volatile boolean available = true;

    /**
     * {@link Node} destroy
     */
    private boolean destroyed = false;

    /**
     * Whether set future to Thread Local when invocation mode is sync
     */
    private static final boolean setFutureWhenSync = Boolean.parseBoolean(System.getProperty(CommonConstants.SET_FUTURE_IN_SYNC_MODE, "true"));

    // -- Constructor

    public AbstractInvoker(Class<T> type, URL url) {
        this(type, url, (Map<String, Object>) null);
    }

    public AbstractInvoker(Class<T> type, URL url, String[] keys) {
        this(type, url, convertAttachment(url, keys));
    }

    public AbstractInvoker(Class<T> type, URL url, Map<String, Object> attachment) {
        if (type == null) {
            throw new IllegalArgumentException("service type == null");
        }
        if (url == null) {
            throw new IllegalArgumentException("service url == null");
        }
        this.type = type;
        this.url = url;
        
        this.attachment = attachment == null
            ? null
            : Collections.unmodifiableMap(attachment);
        this.version = url.getVersion(DEFAULT_VERSION);

    }

    private static Map<String, Object> convertAttachment(URL url, String[] keys) {
        if (ArrayUtils.isEmpty(keys)) {
            return null;
        }
        Map<String, Object> attachment = new HashMap<>(keys.length);
        for (String key : keys) {
            String value = url.getParameter(key);
            if (value != null && value.length() > 0) {
                attachment.put(key, value);
            }
        }
        return attachment;
    }

    // -- Public api

    @Override
    public Class<T> getInterface() {
        return type;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void destroy() {
        this.destroyed = true;
        setAvailable(false);
    }

    protected void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public String toString() {
        return getInterface() + " -> " + (getUrl() == null ? "" : getUrl().getAddress());
    }

    @Override
    public Result invoke(Invocation inv) throws RpcException {
        // if invoker is destroyed due to address refresh from registry, let's allow the current invoke to proceed
        if (isDestroyed()) {
            logger.warn(PROTOCOL_FAILED_REQUEST, "", "", "Invoker for service " + this + " on consumer " + NetUtils.getLocalHost() + " is destroyed, " + ", dubbo version is " + Version.getVersion() + ", this invoker should not be used any longer");
        }

        RpcInvocation invocation = (RpcInvocation) inv;

        // prepare rpc invocation
        prepareInvocation(invocation);

        // do invoke rpc invocation and return async result
        AsyncRpcResult asyncResult = doInvokeAndReturn(invocation);

        // wait rpc result if sync
        waitForResultIfSync(asyncResult, invocation);

        return asyncResult;
    }

    private void prepareInvocation(RpcInvocation inv) {
        inv.setInvoker(this);

        addInvocationAttachments(inv);

        inv.setInvokeMode(RpcUtils.getInvokeMode(url, inv));

        RpcUtils.attachInvocationIdIfAsync(getUrl(), inv);

        attachInvocationSerializationId(inv);
    }

    /**
     * Attach Invocation Serialization id
     * <p>
     *     <ol>
     *         <li>Obtain the value from <code>prefer_serialization</code></li>
     *         <li>If the preceding information is not obtained, obtain the value from <code>serialization</code></li>
     *         <li>If neither is obtained, use the default value</li>
     *     </ol>
     * </p>
     *
     * @param inv inv
     */
    private void attachInvocationSerializationId(RpcInvocation inv) {
        Byte serializationId = UrlUtils.serializationId(getUrl());

        if (serializationId != null) {
            inv.put(SERIALIZATION_ID_KEY, serializationId);
        }
    }

    private void addInvocationAttachments(RpcInvocation invocation) {
        // invoker attachment
        if (CollectionUtils.isNotEmptyMap(attachment)) {
            invocation.addObjectAttachmentsIfAbsent(attachment);
        }

        // client context attachment
        Map<String, Object> clientContextAttachments = RpcContext.getClientAttachment().getObjectAttachments();
        if (CollectionUtils.isNotEmptyMap(clientContextAttachments)) {
            invocation.addObjectAttachmentsIfAbsent(clientContextAttachments);
        }
    }

    private AsyncRpcResult doInvokeAndReturn(RpcInvocation invocation) {
        AsyncRpcResult asyncResult;
        try {
            asyncResult = (AsyncRpcResult) doInvoke(invocation);
        } catch (InvocationTargetException e) {
            Throwable te = e.getTargetException();
            if (te != null) {
                // if biz exception
                if (te instanceof RpcException) {
                    ((RpcException) te).setCode(RpcException.BIZ_EXCEPTION);
                }
                asyncResult = AsyncRpcResult.newDefaultAsyncResult(null, te, invocation);
            } else {
                asyncResult = AsyncRpcResult.newDefaultAsyncResult(null, e, invocation);
            }
        } catch (RpcException e) {
            // if biz exception
            if (e.isBiz()) {
                asyncResult = AsyncRpcResult.newDefaultAsyncResult(null, e, invocation);
            } else {
                throw e;
            }
        } catch (Throwable e) {
            asyncResult = AsyncRpcResult.newDefaultAsyncResult(null, e, invocation);
        }

        if (setFutureWhenSync || invocation.getInvokeMode() != InvokeMode.SYNC) {
            // set server context
            RpcContext.getServiceContext().setFuture(new FutureAdapter<>(asyncResult.getResponseFuture()));
        }

        return asyncResult;
    }

    private void waitForResultIfSync(AsyncRpcResult asyncResult, RpcInvocation invocation) {
        if (InvokeMode.SYNC != invocation.getInvokeMode()) {
            return;
        }
        try {
            /*
             * NOTICE!
             * must call {@link java.util.concurrent.CompletableFuture#get(long, TimeUnit)} because
             * {@link java.util.concurrent.CompletableFuture#get()} was proved to have serious performance drop.
             */
            Object timeoutKey = invocation.getObjectAttachmentWithoutConvert(TIMEOUT_KEY);
            long timeout = RpcUtils.convertToNumber(timeoutKey, Integer.MAX_VALUE);

            asyncResult.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RpcException("Interrupted unexpectedly while waiting for remote result to return! method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
        } catch (ExecutionException e) {
            Throwable rootCause = e.getCause();
            if (rootCause instanceof TimeoutException) {
                throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "Invoke remote method timeout. method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
            } else if (rootCause instanceof RemotingException) {
                throw new RpcException(RpcException.NETWORK_EXCEPTION, "Failed to invoke remote method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
            } else {
                throw new RpcException(RpcException.UNKNOWN_EXCEPTION, "Fail to invoke remote method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
            }
        } catch (java.util.concurrent.TimeoutException e) {
            throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "Invoke remote method timeout. method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
        } catch (Throwable e) {
            throw new RpcException(e.getMessage(), e);
        }
    }

    // -- Protected api

    protected ExecutorService getCallbackExecutor(URL url, Invocation inv) {
        if (InvokeMode.SYNC == RpcUtils.getInvokeMode(getUrl(), inv)) {
            return new ThreadlessExecutor();
        }
        return ExecutorRepository.getInstance(url.getOrDefaultApplicationModel()).getExecutor(url);
    }

    /**
     * Specific implementation of the {@link #invoke(Invocation)} method
     */
    protected abstract Result doInvoke(Invocation invocation) throws Throwable;
}
