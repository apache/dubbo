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
package org.apache.dubbo.metadata;

/**
 * <pre>
 * Response format enumeration.
 * </pre>
 *
 * Protobuf enum {@code org.apache.dubbo.metadata.OpenAPIFormat}
 */
public enum OpenAPIFormat implements com.google.protobuf.ProtocolMessageEnum {
    /**
     * <pre>
     * JSON format.
     * </pre>
     *
     * <code>JSON = 0;</code>
     */
    JSON(0),
    /**
     * <pre>
     * YAML format.
     * </pre>
     *
     * <code>YAML = 1;</code>
     */
    YAML(1),
    /**
     * <pre>
     * PROTO format.
     * </pre>
     *
     * <code>PROTO = 2;</code>
     */
    PROTO(2),
    UNRECOGNIZED(-1),
    ;

    /**
     * <pre>
     * JSON format.
     * </pre>
     *
     * <code>JSON = 0;</code>
     */
    public static final int JSON_VALUE = 0;
    /**
     * <pre>
     * YAML format.
     * </pre>
     *
     * <code>YAML = 1;</code>
     */
    public static final int YAML_VALUE = 1;
    /**
     * <pre>
     * PROTO format.
     * </pre>
     *
     * <code>PROTO = 2;</code>
     */
    public static final int PROTO_VALUE = 2;

    public final int getNumber() {
        if (this == UNRECOGNIZED) {
            throw new IllegalArgumentException("Can't get the number of an unknown enum value.");
        }
        return value;
    }

    /**
     * @param value The numeric wire value of the corresponding enum entry.
     * @return The enum associated with the given numeric wire value.
     * @deprecated Use {@link #forNumber(int)} instead.
     */
    @Deprecated
    public static OpenAPIFormat valueOf(int value) {
        return forNumber(value);
    }

    /**
     * @param value The numeric wire value of the corresponding enum entry.
     * @return The enum associated with the given numeric wire value.
     */
    public static OpenAPIFormat forNumber(int value) {
        switch (value) {
            case 0:
                return JSON;
            case 1:
                return YAML;
            case 2:
                return PROTO;
            default:
                return null;
        }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<OpenAPIFormat> internalGetValueMap() {
        return internalValueMap;
    }

    private static final com.google.protobuf.Internal.EnumLiteMap<OpenAPIFormat> internalValueMap =
            new com.google.protobuf.Internal.EnumLiteMap<OpenAPIFormat>() {
                public OpenAPIFormat findValueByNumber(int number) {
                    return OpenAPIFormat.forNumber(number);
                }
            };

    public final com.google.protobuf.Descriptors.EnumValueDescriptor getValueDescriptor() {
        if (this == UNRECOGNIZED) {
            throw new IllegalStateException("Can't get the descriptor of an unrecognized enum value.");
        }
        return getDescriptor().getValues().get(ordinal());
    }

    public final com.google.protobuf.Descriptors.EnumDescriptor getDescriptorForType() {
        return getDescriptor();
    }

    public static final com.google.protobuf.Descriptors.EnumDescriptor getDescriptor() {
        return MetadataServiceV2OuterClass.getDescriptor().getEnumTypes().get(0);
    }

    private static final OpenAPIFormat[] VALUES = values();

    public static OpenAPIFormat valueOf(com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
        if (desc.getType() != getDescriptor()) {
            throw new IllegalArgumentException("EnumValueDescriptor is not for this type.");
        }
        if (desc.getIndex() == -1) {
            return UNRECOGNIZED;
        }
        return VALUES[desc.getIndex()];
    }

    private final int value;

    private OpenAPIFormat(int value) {
        this.value = value;
    }

    // @@protoc_insertion_point(enum_scope:org.apache.dubbo.metadata.OpenAPIFormat)
}
