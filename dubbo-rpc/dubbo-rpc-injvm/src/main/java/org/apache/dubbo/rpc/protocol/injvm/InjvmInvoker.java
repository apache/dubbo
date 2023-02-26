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
package org.apache.dubbo.rpc.protocol.injvm;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.threadlocal.InternalThreadLocalMap;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Constants;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.FutureContext;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.InvokeMode;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceModel;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.LOCALHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.config.Constants.SERVER_THREAD_POOL_NAME;
import static org.apache.dubbo.rpc.Constants.ASYNC_KEY;

/**
 * InjvmInvoker
 */
public class InjvmInvoker<T> extends AbstractInvoker<T> {

    private final String key;

    private final Map<String, Exporter<?>> exporterMap;

    private final ExecutorRepository executorRepository;

    private final ParamDeepCopyUtil paramDeepCopyUtil;

    private final boolean shouldIgnoreSameModule;

    InjvmInvoker(Class<T> type, URL url, String key, Map<String, Exporter<?>> exporterMap) {
        super(type, url);
        this.key = key;
        this.exporterMap = exporterMap;
        this.executorRepository = ExecutorRepository.getInstance(url.getOrDefaultApplicationModel());
        this.paramDeepCopyUtil = url.getOrDefaultFrameworkModel().getExtensionLoader(ParamDeepCopyUtil.class)
            .getExtension(url.getParameter(CommonConstants.INJVM_COPY_UTIL_KEY, DefaultParamDeepCopyUtil.NAME));
        this.shouldIgnoreSameModule = url.getParameter(CommonConstants.INJVM_IGNORE_SAME_MODULE_KEY, false);
    }

    @Override
    public boolean isAvailable() {
        InjvmExporter<?> exporter = (InjvmExporter<?>) exporterMap.get(key);
        if (exporter == null) {
            return false;
        } else {
            return super.isAvailable();
        }
    }

    @Override
    public Result doInvoke(Invocation invocation) throws Throwable {
        Exporter<?> exporter = InjvmProtocol.getExporter(exporterMap, getUrl());
        if (exporter == null) {
            throw new RpcException("Service [" + key + "] not found.");
        }
        RpcContext.getServiceContext().setRemoteAddress(LOCALHOST_VALUE, 0);
        // Solve local exposure, the server opens the token, and the client call fails.
        Invoker<?> invoker = exporter.getInvoker();
        URL serverURL = invoker.getUrl();
        boolean serverHasToken = serverURL.hasParameter(Constants.TOKEN_KEY);
        if (serverHasToken) {
            invocation.setAttachment(Constants.TOKEN_KEY, serverURL.getParameter(Constants.TOKEN_KEY));
        }

        int timeout = RpcUtils.calculateTimeout(getUrl(), invocation, invocation.getMethodName(), DEFAULT_TIMEOUT);
        if (timeout <= 0) {
            return AsyncRpcResult.newDefaultAsyncResult(new RpcException(RpcException.TIMEOUT_TERMINATE,
                "No time left for making the following call: " + invocation.getServiceName() + "."
                    + invocation.getMethodName() + ", terminate directly."), invocation);
        }
        invocation.setAttachment(TIMEOUT_KEY, String.valueOf(timeout));


        String desc = ReflectUtils.getDesc(invocation.getParameterTypes());

        // recreate invocation ---> deep copy parameters
        Invocation copiedInvocation = recreateInvocation(invocation, invoker, desc);

        if (isAsync(invoker.getUrl(), getUrl())) {
            ((RpcInvocation) copiedInvocation).setInvokeMode(InvokeMode.ASYNC);
            // use consumer executor
            ExecutorService executor = executorRepository.createExecutorIfAbsent(ExecutorUtil.setThreadName(getUrl(), SERVER_THREAD_POOL_NAME));
            CompletableFuture<AppResponse> appResponseFuture = CompletableFuture.supplyAsync(() -> {
                Result result = invoker.invoke(copiedInvocation);
                if (result.hasException()) {
                    AppResponse appResponse = new AppResponse(result.getException());
                    appResponse.setObjectAttachments(new HashMap<>(result.getObjectAttachments()));
                    return appResponse;
                } else {
                    rebuildValue(invocation, desc, result);
                    AppResponse appResponse = new AppResponse(result.getValue());
                    appResponse.setObjectAttachments(new HashMap<>(result.getObjectAttachments()));
                    return appResponse;
                }
            }, executor);
            // save for 2.6.x compatibility, for example, TraceFilter in Zipkin uses com.alibaba.xxx.FutureAdapter
            FutureContext.getContext().setCompatibleFuture(appResponseFuture);
            AsyncRpcResult result = new AsyncRpcResult(appResponseFuture, copiedInvocation);
            result.setExecutor(executor);
            return result;
        } else {
            Result result;
            // clear thread local before child invocation, prevent context pollution
            InternalThreadLocalMap originTL = InternalThreadLocalMap.getAndRemove();
            try {
                result = invoker.invoke(copiedInvocation);
            } finally {
                InternalThreadLocalMap.set(originTL);
            }
            CompletableFuture<AppResponse> future = new CompletableFuture<>();
            AppResponse rpcResult = new AppResponse(copiedInvocation);
            if (result instanceof AsyncRpcResult) {
                result.whenCompleteWithContext((r, t) -> {
                    if (t != null) {
                        rpcResult.setException(t);
                    } else {
                        if (r.hasException()) {
                            rpcResult.setException(r.getException());
                        } else {
                            Object rebuildValue = rebuildValue(invocation, desc, r.getValue());
                            rpcResult.setValue(rebuildValue);
                        }
                    }
                    rpcResult.setObjectAttachments(new HashMap<>(r.getObjectAttachments()));
                    future.complete(rpcResult);
                });
            } else {
                if (result.hasException()) {
                    rpcResult.setException(result.getException());
                } else {
                    Object rebuildValue = rebuildValue(invocation, desc, result.getValue());
                    rpcResult.setValue(rebuildValue);
                }
                rpcResult.setObjectAttachments(new HashMap<>(result.getObjectAttachments()));
                future.complete(rpcResult);
            }
            return new AsyncRpcResult(future, invocation);

        }
    }

