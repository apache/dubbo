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
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.AbstractRestFilter;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestExtensionAdapter;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestFilter;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestFilter.Listener;
import org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs.Helper;
import org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import java.util.Objects;

@Activate(onClass = "javax.ws.rs.ext.ExceptionMapper")
public final class ExceptionMapperAdapter implements RestExtensionAdapter<ExceptionMapper<Throwable>> {

    @Override
    public boolean accept(Object extension) {
        return extension instanceof ExceptionMapper;
    }

    @Override
    public RestFilter adapt(ExceptionMapper<Throwable> extension) {
        return new Filter(extension);
    }

    private static final class Filter extends AbstractRestFilter<ExceptionMapper<Throwable>> implements Listener {

        private final Class<?> exceptionType;

        public Filter(ExceptionMapper<Throwable> extension) {
            super(extension);
            exceptionType = Objects.requireNonNull(TypeUtils.getSuperGenericType(extension.getClass()));
        }

        @Override
        public void onResponse(Result result, HttpRequest request, HttpResponse response) throws Exception {
            if (result.hasException()) {
                Throwable t = result.getException();
                if (exceptionType.isInstance(t)) {
                    try (Response r = extension.toResponse(t)) {
                        response.setBody(Helper.toBody(r));
                    }
                }
            }
        }

        @Override
        public void onError(Throwable t, HttpRequest request, HttpResponse response) {
            if (exceptionType.isInstance(t)) {
                try (Response r = extension.toResponse(t)) {
                    response.setBody(Helper.toBody(r));
                }
            }
        }
    }
}
