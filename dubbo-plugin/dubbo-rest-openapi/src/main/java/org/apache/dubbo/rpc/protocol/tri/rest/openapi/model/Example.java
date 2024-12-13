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

import java.util.Map;

public final class Example extends Node<Example> {

    private String summary;
    private String description;
    private Object value;
    private String externalValue;

    public String getSummary() {
        return summary;
    }

    public Example setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Example setDescription(String description) {
        this.description = description;
        return this;
    }

    public Object getValue() {
        return value;
    }

    public Example setValue(Object value) {
        this.value = value;
        return this;
    }

    public String getExternalValue() {
        return externalValue;
    }

    public Example setExternalValue(String externalValue) {
        this.externalValue = externalValue;
        return this;
    }

    @Override
    public Map<String, Object> writeTo(Map<String, Object> exampleNode, Context context) {
        write(exampleNode, "summary", summary);
        write(exampleNode, "description", description);
        write(exampleNode, "value", value);
        write(exampleNode, "externalValue", externalValue);
        writeExtensions(exampleNode);
        return exampleNode;
    }
}
