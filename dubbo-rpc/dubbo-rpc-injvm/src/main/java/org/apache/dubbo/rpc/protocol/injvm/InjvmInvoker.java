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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Constants;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.FutureContext;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.InvokeMode;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;
import org.apache.dubbo.rpc.protocol.DelegateExporterMap;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static org.apache.dubbo.common.constants.CommonConstants.LOCALHOST_VALUE;
import static org.apache.dubbo.rpc.Constants.ASYNC_KEY;

/**
 * InjvmInvoker
 */
class InjvmInvoker<T> extends AbstractInvoker<T> {

    private final String key;

    private final DelegateExporterMap delegateExporterMap;

    private final ExecutorRepository executorRepository = ExtensionLoader.getExtensionLoader(ExecutorRepository.class).getDefaultExtension();

    InjvmInvoker(Class<T> type, URL url, String key, DelegateExporterMap delegateExporterMap) {
        super(type, url);
        this.key = key;
        this.delegateExporterMap = delegateExporterMap;
    }

    @Override
    public boolean isAvailable() {
        InjvmExporter<?> exporter = (InjvmExporter<?>) delegateExporterMap.getExport(key);
        if (exporter == null) {
            return false;
        } else {
            return super.isAvailable();
        }
    }

    @Override
    public Result doInvoke(Invocation invocation) throws Throwable {
        Exporter<?> exporter = InjvmProtocol.getExporter(delegateExporterMap, getUrl());
        if (exporter == null) {
            throw new RpcException("Service [" + key + "] not found.");
        }
        RpcContext.getContext().setRemoteAddress(LOCALHOST_VALUE, 0);
        // Solve local exposure, the server opens the token, and the client call fails.
        URL serverURL = exporter.getInvoker().getUrl();
        boolean serverHasToken = serverURL.hasParameter(Constants.TOKEN_KEY);
        if (serverHasToken) {
            invocation.setAttachment(Constants.TOKEN_KEY, serverURL.getParameter(Constants.TOKEN_KEY));
        }

        if (isAsync(exporter.getInvoker().getUrl(), getUrl())) {
            ((RpcInvocation) invocation).setInvokeMode(InvokeMode.ASYNC);
            // use consumer executor
            ExecutorService executor = executorRepository.createExecutorIfAbsent(getUrl());
            CompletableFuture<AppResponse> appResponseFuture = CompletableFuture.supplyAsync(() -> {
                Result result = exporter.getInvoker().invoke(invocation);
                if (result.hasException()) {
                    return new AppResponse(result.getException());
                } else {
                    return new AppResponse(result.getValue());
                }
            }, executor);
            // save for 2.6.x compatibility, for example, TraceFilter in Zipkin uses com.alibaba.xxx.FutureAdapter
            FutureContext.getContext().setCompatibleFuture(appResponseFuture);
            AsyncRpcResult result = new AsyncRpcResult(appResponseFuture, invocation);
            result.setExecutor(executor);
            return result;
        } else {
            return exporter.getInvoker().invoke(invocation);
        }
    }

    private boolean isAsync(URL remoteUrl, URL localUrl) {
        if (localUrl.hasParameter(ASYNC_KEY)) {
            return localUrl.getParameter(ASYNC_KEY, false);
        }
        return remoteUrl.getParameter(ASYNC_KEY, false);
    }
}
