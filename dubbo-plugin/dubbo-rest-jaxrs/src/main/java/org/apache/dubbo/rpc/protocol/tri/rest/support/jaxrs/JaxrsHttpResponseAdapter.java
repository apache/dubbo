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
package org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs;

import org.apache.dubbo.remoting.http12.HttpCookie;
import org.apache.dubbo.remoting.http12.HttpResponse;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;

import java.io.OutputStream;

public final class JaxrsHttpResponseAdapter implements org.jboss.resteasy.spi.HttpResponse {

    private final HttpResponse response;

    private MultivaluedMap<String, Object> headers;

    public JaxrsHttpResponseAdapter(HttpResponse response) {
        this.response = response;
    }

    @Override
    public int getStatus() {
        return response.status();
    }

    @Override
    public void setStatus(int status) {
        response.setStatus(status);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MultivaluedMap<String, Object> getOutputHeaders() {
        MultivaluedMap<String, Object> headers = this.headers;
        if (headers == null) {
            headers = new MultivaluedMapWrapper(response.headers());
            this.headers = headers;
        }
        return headers;
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
    public void addNewCookie(NewCookie cookie) {
        HttpCookie hCookie = new HttpCookie(cookie.getName(), cookie.getValue());
        hCookie.setDomain(cookie.getDomain());
        hCookie.setPath(cookie.getPath());
        hCookie.setMaxAge(cookie.getMaxAge());
        hCookie.setSecure(cookie.isSecure());
        hCookie.setHttpOnly(cookie.isHttpOnly());
        response.addCookie(hCookie);
    }

    @Override
    public void sendError(int status) {
        response.sendError(status);
    }

    @Override
    public void sendError(int status, String message) {
        response.sendError(status, message);
    }

    @Override
    public boolean isCommitted() {
        return response.isCommitted();
    }

    @Override
    public void reset() {
        response.reset();
    }

    @Override
    public void flushBuffer() {}
}
