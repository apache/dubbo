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
package org.apache.dubbo.remoting.http12;

import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.exception.DecodeException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.CookieHeaderNames.SameSite;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

public final class HttpUtils {

    public static final HttpDataFactory DATA_FACTORY = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);

    private HttpUtils() {}

    public static List<HttpCookie> decodeCookies(String value) {
        List<HttpCookie> cookies = new ArrayList<>();
        for (Cookie c : ServerCookieDecoder.LAX.decodeAll(value)) {
            cookies.add(new HttpCookie(c.name(), c.value()));
        }
        return cookies;
    }

    public static List<String> encodeCookies(Collection<HttpCookie> cookies) {
        List<String> encodedCookies = new ArrayList<>(cookies.size());
        for (HttpCookie cookie : cookies) {
            DefaultCookie c = new DefaultCookie(cookie.name(), cookie.value());
            c.setPath(cookie.path());
            c.setDomain(cookie.domain());
            c.setMaxAge(cookie.maxAge());
            c.setSecure(cookie.secure());
            c.setHttpOnly(cookie.httpOnly());
            c.setSameSite(SameSite.valueOf(cookie.sameSite()));
            encodedCookies.add(ServerCookieEncoder.LAX.encode(c));
        }
        return encodedCookies;
    }

    public static List<String> parseAccept(String header) {
        Map<Float, String> mediaTypes = new TreeMap<>();
        if (header == null) {
            return Collections.emptyList();
        }
        for (String item : StringUtils.tokenize(header, ',')) {
            String[] pair = StringUtils.tokenize(item, ';');
            mediaTypes.put(pair.length > 1 ? Float.parseFloat(pair[1]) : 1.0F, pair[0]);
        }
        return new ArrayList<>(mediaTypes.values());
    }

    public static List<Locale> parseAcceptLanguage(String header) {
        Map<Float, Locale> locales = new TreeMap<>();
        if (header == null) {
            return Collections.emptyList();
        }
        for (String item : StringUtils.tokenize(header, ',')) {
            String[] pair = StringUtils.tokenize(item, ';');
            locales.put(pair.length > 1 ? Float.parseFloat(pair[1]) : 1.0F, parseLocale(pair[0]));
        }
        return new ArrayList<>(locales.values());
    }

    public static List<Locale> parseContentLanguage(String header) {
        List<Locale> locales = new ArrayList<>();
        if (header == null) {
            return Collections.emptyList();
        }
        for (String item : StringUtils.tokenize(header, ',')) {
            locales.add(parseLocale(item));
        }
        return locales;
    }

    public static Locale parseLocale(String locale) {
        String[] parts = StringUtils.tokenize(locale, '-', '_');
        switch (parts.length) {
            case 2:
                return new Locale(parts[0], parts[1]);
            case 3:
                return new Locale(parts[0], parts[1], parts[2]);
            default:
                return new Locale(parts[0]);
        }
    }

    @SuppressWarnings("deprecation")
    public static HttpPostRequestDecoder createPostRequestDecoder(
            HttpRequest request, InputStream inputStream, String charset) {
        ByteBuf data;
        boolean canMark = inputStream.markSupported();
        try {
            if (canMark) {
                inputStream.mark(Integer.MAX_VALUE);
            }
            data = Unpooled.wrappedBuffer(StreamUtils.readBytes(inputStream));
        } catch (IOException e) {
            throw new DecodeException("Error while reading post data: " + e.getMessage(), e);
        } finally {
            try {
                if (canMark) {
                    inputStream.reset();
                } else {
                    inputStream.close();
                }
            } catch (IOException ignored) {
            }
        }
        DefaultFullHttpRequest nRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.POST,
                request.uri(),
                data,
                new DefaultHttpHeaders(),
                new DefaultHttpHeaders(false));
        request.headers().forEach(nRequest.headers()::set);
        if (charset == null) {
            return new HttpPostRequestDecoder(DATA_FACTORY, nRequest);
        } else {
            return new HttpPostRequestDecoder(DATA_FACTORY, nRequest, Charset.forName(charset));
        }
    }

    public static String readPostValue(InterfaceHttpData item) {
        try {
            return ((Attribute) item).getValue();
        } catch (IOException e) {
            throw new DecodeException("Error while reading post value: " + e.getMessage(), e);
        }
    }

    public static HttpRequest.FileUpload readUpload(InterfaceHttpData item) {
        return new DefaultFileUploadAdaptee((FileUpload) item);
    }

    private static class DefaultFileUploadAdaptee implements HttpRequest.FileUpload {
        private final FileUpload fu;
        private InputStream inputStream;

        DefaultFileUploadAdaptee(FileUpload fu) {
            this.fu = fu;
        }

        @Override
        public String name() {
            return fu.getName();
        }

        @Override
        public String filename() {
            return fu.getFilename();
        }

        @Override
        public String contentType() {
            return fu.getContentType();
        }

        @Override
        public int size() {
            return (int) fu.length();
        }

        @Override
        public InputStream inputStream() {
            if (inputStream == null) {
                inputStream = new ByteBufInputStream(fu.content());
            }
            return inputStream;
        }
    }
}
