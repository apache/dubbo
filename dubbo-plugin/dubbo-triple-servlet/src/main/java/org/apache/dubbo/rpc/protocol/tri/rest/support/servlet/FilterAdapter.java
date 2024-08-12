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
package org.apache.dubbo.rpc.protocol.tri.rest.support.servlet;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.message.HttpMessageAdapterFactory;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.RestException;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestExtensionAdapter;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestFilter;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestUtils;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import java.io.IOException;
import java.util.Arrays;

@Activate(onClass = "javax.servlet.Filter")
public final class FilterAdapter implements RestExtensionAdapter<Filter> {

    private final ServletHttpMessageAdapterFactory adapterFactory;

    public FilterAdapter(FrameworkModel frameworkModel) {
        adapterFactory = (ServletHttpMessageAdapterFactory)
                frameworkModel.getExtension(HttpMessageAdapterFactory.class, "servlet");
    }

    @Override
    public boolean accept(Object extension) {
        return extension instanceof Filter;
    }

    @Override
    public RestFilter adapt(Filter extension) {
        try {
            String filterName = extension.getClass().getSimpleName();
            extension.init((FilterConfig) adapterFactory.adaptFilterConfig(filterName));
        } catch (ServletException e) {
            throw new RestException(e);
        }
        return new FilterRestFilter(extension);
    }

    private static final class FilterRestFilter implements RestFilter {

        private final Filter filter;

        @Override
        public int getPriority() {
            return RestUtils.getPriority(filter);
        }

        @Override
        public String[] getPatterns() {
            return RestUtils.getPattens(filter);
        }

        public FilterRestFilter(Filter filter) {
            this.filter = filter;
        }

        @Override
        public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) throws Exception {
            filter.doFilter((ServletRequest) request, (ServletResponse) response, (q, p) -> {
                try {
                    chain.doFilter(request, response);
                } catch (RuntimeException | IOException | ServletException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ServletException(e);
                }
            });
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("RestFilter{filter=");
            sb.append(filter);
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
