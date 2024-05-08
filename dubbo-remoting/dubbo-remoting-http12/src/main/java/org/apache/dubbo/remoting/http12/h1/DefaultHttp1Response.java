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
import org.apache.dubbo.remoting.http12.HttpInputMessage;
import org.apache.dubbo.remoting.http12.HttpMetadata;

import java.io.IOException;
import java.io.InputStream;

public class DefaultHttp1Response implements HttpMetadata, HttpInputMessage {

    private final HttpMetadata httpMetadata;

    private final HttpInputMessage httpInputMessage;

    public DefaultHttp1Response(HttpMetadata httpMetadata, HttpInputMessage httpInputMessage) {
        this.httpMetadata = httpMetadata;
        this.httpInputMessage = httpInputMessage;
    }

    @Override
    public InputStream getBody() {
        return httpInputMessage.getBody();
    }

    @Override
    public HttpHeaders headers() {
        return httpMetadata.headers();
    }

    @Override
    public void close() throws IOException {
        httpInputMessage.close();
    }
}
