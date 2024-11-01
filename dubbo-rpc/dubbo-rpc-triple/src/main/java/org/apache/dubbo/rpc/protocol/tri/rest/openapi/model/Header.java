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

import org.apache.dubbo.rpc.protocol.tri.rest.openapi.WriteContext;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Header extends Node<Header> {

    public enum Style {
        SIMPLE("simple");

        private final String value;

        Style(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private String description;
    private Boolean required;
    private Boolean deprecated;
    private Boolean allowEmptyValue;
    private final Style style = Style.SIMPLE;
    private Boolean explode;
    private Schema schema;
    private Object example;
    private Map<String, Example> examples;
    private Map<String, MediaType> content;

    public String getDescription() {
        return description;
    }

    public Header setDescription(String description) {
        this.description = description;
        return this;
    }

    public Boolean getRequired() {
        return required;
    }

    public Header setRequired(Boolean required) {
        this.required = required;
        return this;
    }

    public Boolean getDeprecated() {
        return deprecated;
    }

    public Header setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
        return this;
    }

    public Boolean getAllowEmptyValue() {
        return allowEmptyValue;
    }

    public Header setAllowEmptyValue(Boolean allowEmptyValue) {
        this.allowEmptyValue = allowEmptyValue;
        return this;
    }

    public Style getStyle() {
        return style;
    }

    public Boolean getExplode() {
        return explode;
    }

    public Header setExplode(Boolean explode) {
        this.explode = explode;
        return this;
    }

    public Schema getSchema() {
        return schema;
    }

    public Header setSchema(Schema schema) {
        this.schema = schema;
        return this;
    }

    public Object getExample() {
        return example;
    }

    public Header setExample(Object example) {
        this.example = example;
        return this;
    }

    public Map<String, Example> getExamples() {
        return examples;
    }

    public Header setExamples(Map<String, Example> examples) {
        this.examples = examples;
        return this;
    }

    public Header addExample(String name, Example example) {
        if (examples == null) {
            examples = new LinkedHashMap<>();
        }
        examples.put(name, example);
        return this;
    }

    public Header removeExample(String name) {
        if (examples != null) {
            examples.remove(name);
        }
        return this;
    }

    public Map<String, MediaType> getContent() {
        return content;
    }

    public Header setContent(Map<String, MediaType> content) {
        this.content = content;
        return this;
    }

    public Header addContent(String name, MediaType mediaType) {
        if (content == null) {
            content = new LinkedHashMap<>();
        }
        content.put(name, mediaType);
        return this;
    }

    public Header removeContent(String name) {
        if (content != null) {
            content.remove(name);
        }
        return this;
    }

    @Override
    public Header clone() {
        Header clone = super.clone();
        clone.schema = clone(schema);
        clone.examples = clone(examples);
        clone.content = clone(content);
        return clone;
    }

    @Override
    public Map<String, Object> writeTo(Map<String, Object> node, WriteContext context) {
        write(node, "description", description);
        write(node, "required", required);
        write(node, "deprecated", deprecated);
        write(node, "allowEmptyValue", allowEmptyValue);
        write(node, "style", style);
        write(node, "explode", explode);
        write(node, "schema", schema, context);
        write(node, "example", example);
        write(node, "examples", examples, context);
        write(node, "content", content, context);
        writeExtensions(node);
        return node;
    }
}
