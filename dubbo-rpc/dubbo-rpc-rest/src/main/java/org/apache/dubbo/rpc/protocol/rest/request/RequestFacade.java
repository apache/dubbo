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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


public abstract class RequestFacade<T> implements Request {
    protected Map<String, ArrayList<String>> headers = new HashMap<>();
    protected Map<String, ArrayList<String>> parameters = new HashMap<>();

    protected String path;
    protected T request;

    public RequestFacade(T request) {
        this.request = request;
        initHeaders();
        initParameters();
    }

    protected void initHeaders() {

    }


    protected void initParameters() {
        String requestURI = getRequestURI();

        if (requestURI != null && requestURI.contains("?")) {

            String queryString = requestURI.substring(requestURI.indexOf("?") + 1);
            path = requestURI.substring(0, requestURI.indexOf("?"));


            String[] split = queryString.split("&");

            for (String params : split) {

                String[] splits = params.split("=");

                String name = splits[0];

                if (name.length() == 0) {
                    continue;
                }

                ArrayList<String> values = parameters.get(name);

                if (values == null) {
                    values = new ArrayList<>();
                    parameters.put(name, values);
                }

                if (splits.length == 1) {

                    values.add("");

                } else {
                    values.add(splits[1]);
                }
            }
        } else {
            path = requestURI;
        }
    }


    public T getRequest() {
        return request;
    }

    public abstract String getHeader(String name);


    public abstract Enumeration<String> getHeaders(String name);


    public abstract Enumeration<String> getHeaderNames();


    public abstract int getIntHeader(String name);


    public abstract String getMethod();


    public abstract String getPathInfo();

    public abstract String getContextPath();


    public abstract String getQueryString();

    public abstract String getRequestURI();


    public abstract StringBuffer getRequestURL();


    public abstract String getServletPath();

    public abstract String getParameter(String name);

    public abstract Enumeration<String> getParameterNames();

    public abstract String[] getParameterValues(String name);

    public abstract Map<String, String[]> getParameterMap();

    public abstract int getServerPort();

    public abstract String getRemoteAddr();


    public abstract String getRemoteHost();

    public abstract int getRemotePort();

    public abstract String getLocalAddr();


    public abstract int getLocalPort();

    public abstract InputStream getInputStream() throws IOException;


}
