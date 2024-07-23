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

import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RequestUtils;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.spi.ResteasyAsynchronousContext;
import org.jboss.resteasy.spi.ResteasyUriInfo;

public final class JaxrsHttpRequestAdapter implements org.jboss.resteasy.spi.HttpRequest {

    private final HttpRequest request;

    private HttpHeaders headers;
    private ResteasyUriInfo uriInfo;

    public JaxrsHttpRequestAdapter(HttpRequest request) {
        this.request = request;
    }

    @Override
    public HttpHeaders getHttpHeaders() {
        HttpHeaders headers = this.headers;
        if (headers == null) {
            headers = new ResteasyHttpHeaders(new MultivaluedMapWrapper<>(request.headers()));
            this.headers = headers;
        }
        return headers;
    }

    @Override
    public MultivaluedMap<String, String> getMutableHeaders() {
        return headers.getRequestHeaders();
    }

    @Override
    public InputStream getInputStream() {
        return request.inputStream();
    }

    @Override
    public void setInputStream(InputStream stream) {
        request.setInputStream(stream);
    }

    @Override
    public ResteasyUriInfo getUri() {
        ResteasyUriInfo uriInfo = this.uriInfo;
        if (uriInfo == null) {
            uriInfo = new ResteasyUriInfo(request.rawPath(), request.query(), "/");
            this.uriInfo = uriInfo;
        }
        return uriInfo;
    }

    @Override
    public String getHttpMethod() {
        return request.method();
    }

    @Override
    public void setHttpMethod(String method) {
        request.setMethod(method);
    }

    @Override
    public void setRequestUri(URI requestUri) throws IllegalStateException {
        String query = requestUri.getRawQuery();
        request.setUri(requestUri.getRawPath() + (query == null ? "" : '?' + query));
    }

    @Override
    public void setRequestUri(URI baseUri, URI requestUri) throws IllegalStateException {
        String query = requestUri.getRawQuery();
        request.setUri(baseUri.getRawPath() + requestUri.getRawPath() + (query == null ? "" : '?' + query));
    }

    @Override
    public MultivaluedMap<String, String> getFormParameters() {
        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        for (String name : request.formParameterNames()) {
            List<String> values = request.formParameterValues(name);
            if (values == null) {
                continue;
            }
            for (String value : values) {
                result.add(name, RequestUtils.encodeURL(value));
            }
        }
        return result;
    }

    @Override
    public MultivaluedMap<String, String> getDecodedFormParameters() {
        return new MultivaluedMapWrapper<>(RequestUtils.getFormParametersMap(request));
    }

    @Override
    public Object getAttribute(String attribute) {
        return request.attribute(attribute);
    }

    @Override
    public void setAttribute(String name, Object value) {
        request.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        request.removeAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(request.attributeNames());
    }

    @Override
    public ResteasyAsynchronousContext getAsyncContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInitial() {
        return false;
    }

    @Override
    public void forward(String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean wasForwarded() {
        return false;
    }
}
