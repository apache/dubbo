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

import org.apache.dubbo.metadata.swagger.model.SpecVersion;
import org.apache.dubbo.metadata.swagger.model.annotations.OpenAPI30;
import org.apache.dubbo.metadata.swagger.model.annotations.OpenAPI31;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.apache.dubbo.metadata.swagger.model.Components.COMPONENTS_SCHEMAS_REF;

/**
 * Schema
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md#schemaObject"
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md#schemaObject"
 */
public class Schema<T> {

    public static final String BIND_TYPE_AND_TYPES = "bind-type";
    public static final String BINARY_STRING_CONVERSION_PROPERTY = "binary-string-conversion";

    public enum BynaryStringConversion {
        BINARY_STRING_CONVERSION_BASE64("base64"),
        BINARY_STRING_CONVERSION_DEFAULT_CHARSET("default"),
        BINARY_STRING_CONVERSION_STRING_SCHEMA("string-schema");
        private String value;

        BynaryStringConversion(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static final String SCHEMA_RESOLUTION_PROPERTY = "schema-resolution";

    public enum SchemaResolution {
        DEFAULT("default"),
        INLINE("inline"),
        ALL_OF("all-of"),
        ALL_OF_REF("all-of-ref");

        private String value;

        SchemaResolution(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    protected T _default;

    private String name;
    private String title = null;
    private BigDecimal multipleOf = null;
    private BigDecimal maximum = null;

    @OpenAPI30
    private Boolean exclusiveMaximum = null;

    private BigDecimal minimum = null;

    @OpenAPI30
    private Boolean exclusiveMinimum = null;

    private Integer maxLength = null;
    private Integer minLength = null;
    private String pattern = null;
    private Integer maxItems = null;
    private Integer minItems = null;
    private Boolean uniqueItems = null;
    private Integer maxProperties = null;
    private Integer minProperties = null;
    private List<String> required = null;

    @OpenAPI30
    private String type = null;

    private Schema not = null;
    private Map<String, Schema> properties = null;
    private Object additionalProperties = null;
    private String description = null;
    private String format = null;
    private String $ref = null;

    @OpenAPI30
    private Boolean nullable = null;

    private Boolean readOnly = null;
    private Boolean writeOnly = null;
    protected T example = null;
    private Boolean deprecated = null;
    private Map<String, Object> extensions = null;
    protected List<T> _enum = null;

    private boolean exampleSetFlag;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private List<Schema> prefixItems = null;

    private List<Schema> allOf = null;
    private List<Schema> anyOf = null;
    private List<Schema> oneOf = null;

    private Schema<?> items = null;

    protected T _const;

    private SpecVersion specVersion = SpecVersion.V30;

    public SpecVersion getSpecVersion() {
        return this.specVersion;
    }

    public void setSpecVersion(SpecVersion specVersion) {
        this.specVersion = specVersion;
    }

    public Schema specVersion(SpecVersion specVersion) {
        this.setSpecVersion(specVersion);
        return this;
    }

    /*
    @OpenAPI31 fields and accessors
    */

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private Set<String> types;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private Map<String, Schema> patternProperties = null;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private BigDecimal exclusiveMaximumValue = null;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private BigDecimal exclusiveMinimumValue = null;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private Schema contains = null;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private String $id;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private String $schema;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private String $anchor;

    /**
     * @since 2.2.14 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private String $vocabulary;

    /**
     * @since 2.2.14 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private String $dynamicAnchor;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private String contentEncoding;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private String contentMediaType;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private Schema contentSchema;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private Schema propertyNames;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private Schema unevaluatedProperties;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private Integer maxContains;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private Integer minContains;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private Schema additionalItems;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private Schema unevaluatedItems;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private Schema _if;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private Schema _else;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private Schema then;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private Map<String, Schema> dependentSchemas;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private Map<String, List<String>> dependentRequired;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private String $comment;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    private List<T> examples;

    /**
     * @since 2.2.2 (OpenAPI 3.1.0)
     *
     * when set, this represents a boolean schema value
     */
    @OpenAPI31
    private Boolean booleanSchemaValue;

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema getContains() {
        return contains;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setContains(Schema contains) {
        this.contains = contains;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public String get$id() {
        return $id;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void set$id(String $id) {
        this.$id = $id;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public String get$schema() {
        return $schema;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void set$schema(String $schema) {
        this.$schema = $schema;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public String get$anchor() {
        return $anchor;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void set$anchor(String $anchor) {
        this.$anchor = $anchor;
    }

    /**
     * returns the exclusiveMaximumValue property from a Schema instance for OpenAPI 3.1.x
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     * @return BigDecimal exclusiveMaximumValue
     *
     **/
    @OpenAPI31
    public BigDecimal getExclusiveMaximumValue() {
        return exclusiveMaximumValue;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setExclusiveMaximumValue(BigDecimal exclusiveMaximumValue) {
        this.exclusiveMaximumValue = exclusiveMaximumValue;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema exclusiveMaximumValue(BigDecimal exclusiveMaximumValue) {
        this.exclusiveMaximumValue = exclusiveMaximumValue;
        return this;
    }

    /**
     * returns the exclusiveMinimumValue property from a Schema instance for OpenAPI 3.1.x
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     * @return BigDecimal exclusiveMinimumValue
     *
     **/
    @OpenAPI31
    public BigDecimal getExclusiveMinimumValue() {
        return exclusiveMinimumValue;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setExclusiveMinimumValue(BigDecimal exclusiveMinimumValue) {
        this.exclusiveMinimumValue = exclusiveMinimumValue;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema exclusiveMinimumValue(BigDecimal exclusiveMinimumValue) {
        this.exclusiveMinimumValue = exclusiveMinimumValue;
        return this;
    }

    /**
     * returns the patternProperties property from a Schema instance.
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     * @return Map&lt;String, Schema&gt; patternProperties
     **/
    @OpenAPI31
    public Map<String, Schema> getPatternProperties() {
        return patternProperties;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setPatternProperties(Map<String, Schema> patternProperties) {
        this.patternProperties = patternProperties;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema patternProperties(Map<String, Schema> patternProperties) {
        this.patternProperties = patternProperties;
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema addPatternProperty(String key, Schema patternPropertiesItem) {
        if (this.patternProperties == null) {
            this.patternProperties = new LinkedHashMap<>();
        }
        this.patternProperties.put(key, patternPropertiesItem);
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema contains(Schema contains) {
        this.contains = contains;
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema $id(String $id) {
        this.$id = $id;
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Set<String> getTypes() {
        return types;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setTypes(Set<String> types) {
        this.types = types;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public boolean addType(String type) {
        if (types == null) {
            types = new LinkedHashSet<>();
        }
        return types.add(type);
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema $schema(String $schema) {
        this.$schema = $schema;
        return this;
    }

    /**
     *
     * @since 2.2.8 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public String get$vocabulary() {
        return $vocabulary;
    }

    /**
     *
     * @since 2.2.8 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void set$vocabulary(String $vocabulary) {
        this.$vocabulary = $vocabulary;
    }

    /**
     *
     * @since 2.2.8 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema $vocabulary(String $vocabulary) {
        this.$vocabulary = $vocabulary;
        return this;
    }

    /**
     *
     * @since 2.2.8 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public String get$dynamicAnchor() {
        return $dynamicAnchor;
    }

    /**
     *
     * @since 2.2.8 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void set$dynamicAnchor(String $dynamicAnchor) {
        this.$dynamicAnchor = $dynamicAnchor;
    }

    /**
     *
     * @since 2.2.8 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema $dynamicAnchor(String $dynamicAnchor) {
        this.$dynamicAnchor = $dynamicAnchor;
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema $anchor(String $anchor) {
        this.$anchor = $anchor;
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema types(Set<String> types) {
        this.types = types;
        return this;
    }

    /*
    INTERNAL MEMBERS @OpenAPI31
     */

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    protected Map<String, Object> jsonSchema = null;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Map<String, Object> getJsonSchema() {
        return jsonSchema;
    }

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setJsonSchema(Map<String, Object> jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema jsonSchema(Map<String, Object> jsonSchema) {
        this.jsonSchema = jsonSchema;
        return this;
    }

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    protected transient Object jsonSchemaImpl = null;

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Object getJsonSchemaImpl() {
        return jsonSchemaImpl;
    }

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setJsonSchemaImpl(Object jsonSchemaImpl) {
        this.jsonSchemaImpl = jsonSchemaImpl;
    }

    /**
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema jsonSchemaImpl(Object jsonSchemaImpl) {
        setJsonSchemaImpl(jsonSchemaImpl);
        return this;
    }

    /*
    CONSTRUCTORS
     */

    public Schema() {}

    protected Schema(String type, String format) {
        this.type = type;
        this.addType(type);
        this.format = format;
    }

    public Schema(SpecVersion specVersion) {
        this.specVersion = specVersion;
    }

    protected Schema(String type, String format, SpecVersion specVersion) {
        this.type = type;
        this.addType(type);
        this.format = format;
        this.specVersion = specVersion;
    }

    /*
    ACCESSORS
     */

    /**
     * returns the allOf property from a ComposedSchema instance.
     *
     * @return List&lt;Schema&gt; allOf
     **/
    public List<Schema> getAllOf() {
        return allOf;
    }

    public void setAllOf(List<Schema> allOf) {
        this.allOf = allOf;
    }

    public Schema allOf(List<Schema> allOf) {
        this.allOf = allOf;
        return this;
    }

    public Schema addAllOfItem(Schema allOfItem) {
        if (this.allOf == null) {
            this.allOf = new ArrayList<>();
        }
        this.allOf.add(allOfItem);
        return this;
    }

    /**
     * returns the anyOf property from a ComposedSchema instance.
     *
     * @return List&lt;Schema&gt; anyOf
     **/
    public List<Schema> getAnyOf() {
        return anyOf;
    }

    public void setAnyOf(List<Schema> anyOf) {
        this.anyOf = anyOf;
    }

    public Schema anyOf(List<Schema> anyOf) {
        this.anyOf = anyOf;
        return this;
    }

    public Schema addAnyOfItem(Schema anyOfItem) {
        if (this.anyOf == null) {
            this.anyOf = new ArrayList<>();
        }
        this.anyOf.add(anyOfItem);
        return this;
    }

    /**
     * returns the oneOf property from a ComposedSchema instance.
     *
     * @return List&lt;Schema&gt; oneOf
     **/
    public List<Schema> getOneOf() {
        return oneOf;
    }

    public void setOneOf(List<Schema> oneOf) {
        this.oneOf = oneOf;
    }

    public Schema oneOf(List<Schema> oneOf) {
        this.oneOf = oneOf;
        return this;
    }

    public Schema addOneOfItem(Schema oneOfItem) {
        if (this.oneOf == null) {
            this.oneOf = new ArrayList<>();
        }
        this.oneOf.add(oneOfItem);
        return this;
    }

    /**
     * returns the items property from a ArraySchema instance.
     *
     * @return Schema items
     **/
    public Schema<?> getItems() {
        return items;
    }

    public void setItems(Schema<?> items) {
        this.items = items;
    }

    public Schema items(Schema<?> items) {
        this.items = items;
        return this;
    }

    /**
     * returns the name property from a from a Schema instance. Ignored in serialization.
     *
     * @return String name
     **/
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Schema name(String name) {
        this.setName(name);
        return this;
    }

    /**
     * returns the title property from a Schema instance.
     *
     * @return String title
     **/
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Schema title(String title) {
        this.title = title;
        return this;
    }

    /**
     * returns the _default property from a Schema instance.
     *
     * @return String _default
     **/
    public T getDefault() {
        return _default;
    }

    public void setDefault(Object _default) {
        this._default = cast(_default);
    }

    @SuppressWarnings("unchecked")
    protected T cast(Object value) {
        return (T) value;
    }

    public List<T> getEnum() {
        return _enum;
    }

    public void setEnum(List<T> _enum) {
        this._enum = _enum;
    }

    public void addEnumItemObject(T _enumItem) {
        if (this._enum == null) {
            this._enum = new ArrayList<>();
        }
        this._enum.add(cast(_enumItem));
    }

    /**
     * returns the multipleOf property from a Schema instance.
     * <p>
     * minimum: 0
     *
     * @return BigDecimal multipleOf
     **/
    public BigDecimal getMultipleOf() {
        return multipleOf;
    }

    public void setMultipleOf(BigDecimal multipleOf) {
        this.multipleOf = multipleOf;
    }

    public Schema multipleOf(BigDecimal multipleOf) {
        this.multipleOf = multipleOf;
        return this;
    }

    /**
     * returns the maximum property from a Schema instance.
     *
     * @return BigDecimal maximum
     **/
    public BigDecimal getMaximum() {
        return maximum;
    }

    public void setMaximum(BigDecimal maximum) {
        this.maximum = maximum;
    }

    public Schema maximum(BigDecimal maximum) {
        this.maximum = maximum;
        return this;
    }

    /**
     * returns the exclusiveMaximum property from a Schema instance for OpenAPI 3.0.x
     *
     * @return Boolean exclusiveMaximum
     **/
    @OpenAPI30
    public Boolean getExclusiveMaximum() {
        return exclusiveMaximum;
    }

    @OpenAPI30
    public void setExclusiveMaximum(Boolean exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
    }

    @OpenAPI30
    public Schema exclusiveMaximum(Boolean exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
        return this;
    }

    /**
     * returns the minimum property from a Schema instance.
     *
     * @return BigDecimal minimum
     **/
    public BigDecimal getMinimum() {
        return minimum;
    }

    public void setMinimum(BigDecimal minimum) {
        this.minimum = minimum;
    }

    public Schema minimum(BigDecimal minimum) {
        this.minimum = minimum;
        return this;
    }

    /**
     * returns the exclusiveMinimum property from a Schema instance for OpenAPI 3.0.x
     *
     * @return Boolean exclusiveMinimum
     **/
    public Boolean getExclusiveMinimum() {
        return exclusiveMinimum;
    }

    public void setExclusiveMinimum(Boolean exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
    }

    public Schema exclusiveMinimum(Boolean exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
        return this;
    }

    /**
     * returns the maxLength property from a Schema instance.
     * <p>
     * minimum: 0
     *
     * @return Integer maxLength
     **/
    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public Schema maxLength(Integer maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    /**
     * returns the minLength property from a Schema instance.
     * <p>
     * minimum: 0
     *
     * @return Integer minLength
     **/
    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    public Schema minLength(Integer minLength) {
        this.minLength = minLength;
        return this;
    }

    /**
     * returns the pattern property from a Schema instance.
     *
     * @return String pattern
     **/
    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Schema pattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    /**
     * returns the maxItems property from a Schema instance.
     * <p>
     * minimum: 0
     *
     * @return Integer maxItems
     **/
    public Integer getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(Integer maxItems) {
        this.maxItems = maxItems;
    }

    public Schema maxItems(Integer maxItems) {
        this.maxItems = maxItems;
        return this;
    }

    /**
     * returns the minItems property from a Schema instance.
     * <p>
     * minimum: 0
     *
     * @return Integer minItems
     **/
    public Integer getMinItems() {
        return minItems;
    }

    public void setMinItems(Integer minItems) {
        this.minItems = minItems;
    }

    public Schema minItems(Integer minItems) {
        this.minItems = minItems;
        return this;
    }

    /**
     * returns the uniqueItems property from a Schema instance.
     *
     * @return Boolean uniqueItems
     **/
    public Boolean getUniqueItems() {
        return uniqueItems;
    }

    public void setUniqueItems(Boolean uniqueItems) {
        this.uniqueItems = uniqueItems;
    }

    public Schema uniqueItems(Boolean uniqueItems) {
        this.uniqueItems = uniqueItems;
        return this;
    }

    /**
     * returns the maxProperties property from a Schema instance.
     * <p>
     * minimum: 0
     *
     * @return Integer maxProperties
     **/
    public Integer getMaxProperties() {
        return maxProperties;
    }

    public void setMaxProperties(Integer maxProperties) {
        this.maxProperties = maxProperties;
    }

    public Schema maxProperties(Integer maxProperties) {
        this.maxProperties = maxProperties;
        return this;
    }

    /**
     * returns the minProperties property from a Schema instance.
     * <p>
     * minimum: 0
     *
     * @return Integer minProperties
     **/
    public Integer getMinProperties() {
        return minProperties;
    }

    public void setMinProperties(Integer minProperties) {
        this.minProperties = minProperties;
    }

    public Schema minProperties(Integer minProperties) {
        this.minProperties = minProperties;
        return this;
    }

    /**
     * returns the required property from a Schema instance.
     *
     * @return List&lt;String&gt; required
     **/
    public List<String> getRequired() {
        return required;
    }

    public void setRequired(List<String> required) {
        List<String> list = new ArrayList<>();
        if (required != null) {
            for (String req : required) {
                if (this.properties == null || this.properties.containsKey(req)) {
                    list.add(req);
                }
            }
        }
        Collections.sort(list);
        if (list.isEmpty()) {
            list = null;
        }
        this.required = list;
    }

    public Schema required(List<String> required) {
        this.required = required;
        return this;
    }

    public Schema addRequiredItem(String requiredItem) {
        if (this.required == null) {
            this.required = new ArrayList<>();
        }
        this.required.add(requiredItem);
        Collections.sort(required);
        return this;
    }

    /**
     * returns the type property from a Schema instance.
     *
     * @return String type
     **/
    public String getType() {
        boolean bindTypes = Boolean.valueOf(System.getProperty(BIND_TYPE_AND_TYPES, "false"));
        if (bindTypes && type == null && types != null && types.size() == 1) {
            return types.iterator().next();
        }
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Schema type(String type) {
        this.type = type;
        return this;
    }

    /**
     * returns the not property from a Schema instance.
     *
     * @return Schema not
     **/
    public Schema getNot() {
        return not;
    }

    public void setNot(Schema not) {
        this.not = not;
    }

    public Schema not(Schema not) {
        this.not = not;
        return this;
    }

    /**
     * returns the properties property from a Schema instance.
     *
     * @return Map&lt;String, Schema&gt; properties
     **/
    public Map<String, Schema> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Schema> properties) {
        this.properties = properties;
    }

    public Schema properties(Map<String, Schema> properties) {
        this.properties = properties;
        return this;
    }

    @Deprecated
    public Schema addProperties(String key, Schema property) {
        return addProperty(key, property);
    }

    /**
     *
     * @since 2.2.0
     */
    public Schema addProperty(String key, Schema property) {
        if (this.properties == null) {
            this.properties = new LinkedHashMap<>();
        }
        this.properties.put(key, property);
        return this;
    }

    /**
     * returns the additionalProperties property from a Schema instance. Can be either a Boolean or a Schema
     *
     * @return Object additionalProperties
     **/
    public Object getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(Object additionalProperties) {
        if (additionalProperties != null
                && !(additionalProperties instanceof Boolean)
                && !(additionalProperties instanceof Schema)) {
            throw new IllegalArgumentException("additionalProperties must be either a Boolean or a Schema instance");
        }
        this.additionalProperties = additionalProperties;
    }

    public Schema additionalProperties(Object additionalProperties) {
        setAdditionalProperties(additionalProperties);
        return this;
    }

    /**
     * returns the description property from a Schema instance.
     *
     * @return String description
     **/
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Schema description(String description) {
        this.description = description;
        return this;
    }

    /**
     * returns the format property from a Schema instance.
     *
     * @return String format
     **/
    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Schema format(String format) {
        this.format = format;
        return this;
    }

    /**
     * returns the $ref property from a Schema instance.
     *
     * @return String $ref
     **/
    public String get$ref() {
        return $ref;
    }

    public void set$ref(String $ref) {
        if ($ref != null && !$ref.startsWith("#") && ($ref.indexOf('.') == -1 && $ref.indexOf('/') == -1)) {
            $ref = COMPONENTS_SCHEMAS_REF + $ref;
        }
        this.$ref = $ref;
    }

    public Schema $ref(String $ref) {

        set$ref($ref);
        return this;
    }

    public Schema raw$ref(String $ref) {
        this.$ref = $ref;
        return this;
    }

    /**
     * returns the nullable property from a Schema instance.
     *
     * @return Boolean nullable
     **/
    @OpenAPI30
    public Boolean getNullable() {
        return nullable;
    }

    @OpenAPI30
    public void setNullable(Boolean nullable) {
        this.nullable = nullable;
    }

    @OpenAPI30
    public Schema nullable(Boolean nullable) {
        this.nullable = nullable;
        return this;
    }

    /**
     * returns the readOnly property from a Schema instance.
     *
     * @return Boolean readOnly
     **/
    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public Schema readOnly(Boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    /**
     * returns the writeOnly property from a Schema instance.
     *
     * @return Boolean writeOnly
     **/
    public Boolean getWriteOnly() {
        return writeOnly;
    }

    public void setWriteOnly(Boolean writeOnly) {
        this.writeOnly = writeOnly;
    }

    public Schema writeOnly(Boolean writeOnly) {
        this.writeOnly = writeOnly;
        return this;
    }

    /**
     * returns the example property from a Schema instance.
     *
     * @return String example
     **/
    public Object getExample() {
        return example;
    }

    public void setExample(Object example) {
        this.example = cast(example);
        if (!(example != null && this.example == null)) {
            exampleSetFlag = true;
        }
    }

    public Schema example(Object example) {
        setExample(example);
        return this;
    }

    /**
     * returns the deprecated property from a Schema instance.
     *
     * @return Boolean deprecated
     **/
    public Boolean getDeprecated() {
        return deprecated;
    }

    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    public Schema deprecated(Boolean deprecated) {
        this.deprecated = deprecated;
        return this;
    }

    /**
     * returns true if example setter has been invoked
     * Used to flag explicit setting to null of example (vs missing field) while deserializing from json/yaml string
     *
     * @return boolean exampleSetFlag
     **/
    public boolean getExampleSetFlag() {
        return exampleSetFlag;
    }

    public void setExampleSetFlag(boolean exampleSetFlag) {
        this.exampleSetFlag = exampleSetFlag;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public List<Schema> getPrefixItems() {
        return prefixItems;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setPrefixItems(List<Schema> prefixItems) {
        this.prefixItems = prefixItems;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema prefixItems(List<Schema> prefixItems) {
        this.prefixItems = prefixItems;
        return this;
    }

    /**
     *
     * @since 2.2.12 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema addPrefixItem(Schema prefixItem) {
        if (this.prefixItems == null) {
            this.prefixItems = new ArrayList<>();
        }
        this.prefixItems.add(prefixItem);
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public String getContentEncoding() {
        return contentEncoding;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema contentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public String getContentMediaType() {
        return contentMediaType;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setContentMediaType(String contentMediaType) {
        this.contentMediaType = contentMediaType;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema contentMediaType(String contentMediaType) {
        this.contentMediaType = contentMediaType;
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema getContentSchema() {
        return contentSchema;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setContentSchema(Schema contentSchema) {
        this.contentSchema = contentSchema;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema contentSchema(Schema contentSchema) {
        this.contentSchema = contentSchema;
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema getPropertyNames() {
        return propertyNames;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setPropertyNames(Schema propertyNames) {
        this.propertyNames = propertyNames;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema propertyNames(Schema propertyNames) {
        this.propertyNames = propertyNames;
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema getUnevaluatedProperties() {
        return unevaluatedProperties;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setUnevaluatedProperties(Schema unevaluatedProperties) {
        this.unevaluatedProperties = unevaluatedProperties;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema unevaluatedProperties(Schema unevaluatedProperties) {
        this.unevaluatedProperties = unevaluatedProperties;
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Integer getMaxContains() {
        return maxContains;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setMaxContains(Integer maxContains) {
        this.maxContains = maxContains;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema maxContains(Integer maxContains) {
        this.maxContains = maxContains;
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Integer getMinContains() {
        return minContains;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setMinContains(Integer minContains) {
        this.minContains = minContains;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema minContains(Integer minContains) {
        this.minContains = minContains;
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema getAdditionalItems() {
        return additionalItems;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setAdditionalItems(Schema additionalItems) {
        this.additionalItems = additionalItems;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema additionalItems(Schema additionalItems) {
        this.additionalItems = additionalItems;
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema getUnevaluatedItems() {
        return unevaluatedItems;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setUnevaluatedItems(Schema unevaluatedItems) {
        this.unevaluatedItems = unevaluatedItems;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema unevaluatedItems(Schema unevaluatedItems) {
        this.unevaluatedItems = unevaluatedItems;
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema getIf() {
        return _if;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setIf(Schema _if) {
        this._if = _if;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema _if(Schema _if) {
        this._if = _if;
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema getElse() {
        return _else;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setElse(Schema _else) {
        this._else = _else;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema _else(Schema _else) {
        this._else = _else;
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema getThen() {
        return then;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setThen(Schema then) {
        this.then = then;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema then(Schema then) {
        this.then = then;
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Map<String, Schema> getDependentSchemas() {
        return dependentSchemas;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setDependentSchemas(Map<String, Schema> dependentSchemas) {
        this.dependentSchemas = dependentSchemas;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema dependentSchemas(Map<String, Schema> dependentSchemas) {
        this.dependentSchemas = dependentSchemas;
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Map<String, List<String>> getDependentRequired() {
        return dependentRequired;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setDependentRequired(Map<String, List<String>> dependentRequired) {
        this.dependentRequired = dependentRequired;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema dependentRequired(Map<String, List<String>> dependentRequired) {
        this.dependentRequired = dependentRequired;
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public String get$comment() {
        return $comment;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void set$comment(String $comment) {
        this.$comment = $comment;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema $comment(String $comment) {
        this.$comment = $comment;
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public List<T> getExamples() {
        return examples;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setExamples(List<T> examples) {
        this.examples = examples;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema<T> examples(List<T> examples) {
        this.examples = examples;
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void addExample(T example) {
        if (this.examples == null) {
            this.examples = new ArrayList<>();
        }
        this.examples.add(example);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Schema schema = (Schema) o;
        return Objects.equals(this.title, schema.title)
                && Objects.equals(this.multipleOf, schema.multipleOf)
                && Objects.equals(this.maximum, schema.maximum)
                && Objects.equals(this.exclusiveMaximum, schema.exclusiveMaximum)
                && Objects.equals(this.exclusiveMaximumValue, schema.exclusiveMaximumValue)
                && Objects.equals(this.minimum, schema.minimum)
                && Objects.equals(this.exclusiveMinimum, schema.exclusiveMinimum)
                && Objects.equals(this.exclusiveMinimumValue, schema.exclusiveMinimumValue)
                && Objects.equals(this.maxLength, schema.maxLength)
                && Objects.equals(this.minLength, schema.minLength)
                && Objects.equals(this.pattern, schema.pattern)
                && Objects.equals(this.maxItems, schema.maxItems)
                && Objects.equals(this.minItems, schema.minItems)
                && Objects.equals(this.uniqueItems, schema.uniqueItems)
                && Objects.equals(this.maxProperties, schema.maxProperties)
                && Objects.equals(this.minProperties, schema.minProperties)
                && Objects.equals(this.required, schema.required)
                && Objects.equals(this.type, schema.type)
                && Objects.equals(this.not, schema.not)
                && Objects.equals(this.properties, schema.properties)
                && Objects.equals(this.additionalProperties, schema.additionalProperties)
                && Objects.equals(this.description, schema.description)
                && Objects.equals(this.format, schema.format)
                && Objects.equals(this.$ref, schema.$ref)
                && Objects.equals(this.nullable, schema.nullable)
                && Objects.equals(this.readOnly, schema.readOnly)
                && Objects.equals(this.writeOnly, schema.writeOnly)
                && Objects.equals(this.example, schema.example)
                && Objects.equals(this.deprecated, schema.deprecated)
                && Objects.equals(this.extensions, schema.extensions)
                && Objects.equals(this._enum, schema._enum)
                && Objects.equals(this.contains, schema.contains)
                && Objects.equals(this.patternProperties, schema.patternProperties)
                && Objects.equals(this.$id, schema.$id)
                && Objects.equals(this.$anchor, schema.$anchor)
                && Objects.equals(this.$schema, schema.$schema)
                && Objects.equals(this.$vocabulary, schema.$vocabulary)
                && Objects.equals(this.$dynamicAnchor, schema.$dynamicAnchor)
                && Objects.equals(this.types, schema.types)
                && Objects.equals(this.allOf, schema.allOf)
                && Objects.equals(this.anyOf, schema.anyOf)
                && Objects.equals(this.oneOf, schema.oneOf)
                && Objects.equals(this._const, schema._const)
                && Objects.equals(this._default, schema._default)
                && Objects.equals(this.contentEncoding, schema.contentEncoding)
                && Objects.equals(this.contentMediaType, schema.contentMediaType)
                && Objects.equals(this.contentSchema, schema.contentSchema)
                && Objects.equals(this.propertyNames, schema.propertyNames)
                && Objects.equals(this.unevaluatedProperties, schema.unevaluatedProperties)
                && Objects.equals(this.maxContains, schema.maxContains)
                && Objects.equals(this.minContains, schema.minContains)
                && Objects.equals(this.additionalItems, schema.additionalItems)
                && Objects.equals(this.unevaluatedItems, schema.unevaluatedItems)
                && Objects.equals(this._if, schema._if)
                && Objects.equals(this._else, schema._else)
                && Objects.equals(this.then, schema.then)
                && Objects.equals(this.dependentRequired, schema.dependentRequired)
                && Objects.equals(this.dependentSchemas, schema.dependentSchemas)
                && Objects.equals(this.$comment, schema.$comment)
                && Objects.equals(this.examples, schema.examples)
                && Objects.equals(this.prefixItems, schema.prefixItems)
                && Objects.equals(this.items, schema.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                title,
                multipleOf,
                maximum,
                exclusiveMaximum,
                exclusiveMaximumValue,
                minimum,
                exclusiveMinimum,
                exclusiveMinimumValue,
                maxLength,
                minLength,
                pattern,
                maxItems,
                minItems,
                uniqueItems,
                maxProperties,
                minProperties,
                required,
                type,
                not,
                properties,
                additionalProperties,
                description,
                format,
                $ref,
                nullable,
                readOnly,
                writeOnly,
                example,
                deprecated,
                extensions,
                _enum,
                _default,
                patternProperties,
                $id,
                $anchor,
                $schema,
                $vocabulary,
                $dynamicAnchor,
                types,
                allOf,
                anyOf,
                oneOf,
                _const,
                contentEncoding,
                contentMediaType,
                contentSchema,
                propertyNames,
                unevaluatedProperties,
                maxContains,
                minContains,
                additionalItems,
                unevaluatedItems,
                _if,
                _else,
                then,
                dependentRequired,
                dependentSchemas,
                $comment,
                examples,
                prefixItems,
                items);
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public void addExtension(String name, Object value) {
        if (name == null || name.isEmpty() || (specVersion == SpecVersion.V30 && !name.startsWith("x-"))) {
            return;
        }
        if (this.extensions == null) {
            this.extensions = new LinkedHashMap<>();
        }
        this.extensions.put(name, value);
    }

    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    public Schema extensions(Map<String, Object> extensions) {
        this.extensions = extensions;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Schema {\n");
        Object typeStr = specVersion == SpecVersion.V30 ? type : types;
        sb.append("    type: ").append(toIndentedString(typeStr)).append("\n");
        sb.append("    format: ").append(toIndentedString(format)).append("\n");
        sb.append("    $ref: ").append(toIndentedString($ref)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    title: ").append(toIndentedString(title)).append("\n");
        sb.append("    multipleOf: ").append(toIndentedString(multipleOf)).append("\n");
        sb.append("    maximum: ").append(toIndentedString(maximum)).append("\n");
        Object exclusiveMaximumStr = specVersion == SpecVersion.V30 ? exclusiveMaximum : exclusiveMaximumValue;
        sb.append("    exclusiveMaximum: ")
                .append(toIndentedString(exclusiveMaximumStr))
                .append("\n");
        sb.append("    minimum: ").append(toIndentedString(minimum)).append("\n");
        Object exclusiveMinimumStr = specVersion == SpecVersion.V30 ? exclusiveMinimum : exclusiveMinimumValue;
        sb.append("    exclusiveMinimum: ")
                .append(toIndentedString(exclusiveMinimumStr))
                .append("\n");
        sb.append("    maxLength: ").append(toIndentedString(maxLength)).append("\n");
        sb.append("    minLength: ").append(toIndentedString(minLength)).append("\n");
        sb.append("    pattern: ").append(toIndentedString(pattern)).append("\n");
        sb.append("    maxItems: ").append(toIndentedString(maxItems)).append("\n");
        sb.append("    minItems: ").append(toIndentedString(minItems)).append("\n");
        sb.append("    uniqueItems: ").append(toIndentedString(uniqueItems)).append("\n");
        sb.append("    maxProperties: ").append(toIndentedString(maxProperties)).append("\n");
        sb.append("    minProperties: ").append(toIndentedString(minProperties)).append("\n");
        sb.append("    required: ").append(toIndentedString(required)).append("\n");
        sb.append("    not: ").append(toIndentedString(not)).append("\n");
        sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
        sb.append("    additionalProperties: ")
                .append(toIndentedString(additionalProperties))
                .append("\n");
        sb.append("    nullable: ").append(toIndentedString(nullable)).append("\n");
        sb.append("    readOnly: ").append(toIndentedString(readOnly)).append("\n");
        sb.append("    writeOnly: ").append(toIndentedString(writeOnly)).append("\n");
        sb.append("    example: ").append(toIndentedString(example)).append("\n");
        sb.append("    deprecated: ").append(toIndentedString(deprecated)).append("\n");
        if (specVersion == SpecVersion.V31) {
            sb.append("    patternProperties: ")
                    .append(toIndentedString(patternProperties))
                    .append("\n");
            sb.append("    contains: ").append(toIndentedString(contains)).append("\n");
            sb.append("    $id: ").append(toIndentedString($id)).append("\n");
            sb.append("    $anchor: ").append(toIndentedString($anchor)).append("\n");
            sb.append("    $schema: ").append(toIndentedString($schema)).append("\n");
            sb.append("    $vocabulary: ").append(toIndentedString($vocabulary)).append("\n");
            sb.append("    $dynamicAnchor: ")
                    .append(toIndentedString($dynamicAnchor))
                    .append("\n");
            sb.append("    const: ").append(toIndentedString(_const)).append("\n");
            sb.append("    contentEncoding: ")
                    .append(toIndentedString(contentEncoding))
                    .append("\n");
            sb.append("    contentMediaType: ")
                    .append(toIndentedString(contentMediaType))
                    .append("\n");
            sb.append("    contentSchema: ")
                    .append(toIndentedString(contentSchema))
                    .append("\n");
            sb.append("    propertyNames: ")
                    .append(toIndentedString(propertyNames))
                    .append("\n");
            sb.append("    unevaluatedProperties: ")
                    .append(toIndentedString(unevaluatedProperties))
                    .append("\n");
            sb.append("    maxContains: ").append(toIndentedString(maxContains)).append("\n");
            sb.append("    minContains: ").append(toIndentedString(minContains)).append("\n");
            sb.append("    additionalItems: ")
                    .append(toIndentedString(additionalItems))
                    .append("\n");
            sb.append("    unevaluatedItems: ")
                    .append(toIndentedString(unevaluatedItems))
                    .append("\n");
            sb.append("    _if: ").append(toIndentedString(_if)).append("\n");
            sb.append("    _else: ").append(toIndentedString(_else)).append("\n");
            sb.append("    then: ").append(toIndentedString(then)).append("\n");
            sb.append("    dependentRequired: ")
                    .append(toIndentedString(dependentRequired))
                    .append("\n");
            sb.append("    dependentSchemas: ")
                    .append(toIndentedString(dependentSchemas))
                    .append("\n");
            sb.append("    $comment: ").append(toIndentedString($comment)).append("\n");
            sb.append("    prefixItems: ").append(toIndentedString(prefixItems)).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    protected String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

    public Schema _default(T _default) {
        this._default = _default;
        return this;
    }

    public Schema _enum(List<T> _enum) {
        this._enum = _enum;
        return this;
    }

    public Schema exampleSetFlag(boolean exampleSetFlag) {
        this.exampleSetFlag = exampleSetFlag;
        return this;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public T getConst() {
        return _const;
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setConst(Object _const) {
        this._const = cast(_const);
    }

    /**
     *
     * @since 2.2.0 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema _const(Object _const) {
        this._const = cast(_const);
        return this;
    }

    /**
     *
     * @since 2.2.2 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Boolean getBooleanSchemaValue() {
        return booleanSchemaValue;
    }

    /**
     *
     * @since 2.2.2 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public void setBooleanSchemaValue(Boolean booleanSchemaValue) {
        this.booleanSchemaValue = booleanSchemaValue;
    }

    /**
     *
     * @since 2.2.2 (OpenAPI 3.1.0)
     */
    @OpenAPI31
    public Schema booleanSchemaValue(Boolean booleanSchemaValue) {
        this.booleanSchemaValue = booleanSchemaValue;
        return this;
    }
}
