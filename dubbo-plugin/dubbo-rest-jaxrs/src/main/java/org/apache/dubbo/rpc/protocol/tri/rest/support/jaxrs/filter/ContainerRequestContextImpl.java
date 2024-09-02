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

import org.apache.dubbo.remoting.http12.HttpCookie;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs.Helper;
import org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs.JaxrsHttpRequestAdapter;
import org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs.JaxrsHttpResponseAdapter;
import org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs.MultivaluedMapWrapper;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jboss.resteasy.specimpl.RequestImpl;
import org.jboss.resteasy.spi.ResteasyUriInfo;

final class ContainerRequestContextImpl implements ContainerRequestContext {

    private final HttpRequest request;
    private final HttpResponse response;

    private Request req;
    private MultivaluedMap<String, String> headers;
    private UriInfo uriInfo;

    private boolean aborted;

    public ContainerRequestContextImpl(HttpRequest request, HttpResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public Object getProperty(String name) {
        return request.attribute(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return request.parameterNames();
    }

    @Override
    public void setProperty(String name, Object object) {
        request.setAttribute(name, object);
    }

    @Override
    public void removeProperty(String name) {
        request.removeAttribute(name);
    }

    @Override
    public UriInfo getUriInfo() {
        UriInfo uriInfo = this.uriInfo;
        if (uriInfo == null) {
            uriInfo = new ResteasyUriInfo(request.rawPath(), request.query(), "/");
            this.uriInfo = uriInfo;
        }
        return uriInfo;
    }

    @Override
    public void setRequestUri(URI requestUri) {
        String query = requestUri.getRawQuery();
        request.setUri(requestUri.getRawPath() + (query == null ? "" : '?' + query));
    }

    @Override
    public void setRequestUri(URI baseUri, URI requestUri) {
        String query = requestUri.getRawQuery();
        request.setUri(baseUri.getRawPath() + requestUri.getRawPath() + (query == null ? "" : '?' + query));
    }

    @Override
    public Request getRequest() {
        Request req = this.req;
        if (req == null) {
            req = new RequestImpl(new JaxrsHttpRequestAdapter(request), new JaxrsHttpResponseAdapter(response));
            this.req = req;
        }
        return req;
    }

    @Override
    public String getMethod() {
        return request.method();
    }

    @Override
    public void setMethod(String method) {
        request.setMethod(method);
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
    public String getHeaderString(String name) {
        return request.header(name);
    }

    @Override
    public Date getDate() {
        return null;
    }

    @Override
    public Locale getLanguage() {
        return request.locale();
    }

    @Override
    public int getLength() {
        return request.contentLength();
    }

    @Override
    public MediaType getMediaType() {
        return Helper.toMediaType(request.mediaType());
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        return Helper.toMediaTypes(request.accept());
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return request.locales();
    }

    @Override
    public Map<String, Cookie> getCookies() {
        Collection<HttpCookie> cookies = request.cookies();
        Map<String, Cookie> result = new HashMap<>(cookies.size());
        for (HttpCookie cookie : cookies) {
            result.put(cookie.name(), Helper.convert(cookie));
        }
        return result;
    }

    @Override
    @SuppressWarnings("resource")
    public boolean hasEntity() {
        return request.inputStream() != null;
    }

    @Override
    public InputStream getEntityStream() {
        return request.inputStream();
    }

    @Override
    public void setEntityStream(InputStream input) {
        request.setInputStream(input);
    }

    @Override
    public SecurityContext getSecurityContext() {
        return null;
    }

    @Override
    public void setSecurityContext(SecurityContext context) {}

    @Override
    public void abortWith(Response response) {
        this.response.setBody(Helper.toBody(response));
        aborted = true;
    }

    public boolean isAborted() {
        return aborted;
    }
}
