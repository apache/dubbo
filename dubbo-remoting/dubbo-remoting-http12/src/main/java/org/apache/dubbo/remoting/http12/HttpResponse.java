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

import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface HttpResponse {

    int status();

    void setStatus(int status);

    String header(CharSequence name);

    Date dateHeader(CharSequence name);

    List<String> headerValues(CharSequence name);

    boolean hasHeader(CharSequence name);

    Collection<String> headerNames();

    HttpHeaders headers();

    void addHeader(CharSequence name, String value);

    void addHeader(CharSequence name, Date value);

    void setHeader(CharSequence name, String value);

    void setHeader(CharSequence name, Date value);

    void setHeader(CharSequence name, List<String> value);

    void addCookie(HttpCookie cookie);

    String contentType();

    void setContentType(String contentType);

    String mediaType();

    String charset();

    void setCharset(String charset);

    String locale();

    void setLocale(String locale);

    Object body();

    void setBody(Object body);

    OutputStream outputStream();

    void setOutputStream(OutputStream os);

    void sendRedirect(String location);

    void sendError(int status);

    void sendError(int status, String message);

    boolean isEmpty();

    boolean isContentEmpty();

    boolean isCommitted();

    void commit();

    void setCommitted(boolean committed);

    void reset();

    void resetBuffer();

    HttpResult<Object> toHttpResult();
}
