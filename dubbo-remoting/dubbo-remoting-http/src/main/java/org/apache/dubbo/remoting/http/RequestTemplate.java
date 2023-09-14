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
package org.apache.dubbo.remoting.http;


import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.Invocation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class RequestTemplate implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String CONTENT_ENCODING = "Content-Encoding";
    private static final String CONTENT_LENGTH = "Content-Length";
    public static final String ENCODING_GZIP = "gzip";
    public static final String ENCODING_DEFLATE = "deflate";
    private static final List<String> EMPTY_ARRAYLIST = new ArrayList<>();

    private final Map<String, Collection<String>> queries = new LinkedHashMap<String, Collection<String>>();
    private final Map<String, Collection<String>> headers = new LinkedHashMap<String, Collection<String>>();
    private String httpMethod;
    private String path;
    private String address;
    private Object body;
    private byte[] byteBody = new byte[0];
    private String protocol = "http://";
    private final Invocation invocation;
    private String contextPath = "";
    private Class<?> bodyType;


    public RequestTemplate(Invocation invocation, String httpMethod, String address) {
        this(invocation, httpMethod, address, "");
    }

    public RequestTemplate(Invocation invocation, String httpMethod, String address, String contextPath) {
        this.httpMethod = httpMethod;
        this.address = address;
        this.invocation = invocation;
        this.contextPath = contextPath;
    }

    public String getURL() {
        StringBuilder stringBuilder = new StringBuilder(getProtocol() + address);

        stringBuilder.append(getUri());
        return stringBuilder.toString();
    }

    public String getUri() {
        StringBuilder stringBuilder = new StringBuilder(getContextPath() + path);
        return stringBuilder.append(getQueryString()).toString();
    }

    public String getQueryString() {

        if (queries.isEmpty()) {
            return "";
        }

        StringBuilder queryBuilder = new StringBuilder("?");
        for (String field : queries.keySet()) {

            Collection<String> queryValues = queries.get(field);

            if (queryValues == null || queryValues.isEmpty()) {
                continue;
            }

            for (String value : queryValues) {
                queryBuilder.append('&');
                queryBuilder.append(field);
                if (value == null) {
                    continue;
                }

                queryBuilder.append('=');
                queryBuilder.append(value);
            }
        }

        return queryBuilder.toString().replace("?&", "?");

    }


    public RequestTemplate path(String path) {
        this.path = path;
        return this;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public RequestTemplate httpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public byte[] getSerializedBody() {
        return byteBody;
    }

    public void serializeBody(byte[] body) {
        addHeader(CONTENT_LENGTH, body.length); // must header
        this.byteBody = body;
    }

    public boolean isBodyEmpty() {
        return getUnSerializedBody() == null;
    }

    public RequestTemplate body(Object body,Class bodyType) {
        this.body = body;
        setBodyType(bodyType);
        return this;
    }

    public Object getUnSerializedBody() {
        return body;
    }

    public Map<String, Collection<String>> getAllHeaders() {
        return headers;
    }

    public Collection<String> getHeaders(String name) {
        return headers.get(name);
    }

    public String getHeader(String name) {
        if (headers.containsKey(name)) {

            Collection<String> headers = getHeaders(name);

            if (headers.isEmpty()) {
                return null;
            }
            String[] strings = headers.toArray(new String[0]);
            return strings[0];

        } else {
            return null;
        }
    }

    public Collection<String> getEncodingValues() {
        if (headers.containsKey(CONTENT_ENCODING)) {
            return headers.get(CONTENT_ENCODING);
        }
        return EMPTY_ARRAYLIST;
    }

    public boolean isGzipEncodedRequest() {
        return getEncodingValues().contains(ENCODING_GZIP);
    }

    public boolean isDeflateEncodedRequest() {
        return getEncodingValues().contains(ENCODING_DEFLATE);
    }

    public void addHeader(String key, String value) {
        addValueByKey(key, value, this.headers);
    }

    public void addHeader(String key, Object value) {
        addValueByKey(key, String.valueOf(value), this.headers);
    }

    public void addKeepAliveHeader(int time) {
        addHeader(Constants.KEEP_ALIVE_HEADER, time);
        addHeader(Constants.CONNECTION, Constants.KEEP_ALIVE);
    }

    public void addHeaders(String key, Collection<String> values) {
        Collection<String> header = getHeaders(key);

        if (header == null) {
            header = new HashSet<>();
            this.headers.put(key, header);
        }
        header.addAll(values);
    }


    public void addParam(String key, String value) {
        addValueByKey(key, value, this.queries);
    }

    public void addParam(String key, Object value) {
        addParam(key, String.valueOf(value));
    }

    public Map<String, Collection<String>> getQueries() {
        return queries;
    }

    public Collection<String> getParam(String key) {
        return getQueries().get(key);
    }

    public void addParams(String key, Collection<String> values) {
        Collection<String> params = getParam(key);

        if (params == null) {
            params = new HashSet<>();
            this.queries.put(key, params);
        }
        params.addAll(values);
    }


    public void addValueByKey(String key, String value, Map<String, Collection<String>> maps) {

        if (value == null) {
            return;
        }

        Collection<String> values = null;
        if (!maps.containsKey(key)) {
            values = new HashSet<>();
            maps.put(key, values);
        }
        values = maps.get(key);


        values.add(value);

    }


    public Integer getContentLength() {

        if (!getAllHeaders().containsKey(CONTENT_LENGTH)) {
            return null;
        }

        HashSet<String> strings = (HashSet<String>) getAllHeaders().get(CONTENT_LENGTH);

        return Integer.parseInt(new ArrayList<>(strings).get(0));

    }


    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        addHeader("Host", address);// must header
        this.address = address;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }


    public Invocation getInvocation() {
        return invocation;
    }

    public String getContextPath() {
        if (contextPath == null || contextPath.length() == 0) {
            return "";
        }

        if (contextPath.startsWith("/")) {
            return contextPath;
        } else {
            return "/" + contextPath;
        }

    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public Class<?> getBodyType() {
        return bodyType;
    }

    public void setBodyType(Class<?> bodyType) {
        this.bodyType = bodyType;
    }
}
