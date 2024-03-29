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
package org.apache.dubbo.rpc.protocol.rest.extension.resteasy.filter;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.jboss.resteasy.core.interception.jaxrs.SuspendableContainerRequestContext;
import org.jboss.resteasy.plugins.server.netty.NettyHttpRequest;
import org.jboss.resteasy.specimpl.BuiltResponse;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.spi.ApplicationException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

public class DubboPreMatchContainerRequestContext implements SuspendableContainerRequestContext {
    protected final NettyHttpRequest httpRequest;
    protected Response response;
    private ContainerRequestFilter[] requestFilters;
    private int currentFilter;
    private boolean suspended;
    private boolean filterReturnIsMeaningful = true;
    private Supplier<BuiltResponse> continuation;
    private Map<Class<?>, Object> contextDataMap;
    private boolean inFilter;
    private Throwable throwable;
    private boolean startedContinuation;

    public DubboPreMatchContainerRequestContext(
            final NettyHttpRequest request,
            final ContainerRequestFilter[] requestFilters,
            final Supplier<BuiltResponse> continuation) {
        this.httpRequest = request;
        this.requestFilters = requestFilters;
        this.continuation = continuation;
        contextDataMap = ResteasyProviderFactory.getContextDataMap();
    }

    public NettyHttpRequest getHttpRequest() {
        return httpRequest;
    }

    public Response getResponseAbortedWith() {
        return response;
    }

    @Override
    public Object getProperty(String name) {
        return httpRequest.getAttribute(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        ArrayList<String> names = new ArrayList<>();
        Enumeration<String> enames = httpRequest.getAttributeNames();
        while (enames.hasMoreElements()) {
            names.add(enames.nextElement());
        }
        return names;
    }

    @Override
    public void setProperty(String name, Object object) {
        httpRequest.setAttribute(name, object);
    }

    @Override
    public void removeProperty(String name) {
        httpRequest.removeAttribute(name);
    }

    @Override
    public UriInfo getUriInfo() {
        return httpRequest.getUri();
    }

    @Override
    public void setRequestUri(URI requestUri) throws IllegalStateException {
        httpRequest.setRequestUri(requestUri);
    }

    @Override
    public void setRequestUri(URI baseUri, URI requestUri) throws IllegalStateException {
        httpRequest.setRequestUri(baseUri, requestUri);
    }

    @Override
    public String getMethod() {
        return httpRequest.getHttpMethod();
    }

    @Override
    public void setMethod(String method) {
        httpRequest.setHttpMethod(method);
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return ((ResteasyHttpHeaders) httpRequest.getHttpHeaders()).getMutableHeaders();
    }

    @Override
    public Date getDate() {
        return httpRequest.getHttpHeaders().getDate();
    }

    @Override
    public Locale getLanguage() {
        return httpRequest.getHttpHeaders().getLanguage();
    }

    @Override
    public int getLength() {
        return httpRequest.getHttpHeaders().getLength();
    }

    @Override
    public MediaType getMediaType() {
        return httpRequest.getHttpHeaders().getMediaType();
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        return httpRequest.getHttpHeaders().getAcceptableMediaTypes();
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return httpRequest.getHttpHeaders().getAcceptableLanguages();
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return httpRequest.getHttpHeaders().getCookies();
    }

    @Override
    public boolean hasEntity() {
        return getMediaType() != null;
    }

    @Override
    public InputStream getEntityStream() {
        return httpRequest.getInputStream();
    }

    @Override
    public void setEntityStream(InputStream entityStream) {
        httpRequest.setInputStream(entityStream);
    }

    @Override
    public SecurityContext getSecurityContext() {
        return ResteasyProviderFactory.getContextData(SecurityContext.class);
    }

    @Override
    public void setSecurityContext(SecurityContext context) {
        ResteasyProviderFactory.pushContext(SecurityContext.class, context);
    }

    @Override
    public Request getRequest() {
        return ResteasyProviderFactory.getContextData(Request.class);
    }

    @Override
    public String getHeaderString(String name) {
        return httpRequest.getHttpHeaders().getHeaderString(name);
    }

    @Override
    public synchronized void suspend() {
        if (continuation == null) throw new RuntimeException("Suspend not supported yet");
        suspended = true;
    }

    @Override
    public synchronized void abortWith(Response response) {
        if (suspended && !inFilter) {
            try (ResteasyProviderFactory.CloseableContext c =
                    ResteasyProviderFactory.addCloseableContextDataLevel(contextDataMap)) {
                httpRequest.getAsyncContext().getAsyncResponse().resume(response);
            }
        } else {
            // not suspended, or suspend/abortWith within filter, same thread: collect and move on
            this.response = response;
            suspended = false;
        }
    }

    @Override
    public synchronized void resume() {
        if (!suspended) throw new RuntimeException("Cannot resume: not suspended");
        if (inFilter) {
            // suspend/resume within filter, same thread: just ignore and move on
            suspended = false;
            return;
        }

        // go on, but with proper exception handling
        try (ResteasyProviderFactory.CloseableContext c =
                ResteasyProviderFactory.addCloseableContextDataLevel(contextDataMap)) {
            filter();
        } catch (Throwable t) {
            // don't throw to client
            writeException(t);
        }
    }

    @Override
    public synchronized void resume(Throwable t) {
        if (!suspended) throw new RuntimeException("Cannot resume: not suspended");
        if (inFilter) {
            // not suspended, or suspend/abortWith within filter, same thread: collect and move on
            throwable = t;
            suspended = false;
        } else {
            try (ResteasyProviderFactory.CloseableContext c =
                    ResteasyProviderFactory.addCloseableContextDataLevel(contextDataMap)) {
                writeException(t);
            }
        }
    }

    private void writeException(Throwable t) {
        /*
         * Here, contrary to ContainerResponseContextImpl.writeException, we can use the async response
         * to write the exception, because it calls the right response filters, complete() and callbacks
         */
        httpRequest.getAsyncContext().getAsyncResponse().resume(t);
    }

    public synchronized BuiltResponse filter() throws Throwable {
        while (currentFilter < requestFilters.length) {
            ContainerRequestFilter filter = requestFilters[currentFilter++];
            try {
                suspended = false;
                response = null;
                throwable = null;
                inFilter = true;
                filter.filter(this);
            } catch (IOException e) {
                throw new ApplicationException(e);
            } finally {
                inFilter = false;
            }
            if (suspended) {
                if (!httpRequest.getAsyncContext().isSuspended())
                    // ignore any abort request until we are resumed
                    filterReturnIsMeaningful = false;
                response = null;
                return null;
            }
            BuiltResponse serverResponse = (BuiltResponse) getResponseAbortedWith();
            if (serverResponse != null) {
                // handle the case where we've been suspended by a previous filter
                return serverResponse;
            }

            if (throwable != null) {
                // handle the case where we've been suspended by a previous filter
                throw throwable;
            }
        }
        // here it means we reached the last filter
        // some frameworks don't support async request filters, in which case suspend() is forbidden
        // so if we get here we're still synchronous and don't have a continuation, which must be in
        // the caller
        startedContinuation = true;
        if (continuation == null) return null;
        // in any case, return the continuation: sync will use it, and async will ignore it
        return continuation.get();
    }

    public boolean startedContinuation() {
        return startedContinuation;
    }
}
