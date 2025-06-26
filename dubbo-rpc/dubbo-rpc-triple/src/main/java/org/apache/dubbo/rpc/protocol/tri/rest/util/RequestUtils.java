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
package org.apache.dubbo.rpc.protocol.tri.rest.util;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageDecoder;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public final class RequestUtils {

    public static final String EMPTY_BODY = "";

    private RequestUtils() {}

    public static boolean isRestRequest(HttpRequest request) {
        return request != null && request.hasAttribute(RestConstants.PATH_ATTRIBUTE);
    }

    public static boolean isMultiPart(HttpRequest request) {
        String contentType = request.contentType();
        return contentType != null && contentType.startsWith(MediaType.MULTIPART_FORM_DATA.getName());
    }

    public static boolean isFormOrMultiPart(HttpRequest request) {
        String contentType = request.contentType();
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith(MediaType.APPLICATION_FROM_URLENCODED.getName())
                || contentType.startsWith(MediaType.MULTIPART_FORM_DATA.getName());
    }

    public static String decodeURL(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new DecodeException(e);
        }
    }

    public static String encodeURL(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new EncodeException(e);
        }
    }

    public static Map<String, List<String>> getParametersMap(HttpRequest request) {
        Collection<String> paramNames = request.parameterNames();
        if (paramNames.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> params = CollectionUtils.newLinkedHashMap(paramNames.size());
        for (String paramName : paramNames) {
            params.put(paramName, request.parameterValues(paramName));
        }
        return params;
    }

    public static Map<String, List<String>> getFormParametersMap(HttpRequest request) {
        Collection<String> paramNames = request.formParameterNames();
        if (paramNames.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> params = CollectionUtils.newLinkedHashMap(paramNames.size());
        for (String paramName : paramNames) {
            params.put(paramName, request.formParameterValues(paramName));
        }
        return params;
    }

    public static Map<String, Object> getParametersMapStartingWith(HttpRequest request, String prefix) {
        Collection<String> paramNames = request.parameterNames();
        if (paramNames.isEmpty()) {
            return Collections.emptyMap();
        }
        if (prefix == null) {
            prefix = StringUtils.EMPTY_STRING;
        }
        Map<String, Object> params = CollectionUtils.newLinkedHashMap(paramNames.size());
        for (String paramName : paramNames) {
            if (prefix.isEmpty() || paramName.startsWith(prefix)) {
                String name = paramName.substring(prefix.length());
                List<String> values = request.parameterValues(paramName);
                if (CollectionUtils.isEmpty(values)) {
                    continue;
                }
                params.put(name, values.size() == 1 ? values.get(0) : values);
            }
        }
        return params;
    }

    public static String getPathVariable(HttpRequest request, String name) {
        Map<String, String> variableMap = request.attribute(RestConstants.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variableMap == null) {
            return null;
        }
        String value = variableMap.get(name);
        if (value == null) {
            return null;
        }
        int index = value.indexOf(';');
        return decodeURL(index == -1 ? value : value.substring(0, index));
    }

    public static Map<String, List<String>> parseMatrixVariables(String matrixVariables) {
        Map<String, List<String>> result = null;
        StringTokenizer pairs = new StringTokenizer(matrixVariables, ";");
        while (pairs.hasMoreTokens()) {
            String pair = pairs.nextToken();
            int index = pair.indexOf('=');
            if (index == -1) {
                if (result == null) {
                    result = new LinkedHashMap<>();
                }
                result.computeIfAbsent(pair, k -> new ArrayList<>()).add(StringUtils.EMPTY_STRING);
                continue;
            }
            String name = pair.substring(0, index);
            if ("jsessionid".equalsIgnoreCase(name)) {
                continue;
            }
            if (result == null) {
                result = new LinkedHashMap<>();
            }
            for (String value : StringUtils.tokenize(pair.substring(index + 1), ',')) {
                result.computeIfAbsent(name, k -> new ArrayList<>()).add(decodeURL(value));
            }
        }
        return result;
    }

    public static List<String> parseMatrixVariableValues(Map<String, String> variableMap, String name) {
        if (variableMap == null) {
            return Collections.emptyList();
        }
        List<String> result = null;
        for (Map.Entry<String, String> entry : variableMap.entrySet()) {
            Map<String, List<String>> matrixVariables = parseMatrixVariables(entry.getValue());
            if (matrixVariables == null) {
                continue;
            }
            List<String> values = matrixVariables.get(name);
            if (values == null) {
                continue;
            }
            if (result == null) {
                result = new ArrayList<>();
            }
            result.addAll(values);
        }
        return result == null ? Collections.emptyList() : result;
    }

    public static Object decodeBody(HttpRequest request, Type type) {
        HttpMessageDecoder decoder = request.attribute(RestConstants.BODY_DECODER_ATTRIBUTE);
        if (decoder == null) {
            return null;
        }
        if (decoder.mediaType().isPureText()) {
            type = String.class;
        }

        InputStream is = request.inputStream();
        try {
            int available = is.available();
            if (available == 0) {
                if (type instanceof Class) {
                    Class<?> clazz = (Class<?>) type;
                    if (clazz == String.class) {
                        return EMPTY_BODY;
                    }
                    if (clazz == byte[].class) {
                        return new byte[0];
                    }
                }
                return null;
            }
        } catch (IOException e) {
            throw new DecodeException("Error reading is", e);
        }

        boolean canMark = is.markSupported();
        try {
            if (canMark) {
                is.mark(Integer.MAX_VALUE);
            }
            return decoder.decode(is, type, request.charsetOrDefault());
        } finally {
            try {
                if (canMark) {
                    is.reset();
                } else {
                    is.close();
                }
            } catch (IOException ignored) {
            }
        }
    }

    public static Object decodeBodyAsObject(HttpRequest request) {
        Object value = request.attribute(RestConstants.BODY_ATTRIBUTE);
        if (value == null) {
            value = decodeBody(request, Object.class);
            if (value != null) {
                request.setAttribute(RestConstants.BODY_ATTRIBUTE, value);
            }
        }
        return value;
    }
}
