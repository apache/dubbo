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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
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

    private List<ArgInfo> argInfos;

    private Method reflectMethod;

    /**
     *  make a distinction between mvc & resteasy
     */
    private Class codeStyle;

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
            indexToName = new LinkedHashMap<>();
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

    public List<ArgInfo> getArgInfos() {
        if (argInfos == null) {
            argInfos = new ArrayList<>();
        }
        return argInfos;
    }

    public void addArgInfo(ArgInfo argInfo) {
        getArgInfos().add(argInfo);
    }


    public Method getReflectMethod() {
        return reflectMethod;
    }

    public void setReflectMethod(Method reflectMethod) {
        this.reflectMethod = reflectMethod;
    }

    public Class getCodeStyle() {
        return codeStyle;
    }

    public void setCodeStyle(Class codeStyle) {
        this.codeStyle = codeStyle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RestMethodMetadata)) {
            return false;
        }
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
        return "RestMethodMetadata{" +
            "method=" + method +
            ", request=" + request +
            ", urlIndex=" + urlIndex +
            ", bodyIndex=" + bodyIndex +
            ", headerMapIndex=" + headerMapIndex +
            ", bodyType='" + bodyType + '\'' +
            ", indexToName=" + indexToName +
            ", formParams=" + formParams +
            ", indexToEncoded=" + indexToEncoded +
            ", argInfos=" + argInfos +
            ", reflectMethod=" + reflectMethod +
            ", codeStyle=" + codeStyle +
            '}';
    }
}
