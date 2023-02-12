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
package org.apache.dubbo.remoting.http.restclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.dubbo.remoting.http.BaseRestClient;
import org.apache.dubbo.remoting.http.RequestTemplate;
import org.apache.dubbo.remoting.http.RestResult;
import org.apache.dubbo.remoting.http.config.HttpClientConfig;


public class URLConnectionRestClient extends BaseRestClient<HttpURLConnection> {

    public URLConnectionRestClient(HttpClientConfig clientConfig) {
        super(clientConfig);
    }

    @Override
    public CompletableFuture<RestResult> send(RequestTemplate requestTemplate) {

        CompletableFuture<RestResult> future = new CompletableFuture<>();

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(requestTemplate.getURL()).openConnection();
            connection.setConnectTimeout(clientConfig.getConnectTimeout());
            connection.setReadTimeout(clientConfig.getReadTimeout());
            connection.setAllowUserInteraction(false);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod(requestTemplate.getHttpMethod());

            // writeHeaders

            for (String field : requestTemplate.getAllHeaders().keySet()) {
                for (String value : requestTemplate.getHeaders(field)) {
                    connection.addRequestProperty(field, value);
                }
            }


            // writeBody

            boolean gzipEncodedRequest = requestTemplate.isGzipEncodedRequest();
            boolean deflateEncodedRequest = requestTemplate.isDeflateEncodedRequest();
            if (requestTemplate.isBodyEmpty()) {
                future.complete(new RestResult() {
                    @Override
                    public String getContentType() {
                        return connection.getContentType();
                    }

                    @Override
                    public InputStream getBody() throws IOException {
                        return connection.getInputStream();
                    }

                    @Override
                    public Map<String, List<String>> headers() {
                        return connection.getHeaderFields();
                    }

                    @Override
                    public InputStream getErrorResponse() throws IOException {
                        return connection.getErrorStream();
                    }

                    @Override
                    public int getResponseCode() throws IOException {
                        return connection.getResponseCode();
                    }
                });
                return future;
            }
            Integer contentLength = requestTemplate.getContentLength();

            if (contentLength != null) {
                connection.setFixedLengthStreamingMode(contentLength);
            } else {
                connection.setChunkedStreamingMode(clientConfig.getChunkLength());
            }
            connection.setDoOutput(true);

            OutputStream out = connection.getOutputStream();
            if (gzipEncodedRequest) {
                out = new GZIPOutputStream(out);
            } else if (deflateEncodedRequest) {
                out = new DeflaterOutputStream(out);
            }
            try {
                out.write(requestTemplate.getSerializedBody());
            } finally {
                try {
                    out.close();
                } catch (IOException suppressed) {
                }
            }

            future.complete(new RestResult() {
                @Override
                public String getContentType() {
                    return connection.getContentType();
                }

                @Override
                public InputStream getBody() throws IOException {
                    return connection.getInputStream();
                }

                @Override
                public Map<String, List<String>> headers() {
                    return connection.getHeaderFields();
                }

                @Override
                public InputStream getErrorResponse() throws IOException {
                    return connection.getErrorStream();
                }

                @Override
                public int getResponseCode() throws IOException {
                    return connection.getResponseCode();
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    @Override
    public void close() {

    }

    @Override
    public void close(int timeout) {

    }

    @Override
    public boolean isClosed() {
        return true;
    }

    public HttpURLConnection createHttpClient(HttpClientConfig httpClientConfig) {

        return null;
    }

}
