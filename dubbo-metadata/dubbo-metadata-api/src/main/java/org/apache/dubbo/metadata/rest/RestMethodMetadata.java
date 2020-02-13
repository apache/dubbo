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

import org.apache.dubbo.metadata.definition.model.MethodDefinition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyList;

/**
 * The metadata class for {@link RequestMetadata HTTP(REST) request} and
 * its binding {@link MethodDefinition method definition}
 *
 * @since 2.7.6
 */
public class RestMethodMetadata implements Serializable {

    private static final long serialVersionUID = 2935252016200830694L;

    private MethodDefinition method;

    private RequestMetadata request;

    private Integer urlIndex;

    private Integer bodyIndex;

    private Integer headerMapIndex;

    private String bodyType;

    private Map<Integer, Collection<String>> indexToName;

    private List<String> formParams;

    private Map<Integer, Boolean> indexToEncoded;

    public MethodDefinition getMethod() {
        if (method == null) {
            method = new MethodDefinition();
        }
        return method;
    }

    public void setMethod(MethodDefinition method) {
        this.method = method;
    }

    public RequestMetadata getRequest() {
        if (request == null) {
            request = new RequestMetadata();
        }
        return request;
    }

    public void setRequest(RequestMetadata request) {
        this.request = request;
    }

    public Integer getUrlIndex() {
        return urlIndex;
    }

    public void setUrlIndex(Integer urlIndex) {
        this.urlIndex = urlIndex;
    }

    public Integer getBodyIndex() {
        return bodyIndex;
    }

    public void setBodyIndex(Integer bodyIndex) {
        this.bodyIndex = bodyIndex;
    }

    public Integer getHeaderMapIndex() {
        return headerMapIndex;
    }

    public void setHeaderMapIndex(Integer headerMapIndex) {
        this.headerMapIndex = headerMapIndex;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    public Map<Integer, Collection<String>> getIndexToName() {
        if (indexToName == null) {
            indexToName = new HashMap<>();
        }
        return indexToName;
    }

    public void setIndexToName(Map<Integer, Collection<String>> indexToName) {
        this.indexToName = indexToName;
    }

    public void addIndexToName(Integer index, String name) {
        if (index == null) {
            return;
        }

        if (name.startsWith("arg") && name.endsWith(index.toString())) {
            // Ignore this value because of the Java byte-code without the metadata of method parameters
            return;
        }

        Map<Integer, Collection<String>> indexToName = getIndexToName();
        Collection<String> parameterNames = indexToName.computeIfAbsent(index, i -> new ArrayList<>(1));
        parameterNames.add(name);
    }

    public boolean hasIndexedName(Integer index, String name) {
        Map<Integer, Collection<String>> indexToName = getIndexToName();
        return indexToName.getOrDefault(index, emptyList()).contains(name);
    }

    public List<String> getFormParams() {
        return formParams;
    }

    public void setFormParams(List<String> formParams) {
        this.formParams = formParams;
    }

    public Map<Integer, Boolean> getIndexToEncoded() {
        return indexToEncoded;
    }

    public void setIndexToEncoded(Map<Integer, Boolean> indexToEncoded) {
        this.indexToEncoded = indexToEncoded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RestMethodMetadata)) return false;
        RestMethodMetadata that = (RestMethodMetadata) o;
        return Objects.equals(getMethod(), that.getMethod()) &&
                Objects.equals(getRequest(), that.getRequest()) &&
                Objects.equals(getUrlIndex(), that.getUrlIndex()) &&
                Objects.equals(getBodyIndex(), that.getBodyIndex()) &&
                Objects.equals(getHeaderMapIndex(), that.getHeaderMapIndex()) &&
                Objects.equals(getBodyType(), that.getBodyType()) &&
                Objects.equals(getFormParams(), that.getFormParams()) &&
                Objects.equals(getIndexToEncoded(), that.getIndexToEncoded());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMethod(), getRequest(), getUrlIndex(), getBodyIndex(), getHeaderMapIndex(), getBodyType(), getFormParams(), getIndexToEncoded());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RestMethodMetadata{");
        sb.append("method=").append(method);
        sb.append(", request=").append(request);
        sb.append(", urlIndex=").append(urlIndex);
        sb.append(", bodyIndex=").append(bodyIndex);
        sb.append(", headerMapIndex=").append(headerMapIndex);
        sb.append(", bodyType='").append(bodyType).append('\'');
        sb.append(", indexToName=").append(indexToName);
        sb.append(", formParams=").append(formParams);
        sb.append(", indexToEncoded=").append(indexToEncoded);
        sb.append('}');
        return sb.toString();
    }
}
