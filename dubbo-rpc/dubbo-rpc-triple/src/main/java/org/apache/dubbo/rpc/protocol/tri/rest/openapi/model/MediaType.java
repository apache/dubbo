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

import org.apache.dubbo.rpc.protocol.tri.rest.openapi.Context;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MediaType extends Node<MediaType> {

    private Schema schema;
    private Object example;
    private Map<String, Example> examples;
    private Map<String, Encoding> encoding;

    public Schema getSchema() {
        return schema;
    }

    public MediaType setSchema(Schema schema) {
        this.schema = schema;
        return this;
    }

    public Object getExample() {
        return example;
    }

    public MediaType setExample(Object example) {
        this.example = example;
        return this;
    }

    public Map<String, Example> getExamples() {
        return examples;
    }

    public MediaType setExamples(Map<String, Example> examples) {
        this.examples = examples;
        return this;
    }

    public MediaType addExample(String name, Example example) {
        if (examples == null) {
            examples = new LinkedHashMap<>();
        }
        examples.put(name, example);
        return this;
    }

    public MediaType removeExample(String name) {
        if (examples != null) {
            examples.remove(name);
        }
        return this;
    }

    public Map<String, Encoding> getEncoding() {
        return encoding;
    }

    public MediaType setEncoding(Map<String, Encoding> encoding) {
        this.encoding = encoding;
        return this;
    }

    public MediaType addEncoding(String name, Encoding encoding) {
        if (this.encoding == null) {
            this.encoding = new LinkedHashMap<>();
        }
        this.encoding.put(name, encoding);
        return this;
    }

    public MediaType removeEncoding(String name) {
        if (encoding != null) {
            encoding.remove(name);
        }
        return this;
    }

    @Override
    public MediaType clone() {
        MediaType clone = super.clone();
        clone.schema = clone(schema);
        clone.examples = clone(examples);
        clone.encoding = clone(encoding);
        return clone;
    }

    @Override
    public Map<String, Object> writeTo(Map<String, Object> node, Context context) {
        write(node, "schema", schema, context);
        write(node, "example", example);
        write(node, "examples", examples, context);
        write(node, "encoding", encoding, context);
        writeExtensions(node);
        return node;
    }
}
