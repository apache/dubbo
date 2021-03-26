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
import java.util.HashMap;
import java.util.List;
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
        List<String> methodList = new ArrayList<>();
        for (Method method : methods) {
            Map<String, Object> methodMap = new HashMap<>();
            methodMap.put("name", method.name());
            methodMap.put("timeout", method.timeout());
            methodMap.put("retries", method.retries());
            methodMap.put("loadbalance", method.loadbalance());
            methodMap.put("async", method.async());
            methodMap.put("actives", method.actives());
            methodMap.put("executes", method.executes());
            methodMap.put("deprecated", method.deprecated());
            methodMap.put("sticky", method.sticky());
            methodMap.put("isReturn", method.isReturn());
            methodMap.put("oninvoke", method.oninvoke());
            methodMap.put("onreturn", method.onreturn());
            methodMap.put("onthrow", method.onthrow());
            methodMap.put("cache", method.cache());
            methodMap.put("validation", method.validation());
            methodMap.put("merger", method.merger());
            methodMap.put("arguments", generateArgumentsString(method.arguments()));
            methodList.add(convertToString(methodMap, "@Method("));
        }
        return methodList.stream().sorted().collect(Collectors.joining(",", "[", "]"));
    }

    private static String generateArgumentsString(Argument[] arguments) {
        if (arguments.length == 0) {
            return null;
        }
        List<String> argumentList = new ArrayList<>();
        for (Argument argument : arguments) {
            Map<String, Object> argMap = new HashMap<>();
            argMap.put("index", argument.index());
            argMap.put("type", argument.type());
            argMap.put("callback", argument.callback());
            argumentList.add(convertToString(argMap, "@Argument("));
        }
        return argumentList.stream().sorted().collect(Collectors.joining(",", "[", "]"));
    }

    private static String convertToString(Map<String, Object> map, String prefix) {
        return map.entrySet().stream()
                .filter(e -> e.getValue() != null && String.valueOf(e.getValue()).length() > 0)
                .map(e -> e.getKey() + "=" + e.getValue())
                .sorted()
                .collect(Collectors.joining(",", prefix, ")"));
    }
}
