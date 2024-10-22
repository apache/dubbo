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

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.metadata.swagger.model.responses.ApiResponse;
import org.apache.dubbo.metadata.swagger.model.responses.ApiResponses;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

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
     * The Headers.
     */
    private final LinkedHashMap<String, String> headers = new LinkedHashMap<>();

    /**
     * The Locale.
     */
    private final Locale locale;

    /**
     * The Method overloaded.
     */
    private boolean methodOverloaded;

    /**
     * The Class produces.
     */
    private String[] classProduces;

    /**
     * The Class consumes.
     */
    private String[] classConsumes;

    /**
     * The Method produces.
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

    /**
     * The Use return type schema.
     */
    private boolean useReturnTypeSchema;

    /**
     * Instantiates a new Method attributes.
     *
     * @param methodProducesNew        the method produces new
     * @param defaultConsumesMediaType the default consumes media type
     * @param defaultProducesMediaType the default produces media type
     * @param genericMapResponse       the generic map response
     * @param locale                   the locale
     */
    public MethodAttributes(
            String[] methodProducesNew,
            String defaultConsumesMediaType,
            String defaultProducesMediaType,
            Map<String, ApiResponse> genericMapResponse,
            Locale locale) {
        this.methodProduces = methodProducesNew;
        this.defaultConsumesMediaType = defaultConsumesMediaType;
        this.defaultProducesMediaType = defaultProducesMediaType;
        this.genericMapResponse = genericMapResponse;
        this.locale = locale;
    }

    /**
     * Instantiates a new Method attributes.
     *
     * @param defaultConsumesMediaType the default consumes media type
     * @param defaultProducesMediaType the default produces media type
     * @param locale                   the locale
     */
    public MethodAttributes(String defaultConsumesMediaType, String defaultProducesMediaType, Locale locale) {
        this.defaultConsumesMediaType = defaultConsumesMediaType;
        this.defaultProducesMediaType = defaultProducesMediaType;
        this.locale = locale;
    }

    /**
     * Instantiates a new Method attributes.
     *
     * @param defaultConsumesMediaType the default consumes media type
     * @param defaultProducesMediaType the default produces media type
     * @param methodConsumes           the method consumes
     * @param methodProduces           the method produces
     * @param headers                  the headers
     * @param locale                   the locale
     */
    public MethodAttributes(
            String defaultConsumesMediaType,
            String defaultProducesMediaType,
            String[] methodConsumes,
            String[] methodProduces,
            String[] headers,
            Locale locale) {
        this.defaultConsumesMediaType = defaultConsumesMediaType;
        this.defaultProducesMediaType = defaultProducesMediaType;
        this.methodProduces = methodProduces;
        this.methodConsumes = methodConsumes;
        this.locale = locale;
        setHeaders(headers);
    }

    /**
     * Get class produces string [ ].
     *
     * @return the string [ ]
     */
    public String[] getClassProduces() {
        return classProduces;
    }

    /**
     * Sets class produces.
     *
     * @param classProduces the class produces
     */
    public void setClassProduces(String[] classProduces) {
        this.classProduces = classProduces;
    }

    /**
     * Get class consumes string [ ].
     *
     * @return the string [ ]
     */
    public String[] getClassConsumes() {
        return classConsumes;
    }

    /**
     * Sets class consumes.
     *
     * @param classConsumes the class consumes
     */
    public void setClassConsumes(String[] classConsumes) {
        this.classConsumes = classConsumes;
    }

    /**
     * Get method produces string [ ].
     *
     * @return the string [ ]
     */
    public String[] getMethodProduces() {
        return methodProduces;
    }

    /**
     * Get method consumes string [ ].
     *
     * @return the string [ ]
     */
    public String[] getMethodConsumes() {
        return methodConsumes;
    }

    /**
     * Fill methods.
     *
     * @param produces the produces
     * @param consumes the consumes
     * @param headers  the headers
     */
    private void fillMethods(String[] produces, String[] consumes, String[] headers) {
        if (ArrayUtils.isNotEmpty(produces)) {
            methodProduces = mergeArrays(methodProduces, produces);
        } else if (ArrayUtils.isNotEmpty(classProduces)) {
            methodProduces = mergeArrays(methodProduces, classProduces);
        } else if (ArrayUtils.isEmpty(methodProduces)) {
            methodProduces = new String[] {defaultProducesMediaType};
        }

        if (ArrayUtils.isNotEmpty(consumes)) {
            methodConsumes = mergeArrays(methodConsumes, consumes);
        } else if (ArrayUtils.isNotEmpty(classConsumes)) {
            methodConsumes = mergeArrays(methodConsumes, classConsumes);
        } else if (ArrayUtils.isEmpty(methodConsumes)) {
            methodConsumes = new String[] {defaultConsumesMediaType};
        }
        setHeaders(headers);
    }

    /**
     * Merge string arrays into one array with unique values
     *
     * @param array1 the array1
     * @param array2 the array2
     * @return the string [ ]
     */
    private String[] mergeArrays(@Nullable String[] array1, String[] array2) {
        Set<String> uniqueValues = array1 == null
                ? new LinkedHashSet<>()
                : Arrays.stream(array1).collect(Collectors.toCollection(LinkedHashSet::new));
        uniqueValues.addAll(Arrays.asList(array2));
        return uniqueValues.toArray(new String[0]);
    }

    /**
     * Is method overloaded boolean.
     *
     * @return the boolean
     */
    public boolean isMethodOverloaded() {
        return methodOverloaded;
    }

    /**
     * Sets method overloaded.
     *
     * @param overloaded the overloaded
     */
    public void setMethodOverloaded(boolean overloaded) {
        methodOverloaded = overloaded;
    }

    /**
     * Gets headers.
     *
     * @return the headers
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Sets headers.
     *
     * @param headers the headers
     */
    private void setHeaders(String[] headers) {
        if (ArrayUtils.isNotEmpty(headers))
            for (String header : headers) {
                if (!header.contains("!=")) {
                    String[] keyValueHeader = header.split("=");
                    String headerValue = keyValueHeader.length > 1 ? keyValueHeader[1] : "";
                    this.headers.put(keyValueHeader[0], headerValue);
                } else {
                    String[] keyValueHeader = header.split("!=");
                    if (!this.headers.containsKey(keyValueHeader[0]))
                        this.headers.put(keyValueHeader[0], StringUtils.EMPTY);
                }
            }
    }

    /**
     * Calculate generic map response api responses.
     *
     * @param genericMapResponse the generic map response
     * @return the api responses
     */
    public ApiResponses calculateGenericMapResponse(Map<String, ApiResponse> genericMapResponse) {
        ApiResponses apiResponses = new ApiResponses();
        genericMapResponse.forEach(apiResponses::addApiResponse);
        this.genericMapResponse = genericMapResponse;
        return apiResponses;
    }

    /**
     * Gets generic map response.
     *
     * @return the generic map response
     */
    public Map<String, ApiResponse> getGenericMapResponse() {
        return genericMapResponse;
    }

    /**
     * Gets locale.
     *
     * @return the locale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Is use return type schema boolean.
     *
     * @return the boolean
     */
    public boolean isUseReturnTypeSchema() {
        return useReturnTypeSchema;
    }

    /**
     * Sets use return type schema.
     *
     * @param useReturnTypeSchema the use return type schema
     */
    public void setUseReturnTypeSchema(boolean useReturnTypeSchema) {
        this.useReturnTypeSchema = useReturnTypeSchema;
    }

    public void calculateConsumesProduces(Method method) {
        fillMethods(methodProduces, methodConsumes, null);
    }
}
