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
package org.apache.dubbo.remoting.http12.message;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpCookie;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.HttpUtils;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.handler.codec.DateFormatter;

public class DefaultHttpResponse implements HttpResponse {

    private final AtomicBoolean committed = new AtomicBoolean();
    private final HttpHeaders headers = new HttpHeaders();

    private int status;
    private String contentType;
    private List<HttpCookie> cookies;
    private Object body;
    private OutputStream outputStream;

    @Override
    public int status() {
        return status;
    }

    @Override
    public void setStatus(int status) {
        check();
        this.status = status;
    }

    @Override
    public String header(String name) {
        return headers.getFirst(name);
    }

    @Override
    public List<String> headerValues(String name) {
        return headers.get(name);
    }

    @Override
    public Date dateHeader(String name) {
        String value = headers.getFirst(name);
        return StringUtils.isEmpty(value) ? null : DateFormatter.parseHttpDate(value);
    }

    @Override
    public boolean hasHeader(String name) {
        return headers.containsKey(name);
    }

    @Override
    public Collection<String> headerNames() {
        return headers.keySet();
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public void addHeader(String name, String value) {
        check();
        headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    }

    @Override
    public void setHeader(String name, String value) {
        check();
        headers.set(name, value);
    }

    @Override
    public void setHeader(String name, Date value) {
        check();
        headers.set(name, DateFormatter.format(value));
    }

    @Override
    public void setHeader(String name, List<String> values) {
        check();
        headers.put(name, values);
    }

    @Override
    public void addCookie(HttpCookie cookie) {
        check();
        if (cookies == null) {
            cookies = new ArrayList<>();
        }
        cookies.add(cookie);
    }

    @Override
    public String contentType() {
        return getContentType0();
    }

    private String getContentType0() {
        if (contentType == null) {
            String value = headers.getFirst(HttpHeaderNames.CONTENT_TYPE.getName());
            contentType = value == null ? StringUtils.EMPTY_STRING : value.trim();
        }
        return contentType.isEmpty() ? null : contentType;
    }

    @Override
    public void setContentType(String contentType) {
        check();
        this.contentType = contentType == null ? StringUtils.EMPTY_STRING : contentType.trim();
        headers.set(HttpHeaderNames.CONTENT_TYPE.getName(), getContentType0());
    }

    @Override
    public String mediaType() {
        String contentType = getContentType0();
        if (contentType == null) {
            return null;
        }
        int pos = contentType.indexOf(';');
        return pos == -1 ? contentType : contentType.substring(0, pos).trim();
    }

    @Override
    public String charset() {
        String charset = getCharset0();
        return charset == null ? StandardCharsets.UTF_8.name() : charset;
    }

    private String getCharset0() {
        String contentType = getContentType0();
        if (contentType == null) {
            return null;
        }
        int pos = contentType.lastIndexOf("charset=");
        return pos == -1 ? null : contentType.substring(pos + 8);
    }

    @Override
    public String locale() {
        return headers.getFirst("content-language");
    }

    @Override
    public void setLocale(String locale) {
        check();
        headers.set("content-language", locale);
    }

    @Override
    public Object body() {
        return body;
    }

    @Override
    public void setBody(Object body) {
        check();
        this.body = body;
    }

    @Override
    public OutputStream outputStream() {
        if (outputStream == null) {
            outputStream = new ByteArrayOutputStream(1024);
        }
        return null;
    }

    @Override
    public void setOutputStream(OutputStream os) {
        check();
        outputStream = os;
    }

    @Override
    public void sendRedirect(String location) {
        check();
        setStatus(HttpStatus.FOUND.getCode());
        setHeader("location", location);
        commit();
    }

    @Override
    public void sendError(int status) {
        check();
        setStatus(status);
        commit();
    }

    @Override
    public void sendError(int status, String message) {
        check();
        setStatus(status);
        setBody(message);
        commit();
    }

    @Override
    public boolean noContent() {
        return body == null && outputStream == null;
    }

    @Override
    public boolean isEmpty() {
        return status == 0 && headers.isEmpty() && cookies == null && body == null && outputStream == null;
    }

    @Override
    public boolean isCommitted() {
        return committed.get();
    }

    @Override
    public void commit() {
        if (committed.compareAndSet(false, true)) {
            return;
        }

        if (cookies != null) {
            headers.put("set-cookie", HttpUtils.encodeCookies(cookies));
        }

        throw new IllegalStateException("The response has been committed");
    }

    @Override
    public void reset() {
        headers.clear();
        status = 0;
        contentType = null;
        cookies = null;
        body = null;
        resetBuffer();
        committed.set(false);
    }

    @Override
    public void resetBuffer() {
        if (outputStream != null) {
            if (outputStream instanceof ByteArrayOutputStream) {
                ((ByteArrayOutputStream) outputStream).reset();
                return;
            }
            throw new UnsupportedOperationException("The output stream is not supported to reset");
        }
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    @Override
    public Object getBody() {
        return body == null ? outputStream : body;
    }

    private void check() {
        if (committed.get()) {
            throw new IllegalStateException("Response already committed!");
        }
    }
}
