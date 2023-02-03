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
package org.apache.dubbo.rpc.protocol.rest.response;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class URLConnectionHttpResponseFacade extends AbstractHttpResponseFacade<HttpURLConnection> {

    public URLConnectionHttpResponseFacade(Object response) {
        super((HttpURLConnection) response);
    }

    @Override
    public String getContentType() {

        return response.getContentType();


    }

    @Override
    public InputStream getBody() throws IOException {
        return response.getInputStream();
    }

    @Override
    public InputStream getErrorResponse() throws IOException {
        return response.getErrorStream();
    }

    @Override
    public int getResponseCode() throws IOException {
        return response.getResponseCode();
    }
}
