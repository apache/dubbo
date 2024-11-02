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
import org.apache.dubbo.remoting.http12.HttpResult;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.HttpUtils;
import org.apache.dubbo.remoting.http12.message.DefaultHttpResult.Builder;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import io.netty.handler.codec.DateFormatter;

public class DefaultHttpResponse implements HttpResponse {

    private HttpHeaders headers;
    private int status;
    private String contentType;
    private String charset;
    private Object body;
    private OutputStream outputStream;
    private volatile boolean committed;

    @Override
    public int status() {
        return status;
    }

    @Override
    public void setStatus(int status) {
        if (committed) {
            return;
        }
        this.status = status;
    }

    @Override
    public String header(CharSequence name) {
        return headers == null ? null : headers.getFirst(name);
    }

    @Override
    public Date dateHeader(CharSequence name) {
        String value = header(name);
        return StringUtils.isEmpty(value) ? null : DateFormatter.parseHttpDate(value);
    }

    @Override
    public List<String> headerValues(CharSequence name) {
        return headers == null ? Collections.emptyList() : headers.get(name);
    }

    @Override
    public boolean hasHeader(CharSequence name) {
        return headers != null && headers.containsKey(name);
    }

    @Override
    public Collection<String> headerNames() {
        return headers == null ? Collections.emptyList() : headers.names();
    }

    @Override
    public HttpHeaders headers() {
        if (headers == null) {
            headers = HttpHeaders.create();
        }
        return headers;
    }

    @Override
    public void addHeader(CharSequence name, String value) {
        if (committed) {
            return;
        }
        headers().add(name, value);
    }

    @Override
    public void addHeader(CharSequence name, Date value) {
        addHeader(name, DateFormatter.format(value));
    }

    @Override
    public void setHeader(CharSequence name, String value) {
        if (committed) {
            return;
        }
        headers().set(name, value);
    }

    @Override
    public void setHeader(CharSequence name, Date value) {
        setHeader(name, DateFormatter.format(value));
    }

    @Override
    public void setHeader(CharSequence name, List<String> values) {
        if (committed) {
            return;
        }
        headers().set(name, values);
    }

    @Override
    public void addCookie(HttpCookie cookie) {
        addHeader(HttpHeaderNames.SET_COOKIE.getKey(), HttpUtils.encodeCookie(cookie));
    }

    @Override
    public String contentType() {
        String contentType = this.contentType;
        if (contentType == null) {
            contentType = header(HttpHeaderNames.CONTENT_TYPE.getKey());
            contentType = contentType == null ? StringUtils.EMPTY_STRING : contentType.trim();
            this.contentType = contentType;
        }
        return contentType.isEmpty() ? null : contentType;
    }

    @Override
    public void setContentType(String contentType) {
        if (committed) {
            return;
        }
        this.contentType = contentType;
        charset = null;
    }

    @Override
    public String mediaType() {
        String contentType = contentType();
        if (contentType == null) {
            return null;
        }
        int index = contentType.indexOf(';');
        return index == -1 ? contentType : contentType.substring(0, index);
    }

    @Override
    public String charset() {
        String charset = this.charset;
        if (charset == null) {
            String contentType = contentType();
            if (contentType == null) {
                charset = StringUtils.EMPTY_STRING;
            } else {
                int index = contentType.lastIndexOf(HttpUtils.CHARSET_PREFIX);
                charset = index == -1
                        ? StringUtils.EMPTY_STRING
                        : contentType.substring(index + 8).trim();
            }
            this.charset = charset;
        }
        return charset.isEmpty() ? null : charset;
    }

    @Override
    public void setCharset(String charset) {
        if (committed) {
            return;
        }
        String contentType = contentType();
        if (contentType != null) {
            this.contentType = contentType + "; " + HttpUtils.CHARSET_PREFIX + charset;
        }
        this.charset = charset;
    }

    @Override
    public String locale() {
        return header(HttpHeaderNames.CONTENT_LANGUAGE.getKey());
    }

    @Override
    public void setLocale(String locale) {
        if (committed) {
            return;
        }
        setHeader(HttpHeaderNames.CONTENT_LANGUAGE.getKey(), locale);
    }

    @Override
    public Object body() {
        return body;
    }

    @Override
    public void setBody(Object body) {
        if (committed) {
            return;
        }
        this.body = body;
    }

    @Override
    public OutputStream outputStream() {
        if (outputStream == null) {
            outputStream = new ByteArrayOutputStream(1024);
        }
        return outputStream;
    }

    @Override
    public void setOutputStream(OutputStream os) {
        if (committed) {
            return;
        }
        outputStream = os;
    }

    @Override
    public void sendRedirect(String location) {
        check();
        setStatus(HttpStatus.FOUND.getCode());
        setHeader(HttpHeaderNames.LOCATION.getKey(), location);
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
    public boolean isEmpty() {
        return status == 0 && (headers == null || headers.isEmpty()) && isContentEmpty();
    }

    @Override
    public boolean isContentEmpty() {
        if (body != null) {
            return false;
        }
        if (outputStream != null) {
            return outputStream instanceof ByteArrayOutputStream && ((ByteArrayOutputStream) outputStream).size() == 0;
        }
        return true;
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    @Override
    public void commit() {
        committed = true;
    }

    @Override
    public void setCommitted(boolean committed) {
        this.committed = committed;
    }

    @Override
    public void reset() {
        check();
        headers = null;
        status = 0;
        contentType = null;
        body = null;
        resetBuffer();
    }

    @Override
    public void resetBuffer() {
        check();
        if (outputStream == null) {
            return;
        }
        if (outputStream instanceof ByteArrayOutputStream) {
            ((ByteArrayOutputStream) outputStream).reset();
            return;
        }
        String name = outputStream.getClass().getName();
        throw new UnsupportedOperationException("The outputStream type [" + name + "] is not supported to reset");
    }

    private void check() {
        if (committed) {
            throw new IllegalStateException("Response already committed");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public HttpResult<Object> toHttpResult() {
        Builder<Object> builder = HttpResult.builder();
        int status = this.status;
        Object body = this.body;
        builder.headers(headers);

        if (body instanceof HttpResult) {
            HttpResult<Object> result = (HttpResult<Object>) body;
            if (result.getStatus() != 0) {
                status = result.getStatus();
            }
            if (result.getBody() != null) {
                body = result.getBody();
            }
            builder.headers(result.getHeaders());
        }

        return builder.status(status == 0 ? HttpStatus.OK.getCode() : status)
                .body(body == null ? outputStream : body)
                .build();
    }

    @Override
    public String toString() {
        return "DefaultHttpResponse{" + fieldToString() + '}';
    }

    protected final String fieldToString() {
        return "status=" + status + ", contentType='" + contentType() + '\'' + ", body=" + body;
    }
}
