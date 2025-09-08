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

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import zipkin2.reporter.Call;
import zipkin2.reporter.Callback;

class ZipkinWebClientSender extends HttpSender {
    private final String endpoint;

    private final WebClient webClient;

    ZipkinWebClientSender(String endpoint, WebClient webClient) {
        this.endpoint = endpoint;
        this.webClient = webClient;
    }

    @Override
    public HttpPostCall sendSpans(byte[] batchedEncodedSpans) {
        return new WebClientHttpPostCall(this.endpoint, batchedEncodedSpans, this.webClient);
    }

    private static class WebClientHttpPostCall extends HttpPostCall {

        private final String endpoint;

        private final WebClient webClient;

        WebClientHttpPostCall(String endpoint, byte[] body, WebClient webClient) {
            super(body);
            this.endpoint = endpoint;
            this.webClient = webClient;
        }

        @Override
        public Call<Void> clone() {
            return new WebClientHttpPostCall(this.endpoint, getUncompressedBody(), this.webClient);
        }

        @Override
        protected Void doExecute() {
            sendRequest().block();
            return null;
        }

        @Override
        protected void doEnqueue(Callback<Void> callback) {
            sendRequest().subscribe((entity) -> callback.onSuccess(null), callback::onError);
        }

        private Mono<ResponseEntity<Void>> sendRequest() {
            return this.webClient
                    .post()
                    .uri(this.endpoint)
                    .headers(this::addDefaultHeaders)
                    .bodyValue(getBody())
                    .retrieve()
                    .toBodilessEntity();
        }

        private void addDefaultHeaders(HttpHeaders headers) {
            headers.addAll(getDefaultHeaders());
        }
    }
}
