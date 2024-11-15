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
package org.apache.dubbo.rpc.protocol.tri.rest.openapi;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.http12.rest.OpenAPIRequest;
import org.apache.dubbo.remoting.http12.rest.ParamType;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathExpression;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathSegment;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Parameter.In;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.remoting.http12.HttpMethods.DELETE;
import static org.apache.dubbo.remoting.http12.HttpMethods.GET;
import static org.apache.dubbo.remoting.http12.HttpMethods.PATCH;
import static org.apache.dubbo.remoting.http12.HttpMethods.POST;
import static org.apache.dubbo.remoting.http12.HttpMethods.PUT;

public final class Helper {

    private static final String[][] VERBS_TABLE = {
        {
            GET.name(),
            "get",
            "load",
            "fetch",
            "read",
            "retrieve",
            "obtain",
            "list",
            "find",
            "query",
            "search",
            "is",
            "are",
            "was",
            "has",
            "check",
            "verify",
            "test",
            "can",
            "should",
            "need",
            "allow",
            "support",
            "accept"
        },
        {PUT.name(), "put", "replace"},
        {PATCH.name(), "patch", "update", "modify", "edit", "change", "set"},
        {DELETE.name(), "delete", "remove", "erase", "destroy", "drop"}
    };

    private Helper() {}

    public static String toPathValue(PathExpression expr) {
        if (expr.isDirect()) {
            return expr.getPath();
        }
        StringBuilder sb = new StringBuilder(expr.getPath().length());
        int varIndex = 1;
        for (PathSegment segment : expr.getSegments()) {
            sb.append('/');
            switch (segment.getType()) {
                case LITERAL:
                    sb.append(segment.getValue());
                    break;
                case WILDCARD_TAIL:
                    List<String> variables = segment.getVariables();
                    if (variables == null) {
                        sb.append("{path}");
                    } else {
                        sb.append('{').append(variables.get(0)).append('}');
                    }
                    break;
                case VARIABLE:
                    sb.append('{');
                    String value = segment.getValue();
                    if (value.isEmpty()) {
                        sb.append("var").append(varIndex++);
                    } else {
                        sb.append(value);
                    }
                    sb.append('}');
                    break;
                case PATTERN:
                case PATTERN_MULTI:
                    sb.append('{').append("var").append(varIndex++).append('}');
                    break;
                default:
                    break;
            }
        }
        return sb.toString();
    }

    public static Collection<String> guessHttpMethod(MethodMeta method) {
        String name = method.getMethod().getName();
        for (String[] verbs : VERBS_TABLE) {
            for (int i = 1, len = verbs.length; i < len; i++) {
                if (name.startsWith(verbs[i])) {
                    return Collections.singletonList(verbs[0]);
                }
            }
        }
        return Collections.singletonList(POST.name());
    }

    public static In toIn(ParamType paramType) {
        switch (paramType) {
            case PathVariable:
                return In.PATH;
            case Param:
                return In.QUERY;
            case Header:
                return In.HEADER;
            case Cookie:
                return In.COOKIE;
            default:
                return null;
        }
    }

    public static String formatVersion(String version) {
        if (version == null) {
            return null;
        }
        if (version.startsWith("3.1")) {
            return Constants.VERSION_31;
        }
        return Constants.VERSION_30;
    }

    public static OpenAPIRequest formatRequest(OpenAPIRequest request) {
        if (request == null) {
            return new OpenAPIRequest();
        }
        request.setGroup(trim(request.getGroup()));

        String[] tag = trim(request.getTag());
        if (tag != null) {
            Arrays.sort(tag);
        }
        request.setTag(tag);

        String[] service = trim(request.getService());
        if (service != null) {
            Arrays.sort(service);
        }
        request.setService(service);

        request.setVersion(trim(request.getVersion()));
        request.setFormat(trim(request.getFormat()));
        return request;
    }

    public static String parseFormat(String contentType) {
        if (contentType != null) {
            int index = contentType.indexOf('/');
            if (index > 0 && contentType.indexOf("htm", index) == -1) {
                return contentType.substring(index + 1);
            }
        }
        return "json";
    }

    public static String trim(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        str = str.trim();
        return str.isEmpty() ? null : str;
    }

    public static String[] trim(String[] array) {
        if (array == null) {
            return null;
        }
        int len = array.length;
        if (len == 0) {
            return null;
        }
        int p = 0;
        for (int i = 0; i < len; i++) {
            String value = trim(array[i]);
            if (value != null) {
                array[p++] = value;
            }
        }
        int newLen = p + 1;
        return newLen == len ? array : Arrays.copyOf(array, newLen);
    }

    public static Map<String, String> toProperties(String[] array) {
        if (array == null) {
            return Collections.emptyMap();
        }
        int len = array.length;
        if (len == 0) {
            return Collections.emptyMap();
        }
        Map<String, String> properties = CollectionUtils.newLinkedHashMap(len);
        for (String item : array) {
            int index = item.indexOf('=');
            if (index > 0) {
                properties.put(trim(item.substring(0, index)), trim(item.substring(index + 1)));
            } else {
                properties.put(trim(item), null);
            }
        }
        return properties;
    }
}
