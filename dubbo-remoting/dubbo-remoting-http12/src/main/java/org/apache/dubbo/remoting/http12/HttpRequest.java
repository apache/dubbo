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

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public interface HttpRequest extends RequestMetadata {

    boolean isHttp2();

    String method();

    void setMethod(String method);

    String uri();

    void setUri(String uri);

    String path();

    String rawPath();

    String query();

    String header(String name);

    List<String> headerValues(String name);

    Date dateHeader(String name);

    boolean hasHeader(String name);

    Collection<String> headerNames();

    HttpHeaders headers();

    void setHeader(String name, String value);

    void setHeader(String name, List<String> values);

    void setHeader(String name, Date value);

    Collection<HttpCookie> cookies();

    HttpCookie cookie(String name);

    int contentLength();

    String contentType();

    void setContentType(String contentType);

    String mediaType();

    String charset();

    Charset charsetOrDefault();

    void setCharset(String charset);

    String accept();

    Locale locale();

    List<Locale> locales();

    String scheme();

    String serverHost();

    String serverName();

    int serverPort();

    String remoteHost();

    String remoteAddr();

    int remotePort();

    String localHost();

    String localAddr();

    int localPort();

    String parameter(String name);

    String parameter(String name, String defaultValue);

    List<String> parameterValues(String name);

    String queryParameter(String name);

    List<String> queryParameterValues(String name);

    Collection<String> queryParameterNames();

    Map<String, List<String>> queryParameters();

    String formParameter(String name);

    List<String> formParameterValues(String name);

    Collection<String> formParameterNames();

    boolean hasParameter(String name);

    Collection<String> parameterNames();

    Collection<FileUpload> parts();

    FileUpload part(String name);

    <T> T attribute(String name);

    void removeAttribute(String name);

    void setAttribute(String name, Object value);

    boolean hasAttribute(String name);

    Collection<String> attributeNames();

    Map<String, Object> attributes();

    InputStream inputStream();

    void setInputStream(InputStream is);

    interface FileUpload {

        String name();

        String filename();

        String contentType();

        int size();

        InputStream inputStream();
    }
}
