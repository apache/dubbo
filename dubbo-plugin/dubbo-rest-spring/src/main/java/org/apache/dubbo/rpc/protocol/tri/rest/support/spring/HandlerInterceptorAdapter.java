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
package org.apache.dubbo.rpc.protocol.tri.rest.support.spring;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestExtensionAdapter;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestFilter;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestFilter.Listener;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Arrays;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Activate(onClass = "org.springframework.web.servlet.HandlerInterceptor")
public final class HandlerInterceptorAdapter implements RestExtensionAdapter<HandlerInterceptor> {
    @Override
    public boolean accept(Object extension) {
        return extension instanceof HandlerInterceptor;
    }

    @Override
    public RestFilter adapt(HandlerInterceptor extension) {
        return new HandlerInterceptorRestFilter(extension);
    }

    private static final class HandlerInterceptorRestFilter implements RestFilter, Listener {

        private final HandlerInterceptor interceptor;

        @Override
        public int getPriority() {
            return RestUtils.getPriority(interceptor);
        }

        @Override
        public String[] getPatterns() {
            return RestUtils.getPattens(interceptor);
        }

        public HandlerInterceptorRestFilter(HandlerInterceptor interceptor) {
            this.interceptor = interceptor;
        }

        @Override
        public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) throws Exception {
            Object handler = request.attribute(RestConstants.HANDLER_ATTRIBUTE);
            if (interceptor.preHandle((HttpServletRequest) request, (HttpServletResponse) response, handler)) {
                chain.doFilter(request, response);
            }
        }

        @Override
        public void onResponse(Result result, HttpRequest request, HttpResponse response) throws Exception {
            if (result.hasException()) {
                onError(result.getException(), request, response);
                return;
            }
            Object handler = request.attribute(RestConstants.HANDLER_ATTRIBUTE);
            ModelAndView mv = new ModelAndView();
            mv.addObject("result", result);
            interceptor.postHandle((HttpServletRequest) request, (HttpServletResponse) response, handler, mv);
        }

        @Override
        public void onError(Throwable t, HttpRequest request, HttpResponse response) throws Exception {
            Object handler = request.attribute(RestConstants.HANDLER_ATTRIBUTE);
            Exception ex = t instanceof Exception ? (Exception) t : new RpcException(t);
            interceptor.afterCompletion((HttpServletRequest) request, (HttpServletResponse) response, handler, ex);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("RestFilter{interceptor=");
            sb.append(interceptor);
            int priority = getPriority();
            if (priority != 0) {
                sb.append(", priority=").append(priority);
            }
            String[] patterns = getPatterns();
            if (patterns != null) {
                sb.append(", patterns=").append(Arrays.toString(patterns));
            }
            return sb.append('}').toString();
        }
    }
}
