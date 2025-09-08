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
package org.apache.dubbo.spring.boot.observability.autoconfigure.exporter.zipkin;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import zipkin2.reporter.Call;
import zipkin2.reporter.Callback;

class ZipkinRestTemplateSender extends HttpSender {
    private final String endpoint;

    private final RestTemplate restTemplate;

    ZipkinRestTemplateSender(String endpoint, RestTemplate restTemplate) {
        this.endpoint = endpoint;
        this.restTemplate = restTemplate;
    }

    @Override
    public HttpPostCall sendSpans(byte[] batchedEncodedSpans) {
        return new RestTemplateHttpPostCall(this.endpoint, batchedEncodedSpans, this.restTemplate);
    }

    private static class RestTemplateHttpPostCall extends HttpPostCall {

        private final String endpoint;

        private final RestTemplate restTemplate;

        RestTemplateHttpPostCall(String endpoint, byte[] body, RestTemplate restTemplate) {
            super(body);
            this.endpoint = endpoint;
            this.restTemplate = restTemplate;
        }

        @Override
        public Call<Void> clone() {
            return new RestTemplateHttpPostCall(this.endpoint, getUncompressedBody(), this.restTemplate);
        }

        @Override
        protected Void doExecute() {
            HttpEntity<byte[]> request = new HttpEntity<>(getBody(), getDefaultHeaders());
            this.restTemplate.exchange(this.endpoint, HttpMethod.POST, request, Void.class);
            return null;
        }

        @Override
        protected void doEnqueue(Callback<Void> callback) {
            try {
                doExecute();
                callback.onSuccess(null);
            } catch (Exception ex) {
                callback.onError(ex);
            }
        }
    }
}
