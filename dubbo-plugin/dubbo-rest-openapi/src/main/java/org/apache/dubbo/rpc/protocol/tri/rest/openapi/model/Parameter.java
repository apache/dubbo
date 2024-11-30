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
package org.apache.dubbo.rpc.protocol.tri.rest.openapi.model;

import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.Context;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class Parameter extends Node<Parameter> {

    public enum In {
        PATH("path"),
        QUERY("query"),
        HEADER("header"),
        COOKIE("cookie");

        private final String value;

        In(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public enum Style {
        MATRIX("matrix"),
        LABEL("label"),
        FORM("form"),
        SIMPLE("simple"),
        SPACE_DELIMITED("spaceDelimited"),
        PIPE_DELIMITED("pipeDelimited"),
        DEEP_OBJECT("deepObject");

        private final String value;

        Style(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private final String name;
    private final In in;
    private String description;
    private Boolean required;
    private Boolean deprecated;
    private Boolean allowEmptyValue;
    private Style style;
    private Boolean explode;
    private Boolean allowReserved;
    private Schema schema;
    private Object example;
    private Map<String, Example> examples;
    private Map<String, MediaType> contents;

    private transient ParameterMeta meta;

    public Parameter(String name, In in) {
        this.name = Objects.requireNonNull(name);
        this.in = Objects.requireNonNull(in);
    }

    public String getName() {
        return name;
    }

    public In getIn() {
        return in;
    }

    public String getDescription() {
        return description;
    }

    public Parameter setDescription(String description) {
        this.description = description;
        return this;
    }

    public Boolean getRequired() {
        return required;
    }

    public Parameter setRequired(Boolean required) {
        this.required = required;
        return this;
    }

    public Boolean getDeprecated() {
        return deprecated;
    }

    public Parameter setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
        return this;
    }

    public Boolean getAllowEmptyValue() {
        return allowEmptyValue;
    }

    public Parameter setAllowEmptyValue(Boolean allowEmptyValue) {
        this.allowEmptyValue = allowEmptyValue;
        return this;
    }

    public Style getStyle() {
        return style;
    }

    public Parameter setStyle(Style style) {
        this.style = style;
        return this;
    }

    public Boolean getExplode() {
        return explode;
    }

    public Parameter setExplode(Boolean explode) {
        this.explode = explode;
        return this;
    }

    public Boolean getAllowReserved() {
        return allowReserved;
    }

    public Parameter setAllowReserved(Boolean allowReserved) {
        this.allowReserved = allowReserved;
        return this;
    }

    public Schema getSchema() {
        return schema;
    }

    public Parameter setSchema(Schema schema) {
        this.schema = schema;
        return this;
    }

    public Object getExample() {
        return example;
    }

    public Parameter setExample(Object example) {
        this.example = example;
        return this;
    }

    public Map<String, Example> getExamples() {
        return examples;
    }

    public Parameter setExamples(Map<String, Example> examples) {
        this.examples = examples;
        return this;
    }

    public Parameter addExample(String name, Example example) {
        if (examples == null) {
            examples = new LinkedHashMap<>();
        }
        examples.put(name, example);
        return this;
    }

    public Parameter removeExample(String name) {
        if (examples != null) {
            examples.remove(name);
        }
        return this;
    }

    public Map<String, MediaType> getContents() {
        return contents;
    }

    public Parameter setContents(Map<String, MediaType> contents) {
        this.contents = contents;
        return this;
    }

    public Parameter addContent(String name, MediaType content) {
        if (contents == null) {
            contents = new LinkedHashMap<>();
        }
        contents.put(name, content);
        return this;
    }

    public Parameter removeContent(String name) {
        if (contents != null) {
            contents.remove(name);
        }
        return this;
    }

    public ParameterMeta getMeta() {
        return meta;
    }

    public Parameter setMeta(ParameterMeta meta) {
        this.meta = meta;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != Parameter.class) {
            return false;
        }
        Parameter other = (Parameter) obj;
        return name.equals(other.name) && in == other.in;
    }

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + in.hashCode();
    }

    @Override
    public Parameter clone() {
        Parameter clone = super.clone();
        clone.schema = clone(schema);
        clone.examples = clone(examples);
        clone.contents = clone(contents);
        return clone;
    }

    @Override
    public Map<String, Object> writeTo(Map<String, Object> node, Context context) {
        write(node, "name", name);
        write(node, "in", in.toString());
        write(node, "description", description);
        write(node, "required", required);
        write(node, "deprecated", deprecated);
        write(node, "allowEmptyValue", allowEmptyValue);
        write(node, "style", style);
        write(node, "explode", explode);
        write(node, "allowReserved", allowReserved);
        write(node, "schema", schema, context);
        write(node, "example", example);
        write(node, "examples", examples, context);
        write(node, "content", contents, context);
        writeExtensions(node);
        return node;
    }
}
