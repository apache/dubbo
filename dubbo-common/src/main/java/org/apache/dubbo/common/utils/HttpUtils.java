/*
 * Copyright (C) 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.common.utils;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static org.apache.dubbo.common.utils.StringUtils.AND;
import static org.apache.dubbo.common.utils.StringUtils.EQUAL;
import static org.apache.dubbo.common.utils.StringUtils.QUESTION_MASK;
import static org.apache.dubbo.common.utils.StringUtils.SLASH;
import static org.apache.dubbo.common.utils.StringUtils.isEmpty;
import static org.apache.dubbo.common.utils.StringUtils.replace;

/**
 * Http Utilities class
 *
 * @since 2.7.6
 */
public abstract class HttpUtils {

    private static final String UTF_8 = "UTF-8";

    /**
     * HTTP GET method.
     */
    public static final String GET = "GET";
    /**
     * HTTP POST method.
     */
    public static final String POST = "POST";
    /**
     * HTTP PUT method.
     */
    public static final String PUT = "PUT";
    /**
     * HTTP DELETE method.
     */
    public static final String DELETE = "DELETE";
    /**
     * HTTP HEAD method.
     */
    public static final String HEAD = "HEAD";
    /**
     * HTTP OPTIONS method.
     */
    public static final String OPTIONS = "OPTIONS";

    /**
     * The HTTP methods to support
     */
    public static final Set<String> HTTP_METHODS = unmodifiableSet(new LinkedHashSet<>(asList(
            GET, POST, POST, PUT, DELETE, HEAD, OPTIONS
    )));


    public static String buildPath(String rootPath, String... subPaths) {

        Set<String> paths = new LinkedHashSet<>();
        paths.add(rootPath);
        paths.addAll(asList(subPaths));

        return normalizePath(paths.stream()
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.joining(SLASH)));
    }

    /**
     * Normalize path:
     * <ol>
     * <li>To remove query string if presents</li>
     * <li>To remove duplicated slash("/") if exists</li>
     * </ol>
     *
     * @param path path to be normalized
     * @return a normalized path if required
     */
    public static String normalizePath(String path) {
        if (isEmpty(path)) {
            return SLASH;
        }
        String normalizedPath = path;
        int index = normalizedPath.indexOf(QUESTION_MASK);
        if (index > -1) {
            normalizedPath = normalizedPath.substring(0, index);
        }
        return replace(normalizedPath, "//", "/");
    }

//    /**
//     * Get Parameters from the specified query string.
//     * <p>
//     *
//     * @param queryString The query string
//     * @return The query parameters
//     */
//    public static MultivaluedMap<String, String> getParameters(String queryString) {
//        return getParameters(split(queryString, AND_CHAR));
//    }

//    /**
//     * Get Parameters from the specified pairs of name-value.
//     * <p>
//     *
//     * @param pairs The pairs of name-value
//     * @return The query parameters
//     */
//    public static MultivaluedMap<String, String> getParameters(Iterable<String> pairs) {
//        MultivaluedMap<String, String> parameters = new MultivaluedHashMap<>();
//        if (pairs != null) {
//            for (String pair : pairs) {
//                String[] nameAndValue = split(pair, EQUAL_CHAR);
//                String name = decode(nameAndValue[0]);
//                String value = nameAndValue.length < 2 ? null : nameAndValue[1];
//                value = decode(value);
//                addParam(parameters, name, value);
//            }
//        }
//        return parameters;
//    }

//    /**
//     * Get Parameters from the specified pairs of name-value.
//     * <p>
//     *
//     * @param pairs The pairs of name-value
//     * @return The query parameters
//     */
//    public static MultivaluedMap<String, String> getParameters(String... pairs) {
//        return getParameters(asList(pairs));
//    }

    // /**
    // * Parse a read-only {@link MultivaluedMap} of {@link HttpCookie} from {@link
    // HttpHeaders}
    // *
    // * @param httpHeaders {@link HttpHeaders}
    // * @return non-null, the key is a cookie name , the value is {@link HttpCookie}
    // */
    // public static MultivaluedMap<String, HttpCookie> parseCookies(HttpHeaders
    // httpHeaders) {
    //
    // String cookie = httpHeaders.getFirst(COOKIE);
    //
    // String[] cookieNameAndValues = StringUtils.delimitedListToStringArray(cookie,
    // SEMICOLON);
    //
    // MultivaluedMap<String, HttpCookie> cookies = new
    // LinkedMultiValueMap<>(cookieNameAndValues.length);
    //
    // for (String cookeNameAndValue : cookieNameAndValues) {
    // String[] nameAndValue =
    // delimitedListToStringArray(trimWhitespace(cookeNameAndValue), EQUAL);
    // String name = nameAndValue[0];
    // String value = nameAndValue.length < 2 ? null : nameAndValue[1];
    // HttpCookie httpCookie = new HttpCookie(name, value);
    // cookies.add(name, httpCookie);
    // }
    //
    // return cookies;
    // }

    /**
     * To the name and value line sets
     *
     * @param nameAndValuesMap the map of name and values
     * @return non-null
     */
    public static Set<String> toNameAndValuesSet(
            Map<String, List<String>> nameAndValuesMap) {
        Set<String> nameAndValues = new LinkedHashSet<>();
        for (Map.Entry<String, List<String>> entry : nameAndValuesMap.entrySet()) {
            String name = entry.getKey();
            List<String> values = entry.getValue();
            for (String value : values) {
                String nameAndValue = name + EQUAL + value;
                nameAndValues.add(nameAndValue);
            }
        }
        return nameAndValues;
    }

    public static String[] toNameAndValues(Map<String, List<String>> nameAndValuesMap) {
        return toNameAndValuesSet(nameAndValuesMap).toArray(new String[0]);
    }

    /**
     * Generate a string of query string from the specified request parameters {@link Map}
     *
     * @param params the specified request parameters {@link Map}
     * @return non-null
     */
    public static String toQueryString(Map<String, List<String>> params) {
        StringBuilder builder = new StringBuilder();
        for (String line : toNameAndValuesSet(params)) {
            builder.append(line).append(AND);
        }
        return builder.toString();
    }

    /**
     * Decode value
     *
     * @param value the value requires to decode
     * @return the decoded value
     */
    public static String decode(String value) {
        if (value == null) {
            return value;
        }
        String decodedValue = value;
        try {
            decodedValue = URLDecoder.decode(value, UTF_8);
        } catch (UnsupportedEncodingException ex) {
        }
        return decodedValue;
    }

    /**
     * encode value
     *
     * @param value the value requires to encode
     * @return the encoded value
     */
    public static String encode(String value) {
        String encodedValue = value;
        try {
            encodedValue = URLEncoder.encode(value, UTF_8);
        } catch (UnsupportedEncodingException ex) {
        }
        return encodedValue;
    }

//    private static void addParam(MultivaluedMap<String, String> paramsMap, String name,
//                                 String value) {
//        String paramValue = trim(value);
//        if (isEmpty(paramValue)) {
//            paramValue = EMPTY_VALUE;
//        }
//        paramsMap.add(trim(name), paramValue);
//    }
}
