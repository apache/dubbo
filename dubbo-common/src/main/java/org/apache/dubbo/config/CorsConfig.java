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

    private String allowedOrigins;

    private String allowedMethods;

    private String allowedHeaders;

    private String exposedHeaders;

    private Boolean allowCredentials;

    private Boolean allowPrivateNetWork;

    private Long maxAge;

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public String getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(String allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public String getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(String allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public String getExposedHeaders() {
        return exposedHeaders;
    }

    public void setExposedHeaders(String exposedHeaders) {
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

    public Boolean getAllowPrivateNetWork() {
        return allowPrivateNetWork;
    }

    public void setAllowPrivateNetWork(Boolean allowPrivateNetWork) {
        this.allowPrivateNetWork = allowPrivateNetWork;
    }
}
