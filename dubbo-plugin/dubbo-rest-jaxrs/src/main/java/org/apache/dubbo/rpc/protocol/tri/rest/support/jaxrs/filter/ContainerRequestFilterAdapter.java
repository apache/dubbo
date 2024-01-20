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
package org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs.filter;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.AbstractRestFilter;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestExtensionAdapter;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestFilter;

import javax.ws.rs.container.ContainerRequestFilter;

@Activate(onClass = "javax.ws.rs.container.ContainerResponseFilter")
public class ContainerRequestFilterAdapter implements RestExtensionAdapter<ContainerRequestFilter> {

    @Override
    public boolean accept(Object extension) {
        return extension instanceof ContainerRequestFilter;
    }

    @Override
    public RestFilter adapt(ContainerRequestFilter extension) {
        return new Filter(extension);
    }

    private static final class Filter extends AbstractRestFilter<ContainerRequestFilter> {

        public Filter(ContainerRequestFilter extension) {
            super(extension);
        }

        @Override
        public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) throws Exception {
            ContainerRequestContextImpl context = new ContainerRequestContextImpl(request, response);
            extension.filter(context);
            if (context.isAborted()) {
                return;
            }
            chain.doFilter(request, response);
        }
    }
}
