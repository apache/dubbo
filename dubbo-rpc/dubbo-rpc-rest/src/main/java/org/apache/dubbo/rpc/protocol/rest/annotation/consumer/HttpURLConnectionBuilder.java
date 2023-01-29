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
package org.apache.dubbo.rpc.protocol.rest.annotation.consumer;

import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;


public class HttpURLConnectionBuilder {
    private static Set<HttpConnectionPreBuildIntercept> httpConnectionPreBuildIntercepts =
        ApplicationModel.defaultModel().getExtensionLoader(HttpConnectionPreBuildIntercept.class).getSupportedExtensionInstances();


    public static HttpURLConnection build(RequestTemplate requestTemplate,
                                          HttpConnectionConfig connectionConfig,
                                          RestMethodMetadata restMethodMetadata
    ) throws Exception {


        // TODO substract  Client  param is requestTemplate,connectionConfig
        HttpURLConnection connection = createBaseConnection(requestTemplate, connectionConfig);


        writeHeaders(requestTemplate, connection);


        return writeBody(requestTemplate, connectionConfig, connection);

    }



    private static HttpURLConnection createBaseConnection(RequestTemplate requestTemplate,
                                                          HttpConnectionConfig connectionConfig) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(requestTemplate.getUri()).openConnection();
        connection.setConnectTimeout(connectionConfig.getConnectTimeout());
        connection.setReadTimeout(connectionConfig.getReadTimeout());
        connection.setAllowUserInteraction(false);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestMethod(requestTemplate.getHttpMethod());
        return connection;
    }

    private static void writeHeaders(RequestTemplate requestTemplate, HttpURLConnection connection) {
        for (String field : requestTemplate.getAllHeaders().keySet()) {
            for (String value : requestTemplate.getHeaders(field)) {
                connection.addRequestProperty(field, value);
            }
        }
    }

    private static HttpURLConnection writeBody(RequestTemplate requestTemplate,
                                               HttpConnectionConfig connectionConfig,
                                               HttpURLConnection connection) throws IOException {
        boolean gzipEncodedRequest = requestTemplate.isGzipEncodedRequest();
        boolean deflateEncodedRequest = requestTemplate.isDeflateEncodedRequest();
        if (requestTemplate.isBodyEmpty()) {
            return connection;
        }
        Integer contentLength = requestTemplate.getContentLength();

        if (contentLength != null) {
            connection.setFixedLengthStreamingMode(contentLength);
        } else {
            connection.setChunkedStreamingMode(connectionConfig.getChunkLength());
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
        return null;
    }


}
