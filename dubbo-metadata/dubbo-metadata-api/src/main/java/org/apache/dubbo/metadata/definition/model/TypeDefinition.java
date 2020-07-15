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
package org.apache.dubbo.metadata.definition.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 2015/1/27.
 */
public class TypeDefinition implements Serializable {

    private String id;
    private String type;
    @SerializedName("items")
    private List<TypeDefinition> items;
    @SerializedName("enum")
    private List<String> enums;
    private String $ref;
    private Map<String, TypeDefinition> properties;
    private String typeBuilderName;

    public TypeDefinition() {
    }

    public TypeDefinition(String type) {
        this.type = type;
    }

    public String get$ref() {
        return $ref;
    }

    public List<String> getEnums() {
        if (enums == null) {
            enums = new ArrayList<String>();
        }
        return enums;
    }

    public String getId() {
        return id;
    }

    public List<TypeDefinition> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public Map<String, TypeDefinition> getProperties() {
        if (properties == null) {
            properties = new HashMap<>();
        }
        return properties;
    }

    public String getType() {
        return type;
    }

    public String getTypeBuilderName() {
        return typeBuilderName;
    }

    public void set$ref(String $ref) {
        this.$ref = $ref;
    }

    public void setEnums(List<String> enums) {
        this.enums = enums;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setItems(List<TypeDefinition> items) {
        this.items = items;
    }

    public void setProperties(Map<String, TypeDefinition> properties) {
        this.properties = properties;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTypeBuilderName(String typeBuilderName) {
        this.typeBuilderName = typeBuilderName;
    }

    @Override
    public String toString() {
        return "TypeDefinition [id=" + id + ", type=" + type + ", properties=" + properties + ", $ref=" + $ref + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TypeDefinition)) {
            return false;
        }
        TypeDefinition that = (TypeDefinition) o;
        return Objects.equals(getId(), that.getId()) &&
                Objects.equals(getType(), that.getType()) &&
                Objects.equals(getItems(), that.getItems()) &&
                Objects.equals(getEnums(), that.getEnums()) &&
                Objects.equals(get$ref(), that.get$ref()) &&
                Objects.equals(getProperties(), that.getProperties());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getType(), getItems(), getEnums(), get$ref(), getProperties());
    }
}
