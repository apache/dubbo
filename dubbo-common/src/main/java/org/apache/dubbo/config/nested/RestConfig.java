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

/**
 * Configuration for triple rest protocol.
 */
public class RestConfig implements Serializable {

    private static final long serialVersionUID = -8068568976367034755L;

    /**
     * Whether path matching should be match paths with a trailing slash.
     * If enabled, a method mapped to "/users" also matches to "/users/".
     * <p>The default value is {@code true}.
     */
    private Boolean trailingSlashMatch;

    /**
     * Whether path matching uses suffix pattern matching (".*").
     * If enabled, a method mapped to "/users" also matches to "/users.*".
     * <p>This also enables suffix content negotiation, with the media-type
     * inferred from the URL suffix, e.g., ".json" for "application/json".
     * <p>The default value is {@code true}.
     */
    private Boolean suffixPatternMatch;

    /**
     * Whether path matching should be case-sensitive.
     * If enabled, a method mapped to "/users" won't match to "/Users/".
     * <p>The default value is {@code true}.
     */
    private Boolean caseSensitiveMatch;

    /**
     * The parameter name that can be used to specify the response format.
     * <p>The default value is 'format'.
     */
    private String formatParameterName;

    /**
     *  The cors configuration.
     */
    @Nested
    private CorsConfig cors;

    public Boolean getTrailingSlashMatch() {
        return trailingSlashMatch;
    }

    public void setTrailingSlashMatch(Boolean trailingSlashMatch) {
        this.trailingSlashMatch = trailingSlashMatch;
    }

    public Boolean getSuffixPatternMatch() {
        return suffixPatternMatch;
    }

    public void setSuffixPatternMatch(Boolean suffixPatternMatch) {
        this.suffixPatternMatch = suffixPatternMatch;
    }

    public Boolean getCaseSensitiveMatch() {
        return caseSensitiveMatch;
    }

    public void setCaseSensitiveMatch(Boolean caseSensitiveMatch) {
        this.caseSensitiveMatch = caseSensitiveMatch;
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
