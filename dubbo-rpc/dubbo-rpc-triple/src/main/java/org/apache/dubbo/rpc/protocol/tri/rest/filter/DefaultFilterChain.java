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
package org.apache.dubbo.rpc.protocol.tri.rest.filter;

import org.apache.dubbo.common.logger.FluentLogger;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestFilter.FilterChain;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestFilter.Listener;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

final class DefaultFilterChain implements FilterChain, Listener {

    private static final FluentLogger LOGGER = FluentLogger.of(DefaultFilterChain.class);

    private final RestFilter[] filters;
    private final Invocation invocation;
    private final Supplier<Result> action;

    private int cursor;
    private Result result;
    private CompletableFuture<AppResponse> resultFuture;

    DefaultFilterChain(RestFilter[] filters, Invocation invocation, Supplier<Result> action) {
        this.filters = filters;
        this.invocation = invocation;
        this.action = action;
    }

    public Result execute(HttpRequest request, HttpResponse response) throws Exception {
        doFilter(request, response);
        return result;
    }

    @Override
    public void doFilter(HttpRequest request, HttpResponse response) throws Exception {
        if (cursor < filters.length) {
            filters[cursor++].doFilter(request, response, this);
            return;
        }
        if (resultFuture == null) {
            result = action.get();
        } else {
            action.get().whenCompleteWithContext((r, e) -> {
                if (e == null) {
                    resultFuture.complete(new AppResponse(r));
                } else {
                    resultFuture.complete(new AppResponse(e));
                }
            });
        }
    }

    @Override
    public CompletableFuture<Boolean> doFilterAsync(HttpRequest request, HttpResponse response) {
        if (resultFuture == null) {
            resultFuture = new CompletableFuture<>();
            result = new AsyncRpcResult(resultFuture, invocation);
        }
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        future.whenComplete((v, t) -> {
            if (t == null) {
                if (v != null && v) {
                    try {
                        doFilter(request, response);
                    } catch (Exception e) {
                        resultFuture.complete(new AppResponse(e));
                    }
                } else {
                    resultFuture.complete(new AppResponse());
                }
            } else {
                resultFuture.complete(new AppResponse(t));
            }
        });
        return future;
    }

    @Override
    public void onResponse(Result result, HttpRequest request, HttpResponse response) {
        for (int i = cursor - 1; i > -1; i--) {
            RestFilter filter = filters[i];
            if (filter instanceof Listener) {
                try {
                    ((Listener) filter).onResponse(result, request, response);
                } catch (Throwable t) {
                    LOGGER.internalError("Call onResponse for filter [{}] error", filter);
                }
            }
        }
    }

    @Override
    public void onError(Throwable t, HttpRequest request, HttpResponse response) {
        for (int i = cursor - 1; i > -1; i--) {
            RestFilter filter = filters[i];
            if (filter instanceof Listener) {
                try {
                    ((Listener) filter).onError(t, request, response);
                } catch (Throwable th) {
                    LOGGER.internalError("Call onError for filter [{}] error", filter);
                }
            }
        }
    }
}
