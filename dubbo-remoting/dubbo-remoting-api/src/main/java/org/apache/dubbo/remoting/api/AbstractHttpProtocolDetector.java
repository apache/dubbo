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
package org.apache.dubbo.remoting.api;

import org.apache.dubbo.remoting.buffer.ChannelBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.dubbo.remoting.api.AbstractHttpProtocolDetector.HttpMethod.DELETE;
import static org.apache.dubbo.remoting.api.AbstractHttpProtocolDetector.HttpMethod.GET;
import static org.apache.dubbo.remoting.api.AbstractHttpProtocolDetector.HttpMethod.HEAD;
import static org.apache.dubbo.remoting.api.AbstractHttpProtocolDetector.HttpMethod.OPTIONS;
import static org.apache.dubbo.remoting.api.AbstractHttpProtocolDetector.HttpMethod.PATCH;
import static org.apache.dubbo.remoting.api.AbstractHttpProtocolDetector.HttpMethod.POST;
import static org.apache.dubbo.remoting.api.AbstractHttpProtocolDetector.HttpMethod.PUT;
import static org.apache.dubbo.remoting.api.AbstractHttpProtocolDetector.HttpMethod.TRACE;

/**
 * http protocol detector
 */
public abstract class AbstractHttpProtocolDetector implements ProtocolDetector {

    protected int empty = ' ';
    protected static String SIMPLE_HTTP = "XXX HTTP/1";

    protected static final List<HttpMethod> QOS_HTTP_METHOD = Arrays.asList(GET, POST);

    /**
     * rank by frequency
     * first GET ,POST,DELETE,PUT
     * second HEAD,PATCH,OPTIONS,TRACE
     */
    protected static final List<HttpMethod> HTTP_METHODS =
            Arrays.asList(GET, POST, DELETE, PUT, HEAD, PATCH, OPTIONS, TRACE);

    protected static char[][] getHttpMethodsPrefix(int length, List<HttpMethod> httpMethods) {
        if (0 >= length || length > 3) {
            throw new IllegalArgumentException("Current substring length is beyond Http methods length");
        }

        List<char[]> prefix = new ArrayList<>();
        for (HttpMethod httpMethod : httpMethods) {
            prefix.add(httpMethod.getValue().substring(0, length).toCharArray());
        }

        return prefix.toArray(new char[0][]);
    }

    protected static char[][] getHttpMethodsPrefix() {
        return getHttpMethodsPrefix(3, HTTP_METHODS);
    }

    protected static char[][] getQOSHttpMethodsPrefix() {
        return getHttpMethodsPrefix(3, QOS_HTTP_METHOD);
    }

    /**
     * qos /name/appName
     *
     * @param requestUrl
     * @return
     */
    protected boolean isQosRequestURL(String requestUrl) {

        if (requestUrl == null) {
            return false;
        }

        String[] split = requestUrl.split("/");

        if (split.length <= 3) {
            return true;
        }

        return false;
    }

    protected String splitAndGetFirst(String str) {

        return splitAndGet(str, 1);
    }

    protected String splitAndGet(String str, int index) {
        if (str == null) {
            return null;
        }

        String[] split = str.split("/");

        if (split.length - 1 < index) {
            return null;
        }

        return split[index];
    }

    /**
     * between first and second empty char
     *
     * @param buffer
     * @return
     */
    protected String readRequestLine(ChannelBuffer buffer) {

        // GET /test/demo HTTP/1.1
        int firstEmptyIndex = 0;
        // read first empty
        for (int i = 0; i < Integer.MAX_VALUE; i++) {

            int read = getByteByIndex(buffer, i);
            if (read == empty) {
                firstEmptyIndex = i;
                break;
            }
        }

        StringBuilder stringBuilder = new StringBuilder();

        for (int i = firstEmptyIndex + 1; i < Integer.MAX_VALUE; i++) {
            int read = getByteByIndex(buffer, i);
            // second empty break
            if (read == empty) {
                break;
            }
            stringBuilder.append((char) read);
        }

        return stringBuilder.toString();
    }

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
}
