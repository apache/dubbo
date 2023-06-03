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

/**
 * for http methods
 */
public class HttpUtils {
    public static String SIMPLE_HTTP = "XXX HTTP/1";

    public enum HttpMethod {
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

    public static final List<HttpMethod> HTTP_METHODS = Arrays.asList(HttpMethod.values());

    public static char[][] getHttpMethodsPrefix(int length) {
        if (0 >= length || length > 3) {
            throw new IllegalArgumentException("Current substring length is beyond Http methods length");
        }

        List<char[]> prefix = new ArrayList<>();
        for (HttpMethod httpMethod : HTTP_METHODS) {
            prefix.add(httpMethod.getValue().substring(0, length).toCharArray());
        }

        return prefix.toArray(new char[0][]);

    }


    public static char[][] getHttpMethodsPrefix() {
        return getHttpMethodsPrefix(3);
    }

}
