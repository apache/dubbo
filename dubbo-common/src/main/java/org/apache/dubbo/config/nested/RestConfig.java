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
import org.apache.dubbo.config.support.Parameter;

import java.io.Serializable;
import java.util.Map;

/**
 * Configuration for triple rest protocol.
 */
public class RestConfig implements Serializable {

    private static final long serialVersionUID = -8068568976367034755L;

    public static final boolean DEFAULT_TRAILING_SLASH_MATCH = true;
    public static final boolean DEFAULT_SUFFIX_PATTERN_MATCH = true;
    public static final boolean DEFAULT_CASE_SENSITIVE_MATCH = true;
    public static final String DEFAULT_FORMAT_PARAMETER_NAME = "format";

    /**
     * Whether to enable rest support
     * <p>The default value is 'true'.
     */
    private Boolean enabled;

    /**
     *  Whether to enable the default mapping '/{interfaceName}/{methodName}'.
     * <p>The default value is 'true'.
     */
    private Boolean enableDefaultMapping;

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
     * The json framework to use, make sure that dependencies are imported.
     */
    private String jsonFramework;

    /**
     * The disallowed content-types.
     */
    private String[] disallowedContentTypes;

    /**
     *  The cors configuration.
     */
    @Nested
    private CorsConfig cors;

    /**
     * The openapi configuration.
     */
    @Nested
    private OpenAPIConfig openapi;

    /**
     * Multiple configurations for openapi.
     */
    private Map<String, OpenAPIConfig> openapis;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getEnableDefaultMapping() {
        return enableDefaultMapping;
    }

    public void setEnableDefaultMapping(Boolean enableDefaultMapping) {
        this.enableDefaultMapping = enableDefaultMapping;
    }

    public Boolean getTrailingSlashMatch() {
        return trailingSlashMatch;
    }

    @Parameter(excluded = true)
    public boolean getTrailingSlashMatchOrDefault() {
        return trailingSlashMatch == null ? DEFAULT_TRAILING_SLASH_MATCH : trailingSlashMatch;
    }

    public void setTrailingSlashMatch(Boolean trailingSlashMatch) {
        this.trailingSlashMatch = trailingSlashMatch;
    }

    public Boolean getSuffixPatternMatch() {
        return suffixPatternMatch;
    }

    @Parameter(excluded = true)
    public boolean getSuffixPatternMatchOrDefault() {
        return suffixPatternMatch == null ? DEFAULT_SUFFIX_PATTERN_MATCH : suffixPatternMatch;
    }

    public void setSuffixPatternMatch(Boolean suffixPatternMatch) {
        this.suffixPatternMatch = suffixPatternMatch;
    }

    public Boolean getCaseSensitiveMatch() {
        return caseSensitiveMatch;
    }

    @Parameter(excluded = true)
    public boolean getCaseSensitiveMatchOrDefault() {
        return caseSensitiveMatch == null ? DEFAULT_CASE_SENSITIVE_MATCH : caseSensitiveMatch;
    }

    public void setCaseSensitiveMatch(Boolean caseSensitiveMatch) {
        this.caseSensitiveMatch = caseSensitiveMatch;
    }

    public String getFormatParameterName() {
        return formatParameterName;
    }

    @Parameter(excluded = true)
    public String getFormatParameterNameOrDefault() {
        return formatParameterName == null ? DEFAULT_FORMAT_PARAMETER_NAME : formatParameterName;
    }

    public void setFormatParameterName(String formatParameterName) {
        this.formatParameterName = formatParameterName;
    }

    public String getJsonFramework() {
        return jsonFramework;
    }

    public void setJsonFramework(String jsonFramework) {
        this.jsonFramework = jsonFramework;
    }

    public String[] getDisallowedContentTypes() {
        return disallowedContentTypes;
    }

    public void setDisallowedContentTypes(String[] disallowedContentTypes) {
        this.disallowedContentTypes = disallowedContentTypes;
    }

    public CorsConfig getCors() {
        return cors;
    }

    @Parameter(excluded = true)
    public CorsConfig getCorsOrDefault() {
        if (cors == null) {
            cors = new CorsConfig();
        }
        return cors;
    }

    public void setCors(CorsConfig cors) {
        this.cors = cors;
    }

    @Parameter(excluded = true)
    public OpenAPIConfig getOpenapi() {
        return openapi;
    }

    @Parameter(attribute = false)
    public void setOpenapi(OpenAPIConfig openapi) {
        this.openapi = openapi;
    }

    @Parameter(excluded = true)
    public Map<String, OpenAPIConfig> getOpenapis() {
        return openapis;
    }

    @Parameter(attribute = false)
    public void setOpenapis(Map<String, OpenAPIConfig> openapis) {
        this.openapis = openapis;
    }
}
