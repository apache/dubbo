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

import org.apache.dubbo.remoting.http12.message.DefaultHttpResponse;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

public class ServletHttpResponseAdaptee extends DefaultHttpResponse implements HttpServletResponse {

    @Override
    public void addCookie(Cookie cookie) {}

    @Override
    public boolean containsHeader(String name) {
        return false;
    }

    @Override
    public String encodeURL(String url) {
        return null;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return null;
    }

    @Override
    public String encodeUrl(String url) {
        return null;
    }

    @Override
    public String encodeRedirectUrl(String url) {
        return null;
    }

    @Override
    public void setDateHeader(String name, long date) {}

    @Override
    public void addDateHeader(String name, long date) {}

    @Override
    public void setIntHeader(String name, int value) {}

    @Override
    public void addIntHeader(String name, int value) {}

    @Override
    public void setStatus(int sc, String sm) {}

    @Override
    public String getHeader(String name) {
        return null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return null;
    }

    @Override
    public Collection<String> getHeaderNames() {
        return null;
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return null;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return null;
    }

    @Override
    public void setCharacterEncoding(String charset) {}

    @Override
    public void setContentLength(int len) {}

    @Override
    public void setContentLengthLong(long len) {}

    @Override
    public void setBufferSize(int size) {}

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public void flushBuffer() throws IOException {}

    @Override
    public void setLocale(Locale loc) {}

    @Override
    public Locale getLocale() {
        return null;
    }
}
