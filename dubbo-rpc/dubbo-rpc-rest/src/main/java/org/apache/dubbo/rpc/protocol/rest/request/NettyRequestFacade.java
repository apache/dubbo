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
package org.apache.dubbo.rpc.protocol.rest.request;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class NettyRequestFacade extends RequestFacade<FullHttpRequest> {
    protected Map<String, ArrayList<String>> headers;

    private ChannelHandlerContext context;

    public NettyRequestFacade(Object request, ChannelHandlerContext context) {

        super((FullHttpRequest) request);
        headers = new HashMap<>();
        initHeaders();
        this.context = context;
    }


    protected void initHeaders() {
        for (Map.Entry<String, String> header : request.headers()) {

            String key = header.getKey();

            ArrayList<String> tmpHeaders = headers.get(key);

            if (tmpHeaders == null) {
                tmpHeaders = new ArrayList<>();
                headers.put(key, tmpHeaders);
            }

            tmpHeaders.add(header.getValue());
        }
    }

    @Override
    public Object getParts() throws Exception {
        return null;
    }

    @Override
    public Object getPart(String var1) throws Exception {
        return null;
    }

    @Override
    public Object getCookies() {
        return null;
    }

    @Override
    public long getDateHeader(String name) {
        return 0;
    }

    @Override
    public String getHeader(String name) {

        List<String> values = headers.get(name);

        if (values == null || values.isEmpty()) {
            return null;
        } else {
            return values.get(0);
        }

    }

    @Override
    public Enumeration<String> getHeaders(String name) {

        List<String> list = headers.get(name);

        if (list == null) {
            list = new ArrayList<>();
        }


        ListIterator<String> stringListIterator = list.listIterator();

        return new Enumeration<String>() {
            @Override
            public boolean hasMoreElements() {
                return stringListIterator.hasNext();
            }

            @Override
            public String nextElement() {
                return stringListIterator.next();
            }
        };
    }


    @Override
    public Enumeration<String> getHeaderNames() {

        Set<String> strings = headers.keySet();

        return new Enumeration<String>() {
            @Override
            public boolean hasMoreElements() {
                return strings.iterator().hasNext();
            }

            @Override
            public String nextElement() {
                return strings.iterator().next();
            }
        };
    }

    @Override
    public int getIntHeader(String name) {

        String header = getHeader(name);
        if (header == null) {
            return -1;
        }
        return Integer.parseInt(header);
    }

    @Override
    public String getMethod() {
        return request.method().name();
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
        return null;
    }

    @Override
    public String getQueryString() {
        return null;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public String getRequestedSessionId() {
        return null;
    }

    @Override
    public String getRequestURI() {
        return request.uri();
    }

    @Override
    public StringBuffer getRequestURL() {
        return null;
    }

    @Override
    public String getServletPath() {
        return null;
    }

    @Override
    public String changeSessionId() {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return null;
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {

    }

    @Override
    public int getContentLength() {
        return 0;
    }

    @Override
    public long getContentLengthLong() {
        return 0;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public String getParameter(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return null;
    }

    @Override
    public String[] getParameterValues(String name) {
        return new String[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return null;
    }

    @Override
    public String getProtocol() {
        return null;
    }

    @Override
    public String getScheme() {
        return null;
    }

    @Override
    public String getServerName() {
        return null;
    }

    @Override
    public int getServerPort() {
        return 0;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return null;
    }

    @Override
    public String getRemoteAddr() {
        return null;
    }

    @Override
    public String getRemoteHost() {
        return null;
    }

    @Override
    public void setAttribute(String name, Object o) {

    }

    @Override
    public void removeAttribute(String name) {

    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String getRealPath(String path) {
        return null;
    }

    @Override
    public int getRemotePort() {
        return 0;
    }

    @Override
    public String getLocalName() {
        return null;
    }

    @Override
    public String getLocalAddr() {
        return null;
    }

    @Override
    public int getLocalPort() {
        return 0;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        ByteBuf byteBuf = ((HttpContent) request).content();

        if (byteBuf.readableBytes() > 0) {

            return new ByteBufInputStream(byteBuf.retain());
        }
        return new ByteArrayInputStream(new byte[0]);
    }
}
