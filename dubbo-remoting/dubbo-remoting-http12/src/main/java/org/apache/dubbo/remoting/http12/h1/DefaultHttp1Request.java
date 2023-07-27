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
import org.apache.dubbo.remoting.http12.HttpMessage;
import org.apache.dubbo.remoting.http12.RequestMetadata;

import java.io.InputStream;

public class DefaultHttp1Request implements Http1Request {

    private final RequestMetadata httpMetadata;

    private final HttpMessage httpMessage;

    public DefaultHttp1Request(RequestMetadata httpMetadata, HttpMessage httpMessage) {
        this.httpMetadata = httpMetadata;
        this.httpMessage = httpMessage;
    }

    @Override
    public InputStream getBody() {
        return httpMessage.getBody();
    }

    @Override
    public HttpHeaders headers() {
        return httpMetadata.headers();
    }

    @Override
    public String method() {
        return httpMetadata.method();
    }

    @Override
    public String path() {
        return httpMetadata.path();
    }
}
