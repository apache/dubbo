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

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.http12.HttpChannel;
import org.apache.dubbo.remoting.http12.HttpConstants;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpVersion;
import org.apache.dubbo.remoting.http12.message.DefaultHttpRequest;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ReadListener;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ServletHttpRequestAdapter extends DefaultHttpRequest implements HttpServletRequest {

    private final ServletContext servletContext;
    private final HttpSessionFactory sessionFactory;

    private ServletInputStream sis;
    private BufferedReader reader;

    public ServletHttpRequestAdapter(
            HttpMetadata metadata,
            HttpChannel channel,
            ServletContext servletContext,
            HttpSessionFactory sessionFactory) {
        super(metadata, channel);
        this.servletContext = servletContext;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public String getAuthType() {
        return header("www-authenticate");
    }

    @Override
    public Cookie[] getCookies() {
        return Helper.convertCookies(cookies());
    }

    @Override
    public long getDateHeader(String name) {
        Date date = dateHeader(name);
        return date == null ? -1L : date.getTime();
    }

    @Override
    public String getHeader(String name) {
        return header(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return Collections.enumeration(headerValues(name));
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headerNames());
    }

    @Override
    public int getIntHeader(String name) {
        String headerValue = getHeader(name);
        try {
            return Integer.parseInt(headerValue);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public String getMethod() {
        return method();
    }

    @Override
    public String getPathInfo() {
        return null;
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public String getContextPath() {
        return "/";
    }

    @Override
    public String getQueryString() {
        return query();
    }

    @Override
    public String getRemoteUser() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isUserInRole(String role) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Principal getUserPrincipal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRequestedSessionId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRequestURI() {
        return path();
    }

    @Override
    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer(32);
        String scheme = getScheme();
        int port = getServerPort();
        url.append(scheme).append("://").append(getServerName());
        if (HttpConstants.HTTP.equals(scheme) && port != 80 || HttpConstants.HTTPS.equals(scheme) && port != 443) {
            url.append(':');
            url.append(port);
        }
        url.append(path());
        return url;
    }

    @Override
    public String getServletPath() {
        return path();
    }

    @Override
    public HttpSession getSession(boolean create) {
        if (sessionFactory == null) {
            throw new UnsupportedOperationException("No HttpSessionFactory found");
        }
        return sessionFactory.getSession(this, create);
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public String changeSessionId() {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return true;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return true;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    @Override
    public boolean authenticate(HttpServletResponse response) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void login(String username, String password) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void logout() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Part> getParts() {
        return Helper.convertParts(parts());
    }

    @Override
    public FileUploadPart getPart(String name) {
        return Helper.convert(part(name));
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getAttribute(String name) {
        return attribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributeNames());
    }

    @Override
    public String getCharacterEncoding() {
        return charset();
    }

    @Override
    public void setCharacterEncoding(String env) {
        setCharset(env);
    }

    @Override
    public int getContentLength() {
        return contentLength();
    }

    @Override
    public long getContentLengthLong() {
        return contentLength();
    }

    @Override
    public String getContentType() {
        return contentType();
    }

    @Override
    public ServletInputStream getInputStream() {
        if (sis == null) {
            sis = new HttpInputStream(inputStream());
        }
        return sis;
    }

    @Override
    public String getParameter(String name) {
        return parameter(name);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameterNames());
    }

    @Override
    public String[] getParameterValues(String name) {
        List<String> values = parameterValues(name);
        return values == null ? null : values.toArray(new String[0]);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Collection<String> paramNames = parameterNames();
        if (paramNames.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String[]> result = CollectionUtils.newLinkedHashMap(paramNames.size());
        for (String paramName : paramNames) {
            result.put(paramName, getParameterValues(paramName));
        }
        return result;
    }

    @Override
    public String getProtocol() {
        return isHttp2() ? HttpVersion.HTTP2.getProtocol() : HttpVersion.HTTP1.getProtocol();
    }

    @Override
    public String getScheme() {
        return scheme();
    }

    @Override
    public String getServerName() {
        return serverName();
    }

    @Override
    public int getServerPort() {
        return serverPort();
    }

    @Override
    public BufferedReader getReader() {
        if (reader == null) {
            reader = new BufferedReader(new InputStreamReader(inputStream(), charsetOrDefault()));
        }
        return reader;
    }

    @Override
    public String getRemoteAddr() {
        return remoteAddr();
    }

    @Override
    public String getRemoteHost() {
        return String.valueOf(remotePort());
    }

    @Override
    public Locale getLocale() {
        return locale();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return Collections.enumeration(locales());
    }

    @Override
    public boolean isSecure() {
        return HttpConstants.HTTPS.equals(scheme());
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        throw new UnsupportedOperationException();
    }

    public String getRealPath(String path) {
        return null;
    }

    @Override
    public int getRemotePort() {
        return remotePort();
    }

    @Override
    public String getLocalName() {
        return localHost();
    }

    @Override
    public String getLocalAddr() {
        return localAddr();
    }

    @Override
    public int getLocalPort() {
        return localPort();
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        throw new IllegalStateException();
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IllegalStateException {
        throw new IllegalStateException();
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        throw new IllegalStateException();
    }

    @Override
    public DispatcherType getDispatcherType() {
        return DispatcherType.REQUEST;
    }

    /* jakarta placeholder */

    @Override
    public String toString() {
        return "ServletHttpRequestAdapter{" + fieldToString() + '}';
    }

    private static final class HttpInputStream extends ServletInputStream {

        private final InputStream is;

        HttpInputStream(InputStream is) {
            this.is = is;
        }

        @Override
        public int read() throws IOException {
            return is.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return is.read(b);
        }

        @Override
        public void close() throws IOException {
            is.close();
        }

        @Override
        public int readLine(byte[] b, int off, int len) throws IOException {
            return is.read(b, off, len);
        }

        @Override
        public boolean isFinished() {
            try {
                return is.available() == 0;
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException();
        }
    }
}
