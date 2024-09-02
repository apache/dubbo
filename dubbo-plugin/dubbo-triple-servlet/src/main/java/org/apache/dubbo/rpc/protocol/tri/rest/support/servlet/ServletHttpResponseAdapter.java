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

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.http12.HttpUtils;
import org.apache.dubbo.remoting.http12.message.DefaultHttpResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RequestUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

public class ServletHttpResponseAdapter extends DefaultHttpResponse implements HttpServletResponse {

    private ServletOutputStream sos;
    private PrintWriter writer;

    @Override
    public void addCookie(Cookie cookie) {
        addCookie(Helper.convert(cookie));
    }

    @Override
    public boolean containsHeader(String name) {
        return hasHeader(name);
    }

    @Override
    public String encodeURL(String url) {
        return RequestUtils.encodeURL(url);
    }

    @Override
    public String encodeRedirectURL(String url) {
        return RequestUtils.encodeURL(url);
    }

    public String encodeUrl(String url) {
        return RequestUtils.encodeURL(url);
    }

    public String encodeRedirectUrl(String url) {
        return RequestUtils.encodeURL(url);
    }

    public void sendRedirect(String location, int sc, boolean clearBuffer) {
        sendRedirect(location);
    }

    @Override
    public void setDateHeader(String name, long date) {
        setHeader(name, new Date(date));
    }

    @Override
    public void addDateHeader(String name, long date) {
        addHeader(name, new Date(date));
    }

    @Override
    public void setHeader(String name, String value) {
        super.setHeader(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
        super.addHeader(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        setHeader(name, String.valueOf(value));
    }

    @Override
    public void addIntHeader(String name, int value) {
        addHeader(name, String.valueOf(value));
    }

    public void setStatus(int sc, String sm) {
        setStatus(sc);
        setBody(sm);
    }

    @Override
    public int getStatus() {
        return status();
    }

    @Override
    public String getHeader(String name) {
        return header(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return headerValues(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headerNames();
    }

    @Override
    public String getCharacterEncoding() {
        return charset();
    }

    @Override
    public String getContentType() {
        return contentType();
    }

    @Override
    public ServletOutputStream getOutputStream() {
        if (sos == null) {
            sos = new HttpOutputStream(outputStream());
        }
        return sos;
    }

    @Override
    public PrintWriter getWriter() {
        if (writer == null) {
            String ce = getCharacterEncoding();
            Charset charset = ce == null ? StandardCharsets.UTF_8 : Charset.forName(ce);
            writer = new PrintWriter(new OutputStreamWriter(outputStream(), charset), true);
        }
        return writer;
    }

    @Override
    public void setCharacterEncoding(String charset) {
        setCharset(charset);
    }

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
    public void flushBuffer() throws IOException {
        //noinspection resource
        OutputStream os = outputStream();
        if (os instanceof BufferedOutputStream) {
            os.flush();
        }
    }

    @Override
    public void setLocale(Locale loc) {
        setLocale(loc.toLanguageTag());
    }

    @Override
    public Locale getLocale() {
        Locale locale = CollectionUtils.first(HttpUtils.parseContentLanguage(locale()));
        return locale == null ? Locale.getDefault() : locale;
    }

    @Override
    public String toString() {
        return "ServletHttpResponseAdapter{" + fieldToString() + '}';
    }

    private static final class HttpOutputStream extends ServletOutputStream {

        private final OutputStream outputStream;

        private HttpOutputStream(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void write(int b) throws IOException {
            outputStream.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            outputStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            outputStream.write(b, off, len);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            throw new UnsupportedOperationException();
        }
    }
}
