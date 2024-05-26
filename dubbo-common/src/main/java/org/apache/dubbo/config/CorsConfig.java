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
package org.apache.dubbo.config;

import java.io.Serializable;

public class CorsConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * A list of origins for which cross-origin requests are allowed. Values may be a specific domain, e.g.
     * {@code "https://domain1.com"}, or the CORS defined special value {@code "*"} for all origins.
     * <p>By default this is not set which means that no origins are allowed.
     * However, an instance of this class is often initialized further, e.g. for {@code @CrossOrigin}, via
     * {@code org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.CorsMeta.Builder#applyDefault()}.
     */
    private String[] allowedOrigins;

    /**
     * Set the HTTP methods to allow, e.g. {@code "GET"}, {@code "POST"},
     * {@code "PUT"}, etc. The special value {@code "*"} allows all methods.
     * <p>If not set, only {@code "GET"} and {@code "HEAD"} are allowed.
     * <p>By default this is not set.
     */
    private String[] allowedMethods;

    /**
     * /**
     * Set the list of headers that a pre-flight request can list as allowed
     * for use during an actual request. The special value {@code "*"} allows
     * actual requests to send any header.
     * <p>By default this is not set.
     */
    private String[] allowedHeaders;

    /**
     * Set the list of response headers that an actual response might have
     * and can be exposed to the client. The special value {@code "*"}
     * allows all headers to be exposed.
     * <p>By default this is not set.
     */
    private String[] exposedHeaders;

    /**
     * Whether user credentials are supported.
     * <p>By default this is not set (i.e. user credentials are not supported).
     */
    private Boolean allowCredentials;

    /**
     * Configure how long, as a duration, the response from a pre-flight request
     * can be cached by clients.
     */
    private Long maxAge;

    public String[] getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public String[] getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(String[] allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public String[] getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(String[] allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public String[] getExposedHeaders() {
        return exposedHeaders;
    }

    public void setExposedHeaders(String[] exposedHeaders) {
        this.exposedHeaders = exposedHeaders;
    }

    public Boolean getAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(Boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public Long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Long maxAge) {
        this.maxAge = maxAge;
    }
}