    private Class<?> getReturnType(ServiceModel consumerServiceModel, String methodName, String desc) {
        MethodDescriptor consumerMethod = consumerServiceModel.getServiceModel().getMethod(methodName, desc);
        if (consumerMethod != null) {
            Type[] returnTypes = consumerMethod.getReturnTypes();
            if (ArrayUtils.isNotEmpty(returnTypes)) {
                return (Class<?>) returnTypes[0];
            }
        }
        return null;
    }

    private Invocation recreateInvocation(Invocation invocation, Invoker<?> invoker, String desc) {
        ClassLoader originClassLoader = Thread.currentThread().getContextClassLoader();

        ServiceModel providerServiceModel = invoker.getUrl().getServiceModel();

        if (providerServiceModel == null) {
            return invocation;
        }
        String methodName = invocation.getMethodName();

        ServiceModel consumerServiceModel = invocation.getServiceModel();
        boolean shouldSkip = shouldIgnoreSameModule && consumerServiceModel != null &&
            Objects.equals(providerServiceModel.getModuleModel(), consumerServiceModel.getModuleModel());
        if (CommonConstants.$INVOKE.equals(methodName) || shouldSkip) {
            // generic invoke, skip copy arguments
            RpcInvocation copiedInvocation = new RpcInvocation(invocation.getTargetServiceUniqueName(),
                providerServiceModel, methodName, invocation.getServiceName(), invocation.getProtocolServiceKey(),
                invocation.getParameterTypes(), invocation.getArguments(), invocation.copyObjectAttachments(),
                invocation.getInvoker(), new HashMap<>(),
                invocation instanceof RpcInvocation ? ((RpcInvocation) invocation).getInvokeMode() : null);
            copiedInvocation.setInvoker(invoker);
            return copiedInvocation;
        }

        MethodDescriptor providerMethod = providerServiceModel.getServiceModel().getMethod(methodName, desc);
        Object[] realArgument = null;
        if (providerMethod != null) {
            Class<?>[] pts = providerMethod.getParameterClasses();
            Object[] args = invocation.getArguments();

            // switch ClassLoader
            Thread.currentThread().setContextClassLoader(providerServiceModel.getClassLoader());

            try {
                // copy parameters
                if (pts != null && args != null && pts.length == args.length) {
                    realArgument = new Object[pts.length];
                    for (int i = 0; i < pts.length; i++) {
                        realArgument[i] = paramDeepCopyUtil.copy(invoker.getUrl(), args[i], pts[i]);
                    }
                }
                if (realArgument == null) {
                    realArgument = args;
                }

                RpcInvocation copiedInvocation = new RpcInvocation(invocation.getTargetServiceUniqueName(),
                    providerServiceModel, methodName, invocation.getServiceName(), invocation.getProtocolServiceKey(),
                    pts, realArgument, invocation.copyObjectAttachments(),
                    invocation.getInvoker(), new HashMap<>(),
                    invocation instanceof RpcInvocation ? ((RpcInvocation) invocation).getInvokeMode() : null);
                copiedInvocation.setInvoker(invoker);
                return copiedInvocation;
            } finally {
                Thread.currentThread().setContextClassLoader(originClassLoader);
            }
        } else {
            return invocation;
        }
    }

    private Object rebuildValue(Invocation invocation, String desc, Object originValue) {
        Object value = originValue;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            ServiceModel consumerServiceModel = getUrl().getServiceModel();
            if (consumerServiceModel != null) {
                Class<?> returnType = getReturnType(consumerServiceModel, invocation.getMethodName(), desc);
                if (returnType != null) {
                    Thread.currentThread().setContextClassLoader(consumerServiceModel.getClassLoader());
                    value = paramDeepCopyUtil.copy(getUrl(), originValue, returnType);
                }
            }
            return value;
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    private boolean isAsync(URL remoteUrl, URL localUrl) {
        if (localUrl.hasParameter(ASYNC_KEY)) {
            return localUrl.getParameter(ASYNC_KEY, false);
        }
        return remoteUrl.getParameter(ASYNC_KEY, false);
    }
}
