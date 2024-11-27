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

import org.apache.dubbo.rpc.protocol.tri.rest.openapi.Constants;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.Context;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class Schema extends Node<Schema> {

    public enum Type {
        STRING("string"),
        INTEGER("integer"),
        NUMBER("number"),
        BOOLEAN("boolean"),
        OBJECT("object"),
        ARRAY("array");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public Type of(String value) {
            for (Type type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            return STRING;
        }
    }

    private String ref;
    private String format;
    private String name;
    private String title;
    private String description;
    private Object defaultValue;
    private BigDecimal multipleOf;
    private BigDecimal maximum;
    private Boolean exclusiveMaximum;
    private BigDecimal minimum;
    private Boolean exclusiveMinimum;
    private Integer maxLength;
    private Integer minLength;
    private String pattern;
    private Integer maxItems;
    private Integer minItems;
    private Boolean uniqueItems;
    private Integer maxProperties;
    private Integer minProperties;
    private Boolean required;
    private List<Object> enumeration;
    private Type type;
    private Schema items;
    private Map<String, Schema> properties;
    private Schema additionalPropertiesSchema;
    private Boolean additionalPropertiesBoolean;
    private Boolean readOnly;
    private XML xml;
    private ExternalDocs externalDocs;
    private Object example;
    private List<Schema> allOf;
    private List<Schema> oneOf;
    private List<Schema> anyOf;
    private Schema not;
    private Discriminator discriminator;
    private Boolean nullable;
    private Boolean writeOnly;
    private Boolean deprecated;

    private String group;
    private String version;
    private Class<?> javaType;
    private transient Schema targetSchema;
    private transient List<Schema> sourceSchemas;

    public String getRef() {
        return ref;
    }

    public Schema setRef(String ref) {
        this.ref = ref;
        return this;
    }

    public String getFormat() {
        return format;
    }

    public Schema setFormat(String format) {
        this.format = format;
        return this;
    }

    public String getName() {
        return name;
    }

    public Schema setName(String name) {
        this.name = name;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Schema setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Schema setDescription(String description) {
        this.description = description;
        return this;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Schema setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public BigDecimal getMultipleOf() {
        return multipleOf;
    }

    public Schema setMultipleOf(BigDecimal multipleOf) {
        this.multipleOf = multipleOf;
        return this;
    }

    public BigDecimal getMaximum() {
        return maximum;
    }

    public Schema setMaximum(BigDecimal maximum) {
        this.maximum = maximum;
        return this;
    }

    public Boolean getExclusiveMaximum() {
        return exclusiveMaximum;
    }

    public Schema setExclusiveMaximum(Boolean exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
        return this;
    }

    public BigDecimal getMinimum() {
        return minimum;
    }

    public Schema setMinimum(BigDecimal minimum) {
        this.minimum = minimum;
        return this;
    }

    public Boolean getExclusiveMinimum() {
        return exclusiveMinimum;
    }

    public Schema setExclusiveMinimum(Boolean exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
        return this;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public Schema setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public Schema setMinLength(Integer minLength) {
        this.minLength = minLength;
        return this;
    }

    public String getPattern() {
        return pattern;
    }

    public Schema setPattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    public Integer getMaxItems() {
        return maxItems;
    }

    public Schema setMaxItems(Integer maxItems) {
        this.maxItems = maxItems;
        return this;
    }

    public Integer getMinItems() {
        return minItems;
    }

    public Schema setMinItems(Integer minItems) {
        this.minItems = minItems;
        return this;
    }

    public Boolean getUniqueItems() {
        return uniqueItems;
    }

    public Schema setUniqueItems(Boolean uniqueItems) {
        this.uniqueItems = uniqueItems;
        return this;
    }

    public Integer getMaxProperties() {
        return maxProperties;
    }

    public Schema setMaxProperties(Integer maxProperties) {
        this.maxProperties = maxProperties;
        return this;
    }

    public Integer getMinProperties() {
        return minProperties;
    }

    public Schema setMinProperties(Integer minProperties) {
        this.minProperties = minProperties;
        return this;
    }

    public Boolean getRequired() {
        return required;
    }

    public Schema setRequired(Boolean required) {
        this.required = required;
        return this;
    }

    public List<Object> getEnumeration() {
        return enumeration;
    }

    public Schema setEnumeration(List<Object> enumeration) {
        this.enumeration = enumeration;
        return this;
    }

    public Schema addEnumeration(Object enumeration) {
        if (this.enumeration == null) {
            this.enumeration = new ArrayList<>();
        }
        this.enumeration.add(enumeration);
        return this;
    }

    public Schema removeEnumeration(Object enumeration) {
        if (this.enumeration != null) {
            this.enumeration.remove(enumeration);
        }
        return this;
    }

    public Type getType() {
        return type;
    }

    public Schema setType(Type type) {
        this.type = type;
        return this;
    }

    public Schema getItems() {
        return items;
    }

    public Schema setItems(Schema items) {
        this.items = items;
        return this;
    }

    public Map<String, Schema> getProperties() {
        return properties;
    }

    public Schema getProperty(String name) {
        return properties == null ? null : properties.get(name);
    }

    public Schema setProperties(Map<String, Schema> properties) {
        this.properties = properties;
        return this;
    }

    public Schema addProperty(String name, Schema schema) {
        if (schema == null) {
            return this;
        }
        if (properties == null) {
            properties = new LinkedHashMap<>();
        }
        properties.put(name, schema);
        return this;
    }

    public Schema removeProperty(String name) {
        if (properties != null) {
            properties.remove(name);
        }
        return this;
    }

    public Schema getAdditionalPropertiesSchema() {
        return additionalPropertiesSchema;
    }

    public Schema setAdditionalPropertiesSchema(Schema additionalPropertiesSchema) {
        this.additionalPropertiesSchema = additionalPropertiesSchema;
        return this;
    }

    public Boolean getAdditionalPropertiesBoolean() {
        return additionalPropertiesBoolean;
    }

    public Schema setAdditionalPropertiesBoolean(Boolean additionalPropertiesBoolean) {
        this.additionalPropertiesBoolean = additionalPropertiesBoolean;
        return this;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public Schema setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    public XML getXml() {
        return xml;
    }

    public Schema setXml(XML xml) {
        this.xml = xml;
        return this;
    }

    public ExternalDocs getExternalDocs() {
        return externalDocs;
    }

    public Schema setExternalDocs(ExternalDocs externalDocs) {
        this.externalDocs = externalDocs;
        return this;
    }

    public Object getExample() {
        return example;
    }

    public Schema setExample(Object example) {
        this.example = example;
        return this;
    }

    public List<Schema> getAllOf() {
        return allOf;
    }

    public Schema setAllOf(List<Schema> allOf) {
        this.allOf = allOf;
        return this;
    }

    public Schema addAllOf(Schema schema) {
        if (allOf == null) {
            allOf = new ArrayList<>();
        }
        allOf.add(schema);
        return this;
    }

    public List<Schema> getOneOf() {
        return oneOf;
    }

    public Schema setOneOf(List<Schema> oneOf) {
        this.oneOf = oneOf;
        return this;
    }

    public Schema addOneOf(Schema schema) {
        if (oneOf == null) {
            oneOf = new ArrayList<>();
        }
        oneOf.add(schema);
        return this;
    }

    public List<Schema> getAnyOf() {
        return anyOf;
    }

    public Schema setAnyOf(List<Schema> anyOf) {
        this.anyOf = anyOf;
        return this;
    }

    public Schema addAnyOf(Schema schema) {
        if (anyOf == null) {
            anyOf = new ArrayList<>();
        }
        anyOf.add(schema);
        return this;
    }

    public Schema getNot() {
        return not;
    }

    public Schema setNot(Schema not) {
        this.not = not;
        return this;
    }

    public Discriminator getDiscriminator() {
        return discriminator;
    }

    public Schema setDiscriminator(Discriminator discriminator) {
        this.discriminator = discriminator;
        return this;
    }

    public Boolean getNullable() {
        return nullable;
    }

    public Schema setNullable(Boolean nullable) {
        this.nullable = nullable;
        return this;
    }

    public Boolean getWriteOnly() {
        return writeOnly;
    }

    public Schema setWriteOnly(Boolean writeOnly) {
        this.writeOnly = writeOnly;
        return this;
    }

    public Boolean getDeprecated() {
        return deprecated;
    }

    public Schema setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
        return this;
    }

    public String getGroup() {
        return group;
    }

    public Schema setGroup(String group) {
        this.group = group;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public Schema setVersion(String version) {
        this.version = version;
        return this;
    }

    public Class<?> getJavaType() {
        return javaType;
    }

    public Schema setJavaType(Class<?> javaType) {
        this.javaType = javaType;
        return this;
    }

    public Schema getTargetSchema() {
        return targetSchema;
    }

    public Schema setTargetSchema(Schema targetSchema) {
        this.targetSchema = targetSchema;
        return this;
    }

    public List<Schema> getSourceSchemas() {
        return sourceSchemas;
    }

    public Schema setSourceSchemas(List<Schema> sourceSchemas) {
        this.sourceSchemas = sourceSchemas;
        return this;
    }

    public void addSourceSchema(Schema sourceSchema) {
        if (sourceSchemas == null) {
            sourceSchemas = new LinkedList<>();
        }
        sourceSchemas.add(sourceSchema);
    }

    @Override
    public Schema clone() {
        Schema clone = super.clone();
        if (enumeration != null) {
            clone.enumeration = new ArrayList<>(enumeration);
        }
        clone.items = clone(items);
        clone.properties = clone(properties);
        clone.additionalPropertiesSchema = clone(additionalPropertiesSchema);
        clone.xml = clone(xml);
        clone.externalDocs = clone(externalDocs);
        clone.allOf = clone(allOf);
        clone.oneOf = clone(oneOf);
        clone.anyOf = clone(anyOf);
        clone.not = clone(not);
        clone.discriminator = clone(discriminator);
        return clone;
    }

    @Override
    public Map<String, Object> writeTo(Map<String, Object> schema, Context context) {
        if (ref != null) {
            schema.put("$ref", ref);
        }
        write(schema, "format", format);
        write(schema, "title", title);
        write(schema, "description", description);
        write(schema, "default", defaultValue);
        write(schema, "multipleOf", multipleOf);
        write(schema, "maximum", maximum);
        write(schema, "exclusiveMaximum", exclusiveMaximum);
        write(schema, "minimum", minimum);
        write(schema, "exclusiveMinimum", exclusiveMinimum);
        write(schema, "maxLength", maxLength);
        write(schema, "minLength", minLength);
        write(schema, "pattern", pattern);
        write(schema, "maxItems", maxItems);
        write(schema, "minItems", minItems);
        write(schema, "uniqueItems", uniqueItems);
        write(schema, "maxProperties", maxProperties);
        write(schema, "minProperties", minProperties);
        write(schema, "required", required);
        write(schema, "enum", enumeration);
        if (type != null) {
            if (context.isOpenAPI31()) {
                if (nullable == null || !nullable) {
                    write(schema, "type", type.toString());
                } else {
                    write(schema, "type", new String[] {type.toString(), "null"});
                }
            } else {
                write(schema, "type", type.toString());
                write(schema, "nullable", nullable);
            }
        }
        write(schema, "items", items, context);
        write(schema, "properties", properties, context);
        if (additionalPropertiesBoolean == null) {
            write(schema, "additionalProperties", additionalPropertiesSchema, context);
        } else {
            schema.put("additionalProperties", additionalPropertiesBoolean);
        }
        write(schema, "readOnly", readOnly);
        write(schema, "xml", xml, context);
        write(schema, "externalDocs", externalDocs, context);
        write(schema, "example", example);
        write(schema, "allOf", allOf, context);
        write(schema, "oneOf", oneOf, context);
        write(schema, "anyOf", anyOf, context);
        write(schema, "not", not, context);
        write(schema, "discriminator", discriminator, context);
        write(schema, "writeOnly", writeOnly);
        write(schema, "deprecated", deprecated);
        writeExtensions(schema);
        if (javaType != null) {
            schema.put(Constants.X_JAVA_CLASS, javaType.getName());
        }
        return schema;
    }
}
