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

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestFilter.FilterChain;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestFilter.Listener;

import java.util.function.Supplier;

final class DefaultFilterChain implements FilterChain, Listener {

    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(DefaultFilterChain.class);

    private final RestFilter[] filters;
    private final Supplier<Result> action;

    private int cursor;
    private Result result;

    DefaultFilterChain(RestFilter[] filters, Supplier<Result> action) {
        this.filters = filters;
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
        result = action.get();
    }

    @Override
    public void onSuccess(Result result, HttpRequest request, HttpResponse response) {
        for (int i = cursor - 1; i > -1; i--) {
            RestFilter filter = filters[i];
            if (filter instanceof Listener) {
                try {
                    ((Listener) filter).onSuccess(result, request, response);
                } catch (Throwable t) {
                    LOGGER.error(
                            LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION,
                            "",
                            "",
                            "Call onSuccess for filter " + "[" + filter + "] error");
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
                    LOGGER.error(
                            LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION,
                            "",
                            "",
                            "Call onError for filter " + "[" + filter + "] error");
                }
            }
        }
    }
}
