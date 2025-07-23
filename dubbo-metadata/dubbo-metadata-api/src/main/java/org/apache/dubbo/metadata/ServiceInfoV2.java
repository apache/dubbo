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
 * Service information message.
 * </pre>
 *
 * Protobuf type {@code org.apache.dubbo.metadata.ServiceInfoV2}
 */
public final class ServiceInfoV2 extends com.google.protobuf.GeneratedMessageV3
        implements
        // @@protoc_insertion_point(message_implements:org.apache.dubbo.metadata.ServiceInfoV2)
        ServiceInfoV2OrBuilder {
    private static final long serialVersionUID = 0L;

    // Use ServiceInfoV2.newBuilder() to construct.
    private ServiceInfoV2(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private ServiceInfoV2() {
        name_ = "";
        group_ = "";
        version_ = "";
        protocol_ = "";
        path_ = "";
    }

    @Override
    @SuppressWarnings({"unused"})
    protected Object newInstance(UnusedPrivateParameter unused) {
        return new ServiceInfoV2();
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return MetadataServiceV2OuterClass.internal_static_org_apache_dubbo_metadata_ServiceInfoV2_descriptor;
    }

    @SuppressWarnings({"rawtypes"})
    @Override
    protected com.google.protobuf.MapField internalGetMapField(int number) {
        switch (number) {
            case 7:
                return internalGetParams();
            default:
                throw new RuntimeException("Invalid map field number: " + number);
        }
    }

    @Override
    protected FieldAccessorTable internalGetFieldAccessorTable() {
        return MetadataServiceV2OuterClass.internal_static_org_apache_dubbo_metadata_ServiceInfoV2_fieldAccessorTable
                .ensureFieldAccessorsInitialized(ServiceInfoV2.class, Builder.class);
    }

    public static final int NAME_FIELD_NUMBER = 1;

    @SuppressWarnings("serial")
    private volatile Object name_ = "";

    /**
     * <pre>
     * The service name.
     * </pre>
     *
     * <code>string name = 1;</code>
     * @return The name.
     */
    @Override
    public String getName() {
        Object ref = name_;
        if (ref instanceof String) {
            return (String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            String s = bs.toStringUtf8();
            name_ = s;
            return s;
        }
    }

    /**
     * <pre>
     * The service name.
     * </pre>
     *
     * <code>string name = 1;</code>
     * @return The bytes for name.
     */
    @Override
    public com.google.protobuf.ByteString getNameBytes() {
        Object ref = name_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
            name_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    public static final int GROUP_FIELD_NUMBER = 2;

    @SuppressWarnings("serial")
    private volatile Object group_ = "";

    /**
     * <pre>
     * The service group.
     * </pre>
     *
     * <code>string group = 2;</code>
     * @return The group.
     */
    @Override
    public String getGroup() {
        Object ref = group_;
        if (ref instanceof String) {
            return (String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            String s = bs.toStringUtf8();
            group_ = s;
            return s;
        }
    }

    /**
     * <pre>
     * The service group.
     * </pre>
     *
     * <code>string group = 2;</code>
     * @return The bytes for group.
     */
    @Override
    public com.google.protobuf.ByteString getGroupBytes() {
        Object ref = group_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
            group_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    public static final int VERSION_FIELD_NUMBER = 3;

    @SuppressWarnings("serial")
    private volatile Object version_ = "";

    /**
     * <pre>
     * The service version.
     * </pre>
     *
     * <code>string version = 3;</code>
     * @return The version.
     */
    @Override
    public String getVersion() {
        Object ref = version_;
        if (ref instanceof String) {
            return (String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            String s = bs.toStringUtf8();
            version_ = s;
            return s;
        }
    }

    /**
     * <pre>
     * The service version.
     * </pre>
     *
     * <code>string version = 3;</code>
     * @return The bytes for version.
     */
    @Override
    public com.google.protobuf.ByteString getVersionBytes() {
        Object ref = version_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
            version_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    public static final int PROTOCOL_FIELD_NUMBER = 4;

    @SuppressWarnings("serial")
    private volatile Object protocol_ = "";

    /**
     * <pre>
     * The service protocol.
     * </pre>
     *
     * <code>string protocol = 4;</code>
     * @return The protocol.
     */
    @Override
    public String getProtocol() {
        Object ref = protocol_;
        if (ref instanceof String) {
            return (String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            String s = bs.toStringUtf8();
            protocol_ = s;
            return s;
        }
    }

    /**
     * <pre>
     * The service protocol.
     * </pre>
     *
     * <code>string protocol = 4;</code>
     * @return The bytes for protocol.
     */
    @Override
    public com.google.protobuf.ByteString getProtocolBytes() {
        Object ref = protocol_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
            protocol_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    public static final int PORT_FIELD_NUMBER = 5;
    private int port_ = 0;

    /**
     * <pre>
     * The service port.
     * </pre>
     *
     * <code>int32 port = 5;</code>
     * @return The port.
     */
    @Override
    public int getPort() {
        return port_;
    }

    public static final int PATH_FIELD_NUMBER = 6;

    @SuppressWarnings("serial")
    private volatile Object path_ = "";

    /**
     * <pre>
     * The service path.
     * </pre>
     *
     * <code>string path = 6;</code>
     * @return The path.
     */
    @Override
    public String getPath() {
        Object ref = path_;
        if (ref instanceof String) {
            return (String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            String s = bs.toStringUtf8();
            path_ = s;
            return s;
        }
    }

    /**
     * <pre>
     * The service path.
     * </pre>
     *
     * <code>string path = 6;</code>
     * @return The bytes for path.
     */
    @Override
    public com.google.protobuf.ByteString getPathBytes() {
        Object ref = path_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
            path_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    public static final int PARAMS_FIELD_NUMBER = 7;

    private static final class ParamsDefaultEntryHolder {
        static final com.google.protobuf.MapEntry<String, String> defaultEntry =
                com.google.protobuf.MapEntry.<String, String>newDefaultInstance(
                        MetadataServiceV2OuterClass
                                .internal_static_org_apache_dubbo_metadata_ServiceInfoV2_ParamsEntry_descriptor,
                        com.google.protobuf.WireFormat.FieldType.STRING,
                        "",
                        com.google.protobuf.WireFormat.FieldType.STRING,
                        "");
    }

    @SuppressWarnings("serial")
    private com.google.protobuf.MapField<String, String> params_;

    private com.google.protobuf.MapField<String, String> internalGetParams() {
        if (params_ == null) {
            return com.google.protobuf.MapField.emptyMapField(ParamsDefaultEntryHolder.defaultEntry);
        }
        return params_;
    }

    public int getParamsCount() {
        return internalGetParams().getMap().size();
    }

    /**
     * <pre>
     * A map of service parameters.
     * </pre>
     *
     * <code>map&lt;string, string&gt; params = 7;</code>
     */
    @Override
    public boolean containsParams(String key) {
        if (key == null) {
            throw new NullPointerException("map key");
        }
        return internalGetParams().getMap().containsKey(key);
    }

    /**
     * Use {@link #getParamsMap()} instead.
     */
    @Override
    @Deprecated
    public java.util.Map<String, String> getParams() {
        return getParamsMap();
    }

    /**
     * <pre>
     * A map of service parameters.
     * </pre>
     *
     * <code>map&lt;string, string&gt; params = 7;</code>
     */
    @Override
    public java.util.Map<String, String> getParamsMap() {
        return internalGetParams().getMap();
    }

    /**
     * <pre>
     * A map of service parameters.
     * </pre>
     *
     * <code>map&lt;string, string&gt; params = 7;</code>
     */
    @Override
    public /* nullable */ String getParamsOrDefault(
            String key,
            /* nullable */
            String defaultValue) {
        if (key == null) {
            throw new NullPointerException("map key");
        }
        java.util.Map<String, String> map = internalGetParams().getMap();
        return map.containsKey(key) ? map.get(key) : defaultValue;
    }

    /**
     * <pre>
     * A map of service parameters.
     * </pre>
     *
     * <code>map&lt;string, string&gt; params = 7;</code>
     */
    @Override
    public String getParamsOrThrow(String key) {
        if (key == null) {
            throw new NullPointerException("map key");
        }
        java.util.Map<String, String> map = internalGetParams().getMap();
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException();
        }
        return map.get(key);
    }

    private byte memoizedIsInitialized = -1;

    @Override
    public final boolean isInitialized() {
        byte isInitialized = memoizedIsInitialized;
        if (isInitialized == 1) {
            return true;
        }
        if (isInitialized == 0) {
            return false;
        }

        memoizedIsInitialized = 1;
        return true;
    }

    @Override
    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(name_)) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 1, name_);
        }
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(group_)) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 2, group_);
        }
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(version_)) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 3, version_);
        }
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(protocol_)) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 4, protocol_);
        }
        if (port_ != 0) {
            output.writeInt32(5, port_);
        }
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(path_)) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 6, path_);
        }
        com.google.protobuf.GeneratedMessageV3.serializeStringMapTo(
                output, internalGetParams(), ParamsDefaultEntryHolder.defaultEntry, 7);
        getUnknownFields().writeTo(output);
    }

    @Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1) {
            return size;
        }

        size = 0;
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(name_)) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, name_);
        }
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(group_)) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, group_);
        }
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(version_)) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(3, version_);
        }
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(protocol_)) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(4, protocol_);
        }
        if (port_ != 0) {
            size += com.google.protobuf.CodedOutputStream.computeInt32Size(5, port_);
        }
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(path_)) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(6, path_);
        }
        for (java.util.Map.Entry<String, String> entry :
                internalGetParams().getMap().entrySet()) {
            com.google.protobuf.MapEntry<String, String> params__ = ParamsDefaultEntryHolder.defaultEntry
                    .newBuilderForType()
                    .setKey(entry.getKey())
                    .setValue(entry.getValue())
                    .build();
            size += com.google.protobuf.CodedOutputStream.computeMessageSize(7, params__);
        }
        size += getUnknownFields().getSerializedSize();
        memoizedSize = size;
        return size;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ServiceInfoV2)) {
            return super.equals(obj);
        }
        ServiceInfoV2 other = (ServiceInfoV2) obj;

        if (!getName().equals(other.getName())) {
            return false;
        }
        if (!getGroup().equals(other.getGroup())) {
            return false;
        }
        if (!getVersion().equals(other.getVersion())) {
            return false;
        }
        if (!getProtocol().equals(other.getProtocol())) {
            return false;
        }
        if (getPort() != other.getPort()) {
            return false;
        }
        if (!getPath().equals(other.getPath())) {
            return false;
        }
        if (!internalGetParams().equals(other.internalGetParams())) {
            return false;
        }
        if (!getUnknownFields().equals(other.getUnknownFields())) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        if (memoizedHashCode != 0) {
            return memoizedHashCode;
        }
        int hash = 41;
        hash = (19 * hash) + getDescriptor().hashCode();
        hash = (37 * hash) + NAME_FIELD_NUMBER;
        hash = (53 * hash) + getName().hashCode();
        hash = (37 * hash) + GROUP_FIELD_NUMBER;
        hash = (53 * hash) + getGroup().hashCode();
        hash = (37 * hash) + VERSION_FIELD_NUMBER;
        hash = (53 * hash) + getVersion().hashCode();
        hash = (37 * hash) + PROTOCOL_FIELD_NUMBER;
        hash = (53 * hash) + getProtocol().hashCode();
        hash = (37 * hash) + PORT_FIELD_NUMBER;
        hash = (53 * hash) + getPort();
        hash = (37 * hash) + PATH_FIELD_NUMBER;
        hash = (53 * hash) + getPath().hashCode();
        if (!internalGetParams().getMap().isEmpty()) {
            hash = (37 * hash) + PARAMS_FIELD_NUMBER;
            hash = (53 * hash) + internalGetParams().hashCode();
        }
        hash = (29 * hash) + getUnknownFields().hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static ServiceInfoV2 parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static ServiceInfoV2 parseFrom(
            java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static ServiceInfoV2 parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static ServiceInfoV2 parseFrom(
            com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static ServiceInfoV2 parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static ServiceInfoV2 parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static ServiceInfoV2 parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static ServiceInfoV2 parseFrom(
            java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static ServiceInfoV2 parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static ServiceInfoV2 parseDelimitedFrom(
            java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static ServiceInfoV2 parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static ServiceInfoV2 parseFrom(
            com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    @Override
    public Builder newBuilderForType() {
        return newBuilder();
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(ServiceInfoV2 prototype) {
        return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    @Override
    public Builder toBuilder() {
        return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
        Builder builder = new Builder(parent);
        return builder;
    }

    /**
     * <pre>
     * Service information message.
     * </pre>
     *
     * Protobuf type {@code org.apache.dubbo.metadata.ServiceInfoV2}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:org.apache.dubbo.metadata.ServiceInfoV2)
            ServiceInfoV2OrBuilder {
        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return MetadataServiceV2OuterClass.internal_static_org_apache_dubbo_metadata_ServiceInfoV2_descriptor;
        }

        @SuppressWarnings({"rawtypes"})
        protected com.google.protobuf.MapField internalGetMapField(int number) {
            switch (number) {
                case 7:
                    return internalGetParams();
                default:
                    throw new RuntimeException("Invalid map field number: " + number);
            }
        }

        @SuppressWarnings({"rawtypes"})
        protected com.google.protobuf.MapField internalGetMutableMapField(int number) {
            switch (number) {
                case 7:
                    return internalGetMutableParams();
                default:
                    throw new RuntimeException("Invalid map field number: " + number);
            }
        }

        @Override
        protected FieldAccessorTable internalGetFieldAccessorTable() {
            return MetadataServiceV2OuterClass
                    .internal_static_org_apache_dubbo_metadata_ServiceInfoV2_fieldAccessorTable
                    .ensureFieldAccessorsInitialized(ServiceInfoV2.class, Builder.class);
        }

        // Construct using org.apache.dubbo.metadata.ServiceInfoV2.newBuilder()
        private Builder() {}

        private Builder(BuilderParent parent) {
            super(parent);
        }

        @Override
        public Builder clear() {
            super.clear();
            bitField0_ = 0;
            name_ = "";
            group_ = "";
            version_ = "";
            protocol_ = "";
            port_ = 0;
            path_ = "";
            internalGetMutableParams().clear();
            return this;
        }

        @Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return MetadataServiceV2OuterClass.internal_static_org_apache_dubbo_metadata_ServiceInfoV2_descriptor;
        }

        @Override
        public ServiceInfoV2 getDefaultInstanceForType() {
            return ServiceInfoV2.getDefaultInstance();
        }

        @Override
        public ServiceInfoV2 build() {
            ServiceInfoV2 result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @Override
        public ServiceInfoV2 buildPartial() {
            ServiceInfoV2 result = new ServiceInfoV2(this);
            if (bitField0_ != 0) {
                buildPartial0(result);
            }
            onBuilt();
            return result;
        }

        private void buildPartial0(ServiceInfoV2 result) {
            int from_bitField0_ = bitField0_;
            if (((from_bitField0_ & 0x00000001) != 0)) {
                result.name_ = name_;
            }
            if (((from_bitField0_ & 0x00000002) != 0)) {
                result.group_ = group_;
            }
            if (((from_bitField0_ & 0x00000004) != 0)) {
                result.version_ = version_;
            }
            if (((from_bitField0_ & 0x00000008) != 0)) {
                result.protocol_ = protocol_;
            }
            if (((from_bitField0_ & 0x00000010) != 0)) {
                result.port_ = port_;
            }
            if (((from_bitField0_ & 0x00000020) != 0)) {
                result.path_ = path_;
            }
            if (((from_bitField0_ & 0x00000040) != 0)) {
                result.params_ = internalGetParams();
                result.params_.makeImmutable();
            }
        }

        @Override
        public Builder clone() {
            return super.clone();
        }

        @Override
        public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
            return super.setField(field, value);
        }

        @Override
        public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
            return super.clearField(field);
        }

        @Override
        public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
            return super.clearOneof(oneof);
        }

        @Override
        public Builder setRepeatedField(
                com.google.protobuf.Descriptors.FieldDescriptor field, int index, Object value) {
            return super.setRepeatedField(field, index, value);
        }

        @Override
        public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
            return super.addRepeatedField(field, value);
        }

        @Override
        public Builder mergeFrom(com.google.protobuf.Message other) {
            if (other instanceof ServiceInfoV2) {
                return mergeFrom((ServiceInfoV2) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(ServiceInfoV2 other) {
            if (other == ServiceInfoV2.getDefaultInstance()) {
                return this;
            }
            if (!other.getName().isEmpty()) {
                name_ = other.name_;
                bitField0_ |= 0x00000001;
                onChanged();
            }
            if (!other.getGroup().isEmpty()) {
                group_ = other.group_;
                bitField0_ |= 0x00000002;
                onChanged();
            }
            if (!other.getVersion().isEmpty()) {
                version_ = other.version_;
                bitField0_ |= 0x00000004;
                onChanged();
            }
            if (!other.getProtocol().isEmpty()) {
                protocol_ = other.protocol_;
                bitField0_ |= 0x00000008;
                onChanged();
            }
            if (other.getPort() != 0) {
                setPort(other.getPort());
            }
            if (!other.getPath().isEmpty()) {
                path_ = other.path_;
                bitField0_ |= 0x00000020;
                onChanged();
            }
            internalGetMutableParams().mergeFrom(other.internalGetParams());
            bitField0_ |= 0x00000040;
            this.mergeUnknownFields(other.getUnknownFields());
            onChanged();
            return this;
        }

        @Override
        public final boolean isInitialized() {
            return true;
        }

        @Override
        public Builder mergeFrom(
                com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            if (extensionRegistry == null) {
                throw new NullPointerException();
            }
            try {
                boolean done = false;
                while (!done) {
                    int tag = input.readTag();
                    switch (tag) {
                        case 0:
                            done = true;
                            break;
                        case 10: {
                            name_ = input.readStringRequireUtf8();
                            bitField0_ |= 0x00000001;
                            break;
                        } // case 10
                        case 18: {
                            group_ = input.readStringRequireUtf8();
                            bitField0_ |= 0x00000002;
                            break;
                        } // case 18
                        case 26: {
                            version_ = input.readStringRequireUtf8();
                            bitField0_ |= 0x00000004;
                            break;
                        } // case 26
                        case 34: {
                            protocol_ = input.readStringRequireUtf8();
                            bitField0_ |= 0x00000008;
                            break;
                        } // case 34
                        case 40: {
                            port_ = input.readInt32();
                            bitField0_ |= 0x00000010;
                            break;
                        } // case 40
                        case 50: {
                            path_ = input.readStringRequireUtf8();
                            bitField0_ |= 0x00000020;
                            break;
                        } // case 50
                        case 58: {
                            com.google.protobuf.MapEntry<String, String> params__ = input.readMessage(
                                    ParamsDefaultEntryHolder.defaultEntry.getParserForType(), extensionRegistry);
                            internalGetMutableParams().getMutableMap().put(params__.getKey(), params__.getValue());
                            bitField0_ |= 0x00000040;
                            break;
                        } // case 58
                        default: {
                            if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                                done = true; // was an endgroup tag
                            }
                            break;
                        } // default:
                    } // switch (tag)
                } // while (!done)
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                throw e.unwrapIOException();
            } finally {
                onChanged();
            } // finally
            return this;
        }

        private int bitField0_;

        private Object name_ = "";

        /**
         * <pre>
         * The service name.
         * </pre>
         *
         * <code>string name = 1;</code>
         * @return The name.
         */
        public String getName() {
            Object ref = name_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                String s = bs.toStringUtf8();
                name_ = s;
                return s;
            } else {
                return (String) ref;
            }
        }

        /**
         * <pre>
         * The service name.
         * </pre>
         *
         * <code>string name = 1;</code>
         * @return The bytes for name.
         */
        public com.google.protobuf.ByteString getNameBytes() {
            Object ref = name_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
                name_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <pre>
         * The service name.
         * </pre>
         *
         * <code>string name = 1;</code>
         * @param value The name to set.
         * @return This builder for chaining.
         */
        public Builder setName(String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            name_ = value;
            bitField0_ |= 0x00000001;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The service name.
         * </pre>
         *
         * <code>string name = 1;</code>
         * @return This builder for chaining.
         */
        public Builder clearName() {
            name_ = getDefaultInstance().getName();
            bitField0_ = (bitField0_ & ~0x00000001);
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The service name.
         * </pre>
         *
         * <code>string name = 1;</code>
         * @param value The bytes for name to set.
         * @return This builder for chaining.
         */
        public Builder setNameBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            name_ = value;
            bitField0_ |= 0x00000001;
            onChanged();
            return this;
        }

        private Object group_ = "";

        /**
         * <pre>
         * The service group.
         * </pre>
         *
         * <code>string group = 2;</code>
         * @return The group.
         */
        public String getGroup() {
            Object ref = group_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                String s = bs.toStringUtf8();
                group_ = s;
                return s;
            } else {
                return (String) ref;
            }
        }

        /**
         * <pre>
         * The service group.
         * </pre>
         *
         * <code>string group = 2;</code>
         * @return The bytes for group.
         */
        public com.google.protobuf.ByteString getGroupBytes() {
            Object ref = group_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
                group_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <pre>
         * The service group.
         * </pre>
         *
         * <code>string group = 2;</code>
         * @param value The group to set.
         * @return This builder for chaining.
         */
        public Builder setGroup(String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            group_ = value;
            bitField0_ |= 0x00000002;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The service group.
         * </pre>
         *
         * <code>string group = 2;</code>
         * @return This builder for chaining.
         */
        public Builder clearGroup() {
            group_ = getDefaultInstance().getGroup();
            bitField0_ = (bitField0_ & ~0x00000002);
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The service group.
         * </pre>
         *
         * <code>string group = 2;</code>
         * @param value The bytes for group to set.
         * @return This builder for chaining.
         */
        public Builder setGroupBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            group_ = value;
            bitField0_ |= 0x00000002;
            onChanged();
            return this;
        }

        private Object version_ = "";

        /**
         * <pre>
         * The service version.
         * </pre>
         *
         * <code>string version = 3;</code>
         * @return The version.
         */
        public String getVersion() {
            Object ref = version_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                String s = bs.toStringUtf8();
                version_ = s;
                return s;
            } else {
                return (String) ref;
            }
        }

        /**
         * <pre>
         * The service version.
         * </pre>
         *
         * <code>string version = 3;</code>
         * @return The bytes for version.
         */
        public com.google.protobuf.ByteString getVersionBytes() {
            Object ref = version_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
                version_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <pre>
         * The service version.
         * </pre>
         *
         * <code>string version = 3;</code>
         * @param value The version to set.
         * @return This builder for chaining.
         */
        public Builder setVersion(String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            version_ = value;
            bitField0_ |= 0x00000004;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The service version.
         * </pre>
         *
         * <code>string version = 3;</code>
         * @return This builder for chaining.
         */
        public Builder clearVersion() {
            version_ = getDefaultInstance().getVersion();
            bitField0_ = (bitField0_ & ~0x00000004);
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The service version.
         * </pre>
         *
         * <code>string version = 3;</code>
         * @param value The bytes for version to set.
         * @return This builder for chaining.
         */
        public Builder setVersionBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            version_ = value;
            bitField0_ |= 0x00000004;
            onChanged();
            return this;
        }

        private Object protocol_ = "";

        /**
         * <pre>
         * The service protocol.
         * </pre>
         *
         * <code>string protocol = 4;</code>
         * @return The protocol.
         */
        public String getProtocol() {
            Object ref = protocol_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                String s = bs.toStringUtf8();
                protocol_ = s;
                return s;
            } else {
                return (String) ref;
            }
        }

        /**
         * <pre>
         * The service protocol.
         * </pre>
         *
         * <code>string protocol = 4;</code>
         * @return The bytes for protocol.
         */
        public com.google.protobuf.ByteString getProtocolBytes() {
            Object ref = protocol_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
                protocol_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <pre>
         * The service protocol.
         * </pre>
         *
         * <code>string protocol = 4;</code>
         * @param value The protocol to set.
         * @return This builder for chaining.
         */
        public Builder setProtocol(String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            protocol_ = value;
            bitField0_ |= 0x00000008;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The service protocol.
         * </pre>
         *
         * <code>string protocol = 4;</code>
         * @return This builder for chaining.
         */
        public Builder clearProtocol() {
            protocol_ = getDefaultInstance().getProtocol();
            bitField0_ = (bitField0_ & ~0x00000008);
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The service protocol.
         * </pre>
         *
         * <code>string protocol = 4;</code>
         * @param value The bytes for protocol to set.
         * @return This builder for chaining.
         */
        public Builder setProtocolBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            protocol_ = value;
            bitField0_ |= 0x00000008;
            onChanged();
            return this;
        }

        private int port_;

        /**
         * <pre>
         * The service port.
         * </pre>
         *
         * <code>int32 port = 5;</code>
         * @return The port.
         */
        @Override
        public int getPort() {
            return port_;
        }

        /**
         * <pre>
         * The service port.
         * </pre>
         *
         * <code>int32 port = 5;</code>
         * @param value The port to set.
         * @return This builder for chaining.
         */
        public Builder setPort(int value) {

            port_ = value;
            bitField0_ |= 0x00000010;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The service port.
         * </pre>
         *
         * <code>int32 port = 5;</code>
         * @return This builder for chaining.
         */
        public Builder clearPort() {
            bitField0_ = (bitField0_ & ~0x00000010);
            port_ = 0;
            onChanged();
            return this;
        }

        private Object path_ = "";

        /**
         * <pre>
         * The service path.
         * </pre>
         *
         * <code>string path = 6;</code>
         * @return The path.
         */
        public String getPath() {
            Object ref = path_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                String s = bs.toStringUtf8();
                path_ = s;
                return s;
            } else {
                return (String) ref;
            }
        }

        /**
         * <pre>
         * The service path.
         * </pre>
         *
         * <code>string path = 6;</code>
         * @return The bytes for path.
         */
        public com.google.protobuf.ByteString getPathBytes() {
            Object ref = path_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
                path_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <pre>
         * The service path.
         * </pre>
         *
         * <code>string path = 6;</code>
         * @param value The path to set.
         * @return This builder for chaining.
         */
        public Builder setPath(String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            path_ = value;
            bitField0_ |= 0x00000020;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The service path.
         * </pre>
         *
         * <code>string path = 6;</code>
         * @return This builder for chaining.
         */
        public Builder clearPath() {
            path_ = getDefaultInstance().getPath();
            bitField0_ = (bitField0_ & ~0x00000020);
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The service path.
         * </pre>
         *
         * <code>string path = 6;</code>
         * @param value The bytes for path to set.
         * @return This builder for chaining.
         */
        public Builder setPathBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            path_ = value;
            bitField0_ |= 0x00000020;
            onChanged();
            return this;
        }

        private com.google.protobuf.MapField<String, String> params_;

        private com.google.protobuf.MapField<String, String> internalGetParams() {
            if (params_ == null) {
                return com.google.protobuf.MapField.emptyMapField(ParamsDefaultEntryHolder.defaultEntry);
            }
            return params_;
        }

        private com.google.protobuf.MapField<String, String> internalGetMutableParams() {
            if (params_ == null) {
                params_ = com.google.protobuf.MapField.newMapField(ParamsDefaultEntryHolder.defaultEntry);
            }
            if (!params_.isMutable()) {
                params_ = params_.copy();
            }
            bitField0_ |= 0x00000040;
            onChanged();
            return params_;
        }

        public int getParamsCount() {
            return internalGetParams().getMap().size();
        }

        /**
         * <pre>
         * A map of service parameters.
         * </pre>
         *
         * <code>map&lt;string, string&gt; params = 7;</code>
         */
        @Override
        public boolean containsParams(String key) {
            if (key == null) {
                throw new NullPointerException("map key");
            }
            return internalGetParams().getMap().containsKey(key);
        }

        /**
         * Use {@link #getParamsMap()} instead.
         */
        @Override
        @Deprecated
        public java.util.Map<String, String> getParams() {
            return getParamsMap();
        }

        /**
         * <pre>
         * A map of service parameters.
         * </pre>
         *
         * <code>map&lt;string, string&gt; params = 7;</code>
         */
        @Override
        public java.util.Map<String, String> getParamsMap() {
            return internalGetParams().getMap();
        }

        /**
         * <pre>
         * A map of service parameters.
         * </pre>
         *
         * <code>map&lt;string, string&gt; params = 7;</code>
         */
        @Override
        public /* nullable */ String getParamsOrDefault(
                String key,
                /* nullable */
                String defaultValue) {
            if (key == null) {
                throw new NullPointerException("map key");
            }
            java.util.Map<String, String> map = internalGetParams().getMap();
            return map.containsKey(key) ? map.get(key) : defaultValue;
        }

        /**
         * <pre>
         * A map of service parameters.
         * </pre>
         *
         * <code>map&lt;string, string&gt; params = 7;</code>
         */
        @Override
        public String getParamsOrThrow(String key) {
            if (key == null) {
                throw new NullPointerException("map key");
            }
            java.util.Map<String, String> map = internalGetParams().getMap();
            if (!map.containsKey(key)) {
                throw new IllegalArgumentException();
            }
            return map.get(key);
        }

        public Builder clearParams() {
            bitField0_ = (bitField0_ & ~0x00000040);
            internalGetMutableParams().getMutableMap().clear();
            return this;
        }

        /**
         * <pre>
         * A map of service parameters.
         * </pre>
         *
         * <code>map&lt;string, string&gt; params = 7;</code>
         */
        public Builder removeParams(String key) {
            if (key == null) {
                throw new NullPointerException("map key");
            }
            internalGetMutableParams().getMutableMap().remove(key);
            return this;
        }

        /**
         * Use alternate mutation accessors instead.
         */
        @Deprecated
        public java.util.Map<String, String> getMutableParams() {
            bitField0_ |= 0x00000040;
            return internalGetMutableParams().getMutableMap();
        }

        /**
         * <pre>
         * A map of service parameters.
         * </pre>
         *
         * <code>map&lt;string, string&gt; params = 7;</code>
         */
        public Builder putParams(String key, String value) {
            if (key == null) {
                throw new NullPointerException("map key");
            }
            if (value == null) {
                throw new NullPointerException("map value");
            }
            internalGetMutableParams().getMutableMap().put(key, value);
            bitField0_ |= 0x00000040;
            return this;
        }

        /**
         * <pre>
         * A map of service parameters.
         * </pre>
         *
         * <code>map&lt;string, string&gt; params = 7;</code>
         */
        public Builder putAllParams(java.util.Map<String, String> values) {
            internalGetMutableParams().getMutableMap().putAll(values);
            bitField0_ |= 0x00000040;
            return this;
        }

        @Override
        public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
            return super.setUnknownFields(unknownFields);
        }

        @Override
        public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
            return super.mergeUnknownFields(unknownFields);
        }

        // @@protoc_insertion_point(builder_scope:org.apache.dubbo.metadata.ServiceInfoV2)
    }

    // @@protoc_insertion_point(class_scope:org.apache.dubbo.metadata.ServiceInfoV2)
    private static final ServiceInfoV2 DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new ServiceInfoV2();
    }

    public static ServiceInfoV2 getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<ServiceInfoV2> PARSER =
            new com.google.protobuf.AbstractParser<ServiceInfoV2>() {
                @Override
                public ServiceInfoV2 parsePartialFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    Builder builder = newBuilder();
                    try {
                        builder.mergeFrom(input, extensionRegistry);
                    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                        throw e.setUnfinishedMessage(builder.buildPartial());
                    } catch (com.google.protobuf.UninitializedMessageException e) {
                        throw e.asInvalidProtocolBufferException().setUnfinishedMessage(builder.buildPartial());
                    } catch (java.io.IOException e) {
                        throw new com.google.protobuf.InvalidProtocolBufferException(e)
                                .setUnfinishedMessage(builder.buildPartial());
                    }
                    return builder.buildPartial();
                }
            };

    public static com.google.protobuf.Parser<ServiceInfoV2> parser() {
        return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<ServiceInfoV2> getParserForType() {
        return PARSER;
    }

    @Override
    public ServiceInfoV2 getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }
}
