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
package org.apache.dubbo.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class ReferenceAnnotationUtils {
    public static String generateArrayEntryString(Map.Entry<String, Object> entry) {
        String value;
        String[] entryValues = (String[]) entry.getValue();
        if ("parameters".equals(entry.getKey())) {
            // parameters spec is {key1,value1,key2,value2}
            ArrayList<String> kvList = new ArrayList<>();
            for (int i = 0; i < entryValues.length / 2 * 2; i = i + 2) {
                kvList.add(entryValues[i] + "=" + entryValues[i + 1]);
            }
            value = kvList.stream().sorted().collect(Collectors.joining(",", "[", "]"));
        } else {
            //other spec is {string1,string2,string3}
            value = Arrays.stream(entryValues).sorted().collect(Collectors.joining(",", "[", "]"));
        }
        return value;
    }

    public static String generateMethodsString(Method[] methods) {
        if (methods.length == 0) {
            return null;
        }
        StringBuilder methodNameBuilder = new StringBuilder("[");
        for (Method method : methods) {
            methodNameBuilder.append("@Method(");
            trimAppend(methodNameBuilder, "name", method.name());
            trimAppend(methodNameBuilder, "timeout", method.timeout());
            trimAppend(methodNameBuilder, "retries", method.retries());
            trimAppend(methodNameBuilder, "loadbalance", method.loadbalance());
            trimAppend(methodNameBuilder, "async", method.async());
            trimAppend(methodNameBuilder, "actives", method.actives());
            trimAppend(methodNameBuilder, "executes", method.executes());
            trimAppend(methodNameBuilder, "deprecated", method.deprecated());
            trimAppend(methodNameBuilder, "sticky", method.sticky());
            trimAppend(methodNameBuilder, "isReturn", method.isReturn());
            trimAppend(methodNameBuilder, "oninvoke", method.oninvoke());
            trimAppend(methodNameBuilder, "onreturn", method.onreturn());
            trimAppend(methodNameBuilder, "onthrow", method.onthrow());
            trimAppend(methodNameBuilder, "cache", method.cache());
            trimAppend(methodNameBuilder, "validation", method.validation());
            trimAppend(methodNameBuilder, "merger", method.merger());
            trimAppend(methodNameBuilder, "arguments", generateArgumentsString(method.arguments()));
            methodNameBuilder.setCharAt(methodNameBuilder.lastIndexOf(","), ')');
            methodNameBuilder.append(",");
        }
        methodNameBuilder.setCharAt(methodNameBuilder.lastIndexOf(","), ']');
        return methodNameBuilder.toString();
    }

    private static String generateArgumentsString(Argument[] arguments) {
        if (arguments.length == 0) {
            return null;
        }
        StringBuilder argumentStringBuilder = new StringBuilder("[");
        for (Argument argument : arguments) {
            argumentStringBuilder.append("@Argument(");
            trimAppend(argumentStringBuilder, "index", argument.index());
            trimAppend(argumentStringBuilder, "type", argument.type());
            trimAppend(argumentStringBuilder, "callback", argument.callback());
            argumentStringBuilder.setCharAt(argumentStringBuilder.lastIndexOf(","), ')');
            argumentStringBuilder.append(",");
        }
        argumentStringBuilder.setCharAt(argumentStringBuilder.lastIndexOf(","), ']');
        return argumentStringBuilder.toString();
    }

    private static void trimAppend(StringBuilder builder, String name, Object value) {
        if (value != null && String.valueOf(value).length() > 0) {
            builder.append(name).append("=").append(value).append(",");
        }
    }
}
