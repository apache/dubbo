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

package org.apache.dubbo.rpc.stub;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.profiler.Profiler;
import org.apache.dubbo.common.profiler.ProfilerEntry;
import org.apache.dubbo.common.profiler.ProfilerSwitch;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class StubInvoker<T> implements Invoker<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StubInvoker.class);

    public final Class<T> type;
    public final URL url;
    private final Map<String, StubMethodHandler<?, ?>> handlers;

    public StubInvoker(URL url, Class<T> type, Map<String, StubMethodHandler<?, ?>> handlers) {
        this.url = url;
        this.type = type;
        this.handlers = handlers;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void destroy() {
    }

    @Override
    public Class<T> getInterface() {
        return type;
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        ProfilerEntry originEntry = null;
        if (ProfilerSwitch.isEnableSimpleProfiler()) {
            Object fromInvocation = invocation.get(Profiler.PROFILER_KEY);
            if (fromInvocation instanceof ProfilerEntry) {
                ProfilerEntry profiler = Profiler.enter((ProfilerEntry) fromInvocation,
                    "Receive request. Server biz impl invoke begin.");
                invocation.put(Profiler.PROFILER_KEY, profiler);
                originEntry = Profiler.setToBizProfiler(profiler);
            }
        }

        CompletableFuture<?> future = handlers.get(invocation.getMethodName()).invoke(invocation.getArguments());

        if (ProfilerSwitch.isEnableSimpleProfiler()) {
            Object fromInvocation = invocation.get(Profiler.PROFILER_KEY);
            if (fromInvocation instanceof ProfilerEntry) {
                ProfilerEntry profiler = Profiler.release((ProfilerEntry) fromInvocation);
                invocation.put(Profiler.PROFILER_KEY, profiler);
            }
        }
        Profiler.removeBizProfiler();
        if (originEntry != null) {
            Profiler.setToBizProfiler(originEntry);
        }
        CompletableFuture<AppResponse> appResponseFuture = future.handle((obj, t) -> {
            AppResponse result = new AppResponse(invocation);
            if (t != null) {
                if (t instanceof CompletionException) {
                    result.setException(t.getCause());
                } else {
                    result.setException(t);
                }
            } else {
                result.setValue(obj);
            }
            return result;
        });
        return new AsyncRpcResult(appResponseFuture, invocation);
    }
}
