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
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.ArrayUtils;
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

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static org.apache.dubbo.common.constants.CommonConstants.LOCALHOST_VALUE;
import static org.apache.dubbo.rpc.Constants.ASYNC_KEY;

/**
 * InjvmInvoker
 */
public class InjvmInvoker<T> extends AbstractInvoker<T> {

    private final String key;

    private final Map<String, Exporter<?>> exporterMap;

    private final ExecutorRepository executorRepository;

    private final ParamDeepCopyUtil paramDeepCopyUtil;

    InjvmInvoker(Class<T> type, URL url, String key, Map<String, Exporter<?>> exporterMap) {
        super(type, url);
        this.key = key;
        this.exporterMap = exporterMap;
        this.executorRepository = url.getOrDefaultApplicationModel().getExtensionLoader(ExecutorRepository.class).getDefaultExtension();
        this.paramDeepCopyUtil = url.getOrDefaultFrameworkModel().getExtensionLoader(ParamDeepCopyUtil.class)
            .getExtension(url.getParameter("injvm-copy-util", DefaultParamDeepCopyUtil.NAME));
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

        String desc = ReflectUtils.getDesc(invocation.getParameterTypes());

        // recreate invocation ---> deep copy parameters
        Invocation copiedInvocation = recreateInvocation(invocation, invoker, desc);

        if (isAsync(invoker.getUrl(), getUrl())) {
            ((RpcInvocation) copiedInvocation).setInvokeMode(InvokeMode.ASYNC);
            // use consumer executor
            ExecutorService executor = executorRepository.createExecutorIfAbsent(getUrl());
            CompletableFuture<AppResponse> appResponseFuture = CompletableFuture.supplyAsync(() -> {
                Result result = invoker.invoke(copiedInvocation);
                if (result.hasException()) {
                    return new AppResponse(result.getException());
                } else {
                    rebuildValue(invocation, desc, result);
                    return new AppResponse(result.getValue());
                }
            }, executor);
            // save for 2.6.x compatibility, for example, TraceFilter in Zipkin uses com.alibaba.xxx.FutureAdapter
            FutureContext.getContext().setCompatibleFuture(appResponseFuture);
            AsyncRpcResult result = new AsyncRpcResult(appResponseFuture, copiedInvocation);
            result.setExecutor(executor);
            return result;
        } else {
            Result result = invoker.invoke(copiedInvocation);
            if (result.hasException()) {
                return result;
            } else {
                rebuildValue(invocation, desc, result);
                return result;
            }
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
        if(CommonConstants.$INVOKE.equals(methodName)) {
            // generic invoke, skip copy arguments
            RpcInvocation copiedInvocation = new RpcInvocation(invocation.getTargetServiceUniqueName(),
                providerServiceModel, methodName, invocation.getServiceName(), invocation.getProtocolServiceKey(),
                invocation.getParameterTypes(), invocation.getArguments(), new HashMap<>(invocation.getObjectAttachments()),
                invocation.getInvoker(), invocation.getAttributes());
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
                        realArgument[i] = paramDeepCopyUtil.copy(getUrl(), args[i], pts[i]);
                    }
                }
                if (realArgument == null) {
                    realArgument = args;
                }

                RpcInvocation copiedInvocation = new RpcInvocation(invocation.getTargetServiceUniqueName(),
                    providerServiceModel, methodName, invocation.getServiceName(), invocation.getProtocolServiceKey(),
                    pts, realArgument, new HashMap<>(invocation.getObjectAttachments()),
                    invocation.getInvoker(), invocation.getAttributes());
                copiedInvocation.setInvoker(invoker);
                return copiedInvocation;
            } finally {
                Thread.currentThread().setContextClassLoader(originClassLoader);
            }
        } else {
            return invocation;
        }
    }

    private void rebuildValue(Invocation invocation, String desc, Result result) {
        Object originValue = result.getValue();
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
            result.setValue(value);
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
