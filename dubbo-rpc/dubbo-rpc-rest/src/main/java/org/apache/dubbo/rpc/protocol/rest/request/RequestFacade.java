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


import org.apache.dubbo.common.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * request facade for different request
 *
 * @param <T>
 */
public abstract class RequestFacade<T> {
    protected Map<String, ArrayList<String>> headers = new HashMap<>();
    protected Map<String, ArrayList<String>> parameters = new HashMap<>();

    protected String path;
    protected T request;
    protected byte[] body = new byte[0];

    public RequestFacade(T request) {
        this.request = request;
        initHeaders();
        initParameters();
        parseBody();
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
                // key a=  ;value b==c
                int index = params.indexOf("=");
                if (index <= 0) {
                    continue;
                }

                String name = params.substring(0, index);
                String value = params.substring(index + 1);
                if (!StringUtils.isEmpty(name)) {
                    ArrayList<String> values = parameters.get(name);

                    if (values == null) {
                        values = new ArrayList<>();
                        parameters.put(name, values);
                    }
                    values.add(value);

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

    public abstract String getMethod();


    public abstract String getPath();

    public abstract String getContextPath();

    public abstract String getRequestURI();

    public abstract String getParameter(String name);

    public abstract Enumeration<String> getParameterNames();

    public abstract String[] getParameterValues(String name);

    public abstract Map<String, String[]> getParameterMap();

    public abstract String getRemoteAddr();

    public abstract String getRemoteHost();

    public abstract int getRemotePort();

    public abstract String getLocalAddr();

    public abstract String getLocalHost();

    public abstract int getLocalPort();

    public abstract byte[] getInputStream() throws IOException;

    protected abstract void parseBody();


}
