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
package com.alibaba.dubbo.registry.common.route;

import com.alibaba.dubbo.common.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String parsing tools related to interpolation, including Glob mode, Query string, Service URL processing.
 *
 */
public class ParseUtils {

    private static final ConcurrentMap<String, Pattern> REPLACE_PARAMETER_PATTERNS = new ConcurrentHashMap<String, Pattern>();
    public static String METHOD_SPLIT = ",";
    private static Pattern VARIABLE_PATTERN = Pattern.compile(
            "\\$\\s*\\{?\\s*([\\._0-9a-zA-Z]+)\\s*\\}?");
    private static Pattern QUERY_PATTERN = Pattern
            .compile("([&=]?)\\s*([^&=\\s]+)");

    private ParseUtils() {
    }

    /**
     * Execute interpolation (variable insertion).
     *
     * @param expression Expression string containing variables. Variable names in expressions can also be enclosed in <code> {} </ code>。
     * @param params Variable set. Variable names can include <code>. </ Code>, <code> _ </ code> characters.
     * @return After the completion of the interpolation string. Such as: <code> <pre> xxx $ {name} zzz -> xxxjerryzzz </ pre> </ code> (where the variable name = "jerry")
     * @throws IllegalStateException The variables used in the expression string are not in the variable set
     */
    // FIXME Is it reasonable to throw an IllegalStateException??
    public static String interpolate(String expression, Map<String, String> params) {
        if (expression == null || expression.length() == 0) {
            throw new IllegalArgumentException("glob pattern is empty!");
        }
        if (expression.indexOf('$') < 0) {
            return expression;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(expression);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) { // match one by one
            String key = matcher.group(1);
            String value = params == null ? null : params.get(key);
            if (value == null) {
                value = "";
            }
            matcher.appendReplacement(sb, value);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static List<String> interpolate(List<String> expressions, Map<String, String> params) {
        List<String> ret = new ArrayList<String>();

        if (null == expressions || expressions.isEmpty()) {
            return ret;
        }

        for (String expr : expressions) {
            ret.add(interpolate(expr, params));
        }

        return ret;
    }

    /**
     * Match Glob mode. The current implementation only supports <code>*</ code> and supports only one. Does not support <code>?</ Code>.
     * @return For code or value of <code> null </ code>, return <code> false </ code> directly.
     */
    public static boolean isMatchGlobPattern(String pattern, String value) {
        if ("*".equals(pattern))
            return true;
        if ((pattern == null || pattern.length() == 0)
                && (value == null || value.length() == 0))
            return true;
        if ((pattern == null || pattern.length() == 0)
                || (value == null || value.length() == 0))
            return false;

        int i = pattern.lastIndexOf('*');
        // No asterisk found
        if (i == -1) {
            return value.equals(pattern);
        }
        // Asterisk at the end
        else if (i == pattern.length() - 1) {
            return value.startsWith(pattern.substring(0, i));
        }
        // Asterisk at the beginning
        else if (i == 0) {
            return value.endsWith(pattern.substring(i + 1));
        }
        // Asterisk in the middle of the string
        else {
            String prefix = pattern.substring(0, i);
            String suffix = pattern.substring(i + 1);
            return value.startsWith(prefix) && value.endsWith(suffix);
        }
    }

    /**
     * Whether to match Glob mode. Glob mode is the expression to be interpolated. Glob pattern has more than one, as long as matching a pattern, that match is successful.
     *
     * @param patternsNeedInterpolate Multiple Glob patterns to interpolate
         * @param interpolateParams Set of variables used for interpolation
         * @param value Glob mode value
     */
    public static boolean isMatchGlobPatternsNeedInterpolate(
            Collection<String> patternsNeedInterpolate,
            Map<String, String> interpolateParams, String value) {
        if (patternsNeedInterpolate != null && !patternsNeedInterpolate.isEmpty()) {
            for (String patternNeedItp : patternsNeedInterpolate) {
                if (StringUtils.isEmpty(patternNeedItp)) {
                    continue;
                }
                // FIXME ERROR!! The original implementation, here and only the first non-blank pattern comparison, return the corresponding result!
                // FIXME ERROR!! Should be confirmed with Liang Fei!!
                String pattern = interpolate(patternNeedItp, interpolateParams);
                if (isMatchGlobPattern(pattern, value)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the entries in the collection that match the Glob pattern.
     */
    public static Set<String> filterByGlobPattern(String pattern, Collection<String> values) {
        Set<String> ret = new HashSet<String>();
        if (pattern == null || values == null) {
            return ret;
        }

        for (String v : values) {
            if (isMatchGlobPattern(pattern, v)) {
                ret.add(v);
            }
        }
        return ret;
    }

    /**
     * Find the string that matches the Glob pattern. Multiple patterns, as long as a match pattern, it returns this string.
     */
    public static Set<String> filterByGlobPattern(Collection<String> patterns, Collection<String> values) {
        Set<String> ret = new HashSet<String>();
        if (null == patterns || values == null || patterns.isEmpty() || values.isEmpty()) {
            return ret;
        }

        for (String p : patterns) {
            for (String v : values) {
                if (isMatchGlobPattern(p, v)) {
                    ret.add(v);
                }
            }
        }
        return ret;
    }

    /**
     * Whether two Glob patterns have intersection.
     */
    public static boolean hasIntersection(String glob1, String glob2) {
        if (null == glob1 || null == glob2) {
            return false;
        }

        if (glob1.contains("*") && glob2.contains("*")) {
            int index1 = glob1.indexOf("*");
            int index2 = glob2.indexOf("*");

            String s11 = glob1.substring(0, index1);
            String s12 = glob1.substring(index1 + 1, glob1.length());

            String s21 = glob2.substring(0, index2);
            String s22 = glob2.substring(index2 + 1, glob2.length());

            if (!s11.startsWith(s21) && !s21.startsWith(s11)) return false;
            if (!s12.endsWith(s22) && !s22.endsWith(s12)) return false;
            return true;
        } else if (glob1.contains("*")) {
            return isMatchGlobPattern(glob1, glob2);
        } else if (glob2.contains("*")) {
            return isMatchGlobPattern(glob2, glob1);
        } else {
            return glob1.equals(glob2);
        }
    }

    /**
     * Parse Query String into Map. For strings that have only Key, key3 = </ code> is ignored.
     *
     * @param keyPrefix In the output of the Map Key plus a unified prefix.
     * @param query Query String，For example: <code>key1=value1&key2=value2</code>
     * @return When Query String is <code>key1=value1&key2=value2</code>, and prefix is <code>pre.</code>,
     *         then <code>Map{pre.key1=value1, pre.key=value2}</code> will be returned.
     */
    // FIXME Is it reasonable to throw an IllegalStateException??
    public static Map<String, String> parseQuery(String keyPrefix, String query) {
        if (query == null)
            return new HashMap<String, String>();
        if (keyPrefix == null)
            keyPrefix = "";

        Matcher matcher = QUERY_PATTERN.matcher(query);
        Map<String, String> routeQuery = new HashMap<String, String>();
        String key = null;
        while (matcher.find()) { // Match one by one
            String separator = matcher.group(1);
            String content = matcher.group(2);
            if (separator == null || separator.length() == 0
                    || "&".equals(separator)) {
                if (key != null)
                    throw new IllegalStateException("Illegal query string \""
                            + query + "\", The error char '" + separator
                            + "' at index " + matcher.start() + " before \""
                            + content + "\".");
                key = content;
            } else if ("=".equals(separator)) {
                if (key == null)
                    throw new IllegalStateException("Illegal query string \""
                            + query + "\", The error char '" + separator
                            + "' at index " + matcher.start() + " before \""
                            + content + "\".");
                routeQuery.put(keyPrefix + key, content);
                key = null;
            } else {
                if (key == null)
                    throw new IllegalStateException("Illegal query string \""
                            + query + "\", The error char '" + separator
                            + "' at index " + matcher.start() + " before \""
                            + content + "\".");
            }
        }
        /*if (key != null)
        throw new IllegalStateException("Illegal route rule \"" + query
                + "\", The error in the end char: " + key);*/
        return routeQuery;
    }

    public static Map<String, String> parseQuery(String query) {
        return parseQuery("", query);
    }

    /**
     * Replace the value of the url parameter.
     */
    public static String replaceParameter(String query, String key, String value) {
        if (query == null || query.length() == 0) {
            return key + "=" + value;
        }
        if (query.indexOf(key + "=") == -1) {
            return query + "&" + key + "=" + value;
        }
        Pattern pattern = REPLACE_PARAMETER_PATTERNS.get(key);
        if (pattern == null) {
            pattern = Pattern.compile(key.replaceAll("([^(_0-9A-Za-z)])", "\\\\$0") + "=[^&]+");
        }
        Matcher matcher = pattern.matcher(query);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, (key + "=" + value).replace("$", "\\$"));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String appendParamToUri(String uri, String name, String value) {
        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(value)) return uri;
        if (uri.indexOf('?') != -1) {
            uri += "&" + name + "=" + value;
        } else {
            uri += "?" + name + "=" + value;
        }
        return uri;
    }

    public static String appendParamsToUri(String uri, Map<String, String> params) {
        StringBuilder buf = new StringBuilder(uri);
        boolean first = (uri.indexOf('?') < 0);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value))
                continue;
            if (first) {
                buf.append("?");
                first = false;
            } else {
                buf.append("&");
            }
            buf.append(key);
            buf.append("=");
            buf.append(value);
        }
        return buf.toString();
    }

    public static boolean matchEndStarPattern(String value, String pattern) {
        if (!pattern.endsWith("*")) throw new IllegalArgumentException("not end star pattern!");
        String perfix = pattern.substring(0, pattern.length() - 1);
        return value.startsWith(perfix);
    }
}
