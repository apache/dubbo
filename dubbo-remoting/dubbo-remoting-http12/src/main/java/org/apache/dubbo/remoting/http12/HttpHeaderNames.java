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

import io.netty.handler.codec.http2.Http2Headers.PseudoHeaderName;
import io.netty.util.AsciiString;

public enum HttpHeaderNames {
    STATUS(PseudoHeaderName.STATUS.value()),

    PATH(PseudoHeaderName.PATH.value()),

    METHOD(PseudoHeaderName.METHOD.value()),

    ACCEPT(io.netty.handler.codec.http.HttpHeaderNames.ACCEPT),

    CONTENT_TYPE(io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE),

    CONTENT_LENGTH(io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH),

    CONTENT_LANGUAGE(io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LANGUAGE),

    TRANSFER_ENCODING(io.netty.handler.codec.http.HttpHeaderNames.TRANSFER_ENCODING),

    CACHE_CONTROL(io.netty.handler.codec.http.HttpHeaderNames.CACHE_CONTROL),

    LOCATION(io.netty.handler.codec.http.HttpHeaderNames.LOCATION),

    HOST(io.netty.handler.codec.http.HttpHeaderNames.HOST),

    COOKIE(io.netty.handler.codec.http.HttpHeaderNames.COOKIE),

    SET_COOKIE(io.netty.handler.codec.http.HttpHeaderNames.SET_COOKIE),

    LAST_MODIFIED(io.netty.handler.codec.http.HttpHeaderNames.LAST_MODIFIED),

    TE(io.netty.handler.codec.http.HttpHeaderNames.TE),

    ALT_SVC("alt-svc");

    private final String name;
    private final CharSequence key;

    HttpHeaderNames(String name) {
        this.name = name;
        key = AsciiString.cached(name);
    }

    HttpHeaderNames(CharSequence key) {
        name = key.toString();
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public CharSequence getKey() {
        return key;
    }
}
