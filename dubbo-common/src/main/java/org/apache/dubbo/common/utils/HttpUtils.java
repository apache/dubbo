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
package org.apache.dubbo.common.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.dubbo.common.utils.HttpUtils.HttpMethod.GET;
import static org.apache.dubbo.common.utils.HttpUtils.HttpMethod.POST;
import static org.apache.dubbo.common.utils.HttpUtils.HttpMethod.DELETE;
import static org.apache.dubbo.common.utils.HttpUtils.HttpMethod.PUT;
import static org.apache.dubbo.common.utils.HttpUtils.HttpMethod.HEAD;
import static org.apache.dubbo.common.utils.HttpUtils.HttpMethod.PATCH;
import static org.apache.dubbo.common.utils.HttpUtils.HttpMethod.OPTIONS;
import static org.apache.dubbo.common.utils.HttpUtils.HttpMethod.TRACE;

/**
 * for http methods
 */
public class HttpUtils {
    public static String SIMPLE_HTTP = "XXX HTTP/1";

    public static enum HttpMethod {
        GET("GET"),
        HEAD("HEAD"),
        POST("POST"),
        PUT("PUT"),

        PATCH("PATCH"),
        DELETE("DELETE"),
        OPTIONS("OPTIONS"),
        TRACE("TRACE");


        HttpMethod(String value) {
            this.value = value;
        }

        private String value;

        public String getValue() {
            return value;
        }
    }

    /**
     * rank by frequency
     * first GET ,POST,DELETE,PUT
     * second HEAD,PATCH,OPTIONS,TRACE
     */
    public static final List<HttpMethod> QOS_HTTP_METHOD = Arrays.asList(GET, POST);

    public static final List<HttpMethod> HTTP_METHODS = Arrays.asList(GET, POST, DELETE, PUT, HEAD, PATCH, OPTIONS, TRACE);

    public static char[][] getHttpMethodsPrefix(int length, List<HttpMethod> httpMethods) {
        if (0 >= length || length > 3) {
            throw new IllegalArgumentException("Current substring length is beyond Http methods length");
        }

        List<char[]> prefix = new ArrayList<>();
        for (HttpMethod httpMethod : httpMethods) {
            prefix.add(httpMethod.getValue().substring(0, length).toCharArray());
        }

        return prefix.toArray(new char[0][]);

    }


    public static char[][] getHttpMethodsPrefix() {
        return getHttpMethodsPrefix(3, HTTP_METHODS);
    }

    public static char[][] getQOSHttpMethodsPrefix() {
        return getHttpMethodsPrefix(3, QOS_HTTP_METHOD);
    }

    /**
     * qos /name/appName
     *
     * @param requestUrl
     * @return
     */
    public static boolean isQosRequestURL(String requestUrl) {

        if (requestUrl == null) {
            return false;
        }

        String[] split = requestUrl.split("/");

        if (split.length <= 3) {
            return true;
        }

        return false;
    }

    public static String splitAndGetFirst(String str) {

        return splitAndGet(str, 1);
    }

    public static String splitAndGet(String str, int index) {
        if (str == null) {
            return null;
        }

        String[] split = str.split("/");

        if (split.length - 1 < index) {
            return null;
        }

        return split[index];
    }

}
