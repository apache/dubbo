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

import org.apache.dubbo.config.support.Nested;

import java.io.Serializable;

/**
 * Configuration for triple rest protocol.
 */
public class RestConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Maximum allowed size for request bodies.
     * Limits the size of request to prevent excessively large request.
     * <p>The default value is 8MiB.
     */
    private Integer maxBodySize;

    /**
     * Maximum allowed size for response bodies.
     * Limits the size of responses to prevent excessively large response.
     * <p>The default value is 8MiB.
     */
    private Integer maxResponseBodySize;

    /**
     * Whether path matching should be match paths with a trailing slash.
     * If enabled, a method mapped to "/users" also matches to "/users/".
     * <p>The default value is {@code true}.
     */
    private Boolean trailingSlashMatch;

    /**
     * Whether path matching should be case-sensitive.
     * If enabled, a method mapped to "/users" won't match to "/Users/".
     * <p>The default value is {@code false}.
     */
    private Boolean caseSensitiveMatch;

    /**
     * Whether path matching uses suffix pattern matching (".*").
     * If enabled, a method mapped to "/users" also matches to "/users.*".
     * <p>This also enables suffix content negotiation, with the media-type
     * inferred from the URL suffix, e.g., ".json" for "application/json".
     * <p>The default value is {@code true}.
     */
    private Boolean suffixPatternMatch;

    /**
     * The parameter name that can be used to specify the response format.
     * <p>The default value is 'format'.
     */
    private String formatParameterName;

    /**
     *  The config is used to set the Global CORS configuration properties.
     */
    @Nested
    private CorsConfig cors;

    public Integer getMaxBodySize() {
        return maxBodySize;
    }

    public void setMaxBodySize(Integer maxBodySize) {
        this.maxBodySize = maxBodySize;
    }

    public Integer getMaxResponseBodySize() {
        return maxResponseBodySize;
    }

    public void setMaxResponseBodySize(Integer maxResponseBodySize) {
        this.maxResponseBodySize = maxResponseBodySize;
    }

    public Boolean getTrailingSlashMatch() {
        return trailingSlashMatch;
    }

    public void setTrailingSlashMatch(Boolean trailingSlashMatch) {
        this.trailingSlashMatch = trailingSlashMatch;
    }

    public Boolean getCaseSensitiveMatch() {
        return caseSensitiveMatch;
    }

    public void setCaseSensitiveMatch(Boolean caseSensitiveMatch) {
        this.caseSensitiveMatch = caseSensitiveMatch;
    }

    public Boolean getSuffixPatternMatch() {
        return suffixPatternMatch;
    }

    public void setSuffixPatternMatch(Boolean suffixPatternMatch) {
        this.suffixPatternMatch = suffixPatternMatch;
    }

    public String getFormatParameterName() {
        return formatParameterName;
    }

    public void setFormatParameterName(String formatParameterName) {
        this.formatParameterName = formatParameterName;
    }

    public CorsConfig getCors() {
        return cors;
    }

    public void setCors(CorsConfig cors) {
        this.cors = cors;
    }
}
