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

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpChannel;
import org.apache.dubbo.remoting.http12.HttpCookie;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpUtils;
import org.apache.dubbo.remoting.http12.RequestMetadata;
import org.apache.dubbo.remoting.http12.h2.Http2Header;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.netty.handler.codec.DateFormatter;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;

public class DefaultHttpRequest implements HttpRequest {

    private final HttpMetadata metadata;
    private final HttpChannel channel;
    private final HttpHeaders headers;

    private String method;
    private String uri;
    private List<HttpCookie> cookies;
    private String contentType;
    private List<String> accept;
    private List<Locale> locales;
    private QueryStringDecoder decoder;
    private HttpPostRequestDecoder postDecoder;
    private boolean postParsed;
    private Map<String, Object> attributes;
    private InputStream inputStream;

    public DefaultHttpRequest(HttpMetadata metadata, HttpChannel channel) {
        this.metadata = metadata;
        this.channel = channel;
        headers = metadata.headers();
        if (metadata instanceof RequestMetadata) {
            RequestMetadata requestMetadata = (RequestMetadata) metadata;
            method = requestMetadata.method();
            uri = requestMetadata.path();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public boolean isHttp2() {
        return metadata instanceof Http2Header;
    }

    @Override
    public String method() {
        return method;
    }

    @Override
    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public String uri() {
        return uri;
    }

    @Override
    public void setUri(String uri) {
        this.uri = uri;
        decoder = null;
        postDecoder = null;
        postParsed = false;
    }

    @Override
    public String path() {
        return getDecoder().path();
    }

    @Override
    public String rawPath() {
        return getDecoder().rawPath();
    }

    @Override
    public String query() {
        return getDecoder().rawQuery();
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
    public void setHeader(String name, String value) {
        headers.set(name, value);
    }

    @Override
    public void setHeader(String name, Date value) {
        headers.set(name, DateFormatter.format(value));
    }

    @Override
    public void setHeader(String name, List<String> values) {
        headers.put(name, values);
    }

    @Override
    public Collection<HttpCookie> cookies() {
        if (cookies == null) {
            parseCookies();
        }
        return cookies;
    }

    @Override
    public HttpCookie cookie(String name) {
        if (cookies == null) {
            parseCookies();
        }
        for (int i = 0, size = cookies.size(); i < size; i++) {
            HttpCookie cookie = cookies.get(i);
            if (cookie.name().equals(name)) {
                return cookie;
            }
        }
        return null;
    }

    private void parseCookies() {
        cookies = HttpUtils.decodeCookies(headers.getFirst("cookie"));
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
        this.contentType = contentType == null ? StringUtils.EMPTY_STRING : contentType.trim();
        headers.set(HttpHeaderNames.CONTENT_TYPE.getName(), getContentType0());
    }

    @Override
    public int contentLength() {
        String value = headers.getFirst(HttpHeaderNames.CONTENT_LENGTH.getName());
        return value == null ? 0 : Integer.parseInt(value);
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
    public String accept() {
        return headers.getFirst(HttpHeaderNames.ACCEPT.getName());
    }

    @Override
    public Locale locale() {
        return locales().get(0);
    }

    @Override
    public List<Locale> locales() {
        if (locales == null) {
            locales = HttpUtils.parseAcceptLanguage(headers.getFirst("accept-language"));
            if (locales.isEmpty()) {
                locales.add(Locale.getDefault());
            }
        }
        return locales;
    }

    @Override
    public String scheme() {
        String scheme = headers.getFirst("x-forwarded-proto");
        if (isHttp2()) {
            scheme = headers.getFirst(":scheme");
        }
        return scheme == null ? "https" : scheme;
    }

    @Override
    public String serverHost() {
        String host = getHost0();
        return host == null ? localHost() + ':' + localPort() : host;
    }

    @Override
    public String serverName() {
        String host = getHost0();
        if (host != null) {
            int pos = host.lastIndexOf(':');
            return pos == -1 ? host : host.substring(0, pos);
        }
        return localHost();
    }

    @Override
    public int serverPort() {
        String port = headers.getFirst("x-forwarded-port");
        if (port != null) {
            return Integer.parseInt(port);
        }
        String host = getHost0();
        if (host != null) {
            int pos = host.lastIndexOf(':');
            return pos == -1 ? -1 : Integer.parseInt(host.substring(0, pos));
        }
        return localPort();
    }

    private String getHost0() {
        return headers.getFirst(isHttp2() ? ":authority" : "host");
    }

    @Override
    public String remoteHost() {
        return getRemoteAddress().getHostString();
    }

    @Override
    public String remoteAddr() {
        return getRemoteAddress().getAddress().getHostAddress();
    }

    @Override
    public int remotePort() {
        return getRemoteAddress().getPort();
    }

    private InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) channel.remoteAddress();
    }

    @Override
    public String localHost() {
        return getLocalAddress().getHostString();
    }

    @Override
    public String localAddr() {
        return getLocalAddress().getAddress().getHostAddress();
    }

    @Override
    public int localPort() {
        return getLocalAddress().getPort();
    }

    private InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) channel.localAddress();
    }

