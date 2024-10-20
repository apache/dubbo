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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * MediaType
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md#mediaTypeObject"
 */
public class MediaType {
    private Schema schema = null;
    private Object example = null;
    private Map<String, Object> extensions = null;

    private boolean exampleSetFlag;

    /**
     * returns the schema property from a MediaType instance.
     *
     * @return Schema schema
     **/
    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public MediaType schema(Schema schema) {
        this.schema = schema;
        return this;
    }

    /**
     * returns the example property from a MediaType instance.
     *
     * @return String example
     **/
    public Object getExample() {
        return example;
    }

    public void setExample(Object example) {
        if (this.schema == null) {
            this.example = example;
            this.exampleSetFlag = true;
            return;
        }
        this.example = this.schema.cast(example);
        if (!(example != null && this.example == null)) {
            this.exampleSetFlag = true;
        }
    }

    public MediaType example(Object example) {
        setExample(example);
        return this;
    }

    public boolean getExampleSetFlag() {
        return exampleSetFlag;
    }

    public void setExampleSetFlag(boolean exampleSetFlag) {
        this.exampleSetFlag = exampleSetFlag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MediaType mediaType = (MediaType) o;
        return Objects.equals(this.schema, mediaType.schema)
                && Objects.equals(this.example, mediaType.example)
                && Objects.equals(this.extensions, mediaType.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema, example, extensions);
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

    public MediaType extensions(Map<String, Object> extensions) {
        this.extensions = extensions;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class MediaType {\n");

        sb.append("    schema: ").append(toIndentedString(schema)).append("\n");
        sb.append("    example: ").append(toIndentedString(example)).append("\n");
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
