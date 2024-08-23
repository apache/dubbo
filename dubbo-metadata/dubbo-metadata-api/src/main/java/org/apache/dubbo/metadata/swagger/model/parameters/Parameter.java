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
package org.apache.dubbo.metadata.swagger.model.parameters;

import org.apache.dubbo.metadata.swagger.model.annotations.OpenAPI31;
import org.apache.dubbo.metadata.swagger.model.media.Content;
import org.apache.dubbo.metadata.swagger.model.media.Schema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Parameter
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md#parameterObject"
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md#parameterObject"
 */
public class Parameter {
    private String name = null;
    private String in = null;
    private String description = null;
    private Boolean required = null;
    private Boolean deprecated = null;
    private Boolean allowEmptyValue = null;
    private String $ref = null;

    /**
     * Gets or Sets style
     */
    public enum StyleEnum {
        MATRIX("matrix"),
        LABEL("label"),
        FORM("form"),
        SIMPLE("simple"),
        SPACEDELIMITED("spaceDelimited"),
        PIPEDELIMITED("pipeDelimited"),
        DEEPOBJECT("deepObject");

        private String value;

        StyleEnum(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    private StyleEnum style = null;
    private Boolean explode = null;
    private Boolean allowReserved = null;
    private Schema schema = null;
    private Object example = null;
    private Content content = null;
    private Map<String, Object> extensions = null;

    /**
     * returns the name property from a Parameter instance.
     *
     * @return String name
     **/
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Parameter name(String name) {
        this.name = name;
        return this;
    }

    /**
     * returns the in property from a Parameter instance.
     *
     * @return String in
     **/
    public String getIn() {
        return in;
    }

    public void setIn(String in) {
        if ("path".equals(in)) {
            this.required = true;
        }
        this.in = in;
    }

    public Parameter in(String in) {
        setIn(in);
        return this;
    }

    /**
     * returns the description property from a Parameter instance.
     *
     * @return String description
     **/
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Parameter description(String description) {
        this.description = description;
        return this;
    }

    /**
     * returns the required property from a Parameter instance.
     *
     * @return Boolean required
     **/
    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Parameter required(Boolean required) {
        this.required = required;
        return this;
    }

    /**
     * returns the deprecated property from a Parameter instance.
     *
     * @return Boolean deprecated
     **/
    public Boolean getDeprecated() {
        return deprecated;
    }

    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    public Parameter deprecated(Boolean deprecated) {
        this.deprecated = deprecated;
        return this;
    }

    /**
     * returns the allowEmptyValue property from a Parameter instance.
     *
     * @return Boolean allowEmptyValue
     **/
    public Boolean getAllowEmptyValue() {
        return allowEmptyValue;
    }

    public void setAllowEmptyValue(Boolean allowEmptyValue) {
        this.allowEmptyValue = allowEmptyValue;
    }

    public Parameter allowEmptyValue(Boolean allowEmptyValue) {
        this.allowEmptyValue = allowEmptyValue;
        return this;
    }

    /**
     * returns the style property from a Parameter instance.
     *
     * @return StyleEnum style
     **/
    public StyleEnum getStyle() {
        return style;
    }

    public void setStyle(StyleEnum style) {
        this.style = style;
    }

    public Parameter style(StyleEnum style) {
        this.style = style;
        return this;
    }

    /**
     * returns the explode property from a Parameter instance.
     *
     * @return Boolean explode
     **/
    public Boolean getExplode() {
        return explode;
    }

    public void setExplode(Boolean explode) {
        this.explode = explode;
    }

    public Parameter explode(Boolean explode) {
        this.explode = explode;
        return this;
    }

    /**
     * returns the allowReserved property from a Parameter instance.
     *
     * @return Boolean allowReserved
     **/
    public Boolean getAllowReserved() {
        return allowReserved;
    }

    public void setAllowReserved(Boolean allowReserved) {
        this.allowReserved = allowReserved;
    }

    public Parameter allowReserved(Boolean allowReserved) {
        this.allowReserved = allowReserved;
        return this;
    }

    /**
     * returns the schema property from a Parameter instance.
     *
     * @return Schema schema
     **/
    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public Parameter schema(Schema schema) {
        this.schema = schema;
        return this;
    }

    /**
     * returns the example property from a Parameter instance.
     *
     * @return String example
     **/
    public Object getExample() {
        return example;
    }

    public void setExample(Object example) {
        this.example = example;
    }

    public Parameter example(Object example) {
        this.example = example;
        return this;
    }

    /**
     * returns the content property from a Parameter instance.
     *
     * @return Content content
     **/
    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }

    public Parameter content(Content content) {
        this.content = content;
        return this;
    }

    public String get$ref() {
        return $ref;
    }

    public void set$ref(String $ref) {
        if ($ref != null && ($ref.indexOf('.') == -1 && $ref.indexOf('/') == -1)) {
            $ref = "#/components/parameters/" + $ref;
        }
        this.$ref = $ref;
    }

    public Parameter $ref(String $ref) {
        set$ref($ref);
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
        Parameter parameter = (Parameter) o;
        return Objects.equals(this.name, parameter.name)
                && Objects.equals(this.in, parameter.in)
                && Objects.equals(this.description, parameter.description)
                && Objects.equals(this.required, parameter.required)
                && Objects.equals(this.deprecated, parameter.deprecated)
                && Objects.equals(this.allowEmptyValue, parameter.allowEmptyValue)
                && Objects.equals(this.style, parameter.style)
                && Objects.equals(this.explode, parameter.explode)
                && Objects.equals(this.allowReserved, parameter.allowReserved)
                && Objects.equals(this.schema, parameter.schema)
                && Objects.equals(this.example, parameter.example)
                && Objects.equals(this.content, parameter.content)
                && Objects.equals(this.$ref, parameter.$ref)
                && Objects.equals(this.extensions, parameter.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                name,
                in,
                description,
                required,
                deprecated,
                allowEmptyValue,
                style,
                explode,
                allowReserved,
                schema,
                example,
                content,
                $ref,
                extensions);
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public void addExtension(String name, Object value) {
        if (name == null || name.isEmpty() || !name.startsWith("x-")) {
            return;
        }
        if (this.extensions == null) {
            this.extensions = new LinkedHashMap<>();
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

    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    public Parameter extensions(Map<String, Object> extensions) {
        this.extensions = extensions;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Parameter {\n");

        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    in: ").append(toIndentedString(in)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    required: ").append(toIndentedString(required)).append("\n");
        sb.append("    deprecated: ").append(toIndentedString(deprecated)).append("\n");
        sb.append("    allowEmptyValue: ")
                .append(toIndentedString(allowEmptyValue))
                .append("\n");
        sb.append("    style: ").append(toIndentedString(style)).append("\n");
        sb.append("    explode: ").append(toIndentedString(explode)).append("\n");
        sb.append("    allowReserved: ").append(toIndentedString(allowReserved)).append("\n");
        sb.append("    schema: ").append(toIndentedString(schema)).append("\n");
        sb.append("    example: ").append(toIndentedString(example)).append("\n");
        sb.append("    content: ").append(toIndentedString(content)).append("\n");
        sb.append("    $ref: ").append(toIndentedString($ref)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    static String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
