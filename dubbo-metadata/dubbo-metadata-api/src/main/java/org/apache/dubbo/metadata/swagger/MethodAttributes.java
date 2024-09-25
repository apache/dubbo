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
package org.apache.dubbo.metadata.swagger;

import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.metadata.swagger.model.responses.ApiResponse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MethodAttributes {
    /**
     * The Default consumes media type.
     */
    private final String defaultConsumesMediaType;

    /**
     * The Default produces media type.
     */
    private final String defaultProducesMediaType;

    /**
     * The Method consumes.
     */
    private String[] methodProduces = {};

    /**
     * The Method consumes.
     */
    private String[] methodConsumes = {};

    /**
     * The Generic map response.
     */
    private Map<String, ApiResponse> genericMapResponse = new LinkedHashMap<>();

    public String getDefaultConsumesMediaType() {
        return defaultConsumesMediaType;
    }

    public String getDefaultProducesMediaType() {
        return defaultProducesMediaType;
    }

    public String[] getMethodProduces() {
        return methodProduces;
    }

    public void setMethodProduces(String[] methodProduces) {
        this.methodProduces = methodProduces;
    }

    public Map<String, ApiResponse> getGenericMapResponse() {
        return genericMapResponse;
    }

    public void setGenericMapResponse(Map<String, ApiResponse> genericMapResponse) {
        this.genericMapResponse = genericMapResponse;
    }

    public MethodAttributes(String defaultConsumesMediaType, String defaultProducesMediaType, String[] methodProduces) {
        this.defaultConsumesMediaType = defaultConsumesMediaType;
        this.defaultProducesMediaType = defaultProducesMediaType;
        this.methodProduces = methodProduces;
    }

    public void fillMethod() {
        String[] produces = methodProduces;
        String[] consumes = methodConsumes;
        if (ArrayUtils.isNotEmpty(produces)) {
            methodProduces = mergeArrays(methodProduces, produces);
        } else if (ArrayUtils.isEmpty(methodProduces)) {
            methodProduces = new String[] {defaultProducesMediaType};
        }

        if (ArrayUtils.isNotEmpty(consumes)) {
            methodConsumes = mergeArrays(methodConsumes, consumes);
        } else if (ArrayUtils.isEmpty(methodConsumes)) {
            methodConsumes = new String[] {defaultConsumesMediaType};
        }
    }

    private String[] mergeArrays(String[] array1, String[] array2) {
        Set<String> uniqueValues =
                array1 == null ? new HashSet<>() : Arrays.stream(array1).collect(Collectors.toSet());
        uniqueValues.addAll(Arrays.asList(array2));
        return uniqueValues.toArray(new String[0]);
    }
}
