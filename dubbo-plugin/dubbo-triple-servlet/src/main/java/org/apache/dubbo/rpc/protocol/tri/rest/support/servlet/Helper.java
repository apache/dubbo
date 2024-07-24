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

import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.http12.HttpCookie;
import org.apache.dubbo.remoting.http12.HttpRequest.FileUpload;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.RestException;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.Part;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

final class Helper {

    private Helper() {}

    static javax.servlet.http.Cookie[] convertCookies(Collection<HttpCookie> hCookies) {
        javax.servlet.http.Cookie[] cookies = new javax.servlet.http.Cookie[hCookies.size()];
        int i = 0;
        for (HttpCookie cookie : hCookies) {
            cookies[i++] = convert(cookie);
        }
        return cookies;
    }

    static javax.servlet.http.Cookie convert(HttpCookie hCookie) {
        javax.servlet.http.Cookie cookie = new javax.servlet.http.Cookie(hCookie.name(), hCookie.value());
        if (hCookie.domain() != null) {
            cookie.setDomain(hCookie.domain());
        }
        cookie.setMaxAge((int) hCookie.maxAge());
        cookie.setHttpOnly(hCookie.httpOnly());
        cookie.setPath(hCookie.path());
        cookie.setSecure(hCookie.secure());
        return cookie;
    }

    static HttpCookie convert(javax.servlet.http.Cookie sCookie) {
        HttpCookie cookie = new HttpCookie(sCookie.getName(), sCookie.getValue());
        cookie.setDomain(sCookie.getDomain());
        cookie.setMaxAge(sCookie.getMaxAge());
        cookie.setHttpOnly(sCookie.isHttpOnly());
        cookie.setPath(sCookie.getPath());
        cookie.setSecure(sCookie.getSecure());
        return cookie;
    }

    public static Part convert(FileUpload part) {
        return new FileUploadPart(part);
    }

    public static Collection<Part> convertParts(Collection<FileUpload> parts) {
        if (CollectionUtils.isEmpty(parts)) {
            return Collections.emptyList();
        }
        List<Part> result = new ArrayList<>(parts.size());
        for (FileUpload part : parts) {
            result.add(convert(part));
        }
        return result;
    }

    public static final class FileUploadPart implements Part {

        private final FileUpload fileUpload;

        public FileUploadPart(FileUpload fileUpload) {
            this.fileUpload = fileUpload;
        }

        @Override
        public InputStream getInputStream() {
            return fileUpload.inputStream();
        }

        @Override
        public String getContentType() {
            return fileUpload.contentType();
        }

        @Override
        public String getName() {
            return fileUpload.name();
        }

        @Override
        public String getSubmittedFileName() {
            return fileUpload.filename();
        }

        @Override
        public long getSize() {
            return fileUpload.size();
        }

        @Override
        public void write(String fileName) {
            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                StreamUtils.copy(fileUpload.inputStream(), fos);
            } catch (IOException e) {
                throw new RestException(e);
            }
        }

        @Override
        public void delete() {}

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
            return Collections.emptyList();
        }
    }

    public static ServletContext createDummyServletContext(FrameworkModel frameworkModel) {
        return new DummyServletContext(frameworkModel);
    }

    public static FilterConfig createDummyFilterConfig(
            String filterName, FrameworkModel frameworkModel, ServletContext servletContext) {
        return new DummyFilterConfig(filterName, frameworkModel, servletContext);
    }
}
