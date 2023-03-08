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
package org.apache.dubbo.metadata.rest;


import org.apache.dubbo.common.utils.CollectionUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.unmodifiableMap;
import static org.apache.dubbo.common.utils.PathUtils.normalize;
import static org.apache.dubbo.common.utils.StringUtils.SLASH;
import static org.apache.dubbo.common.utils.StringUtils.isBlank;

/**
 * The metadata class for REST request
 *
 * @since 2.7.6
 */
public class RequestMetadata implements Serializable {

    private static final long serialVersionUID = -240099840085329958L;

    private String method;

    private String path;

    private Map<String, List<String>> params = new LinkedHashMap<>();

    private Map<String, List<String>> headers = new LinkedHashMap<>();

    private Set<String> consumes = new LinkedHashSet<>();

    private Set<String> produces = new LinkedHashSet<>();


    /**
     * Default Constructor
     */
    public RequestMetadata() {
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method == null ? null : method.toUpperCase();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = normalize(path);

        if (!path.startsWith(SLASH)) {
            this.path = SLASH + path;
        }

    }

    public Map<String, List<String>> getParams() {
        return unmodifiableMap(params);
    }

    public void setParams(Map<String, List<String>> params) {
        params(params);
    }

    private static void add(Map<String, List<String>> multiValueMap, String key, String value) {
        if (isBlank(key)) {
            return;
        }
        List<String> values = get(multiValueMap, key, true);
        values.add(value);
    }

    private static <T extends Collection<String>> void addAll(Map<String, List<String>> multiValueMap,
                                                              Map<String, T> source) {
        for (Map.Entry<String, T> entry : source.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                add(multiValueMap, key, value);
            }
        }
    }

    private static String getFirst(Map<String, List<String>> multiValueMap, String key) {
        List<String> values = get(multiValueMap, key);
        return CollectionUtils.isNotEmpty(values) ? values.get(0) : null;
    }

    private static List<String> get(Map<String, List<String>> multiValueMap, String key) {
        return get(multiValueMap, key, false);
    }

    private static List<String> get(Map<String, List<String>> multiValueMap, String key, boolean createIfAbsent) {
        return createIfAbsent ? multiValueMap.computeIfAbsent(key, k -> new LinkedList<>()) : multiValueMap.get(key);
    }

    public Map<String, List<String>> getHeaders() {
        return unmodifiableMap(headers);
    }

    public void setHeaders(Map<String, List<String>> headers) {
        headers(headers);
    }

    public Set<String> getConsumes() {
        return consumes;
    }

    public void setConsumes(Set<String> consumes) {
        this.consumes = consumes;
    }

    public Set<String> getProduces() {
        return produces;
    }

    public void setProduces(Set<String> produces) {
        this.produces = produces;
    }

    public Set<String> getParamNames() {
        return new HashSet<>(params.keySet());
    }

    public Set<String> getHeaderNames() {
        return new HashSet<>(headers.keySet());
    }

//    public List<MediaType> getConsumeMediaTypes() {
//        return toMediaTypes(consumes);
//    }
//
//    public List<MediaType> getProduceMediaTypes() {
//        return toMediaTypes(produces);
//    }

    public String getParameter(String name) {
        return getFirst(params, name);
    }

    public String getHeader(String name) {
        return getFirst(headers, name);
    }

    public RequestMetadata addParam(String name, String value) {
        add(params, name, value);
        return this;
    }

    public RequestMetadata addHeader(String name, String value) {
        add(headers, name, value);
        return this;
    }

    private <T extends Collection<String>> RequestMetadata params(Map<String, T> params) {
        addAll(this.params, params);
        return this;
    }

    private <T extends Collection<String>> RequestMetadata headers(Map<String, List<String>> headers) {
        if (headers != null && !headers.isEmpty()) {
            Map<String, List<String>> httpHeaders = new LinkedHashMap<>();
            // Add all headers
            addAll(httpHeaders, headers);
            // Handles "Content-Type" and "Accept" headers if present
//            mediaTypes(httpHeaders, HttpHeaders.CONTENT_TYPE, this.consumes);
//            mediaTypes(httpHeaders, HttpHeaders.ACCEPT, this.produces);
            this.headers.putAll(httpHeaders);
        }
        return this;
    }

    public void appendContextPathFromUrl(String contextPathFromUrl) {
        if (contextPathFromUrl == null) {
            return;
        }
        setPath(contextPathFromUrl + path);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RequestMetadata)) {
            return false;
        }
        RequestMetadata that = (RequestMetadata) o;
        return Objects.equals(method, that.method)
            && Objects.equals(path, that.path)
            && Objects.equals(consumes, that.consumes)
            && Objects.equals(produces, that.produces) &&
            // Metadata should not compare the values
            Objects.equals(getParamNames(), that.getParamNames())
            && Objects.equals(getHeaderNames(), that.getHeaderNames());

    }

    @Override
    public int hashCode() {
        // The values of metadata should not use for the hashCode() method
        return Objects.hash(method, path, consumes, produces, getParamNames(),
            getHeaderNames());
    }

    @Override
    public String toString() {
        return "RequestMetadata{" + "method='" + method + '\'' + ", path='" + path + '\''
            + ", params=" + params + ", headers=" + headers + ", consumes=" + consumes
            + ", produces=" + produces + '}';
    }
}
