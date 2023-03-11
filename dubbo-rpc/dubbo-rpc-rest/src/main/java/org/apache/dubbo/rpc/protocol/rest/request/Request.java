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

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;


public interface Request {

    String getHeader(String name);

    Enumeration<String> getHeaders(String name);

    Enumeration<String> getHeaderNames();

    int getIntHeader(String name);

    String getMethod();

    String getPathInfo();

    String getContextPath();

    String getQueryString();


    String getRequestURI();

    StringBuffer getRequestURL();

    String getServletPath();

    String getContentType();


    String getParameter(String name);


    Enumeration<String> getParameterNames();


    String[] getParameterValues(String name);


    Map<String, String[]> getParameterMap();


    int getServerPort();

    String getRemoteAddr();


    String getRemoteHost();


    int getRemotePort();

    String getLocalAddr();


    int getLocalPort();

    InputStream getInputStream() throws IOException;


}
