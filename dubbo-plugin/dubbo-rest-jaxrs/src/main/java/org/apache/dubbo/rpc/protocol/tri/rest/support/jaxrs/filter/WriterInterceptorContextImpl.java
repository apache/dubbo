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
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs.Helper;
import org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs.MultivaluedMapWrapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.WriterInterceptorContext;

import java.io.OutputStream;

final class WriterInterceptorContextImpl extends InterceptorContextImpl implements WriterInterceptorContext {

    private final HttpResponse response;
    private final Result result;

    private MultivaluedMap<String, Object> headers;

    public WriterInterceptorContextImpl(HttpRequest request, HttpResponse response, Result result) {
        super(request);
        this.response = response;
        this.result = result;
    }

    @Override
    public void proceed() throws WebApplicationException {}

    @Override
    public Object getEntity() {
        return result.getValue();
    }

    @Override
    public void setEntity(Object entity) {
        result.setValue(entity);
    }

    @Override
    public OutputStream getOutputStream() {
        return response.outputStream();
    }

    @Override
    public void setOutputStream(OutputStream os) {
        response.setOutputStream(os);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MultivaluedMap<String, Object> getHeaders() {
        MultivaluedMap<String, Object> headers = this.headers;
        if (headers == null) {
            headers = new MultivaluedMapWrapper(response.headers());
            this.headers = headers;
        }
        return headers;
    }

    @Override
    public MediaType getMediaType() {
        return Helper.toMediaType(response.mediaType());
    }

    @Override
    public void setMediaType(MediaType mediaType) {
        response.setContentType(Helper.toString(mediaType));
    }
}
