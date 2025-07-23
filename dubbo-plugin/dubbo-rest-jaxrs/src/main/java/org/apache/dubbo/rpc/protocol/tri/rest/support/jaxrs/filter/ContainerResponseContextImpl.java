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
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.HttpUtils;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.HandlerMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs.Helper;
import org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs.MultivaluedMapWrapper;

import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.netty.handler.codec.DateFormatter;

final class ContainerResponseContextImpl implements ContainerResponseContext {

    private final HttpRequest request;
    private final HttpResponse response;
    private final Result result;

    private MultivaluedMap<String, String> headers;

    public ContainerResponseContextImpl(HttpRequest request, HttpResponse response, Result result) {
        this.request = request;
        this.response = response;
        this.result = result;
    }

    @Override
    public int getStatus() {
        return response.status();
    }

    @Override
    public void setStatus(int code) {
        response.setStatus(code);
    }

    @Override
    public StatusType getStatusInfo() {
        return Status.fromStatusCode(response.status());
    }

    @Override
    public void setStatusInfo(StatusType statusInfo) {
        response.setStatus(statusInfo.getStatusCode());
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MultivaluedMap<String, Object> getHeaders() {
        return (MultivaluedMap) getStringHeaders();
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        MultivaluedMap<String, String> headers = this.headers;
        if (headers == null) {
            headers = new MultivaluedMapWrapper<>(response.headers());
            this.headers = headers;
        }
        return headers;
    }

    @Override
    public String getHeaderString(String name) {
        return response.header(name);
    }

    @Override
    public Set<String> getAllowedMethods() {
        return Collections.singleton(request.method());
    }

    @Override
    public Date getDate() {
        return null;
    }

    @Override
    public Locale getLanguage() {
        return HttpUtils.parseLocale(response.locale());
    }

    @Override
    public int getLength() {
        return -1;
    }

    @Override
    public MediaType getMediaType() {
        return Helper.toMediaType(response.mediaType());
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        Collection<HttpCookie> cookies = request.cookies();
        Map<String, NewCookie> result = new HashMap<>(cookies.size());
        for (HttpCookie cookie : cookies) {
            result.put(cookie.name(), Helper.convert(cookie));
        }
        return result;
    }

    @Override
    public EntityTag getEntityTag() {
        return null;
    }

    @Override
    public Date getLastModified() {
        String value = response.header(HttpHeaderNames.LAST_MODIFIED.getKey());
        return value == null ? null : DateFormatter.parseHttpDate(value);
    }

    @Override
    public URI getLocation() {
        String location = response.header(HttpHeaderNames.LOCATION.getKey());
        return location == null ? null : URI.create(location);
    }

    @Override
    public Set<Link> getLinks() {
        return null;
    }

    @Override
    public boolean hasLink(String relation) {
        return false;
    }

    @Override
    public Link getLink(String relation) {
        return null;
    }

    @Override
    public Link.Builder getLinkBuilder(String relation) {
        return null;
    }

    @Override
    public boolean hasEntity() {
        return getEntity() != null;
    }

    @Override
    public Object getEntity() {
        return result.getValue();
    }

    @Override
    public Class<?> getEntityClass() {
        return getHandler().getMethod().getReturnType();
    }

    @Override
    public Type getEntityType() {
        return getHandler().getMethod().getGenericReturnType();
    }

    @Override
    public void setEntity(Object entity) {
        result.setValue(entity);
    }

    @Override
    public void setEntity(Object entity, Annotation[] annotations, MediaType mediaType) {
        result.setValue(entity);
        response.setContentType(Helper.toString(mediaType));
    }

    @Override
    public Annotation[] getEntityAnnotations() {
        return new Annotation[0];
    }

    @Override
    public OutputStream getEntityStream() {
        return response.outputStream();
    }

    @Override
    public void setEntityStream(OutputStream outputStream) {
        response.setOutputStream(outputStream);
    }

    private HandlerMeta getHandler() {
        return request.attribute(RestConstants.HANDLER_ATTRIBUTE);
    }
}
