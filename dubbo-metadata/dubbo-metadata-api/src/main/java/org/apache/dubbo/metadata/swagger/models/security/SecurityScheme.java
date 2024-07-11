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
package org.apache.dubbo.metadata.swagger.models.security;

import org.apache.dubbo.metadata.swagger.models.annotations.OpenAPI31;

/**
 * SecurityScheme
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md#securitySchemeObject"
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md#securitySchemeObject"
 */
public class SecurityScheme {
    /**
     * Gets or Sets type
     */
    public enum Type {
        APIKEY("apiKey"),
        HTTP("http"),
        OAUTH2("oauth2"),
        OPENIDCONNECT("openIdConnect"),
        MUTUALTLS("mutualTLS");

        private String value;

        Type(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    private Type type = null;
    private String description = null;
    private String name = null;
    private String $ref = null;

    /**
     * Gets or Sets in
     */
    public enum In {
        COOKIE("cookie"),

        HEADER("header"),

        QUERY("query");

        private String value;

        In(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    private In in = null;
    private String scheme = null;
    private String bearerFormat = null;
    private OAuthFlows flows = null;
    private String openIdConnectUrl = null;
    private java.util.Map<String, Object> extensions = null;

    /**
     * returns the type property from a SecurityScheme instance.
     *
     * @return Type type
     **/
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public SecurityScheme type(Type type) {
        this.type = type;
        return this;
    }

    /**
     * returns the description property from a SecurityScheme instance.
     *
     * @return String description
     **/
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SecurityScheme description(String description) {
        this.description = description;
        return this;
    }

    /**
     * returns the name property from a SecurityScheme instance.
     *
     * @return String name
     **/
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SecurityScheme name(String name) {
        this.name = name;
        return this;
    }

    /**
     * returns the in property from a SecurityScheme instance.
     *
     * @return In in
     **/
    public In getIn() {
        return in;
    }

    public void setIn(In in) {
        this.in = in;
    }

    public SecurityScheme in(In in) {
        this.in = in;
        return this;
    }

    /**
     * returns the scheme property from a SecurityScheme instance.
     *
     * @return String scheme
     **/
    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public SecurityScheme scheme(String scheme) {
        this.scheme = scheme;
        return this;
    }

    /**
     * returns the bearerFormat property from a SecurityScheme instance.
     *
     * @return String bearerFormat
     **/
    public String getBearerFormat() {
        return bearerFormat;
    }

    public void setBearerFormat(String bearerFormat) {
        this.bearerFormat = bearerFormat;
    }

    public SecurityScheme bearerFormat(String bearerFormat) {
        this.bearerFormat = bearerFormat;
        return this;
    }

    /**
     * returns the flows property from a SecurityScheme instance.
     *
     * @return OAuthFlows flows
     **/
    public OAuthFlows getFlows() {
        return flows;
    }

    public void setFlows(OAuthFlows flows) {
        this.flows = flows;
    }

    public SecurityScheme flows(OAuthFlows flows) {
        this.flows = flows;
        return this;
    }

    /**
     * returns the openIdConnectUrl property from a SecurityScheme instance.
     *
     * @return String openIdConnectUrl
     **/
    public String getOpenIdConnectUrl() {
        return openIdConnectUrl;
    }

    public void setOpenIdConnectUrl(String openIdConnectUrl) {
        this.openIdConnectUrl = openIdConnectUrl;
    }

    public SecurityScheme openIdConnectUrl(String openIdConnectUrl) {
        this.openIdConnectUrl = openIdConnectUrl;
        return this;
    }

    public java.util.Map<String, Object> getExtensions() {
        return extensions;
    }

    @OpenAPI31
    public void addExtension31(String name, Object value) {
        if (name != null && (name.startsWith("x-oas-") || name.startsWith("x-oai-"))) {
            return;
        }
        addExtension(name, value);
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

    public void setExtensions(java.util.Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    public SecurityScheme extensions(java.util.Map<String, Object> extensions) {
        this.extensions = extensions;
        return this;
    }

    /**
     * returns the $ref property from an SecurityScheme instance.
     *
     * @return String $ref
     **/
    public String get$ref() {
        return $ref;
    }

    public void set$ref(String $ref) {
        if ($ref != null && ($ref.indexOf('.') == -1 && $ref.indexOf('/') == -1)) {
            $ref = "#/components/securitySchemes/" + $ref;
        }
        this.$ref = $ref;
    }

    public SecurityScheme $ref(String $ref) {
        set$ref($ref);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SecurityScheme)) {
            return false;
        }

        SecurityScheme that = (SecurityScheme) o;

        if (type != that.type) {
            return false;
        }
        if (description != null ? !description.equals(that.description) : that.description != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if ($ref != null ? !$ref.equals(that.$ref) : that.$ref != null) {
            return false;
        }
        if (in != that.in) {
            return false;
        }
        if (scheme != null ? !scheme.equals(that.scheme) : that.scheme != null) {
            return false;
        }
        if (bearerFormat != null ? !bearerFormat.equals(that.bearerFormat) : that.bearerFormat != null) {
            return false;
        }
        if (flows != null ? !flows.equals(that.flows) : that.flows != null) {
            return false;
        }
        if (openIdConnectUrl != null
                ? !openIdConnectUrl.equals(that.openIdConnectUrl)
                : that.openIdConnectUrl != null) {
            return false;
        }
        return extensions != null ? extensions.equals(that.extensions) : that.extensions == null;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + ($ref != null ? $ref.hashCode() : 0);
        result = 31 * result + (in != null ? in.hashCode() : 0);
        result = 31 * result + (scheme != null ? scheme.hashCode() : 0);
        result = 31 * result + (bearerFormat != null ? bearerFormat.hashCode() : 0);
        result = 31 * result + (flows != null ? flows.hashCode() : 0);
        result = 31 * result + (openIdConnectUrl != null ? openIdConnectUrl.hashCode() : 0);
        result = 31 * result + (extensions != null ? extensions.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class SecurityScheme {\n");

        sb.append("    type: ").append(toIndentedString(type)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    in: ").append(toIndentedString(in)).append("\n");
        sb.append("    scheme: ").append(toIndentedString(scheme)).append("\n");
        sb.append("    bearerFormat: ").append(toIndentedString(bearerFormat)).append("\n");
        sb.append("    flows: ").append(toIndentedString(flows)).append("\n");
        sb.append("    openIdConnectUrl: ")
                .append(toIndentedString(openIdConnectUrl))
                .append("\n");
        sb.append("    $ref: ").append(toIndentedString($ref)).append("\n");
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
