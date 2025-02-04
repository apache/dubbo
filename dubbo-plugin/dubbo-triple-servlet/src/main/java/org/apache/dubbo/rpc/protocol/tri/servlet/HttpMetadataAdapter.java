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
package org.apache.dubbo.rpc.protocol.tri.servlet;

import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.h2.Http2Header;

import javax.servlet.http.HttpServletRequest;

import java.util.Enumeration;

import io.netty.handler.codec.http2.Http2Headers.PseudoHeaderName;

public final class HttpMetadataAdapter implements Http2Header {

    private final HttpServletRequest request;

    private HttpHeaders headers;

    HttpMetadataAdapter(HttpServletRequest request) {
        this.request = request;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    @Override
    public HttpHeaders headers() {
        HttpHeaders headers = this.headers;
        if (headers == null) {
            headers = HttpHeaders.create();
            Enumeration<String> en = request.getHeaderNames();
            while (en.hasMoreElements()) {
                String key = en.nextElement();
                Enumeration<String> ven = request.getHeaders(key);
                while (ven.hasMoreElements()) {
                    headers.add(key, ven.nextElement());
                }
            }
            headers.add(PseudoHeaderName.METHOD.value(), method());
            headers.add(PseudoHeaderName.SCHEME.value(), request.getScheme());
            headers.add(PseudoHeaderName.AUTHORITY.value(), request.getHeader(HttpHeaderNames.HOST.getName()));
            headers.add(PseudoHeaderName.PROTOCOL.value(), request.getProtocol());
            this.headers = headers;
        }
        return headers;
    }

    @Override
    public String method() {
        return request.getMethod();
    }

    @Override
    public String path() {
        String query = request.getQueryString();
        return query == null ? request.getRequestURI() : request.getRequestURI() + '?' + query;
    }

    @Override
    public long id() {
        return -1L;
    }

    @Override
    public boolean isEndStream() {
        return false;
    }
}
