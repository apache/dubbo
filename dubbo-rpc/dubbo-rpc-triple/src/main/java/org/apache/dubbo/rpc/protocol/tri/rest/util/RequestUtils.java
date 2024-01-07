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

import java.io.UnsupportedEncodingException;
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

    private RequestUtils() {}

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

    public static Map<?, ?> getParametersMap(HttpRequest request) {
        Collection<String> paramNames = request.parameterNames();
        if (paramNames.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> mapValue = CollectionUtils.newLinkedHashMap(paramNames.size());
        for (String paramName : paramNames) {
            mapValue.put(paramName, request.parameterValues(paramName));
        }
        return mapValue;
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
}
