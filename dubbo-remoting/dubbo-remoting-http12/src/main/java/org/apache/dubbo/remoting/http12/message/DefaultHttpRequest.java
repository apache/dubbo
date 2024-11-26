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
import org.apache.dubbo.remoting.http12.HttpConstants;
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
import io.netty.handler.codec.http2.Http2Headers.PseudoHeaderName;

public class DefaultHttpRequest implements HttpRequest {

    private final HttpMetadata metadata;
    private final HttpChannel channel;
    private final HttpHeaders headers;

    private String method;
    private String uri;
    private String contentType;
    private String charset;
    private List<HttpCookie> cookies;
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

    public HttpMetadata getMetadata() {
        return metadata;
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
    public String header(CharSequence name) {
        return headers.getFirst(name);
    }

    @Override
    public List<String> headerValues(CharSequence name) {
        return headers.get(name);
    }

    @Override
    public Date dateHeader(CharSequence name) {
        String value = headers.getFirst(name);
        return StringUtils.isEmpty(value) ? null : DateFormatter.parseHttpDate(value);
    }

    @Override
    public boolean hasHeader(CharSequence name) {
        return headers.containsKey(name);
    }

    @Override
    public Collection<String> headerNames() {
        return headers.names();
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public void setHeader(CharSequence name, String value) {
        headers.set(name, value);
    }

    @Override
    public void setHeader(CharSequence name, Date value) {
        headers.set(name, DateFormatter.format(value));
    }

    @Override
    public void setHeader(CharSequence name, List<String> values) {
        headers.set(name, values);
    }

    @Override
    public Collection<HttpCookie> cookies() {
        List<HttpCookie> cookies = this.cookies;
        if (cookies == null) {
            cookies = HttpUtils.decodeCookies(header(HttpHeaderNames.COOKIE.getKey()));
            this.cookies = cookies;
        }
        return cookies;
    }

    @Override
    public HttpCookie cookie(String name) {
        List<HttpCookie> cookies = this.cookies;
        if (cookies == null) {
            cookies = HttpUtils.decodeCookies(header(HttpHeaderNames.COOKIE.getKey()));
            this.cookies = cookies;
        }
        for (int i = 0, size = cookies.size(); i < size; i++) {
            HttpCookie cookie = cookies.get(i);
            if (cookie.name().equals(name)) {
                return cookie;
            }
        }
        return null;
    }

    @Override
    public int contentLength() {
        String value = headers.getFirst(HttpHeaderNames.CONTENT_LENGTH.getKey());
        return value == null ? 0 : Integer.parseInt(value);
    }

    @Override
    public String contentType() {
        String contentType = this.contentType;
        if (contentType == null) {
            contentType = headers.getFirst(HttpHeaderNames.CONTENT_TYPE.getKey());
            contentType = contentType == null ? StringUtils.EMPTY_STRING : contentType.trim();
            this.contentType = contentType;
        }
        return contentType.isEmpty() ? null : contentType;
    }

    @Override
    public void setContentType(String contentType) {
        setContentType0(contentType == null ? StringUtils.EMPTY_STRING : contentType.trim());
        charset = null;
    }

    private void setContentType0(String contentType) {
        this.contentType = contentType;
        headers.set(HttpHeaderNames.CONTENT_TYPE.getKey(), contentType());
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
    public Charset charsetOrDefault() {
        String charset = charset();
        return charset == null ? StandardCharsets.UTF_8 : Charset.forName(charset);
    }

    @Override
    public void setCharset(String charset) {
        String contentType = contentType();
        if (contentType != null) {
            setContentType0(contentType + "; " + HttpUtils.CHARSET_PREFIX + charset);
        }
        this.charset = charset;
    }

    @Override
    public String accept() {
        return headers.getFirst(HttpHeaderNames.ACCEPT.getKey());
    }

    @Override
    public Locale locale() {
        return locales().get(0);
    }

    @Override
    public List<Locale> locales() {
        List<Locale> locales = this.locales;
        if (locales == null) {
            locales = HttpUtils.parseAcceptLanguage(headers.getFirst(HttpHeaderNames.CONTENT_LANGUAGE.getKey()));
            if (locales.isEmpty()) {
                locales = Collections.singletonList(Locale.getDefault());
            }
            this.locales = locales;
        }
        return locales;
    }

    @Override
    public String scheme() {
        String scheme = headers.getFirst(HttpConstants.X_FORWARDED_PROTO);
        if (isHttp2()) {
            scheme = headers.getFirst(PseudoHeaderName.SCHEME.value());
        }
        return scheme == null ? HttpConstants.HTTP : scheme;
    }

    @Override
    public String serverHost() {
        String host = getHost0();
        return host == null ? localHost() + ':' + localPort() : host;
    }

    @Override
    public String serverName() {
        String host = headers.getFirst(HttpConstants.X_FORWARDED_HOST);
        if (host != null) {
            return host;
        }
        host = getHost0();
        if (host != null) {
            int index = host.lastIndexOf(':');
            return index == -1 ? host : host.substring(0, index);
        }
        return localHost();
    }

    @Override
    public int serverPort() {
        String port = headers.getFirst(HttpConstants.X_FORWARDED_PORT);
        if (port != null) {
            return Integer.parseInt(port);
        }
        String host = getHost0();
        if (host != null) {
            int index = host.lastIndexOf(':');
            return index == -1 ? -1 : Integer.parseInt(host.substring(0, index));
        }
        return localPort();
    }

    private String getHost0() {
        return headers.getFirst(isHttp2() ? PseudoHeaderName.AUTHORITY.value() : HttpHeaderNames.HOST.getKey());
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
        return formParameter(name);
    }

    @Override
    public String parameter(String name, String defaultValue) {
        String value = parameter(name);
        return value == null ? defaultValue : value;
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
    public String queryParameter(String name) {
        return CollectionUtils.first(queryParameterValues(name));
    }

    @Override
    public List<String> queryParameterValues(String name) {
        return getDecoder().parameters().get(name);
    }

    @Override
    public Collection<String> queryParameterNames() {
        return getDecoder().parameters().keySet();
    }

    @Override
    public Map<String, List<String>> queryParameters() {
        return getDecoder().parameters();
    }

    @Override
    public String formParameter(String name) {
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
    public List<String> formParameterValues(String name) {
        HttpPostRequestDecoder postDecoder = getPostDecoder();
        if (postDecoder == null) {
            return null;
        }
        List<InterfaceHttpData> items = postDecoder.getBodyHttpDatas(name);
        if (items == null) {
            return null;
        }
        List<String> values = null;
        for (int i = 0, size = items.size(); i < size; i++) {
            InterfaceHttpData item = items.get(i);
            if (item.getHttpDataType() == HttpDataType.Attribute) {
                if (values == null) {
                    values = new ArrayList<>();
                }
                values.add(HttpUtils.readPostValue(item));
            }
        }
        return values == null ? Collections.emptyList() : values;
    }

    @Override
    public Collection<String> formParameterNames() {
        HttpPostRequestDecoder postDecoder = getPostDecoder();
        if (postDecoder == null) {
            return Collections.emptyList();
        }
        List<InterfaceHttpData> items = postDecoder.getBodyHttpDatas();
        if (items == null) {
            return Collections.emptyList();
        }
        Set<String> names = null;
        for (int i = 0, size = items.size(); i < size; i++) {
            InterfaceHttpData item = items.get(i);
            if (item.getHttpDataType() == HttpDataType.Attribute) {
                if (names == null) {
                    names = new LinkedHashSet<>();
                }
                names.add(item.getName());
            }
        }
        return names == null ? Collections.emptyList() : names;
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
        return allNames == null ? Collections.emptyList() : allNames;
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
            String charset = charset();
            if (charset == null) {
                decoder = new QueryStringDecoder(uri);
            } else {
                decoder = new QueryStringDecoder(uri, Charset.forName(charset));
            }
        }
        return decoder;
    }

    private HttpPostRequestDecoder getPostDecoder() {
        HttpPostRequestDecoder postDecoder = this.postDecoder;
        if (postDecoder == null) {
            if (postParsed) {
                return null;
            }
            if (inputStream != null && HttpMethods.supportBody(method)) {
                postDecoder = HttpUtils.createPostRequestDecoder(this, inputStream, charset());
                this.postDecoder = postDecoder;
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
    public void removeAttribute(String name) {
        getAttributes().remove(name);
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
        Map<String, Object> attributes = this.attributes;
        if (attributes == null) {
            attributes = new HashMap<>();
            this.attributes = attributes;
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

    @Override
    public String toString() {
        return "DefaultHttpRequest{" + fieldToString() + '}';
    }

    protected final String fieldToString() {
        return "method='" + method + '\'' + ", uri='" + uri + '\'' + ", contentType='" + contentType() + '\'';
    }
}
