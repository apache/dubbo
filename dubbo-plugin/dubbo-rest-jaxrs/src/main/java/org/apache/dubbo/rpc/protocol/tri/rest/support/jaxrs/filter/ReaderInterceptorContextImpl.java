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

import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestFilter.FilterChain;
import org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs.Helper;
import org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs.MultivaluedMapWrapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ReaderInterceptorContext;

import java.io.IOException;
import java.io.InputStream;

final class ReaderInterceptorContextImpl extends InterceptorContextImpl implements ReaderInterceptorContext {

    private final HttpResponse response;
    private final FilterChain chain;

    private MultivaluedMap<String, String> headers;

    public ReaderInterceptorContextImpl(HttpRequest request, HttpResponse response, FilterChain chain) {
        super(request);
        this.response = response;
        this.chain = chain;
        headers = new MultivaluedMapWrapper<>(request.headers());
    }

    @Override
    public Object proceed() throws IOException, WebApplicationException {
        try {
            chain.doFilter(request, response);
        } catch (RuntimeException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
        return null;
    }

    @Override
    public InputStream getInputStream() {
        return request.inputStream();
    }

    @Override
    public void setInputStream(InputStream is) {
        request.setInputStream(is);
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        MultivaluedMap<String, String> headers = this.headers;
        if (headers == null) {
            headers = new MultivaluedMapWrapper<>(request.headers());
            this.headers = headers;
        }
        return headers;
    }

    @Override
    public MediaType getMediaType() {
        return Helper.toMediaType(request.mediaType());
    }

    @Override
    public void setMediaType(MediaType mediaType) {
        request.setContentType(Helper.toString(mediaType));
    }
}
