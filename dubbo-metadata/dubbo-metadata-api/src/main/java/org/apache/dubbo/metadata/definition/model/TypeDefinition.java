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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 2015/1/27.
 */
public class TypeDefinition implements Serializable {

    /**
     * the name of type
     *
     * @see Class#getCanonicalName()
     * @see org.apache.dubbo.metadata.definition.util.ClassUtils#getCanonicalNameForParameterizedType(ParameterizedType) 
     */
    private String type;

    /**
     * the items(generic parameter) of Map/List(ParameterizedType)
     * <p>
     * if this type is not ParameterizedType, the items is null or empty
     */
    @SerializedName("items")
    private List<String> items;

    /**
     * the enum's value
     * <p>
     * If this type is not enum, enums is null or empty
     */
    @SerializedName("enum")
    private List<String> enums;

    /**
     * the key is property name,
     * the value is property's type name
     */
    private Map<String, String> properties;

    public TypeDefinition() {
    }

    public TypeDefinition(String type) {
        this.type = type;
    }

    public List<String> getEnums() {
        if (enums == null) {
            enums = new ArrayList<>();
        }
        return enums;
    }

    public List<String> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public Map<String, String> getProperties() {
        if (properties == null) {
            properties = new HashMap<>();
        }
        return properties;
    }

    public String getType() {
        return type;
    }

    public void setEnums(List<String> enums) {
        this.enums = enums;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "TypeDefinition [type=" + type + ", properties=" + properties + "]";
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
        return Objects.equals(getType(), that.getType()) &&
                Objects.equals(getItems(), that.getItems()) &&
                Objects.equals(getEnums(), that.getEnums()) &&
                Objects.equals(getProperties(), that.getProperties());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getItems(), getEnums(), getProperties());
    }
}
