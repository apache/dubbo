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
package org.apache.dubbo.remoting.http12;

import java.nio.charset.StandardCharsets;

public enum HttpMethods {
    GET,
    HEAD,
    POST,
    PUT,
    PATCH,
    DELETE,
    OPTIONS,
    TRACE;

    public static final byte[][] HTTP_METHODS_BYTES;

    static {
        HttpMethods[] methods = values();
        int len = methods.length;
        HTTP_METHODS_BYTES = new byte[len][];
        for (int i = 0; i < len; i++) {
            HTTP_METHODS_BYTES[i] = methods[i].name().getBytes(StandardCharsets.ISO_8859_1);
        }
    }

    public boolean is(String name) {
        return name().equals(name);
    }

    public boolean supportBody() {
        return this == POST || this == PUT || this == PATCH;
    }

    @SuppressWarnings("StringEquality")
    public static HttpMethods of(String name) {
        // fast-path
        if (name == GET.name()) {
            return GET;
        } else if (name == POST.name()) {
            return POST;
        }
        return valueOf(name);
    }

    public static boolean isGet(String name) {
        return GET.name().equals(name);
    }

    public static boolean isPost(String name) {
        return POST.name().equals(name);
    }

    public static boolean supportBody(String name) {
        return name.charAt(0) == 'P';
    }
}
