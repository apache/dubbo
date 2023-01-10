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

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;


public class JakartaServletRequestFacade extends RequestFacade<HttpServletRequest> {


    public JakartaServletRequestFacade(Object request) {
        super((HttpServletRequest) request);
    }

    @Override
    public Object getParts() throws Exception {
        return request.getParts();
    }

    @Override
    public Object getPart(String var1) throws Exception {
        return request.getPart(var1);
    }

    @Override
    public Object getCookies() {
        return request.getCookies();
    }


    public long getDateHeader(String name) {
        return request.getDateHeader(name);
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
        return request.getPathInfo();
    }


    public String getPathTranslated() {
        return request.getPathTranslated();
    }


    public String getContextPath() {
        return request.getContextPath();
    }


    public String getQueryString() {
        return request.getQueryString();
    }


    public String getRemoteUser() {
        return request.getRemoteUser();
    }


    public boolean isUserInRole(String role) {
        return request.isUserInRole(role);
    }


    public String getRequestedSessionId() {
        return request.getRequestedSessionId();
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


    public String changeSessionId() {
        return request.changeSessionId();
    }


    public boolean isRequestedSessionIdValid() {
        return request.isRequestedSessionIdValid();
    }


    public boolean isRequestedSessionIdFromCookie() {
        return request.isRequestedSessionIdFromCookie();
    }


    public boolean isRequestedSessionIdFromURL() {
        return isRequestedSessionIdFromUrl();
    }


    public boolean isRequestedSessionIdFromUrl() {
        return request.isRequestedSessionIdFromURL();
    }


    public Object getAttribute(String name) {
        return request.getAttribute(name);
    }


    public Enumeration<String> getAttributeNames() {
        return request.getAttributeNames();
    }


    public String getCharacterEncoding() {
        return request.getCharacterEncoding();
    }


    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {

        request.setCharacterEncoding(env);
    }


    public int getContentLength() {
        return request.getContentLength();
    }


    public long getContentLengthLong() {
        return request.getContentLengthLong();
    }


    public String getContentType() {
        return request.getContentType();
    }


    public InputStream getInputStream() throws IOException {
        return request.getInputStream();
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


    public String getProtocol() {
        return request.getProtocol();
    }


    public String getScheme() {
        return request.getScheme();
    }


    public String getServerName() {
        return request.getServerName();
    }


    public int getServerPort() {
        return request.getServerPort();
    }


    public BufferedReader getReader() throws IOException {
        return request.getReader();
    }


    public String getRemoteAddr() {
        return getRemoteAddr();
    }


    public String getRemoteHost() {
        return getRemoteHost();
    }


    public void setAttribute(String name, Object o) {

        request.setAttribute(name, o);
    }


    public void removeAttribute(String name) {
        request.removeAttribute(name);
    }


    public Locale getLocale() {
        return request.getLocale();
    }


    public Enumeration<Locale> getLocales() {
        return request.getLocales();
    }


    public boolean isSecure() {
        return request.isSecure();
    }


    public RequestDispatcher getRequestDispatcher(String path) {
        return getRequestDispatcher(path);
    }


    public String getRealPath(String path) {
        return request.getRealPath(path);
    }


    public int getRemotePort() {
        return request.getRemotePort();
    }


    public String getLocalName() {
        return request.getLocalName();
    }


    public String getLocalAddr() {
        return request.getLocalAddr();
    }


    public int getLocalPort() {
        return request.getRemotePort();
    }


    public ServletContext getServletContext() {
        return request.getServletContext();
    }


    public DispatcherType getDispatcherType() {
        return request.getDispatcherType();
    }
}
