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
package org.apache.dubbo.rpc.protocol.tri.test;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2MetadataFrame;
import org.apache.dubbo.remoting.http12.message.MediaType;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.netty.handler.codec.http2.Http2Headers.PseudoHeaderName;

@SuppressWarnings("UnusedReturnValue")
public class TestRequest {

    private final HttpHeaders headers = HttpHeaders.create();
    private final Map<String, String> cookies = new LinkedHashMap<>();
    private final Map<String, Object> params = new LinkedHashMap<>();
    private final Map<String, String> providerParams = new LinkedHashMap<>();
    private List<Object> bodies;

    public TestRequest(HttpMethods method, String path) {
        setMethod(method);
        setPath(path);
    }

    public TestRequest(String path) {
        setPath(path);
    }

    public TestRequest() {}

    public String getPath() {
        return headers.getFirst(PseudoHeaderName.PATH.value());
    }

    public TestRequest setPath(String path) {
        headers.set(PseudoHeaderName.PATH.value(), path);
        return this;
    }

    public String getMethod() {
        return headers.getFirst(PseudoHeaderName.METHOD.value());
    }

    public TestRequest setMethod(String method) {
        headers.set(PseudoHeaderName.METHOD.value(), method);
        return this;
    }

    public TestRequest setMethod(HttpMethods method) {
        return setMethod(method.name());
    }

    public String getContentType() {
        return headers.getFirst(HttpHeaderNames.CONTENT_TYPE.getKey());
    }

    public TestRequest setContentType(String contentType) {
        if (StringUtils.isNotEmpty(contentType)) {
            headers.set(HttpHeaderNames.CONTENT_TYPE.getKey(), contentType);
        }
        return this;
    }

    public TestRequest setContentType(MediaType mediaType) {
        return setContentType(mediaType.getName());
    }

    public TestRequest setContentType(MediaType mediaType, String charset) {
        return setContentType(mediaType.getName() + "; charset=" + charset);
    }

    public TestRequest setContentType(MediaType mediaType, Charset charset) {
        return setContentType(mediaType.getName() + "; charset=" + charset.name());
    }

    public TestRequest setAccept(String accept) {
        if (StringUtils.isNotEmpty(accept)) {
            headers.set(HttpHeaderNames.ACCEPT.getKey(), accept);
        }
        return this;
    }

    public TestRequest setAccept(MediaType mediaType) {
        return setAccept(mediaType.getName());
    }

    public TestRequest setHeader(String name, Object value) {
        if (value != null) {
            headers.set(name, value.toString());
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public TestRequest setHeaders(Map<String, ?> headers) {
        for (Map.Entry<String, ?> entry : headers.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof List) {
                List<String> items = new ArrayList<>();
                for (Object obj : (List<Object>) value) {
                    if (obj != null) {
                        items.add(obj.toString());
                    }
                }
                this.headers.add(entry.getKey(), items);
            } else if (value instanceof Object[]) {
                List<String> items = new ArrayList<>();
                for (Object obj : (Object[]) value) {
                    if (obj != null) {
                        items.add(obj.toString());
                    }
                }
                this.headers.add(entry.getKey(), items);
            } else if (value != null) {
                this.headers.set(entry.getKey(), value.toString());
            }
        }
        return this;
    }

    public TestRequest setCookie(String name, String value) {
        cookies.put(name, value);
        return this;
    }

    public TestRequest setCookies(Map<String, String> cookies) {
        this.cookies.putAll(cookies);
        return this;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public TestRequest param(String name, Object value) {
        params.put(name, value);
        return this;
    }

    public TestRequest setParams(Map<String, ?> params) {
        this.params.putAll(params);
        return this;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public TestRequest setProviderParam(String name, String value) {
        providerParams.put(name, value);
        return this;
    }

    public TestRequest setProviderParams(Map<String, String> params) {
        providerParams.putAll(params);
        return this;
    }

    public Map<String, String> getProviderParams() {
        return providerParams;
    }

    public TestRequest setBody(Object body) {
        List<Object> bodies = this.bodies;
        if (bodies == null) {
            bodies = new ArrayList<>();
            this.bodies = bodies;
        }
        bodies.add(body);
        return this;
    }

    public List<Object> getBodies() {
        return bodies;
    }

    public TestRequest post(Object body) {
        setMethod(HttpMethods.POST);
        setBody(body);
        return this;
    }

    public TestRequest post() {
        setMethod(HttpMethods.POST);
        return this;
    }

    public Http2Header toMetadata() {
        return new Http2MetadataFrame(headers, !HttpMethods.supportBody(getMethod()));
    }
}
