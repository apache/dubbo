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

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.MEDIA_TYPE_WILDCARD;
import static org.apache.dubbo.metadata.util.HttpUtils.normalizePath;

/**
 * The metadata class for REST request
 *
 * @since 2.7.5
 */
public class RequestMetadata implements Serializable {

    private static final long serialVersionUID = -240099840085329958L;

    private String method;

    private String path;

    private MultivaluedMap<String, String> params = new MultivaluedHashMap<>();

    private MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

    private Set<String> consumes = new LinkedHashSet<>();

    private Set<String> produces = new LinkedHashSet<>();

    /**
     * Default Constructor
     */
    public RequestMetadata() {
    }

    private static void add(String key, String value,
                            MultivaluedMap<String, String> destination) {
        destination.add(key, value);
    }

    private static <T extends Collection<String>> void addAll(Map<String, T> source,
                                                              MultivaluedMap<String, String> destination) {
        for (Map.Entry<String, T> entry : source.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                add(key, value, destination);
            }
        }
    }

    private static void mediaTypes(MultivaluedMap<String, String> httpHeaders, String headerName,
                                   Collection<String> destination) {
        List<String> value = httpHeaders.get(headerName);
        List<MediaType> mediaTypes = parseMediaTypes(value);
        destination.addAll(toMediaTypeValues(mediaTypes));
    }

    private static List<String> toMediaTypeValues(List<MediaType> mediaTypes) {
        List<String> list = new ArrayList<>(mediaTypes.size());
        for (MediaType mediaType : mediaTypes) {
            list.add(mediaType.toString());
        }
        return list;
    }

    private static List<MediaType> toMediaTypes(Collection<String> mediaTypeValues) {
        if (mediaTypeValues.isEmpty()) {
            return Collections.singletonList(new MediaType(MEDIA_TYPE_WILDCARD, null));
        }
        return parseMediaTypes(new LinkedList<>(mediaTypeValues));
    }

    private static List<MediaType> parseMediaTypes(List<String> mediaTypeValues) {
        return mediaTypeValues.stream()
                .map(MediaType::valueOf)
                .collect(Collectors.toList());
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method.toUpperCase();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = normalizePath(path);
    }

    public Map<String, List<String>> getParams() {
        return params;
    }

    public void setParams(Map<String, List<String>> params) {
        params(params);
    }

    public Map<String, List<String>> getHeaders() {
        return Collections.unmodifiableMap(headers);
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
        return params.keySet();
    }

    public Set<String> getHeaderNames() {
        return headers.keySet();
    }

    public List<MediaType> getConsumeMediaTypes() {
        return toMediaTypes(consumes);
    }

    public List<MediaType> getProduceMediaTypes() {
        return toMediaTypes(produces);
    }

    public String getParameter(String name) {
        return this.params.getFirst(name);
    }

    public String getHeader(String name) {
        return this.headers.getFirst(name);
    }

    public RequestMetadata addParam(String name, String value) {
        add(name, value, this.params);
        return this;
    }

    public RequestMetadata addHeader(String name, String value) {
        add(name, value, this.headers);
        return this;
    }

    private <T extends Collection<String>> RequestMetadata params(Map<String, T> params) {
        addAll(params, this.params);
        return this;
    }

    private <T extends Collection<String>> RequestMetadata headers(Map<String, List<String>> headers) {
        if (headers != null && !headers.isEmpty()) {
            MultivaluedMap httpHeaders = new MultivaluedHashMap();
            // Add all headers
            addAll(headers, httpHeaders);
            // Handles "Content-Type" and "Accept" headers if present
            mediaTypes(httpHeaders, HttpHeaders.CONTENT_TYPE, this.consumes);
            mediaTypes(httpHeaders, HttpHeaders.ACCEPT, this.produces);
            this.headers.putAll(httpHeaders);
        }
        return this;
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
        return Objects.equals(method, that.method) && Objects.equals(path, that.path)
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
