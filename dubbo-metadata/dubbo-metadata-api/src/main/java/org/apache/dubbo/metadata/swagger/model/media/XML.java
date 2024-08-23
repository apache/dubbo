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
package org.apache.dubbo.metadata.swagger.model.media;

import org.apache.dubbo.metadata.swagger.model.annotations.OpenAPI31;

import java.util.Objects;

/**
 * XML
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md#xmlObject"
 */
public class XML {
    private String name = null;
    private String namespace = null;
    private String prefix = null;
    private Boolean attribute = null;
    private Boolean wrapped = null;
    private java.util.Map<String, Object> extensions = null;

    /**
     * returns the name property from a XML instance.
     *
     * @return String name
     **/
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public XML name(String name) {
        this.name = name;
        return this;
    }

    /**
     * returns the namespace property from a XML instance.
     *
     * @return String namespace
     **/
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public XML namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    /**
     * returns the prefix property from a XML instance.
     *
     * @return String prefix
     **/
    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public XML prefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    /**
     * returns the attribute property from a XML instance.
     *
     * @return Boolean attribute
     **/
    public Boolean getAttribute() {
        return attribute;
    }

    public void setAttribute(Boolean attribute) {
        this.attribute = attribute;
    }

    public XML attribute(Boolean attribute) {
        this.attribute = attribute;
        return this;
    }

    /**
     * returns the wrapped property from a XML instance.
     *
     * @return Boolean wrapped
     **/
    public Boolean getWrapped() {
        return wrapped;
    }

    public void setWrapped(Boolean wrapped) {
        this.wrapped = wrapped;
    }

    public XML wrapped(Boolean wrapped) {
        this.wrapped = wrapped;
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
        XML XML = (XML) o;
        return Objects.equals(this.name, XML.name)
                && Objects.equals(this.namespace, XML.namespace)
                && Objects.equals(this.prefix, XML.prefix)
                && Objects.equals(this.attribute, XML.attribute)
                && Objects.equals(this.wrapped, XML.wrapped)
                && Objects.equals(this.extensions, XML.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, namespace, prefix, attribute, wrapped, extensions);
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

    public XML extensions(java.util.Map<String, Object> extensions) {
        this.extensions = extensions;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class XML {\n");

        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    namespace: ").append(toIndentedString(namespace)).append("\n");
        sb.append("    prefix: ").append(toIndentedString(prefix)).append("\n");
        sb.append("    attribute: ").append(toIndentedString(attribute)).append("\n");
        sb.append("    wrapped: ").append(toIndentedString(wrapped)).append("\n");
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
