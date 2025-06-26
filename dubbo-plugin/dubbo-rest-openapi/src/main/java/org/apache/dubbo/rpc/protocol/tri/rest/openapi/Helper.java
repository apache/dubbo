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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.rest.OpenAPIRequest;
import org.apache.dubbo.remoting.http12.rest.ParamType;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Parameter.In;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([\\w.-]+)}}");

    private Helper() {}

    public static Collection<String> guessHttpMethod(MethodMeta method) {
        String name = method.getMethod().getName();
        for (String[] verbs : VERBS_TABLE) {
            for (int i = 1, len = verbs.length; i < len; i++) {
                if (name.startsWith(verbs[i])) {
                    String httpMethod = verbs[0];
                    if (GET.name().equals(httpMethod)) {
                        for (ParameterMeta parameter : method.getParameters()) {
                            ParamType paramType = parameter.getNamedValueMeta().paramType();
                            if (paramType == null) {
                                if (parameter.isSimple()) {
                                    continue;
                                }
                                return Arrays.asList(GET.name(), POST.name());
                            } else {
                                switch (paramType) {
                                    case Form:
                                    case Part:
                                    case Body:
                                        return Collections.singletonList(POST.name());
                                    default:
                                }
                            }
                        }
                    }
                    return Collections.singletonList(httpMethod);
                }
            }
        }
        return Collections.singletonList(POST.name());
    }

    public static List<String> extractVariables(String path) {
        List<String> variables = null;
        for (int i = 0, len = path.length(), start = 0; i < len; i++) {
            char c = path.charAt(i);
            if (c == '{') {
                start = i + 1;
            } else if (start > 0 && c == '}') {
                if (variables == null) {
                    variables = new ArrayList<>();
                }
                variables.add(path.substring(start, i));
                start = 0;
            }
        }
        return variables;
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

    public static String formatSpecVersion(String version) {
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
        request.setVersion(trim(request.getVersion()));

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

        request.setOpenapi(trim(request.getOpenapi()));
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
        int newLen = p;
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

    public static Server parseServer(String server) {
        String url = null;
        String description = null;
        int equalIndex = server.indexOf('=');
        if (equalIndex > 0) {
            int index = server.indexOf("://");
            if (index == -1 || index > equalIndex) {
                url = trim(server.substring(equalIndex + 1));
                description = trim(server.substring(0, equalIndex));
            }
        }
        if (url == null) {
            url = trim(server);
        }
        return new Server().setDescription(description).setUrl(url);
    }

    public static void setValue(Supplier<String> getter, Consumer<String> setter) {
        String value = trim(getter.get());
        if (value != null) {
            setter.accept(value);
        }
    }

    public static void setBoolValue(Supplier<String> getter, Consumer<Boolean> setter) {
        String value = trim(getter.get());
        if (value != null) {
            setter.accept(StringUtils.toBoolean(value));
        }
    }

    public static void setValue(AnnotationMeta<?> schema, String key, Consumer<String> setter) {
        String value = trim(schema.getString(key));
        if (value != null) {
            setter.accept(value);
        }
    }

    public static void setBoolValue(AnnotationMeta<?> schema, String key, Consumer<Boolean> setter) {
        Boolean value = schema.getBoolean(key);
        if (Boolean.TRUE.equals(value)) {
            setter.accept(true);
        }
    }

    public static String pathToRef(String path) {
        StringBuilder sb = new StringBuilder(path.length() + 16);
        sb.append("#/paths/");
        for (int i = 0, len = path.length(); i < len; i++) {
            char c = path.charAt(i);
            if (c == '/') {
                sb.append('~').append('1');
            } else if (c == '~') {
                sb.append('~').append('0');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static boolean isVersionGreaterOrEqual(String version1, String version2) {
        int i = 0, j = 0, len1 = version1.length(), len2 = version2.length();
        while (i < len1 || j < len2) {
            int num1 = 0;
            while (i < len1) {
                char c = version1.charAt(i);
                if (Character.isDigit(c)) {
                    num1 = num1 * 10 + (c - '0');
                } else if (c == '.' || c == '-' || c == '_') {
                    i++;
                    break;
                }
                i++;
            }

            int num2 = 0;
            while (j < len2) {
                char c = version2.charAt(j);
                if (Character.isDigit(c)) {
                    num2 = num2 * 10 + (c - '0');
                } else if (c == '.' || c == '-' || c == '_') {
                    j++;
                    break;
                }
                j++;
            }

            if (num1 < num2) {
                return false;
            }
        }
        return true;
    }

    public static String render(String text, Function<String, String> fn) {
        if (text == null) {
            return null;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer(text.length());
        while (matcher.find()) {
            String value = fn.apply(matcher.group(1));
            matcher.appendReplacement(result, value == null ? StringUtils.EMPTY_STRING : value);
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
