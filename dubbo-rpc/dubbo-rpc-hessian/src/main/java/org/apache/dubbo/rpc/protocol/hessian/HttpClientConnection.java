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
package org.apache.dubbo.rpc.protocol.hessian;

import com.caucho.hessian.client.HessianConnection;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * HttpClientConnection
 */
public class HttpClientConnection implements HessianConnection {

    private final HttpClient httpClient;

    private final ByteArrayOutputStream output;

    private final HttpPost request;

    private volatile HttpResponse response;

    public HttpClientConnection(HttpClient httpClient, URL url) {
        this.httpClient = httpClient;
        this.output = new ByteArrayOutputStream();
        this.request = new HttpPost(url.toString());
    }

    @Override
    public void addHeader(String key, String value) {
        request.addHeader(new BasicHeader(key, value));
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return output;
    }

    @Override
    public void sendRequest() throws IOException {
        request.setEntity(new ByteArrayEntity(output.toByteArray()));
        this.response = httpClient.execute(request);
    }

    @Override
    public int getStatusCode() {
        return response == null || response.getStatusLine() == null ? 0 : response.getStatusLine().getStatusCode();
    }

    @Override
    public String getStatusMessage() {
        return response == null || response.getStatusLine() == null ? null : response.getStatusLine().getReasonPhrase();
    }

    @Override
    public String getContentEncoding() {
        return (response == null || response.getEntity() == null || response.getEntity().getContentEncoding() == null) ? null : response.getEntity().getContentEncoding().getValue();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return response == null || response.getEntity() == null ? null : response.getEntity().getContent();
    }

    @Override
    public void close() throws IOException {
        HttpPost request = this.request;
        if (request != null) {
            request.abort();
        }
    }

    @Override
    public void destroy() throws IOException {
    }

}
