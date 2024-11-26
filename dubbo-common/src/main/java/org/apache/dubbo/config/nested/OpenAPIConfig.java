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

import java.io.Serializable;
import java.util.Map;

public class OpenAPIConfig implements Serializable {

    private static final long serialVersionUID = 6943417456345001947L;

    /**
     * Whether to enable OpenAPI support
     * <p>The default value is 'true'.
     */
    private Boolean enabled;

    /**
     * Whether to cache the OpenAPI document.
     * <p>The default value is 'true'.
     */
    private Boolean cache;

    /**
     * The HTTP path where OpenAPI will be registered.
     * <p>The default value is '/dubbo/openapi'.
     */
    private String path;

    /**
     * The title of the OpenAPI information.
     */
    private String infoTitle;

    /**
     * A brief description of the OpenAPI information.
     */
    private String infoDescription;

    /**
     * The version number of the OpenAPI information.
     */
    private String infoVersion;

    /**
     * The name of the contact.
     */
    private String infoContactName;

    /**
     * The url of the contact.
     */
    private String infoContactUrl;

    /**
     * The email address of the contact.
     */
    private String infoContactEmail;

    /**
     * A description of the external documentation.
     */
    private String externalDocsDescription;

    /**
     * The URL of the external documentation.
     */
    private String externalDocsUrl;

    /**
     * A list of servers.
     */
    private String[] servers;

    /**
     * The security scheme.
     */
    private String securityScheme;

    /**
     * The security.
     */
    private String security;

    /**
     * The strategy used to generate operation id and schema name.
     */
    private String nameStrategy;

    /**
     * The default media types that are consumed.
     */
    private String[] defaultConsumesMediaTypes;

    /**
     * The default media types that are produced.
     */
    private String[] defaultProducesMediaTypes;

    /**
     * The default HTTP methods are used.
     */
    private String[] defaultHttpMethods;

    /**
     * The default HTTP status codes are returned.
     */
    private String[] defaultHttpStatusCodes;

    /**
     * Whether to flatten the inherited fields from the parent class into the schema.
     * <p>The default value is {@code false}.
     */
    private Boolean schemaFlatten;

    /**
     * Specifies the classes to be excluded from schema generation.
     * <p>For example:
     * <ul>
     *     <li>com.example.MyClass - Exclude the MyClass class.</li>
     *     <li>com.example. - Exclude all classes in the com.example package.</li>
     *     <li>!com.example.exclude. - Exclude all classes except those in the com.example.exclude package.</li>
     * </ul>
     * Note that the package name should end with a dot (.) or an exclamation mark (!) to indicate the exclusion scope.
     * <p>Multiple classes or package names can be separated by commas, for
     * example: com.example.MyClass,com.example.,!com.example.exclude
     */
    private String[] schemaClassExcludes;

    /**
     * The custom settings.
     */
    private Map<String, String> settings;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getCache() {
        return cache;
    }

    public void setCache(Boolean cache) {
        this.cache = cache;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getInfoTitle() {
        return infoTitle;
    }

    public void setInfoTitle(String infoTitle) {
        this.infoTitle = infoTitle;
    }

    public String getInfoDescription() {
        return infoDescription;
    }

    public void setInfoDescription(String infoDescription) {
        this.infoDescription = infoDescription;
    }

    public String getInfoVersion() {
        return infoVersion;
    }

    public void setInfoVersion(String infoVersion) {
        this.infoVersion = infoVersion;
    }

    public String getInfoContactName() {
        return infoContactName;
    }

    public void setInfoContactName(String infoContactName) {
        this.infoContactName = infoContactName;
    }

    public String getInfoContactUrl() {
        return infoContactUrl;
    }

    public void setInfoContactUrl(String infoContactUrl) {
        this.infoContactUrl = infoContactUrl;
    }

    public String getInfoContactEmail() {
        return infoContactEmail;
    }

    public void setInfoContactEmail(String infoContactEmail) {
        this.infoContactEmail = infoContactEmail;
    }

    public String getExternalDocsDescription() {
        return externalDocsDescription;
    }

    public void setExternalDocsDescription(String externalDocsDescription) {
        this.externalDocsDescription = externalDocsDescription;
    }

    public String getExternalDocsUrl() {
        return externalDocsUrl;
    }

    public void setExternalDocsUrl(String externalDocsUrl) {
        this.externalDocsUrl = externalDocsUrl;
    }

    public String[] getServers() {
        return servers;
    }

    public void setServers(String[] servers) {
        this.servers = servers;
    }

    public String getSecurityScheme() {
        return securityScheme;
    }

    public void setSecurityScheme(String securityScheme) {
        this.securityScheme = securityScheme;
    }

    public String getSecurity() {
        return security;
    }

    public void setSecurity(String security) {
        this.security = security;
    }

    public String getNameStrategy() {
        return nameStrategy;
    }

    public void setNameStrategy(String nameStrategy) {
        this.nameStrategy = nameStrategy;
    }

    public String[] getDefaultConsumesMediaTypes() {
        return defaultConsumesMediaTypes;
    }

    public void setDefaultConsumesMediaTypes(String[] defaultConsumesMediaTypes) {
        this.defaultConsumesMediaTypes = defaultConsumesMediaTypes;
    }

    public String[] getDefaultProducesMediaTypes() {
        return defaultProducesMediaTypes;
    }

    public void setDefaultProducesMediaTypes(String[] defaultProducesMediaTypes) {
        this.defaultProducesMediaTypes = defaultProducesMediaTypes;
    }

    public String[] getDefaultHttpMethods() {
        return defaultHttpMethods;
    }

    public void setDefaultHttpMethods(String[] defaultHttpMethods) {
        this.defaultHttpMethods = defaultHttpMethods;
    }

    public String[] getDefaultHttpStatusCodes() {
        return defaultHttpStatusCodes;
    }

    public void setDefaultHttpStatusCodes(String[] defaultHttpStatusCodes) {
        this.defaultHttpStatusCodes = defaultHttpStatusCodes;
    }

    public Boolean getSchemaFlatten() {
        return schemaFlatten;
    }

    public void setSchemaFlatten(Boolean schemaFlatten) {
        this.schemaFlatten = schemaFlatten;
    }

    public String[] getSchemaClassExcludes() {
        return schemaClassExcludes;
    }

    public void setSchemaClassExcludes(String[] schemaClassExcludes) {
        this.schemaClassExcludes = schemaClassExcludes;
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }

    public String getSetting(String key) {
        return settings == null ? null : settings.get(key);
    }

    public String getSetting(String key, String defaultValue) {
        return settings == null ? defaultValue : settings.getOrDefault(key, defaultValue);
    }
}
