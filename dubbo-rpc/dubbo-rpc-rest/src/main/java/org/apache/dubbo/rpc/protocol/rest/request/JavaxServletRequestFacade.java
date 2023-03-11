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

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;

public class JavaxServletRequestFacade extends RequestFacade<HttpServletRequest> {


    public JavaxServletRequestFacade(Object request) {
        super((HttpServletRequest) request);
    }


    public String getHeader(String name) {
        return request.getHeader(name);
    }


    public Enumeration<String> getHeaders(String name) {
        return request.getHeaders(name);
    }


    public Enumeration<String> getHeaderNames() {
        return request.getHeaderNames();
    }


    public int getIntHeader(String name) {
        return request.getIntHeader(name);
    }


    public String getMethod() {
        return request.getMethod();
    }


    public String getPathInfo() {
        return path;
    }

    public String getContextPath() {
        return request.getContextPath();
    }


    public String getQueryString() {
        return request.getQueryString();
    }


    public String getRequestURI() {
        return request.getRequestURI();
    }


    public StringBuffer getRequestURL() {
        return request.getRequestURL();
    }


    public String getServletPath() {
        return request.getServletPath();
    }


    public String getContentType() {
        return request.getContentType();
    }


    public String getParameter(String name) {
        return request.getParameter(name);
    }


    public Enumeration<String> getParameterNames() {
        return request.getParameterNames();
    }


    public String[] getParameterValues(String name) {
        return request.getParameterValues(name);
    }


    public Map<String, String[]> getParameterMap() {
        return request.getParameterMap();
    }

    public int getServerPort() {
        return request.getServerPort();
    }

    public String getRemoteAddr() {
        return request.getRemoteAddr();
    }


    public String getRemoteHost() {
        return request.getRemoteHost();
    }


    public int getRemotePort() {
        return request.getRemotePort();
    }

    public String getLocalAddr() {
        return request.getLocalAddr();
    }


    public int getLocalPort() {
        return request.getRemotePort();
    }


    public InputStream getInputStream() throws IOException {
        return request.getInputStream();
    }

}
