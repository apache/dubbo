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
package org.apache.dubbo.remoting.http12.h1;

import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.RequestMetadata;

public final class Http1RequestMetadata implements RequestMetadata {

    private final HttpHeaders headers;

    private final String method;

    private final String path;

    public Http1RequestMetadata(HttpHeaders headers, String method, String path) {
        this.headers = headers;
        this.method = method;
        this.path = path;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public String method() {
        return method;
    }

    @Override
    public String path() {
        return path;
    }
}