    @Override
    public String parameter(String name) {
        List<String> values = getDecoder().parameters().get(name);
        if (CollectionUtils.isNotEmpty(values)) {
            return values.get(0);
        }
        HttpPostRequestDecoder postDecoder = getPostDecoder();
        if (postDecoder == null) {
            return null;
        }
        List<InterfaceHttpData> items = postDecoder.getBodyHttpDatas(name);
        if (items == null) {
            return null;
        }
        for (int i = 0, size = items.size(); i < size; i++) {
            InterfaceHttpData item = items.get(i);
            if (item.getHttpDataType() == HttpDataType.Attribute) {
                return HttpUtils.readPostValue(item);
            }
        }
        return null;
    }

    @Override
    public String parameter(String name, String defaultValue) {
        String value = parameter(name);
        return value == null ? defaultValue : value;
    }

    @Override
    public boolean hasParameter(String name) {
        if (getDecoder().parameters().containsKey(name)) {
            return true;
        }
        HttpPostRequestDecoder postDecoder = getPostDecoder();
        if (postDecoder == null) {
            return false;
        }
        List<InterfaceHttpData> items = postDecoder.getBodyHttpDatas(name);
        if (items == null) {
            return false;
        }
        for (int i = 0, size = items.size(); i < size; i++) {
            InterfaceHttpData item = items.get(i);
            if (item.getHttpDataType() == HttpDataType.Attribute) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> parameterValues(String name) {
        List<String> values = getDecoder().parameters().get(name);
        HttpPostRequestDecoder postDecoder = getPostDecoder();
        if (postDecoder == null) {
            return values;
        }
        List<InterfaceHttpData> items = postDecoder.getBodyHttpDatas(name);
        if (items == null) {
            return values;
        }
        for (int i = 0, size = items.size(); i < size; i++) {
            InterfaceHttpData item = items.get(i);
            if (item.getHttpDataType() == HttpDataType.Attribute) {
                if (values == null) {
                    values = new ArrayList<>();
                }
                values.add(HttpUtils.readPostValue(item));
            }
        }
        return values;
    }

    @Override
    public Collection<String> parameterNames() {
        Set<String> names = getDecoder().parameters().keySet();
        HttpPostRequestDecoder postDecoder = getPostDecoder();
        if (postDecoder == null) {
            return names;
        }
        List<InterfaceHttpData> items = postDecoder.getBodyHttpDatas();
        if (items == null) {
            return names;
        }
        Set<String> allNames = null;
        for (int i = 0, size = items.size(); i < size; i++) {
            InterfaceHttpData item = items.get(i);
            if (item.getHttpDataType() == HttpDataType.Attribute) {
                if (allNames == null) {
                    allNames = new LinkedHashSet<>(names);
                }
                allNames.add(item.getName());
            }
        }
        return allNames;
    }

    @Override
    public Collection<FileUpload> parts() {
        HttpPostRequestDecoder postDecoder = getPostDecoder();
        if (postDecoder == null) {
            return Collections.emptyList();
        }
        List<InterfaceHttpData> items = postDecoder.getBodyHttpDatas();
        if (items == null) {
            return Collections.emptyList();
        }
        List<FileUpload> fileUploads = new ArrayList<>();
        for (int i = 0, size = items.size(); i < size; i++) {
            InterfaceHttpData item = items.get(i);
            if (item.getHttpDataType() == HttpDataType.FileUpload) {
                fileUploads.add(HttpUtils.readUpload(item));
            }
        }
        return fileUploads;
    }

    @Override
    public FileUpload part(String name) {
        HttpPostRequestDecoder postDecoder = getPostDecoder();
        if (postDecoder == null) {
            return null;
        }
        List<InterfaceHttpData> items = postDecoder.getBodyHttpDatas(name);
        if (items == null) {
            return null;
        }

        for (int i = 0, size = items.size(); i < size; i++) {
            InterfaceHttpData item = items.get(i);
            if (item.getHttpDataType() == HttpDataType.FileUpload) {
                return HttpUtils.readUpload(item);
            }
        }
        return null;
    }

    private QueryStringDecoder getDecoder() {
        if (decoder == null) {
            String charset = getCharset0();
            if (charset == null) {
                decoder = new QueryStringDecoder(uri);
            } else {
                decoder = new QueryStringDecoder(uri, Charset.forName(charset));
            }
        }
        return decoder;
    }

    private HttpPostRequestDecoder getPostDecoder() {
        if (inputStream == null) {
            return null;
        }
        if (postDecoder == null) {
            if (postParsed) {
                return null;
            }
            if (HttpMethods.isPost(method)) {
                postDecoder = HttpUtils.createPostRequestDecoder(this, inputStream, getCharset0());
            }
            postParsed = true;
        }
        return postDecoder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T attribute(String name) {
        return (T) getAttributes().get(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        getAttributes().put(name, value);
    }

    @Override
    public boolean hasAttribute(String name) {
        return attributes != null && attributes.containsKey(name);
    }

    @Override
    public Collection<String> attributeNames() {
        return getAttributes().keySet();
    }

    @Override
    public Map<String, Object> attributes() {
        return getAttributes();
    }

    private Map<String, Object> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        return attributes;
    }

    @Override
    public InputStream inputStream() {
        return inputStream;
    }

    @Override
    public void setInputStream(InputStream is) {
        inputStream = is;
        if (HttpMethods.isPost(method)) {
            postDecoder = null;
            postParsed = false;
        }
    }
}
