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
package org.apache.dubbo.rpc.protocol.rest.request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Map;


public interface Request {

    Object getParts() throws Exception;

    Object getPart(String var1) throws Exception;

    Object getCookies();

    long getDateHeader(String name);


    String getHeader(String name);


    Enumeration<String> getHeaders(String name);


    Enumeration<String> getHeaderNames();


    int getIntHeader(String name);


    String getMethod();


    String getPathInfo();


    String getPathTranslated();


    String getContextPath();


    String getQueryString();


    String getRemoteUser();


    boolean isUserInRole(String role);


    String getRequestedSessionId();


    String getRequestURI();


    StringBuffer getRequestURL();


    String getServletPath();


    String changeSessionId();


    boolean isRequestedSessionIdValid();


    boolean isRequestedSessionIdFromCookie();


    boolean isRequestedSessionIdFromURL();


    boolean isRequestedSessionIdFromUrl();

    Object getAttribute(String name);


    Enumeration<String> getAttributeNames();


    String getCharacterEncoding();


    void setCharacterEncoding(String env) throws UnsupportedEncodingException;


    int getContentLength();


    long getContentLengthLong();


    String getContentType();


    String getParameter(String name);


    Enumeration<String> getParameterNames();


    String[] getParameterValues(String name);


    Map<String, String[]> getParameterMap();


    String getProtocol();


    String getScheme();


    String getServerName();


    int getServerPort();


    BufferedReader getReader() throws IOException;


    String getRemoteAddr();


    String getRemoteHost();


    void setAttribute(String name, Object o);


    void removeAttribute(String name);


    boolean isSecure();


    String getRealPath(String path);


    int getRemotePort();


    String getLocalName();


    String getLocalAddr();


    int getLocalPort();

    InputStream getInputStream() throws IOException;


}
