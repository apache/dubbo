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
package org.apache.dubbo.config.nested;

import org.apache.dubbo.config.support.Nested;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class ExporterConfig implements Serializable {

    @Nested
    private ZipkinConfig zipkinConfig;

    @Nested
    private OtlpConfig otlpConfig;

    public ZipkinConfig getZipkinConfig() {
        return zipkinConfig;
    }

    public void setZipkinConfig(ZipkinConfig zipkinConfig) {
        this.zipkinConfig = zipkinConfig;
    }

    public OtlpConfig getOtlpConfig() {
        return otlpConfig;
    }

    public void setOtlpConfig(OtlpConfig otlpConfig) {
        this.otlpConfig = otlpConfig;
    }

    public static class ZipkinConfig implements Serializable {

        /**
         * URL to the Zipkin API.
         */
        private String endpoint;

        /**
         * Connection timeout for requests to Zipkin.
         */
        private Duration connectTimeout = Duration.ofSeconds(1);

        /**
         * Read timeout for requests to Zipkin.
         */
        private Duration readTimeout = Duration.ofSeconds(10);

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }
    }

    public static class OtlpConfig implements Serializable {

        /**
         * URL to the Otlp API.
         */
        private String endpoint;

        /**
         * The maximum time to wait for the collector to process an exported batch of spans.
         */
        private Duration timeout = Duration.ofSeconds(10);

        /**
         * The method used to compress payloads. If unset, compression is disabled. Currently
         * supported compression methods include "gzip" and "none".
         */
        private String compressionMethod = "none";

        private Map<String, String> headers = new HashMap<>();

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public String getCompressionMethod() {
            return compressionMethod;
        }

        public void setCompressionMethod(String compressionMethod) {
            this.compressionMethod = compressionMethod;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }
    }
}
