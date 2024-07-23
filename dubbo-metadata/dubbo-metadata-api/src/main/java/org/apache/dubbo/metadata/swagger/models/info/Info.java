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
package org.apache.dubbo.metadata.swagger.models.info;

import org.apache.dubbo.metadata.swagger.models.annotations.OpenAPI31;

import java.util.Objects;

/**
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md#infoObject"
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md#infoObject"
 */
public class Info {
    private String title = null;
    private String description = null;
    private String termsOfService = null;
    private Contact contact = null;
    private License license = null;
    private String version = null;
    private java.util.Map<String, Object> extensions = null;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private String summary = null;

    /**
     * returns the title property from a Info instance.
     *
     * @return String title
     **/
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Info title(String title) {
        this.title = title;
        return this;
    }

    /**
     * returns the description property from a Info instance.
     *
     * @return String description
     **/
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Info description(String description) {
        this.description = description;
        return this;
    }

    /**
     * returns the termsOfService property from a Info instance.
     *
     * @return String termsOfService
     **/
    public String getTermsOfService() {
        return termsOfService;
    }

    public void setTermsOfService(String termsOfService) {
        this.termsOfService = termsOfService;
    }

    public Info termsOfService(String termsOfService) {
        this.termsOfService = termsOfService;
        return this;
    }

    /**
     * returns the contact property from a Info instance.
     *
     * @return Contact contact
     **/
    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public Info contact(Contact contact) {
        this.contact = contact;
        return this;
    }

    /**
     * returns the license property from a Info instance.
     *
     * @return License license
     **/
    public License getLicense() {
        return license;
    }

    public void setLicense(License license) {
        this.license = license;
    }

    public Info license(License license) {
        this.license = license;
        return this;
    }

    /**
     * returns the version property from a Info instance.
     *
     * @return String version
     **/
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Info version(String version) {
        this.version = version;
        return this;
    }

    /**
     * returns the summary property from a Info instance.
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     * @return String
     **/
    @OpenAPI31
    public String getSummary() {
        return summary;
    }

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Info summary(String summary) {
        this.summary = summary;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Info info = (Info) o;
        return Objects.equals(this.title, info.title)
                && Objects.equals(this.description, info.description)
                && Objects.equals(this.termsOfService, info.termsOfService)
                && Objects.equals(this.contact, info.contact)
                && Objects.equals(this.license, info.license)
                && Objects.equals(this.version, info.version)
                && Objects.equals(this.extensions, info.extensions)
                && Objects.equals(this.summary, info.summary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, description, termsOfService, contact, license, version, extensions, summary);
    }

    public java.util.Map<String, Object> getExtensions() {
        return extensions;
    }

    public void addExtension(String name, Object value) {
        if (name == null || name.isEmpty() || !name.startsWith("x-")) {
            return;
        }
        if (this.extensions == null) {
            this.extensions = new java.util.LinkedHashMap<>();
        }
        this.extensions.put(name, value);
    }

    @OpenAPI31
    public void addExtension31(String name, Object value) {
        if (name != null && (name.startsWith("x-oas-") || name.startsWith("x-oai-"))) {
            return;
        }
        addExtension(name, value);
    }

    public void setExtensions(java.util.Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    public Info extensions(java.util.Map<String, Object> extensions) {
        this.extensions = extensions;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Info {\n");

        sb.append("    title: ").append(toIndentedString(title)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    summary: ").append(toIndentedString(summary)).append("\n");
        sb.append("    termsOfService: ")
                .append(toIndentedString(termsOfService))
                .append("\n");
        sb.append("    contact: ").append(toIndentedString(contact)).append("\n");
        sb.append("    license: ").append(toIndentedString(license)).append("\n");
        sb.append("    version: ").append(toIndentedString(version)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
