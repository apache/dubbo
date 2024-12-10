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
 * OpenAPI request message.
 * </pre>
 *
 * Protobuf type {@code org.apache.dubbo.metadata.OpenAPIRequest}
 */
public final class OpenAPIRequest extends com.google.protobuf.GeneratedMessageV3
        implements
        // @@protoc_insertion_point(message_implements:org.apache.dubbo.metadata.OpenAPIRequest)
        OpenAPIRequestOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use OpenAPIRequest.newBuilder() to construct.
    private OpenAPIRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private OpenAPIRequest() {
        group_ = "";
        version_ = "";
        tag_ = com.google.protobuf.LazyStringArrayList.emptyList();
        service_ = com.google.protobuf.LazyStringArrayList.emptyList();
        openapi_ = "";
        format_ = 0;
    }

    @Override
    @SuppressWarnings({"unused"})
    protected Object newInstance(UnusedPrivateParameter unused) {
        return new OpenAPIRequest();
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return MetadataServiceV2OuterClass.internal_static_org_apache_dubbo_metadata_OpenAPIRequest_descriptor;
    }

    @Override
    protected FieldAccessorTable internalGetFieldAccessorTable() {
        return MetadataServiceV2OuterClass.internal_static_org_apache_dubbo_metadata_OpenAPIRequest_fieldAccessorTable
                .ensureFieldAccessorsInitialized(OpenAPIRequest.class, Builder.class);
    }

    private int bitField0_;
    public static final int GROUP_FIELD_NUMBER = 1;

    @SuppressWarnings("serial")
    private volatile Object group_ = "";

    /**
     * <pre>
     * The openAPI group.
     * </pre>
     *
     * <code>string group = 1;</code>
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
     * The openAPI group.
     * </pre>
     *
     * <code>string group = 1;</code>
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

    public static final int VERSION_FIELD_NUMBER = 2;

    @SuppressWarnings("serial")
    private volatile Object version_ = "";

    /**
     * <pre>
     * The openAPI version, using a major.minor.patch versioning scheme
     * e.g. 1.0.1
     * </pre>
     *
     * <code>string version = 2;</code>
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
     * The openAPI version, using a major.minor.patch versioning scheme
     * e.g. 1.0.1
     * </pre>
     *
     * <code>string version = 2;</code>
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

    public static final int TAG_FIELD_NUMBER = 3;

    @SuppressWarnings("serial")
    private com.google.protobuf.LazyStringArrayList tag_ = com.google.protobuf.LazyStringArrayList.emptyList();

    /**
     * <pre>
     * The openAPI tags. Each tag is an or condition.
     * </pre>
     *
     * <code>repeated string tag = 3;</code>
     * @return A list containing the tag.
     */
    public com.google.protobuf.ProtocolStringList getTagList() {
        return tag_;
    }

    /**
     * <pre>
     * The openAPI tags. Each tag is an or condition.
     * </pre>
     *
     * <code>repeated string tag = 3;</code>
     * @return The count of tag.
     */
    public int getTagCount() {
        return tag_.size();
    }

    /**
     * <pre>
     * The openAPI tags. Each tag is an or condition.
     * </pre>
     *
     * <code>repeated string tag = 3;</code>
     * @param index The index of the element to return.
     * @return The tag at the given index.
     */
    public String getTag(int index) {
        return tag_.get(index);
    }

    /**
     * <pre>
     * The openAPI tags. Each tag is an or condition.
     * </pre>
     *
     * <code>repeated string tag = 3;</code>
     * @param index The index of the value to return.
     * @return The bytes of the tag at the given index.
     */
    public com.google.protobuf.ByteString getTagBytes(int index) {
        return tag_.getByteString(index);
    }

    public static final int SERVICE_FIELD_NUMBER = 4;

    @SuppressWarnings("serial")
    private com.google.protobuf.LazyStringArrayList service_ = com.google.protobuf.LazyStringArrayList.emptyList();

    /**
     * <pre>
     * The openAPI services. Each service is an or condition.
     * </pre>
     *
     * <code>repeated string service = 4;</code>
     * @return A list containing the service.
     */
    public com.google.protobuf.ProtocolStringList getServiceList() {
        return service_;
    }

    /**
     * <pre>
     * The openAPI services. Each service is an or condition.
     * </pre>
     *
     * <code>repeated string service = 4;</code>
     * @return The count of service.
     */
    public int getServiceCount() {
        return service_.size();
    }

    /**
     * <pre>
     * The openAPI services. Each service is an or condition.
     * </pre>
     *
     * <code>repeated string service = 4;</code>
     * @param index The index of the element to return.
     * @return The service at the given index.
     */
    public String getService(int index) {
        return service_.get(index);
    }

    /**
     * <pre>
     * The openAPI services. Each service is an or condition.
     * </pre>
     *
     * <code>repeated string service = 4;</code>
     * @param index The index of the value to return.
     * @return The bytes of the service at the given index.
     */
    public com.google.protobuf.ByteString getServiceBytes(int index) {
        return service_.getByteString(index);
    }

    public static final int OPENAPI_FIELD_NUMBER = 5;

    @SuppressWarnings("serial")
    private volatile Object openapi_ = "";

    /**
     * <pre>
     * The openAPI specification version, using a major.minor.patch versioning scheme
     * e.g. 3.0.1, 3.1.0
     * The default value is '3.0.1'.
     * </pre>
     *
     * <code>string openapi = 5;</code>
     * @return The openapi.
     */
    @Override
    public String getOpenapi() {
        Object ref = openapi_;
        if (ref instanceof String) {
            return (String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            String s = bs.toStringUtf8();
            openapi_ = s;
            return s;
        }
    }

    /**
     * <pre>
     * The openAPI specification version, using a major.minor.patch versioning scheme
     * e.g. 3.0.1, 3.1.0
     * The default value is '3.0.1'.
     * </pre>
     *
     * <code>string openapi = 5;</code>
     * @return The bytes for openapi.
     */
    @Override
    public com.google.protobuf.ByteString getOpenapiBytes() {
        Object ref = openapi_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
            openapi_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    public static final int FORMAT_FIELD_NUMBER = 6;
    private int format_ = 0;

    /**
     * <pre>
     * The format of the response.
     * The default value is 'JSON'.
     * </pre>
     *
     * <code>optional .org.apache.dubbo.metadata.OpenAPIFormat format = 6;</code>
     * @return Whether the format field is set.
     */
    @Override
    public boolean hasFormat() {
        return ((bitField0_ & 0x00000001) != 0);
    }

    /**
     * <pre>
     * The format of the response.
     * The default value is 'JSON'.
     * </pre>
     *
     * <code>optional .org.apache.dubbo.metadata.OpenAPIFormat format = 6;</code>
     * @return The enum numeric value on the wire for format.
     */
    @Override
    public int getFormatValue() {
        return format_;
    }

    /**
     * <pre>
     * The format of the response.
     * The default value is 'JSON'.
     * </pre>
     *
     * <code>optional .org.apache.dubbo.metadata.OpenAPIFormat format = 6;</code>
     * @return The format.
     */
    @Override
    public OpenAPIFormat getFormat() {
        OpenAPIFormat result = OpenAPIFormat.forNumber(format_);
        return result == null ? OpenAPIFormat.UNRECOGNIZED : result;
    }

    public static final int PRETTY_FIELD_NUMBER = 7;
    private boolean pretty_ = false;

    /**
     * <pre>
     * Whether to pretty print for json.
     * The default value is 'false'.
     * </pre>
     *
     * <code>optional bool pretty = 7;</code>
     * @return Whether the pretty field is set.
     */
    @Override
    public boolean hasPretty() {
        return ((bitField0_ & 0x00000002) != 0);
    }

    /**
     * <pre>
     * Whether to pretty print for json.
     * The default value is 'false'.
     * </pre>
     *
     * <code>optional bool pretty = 7;</code>
     * @return The pretty.
     */
    @Override
    public boolean getPretty() {
        return pretty_;
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
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(group_)) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 1, group_);
        }
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(version_)) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 2, version_);
        }
        for (int i = 0; i < tag_.size(); i++) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 3, tag_.getRaw(i));
        }
        for (int i = 0; i < service_.size(); i++) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 4, service_.getRaw(i));
        }
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(openapi_)) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 5, openapi_);
        }
        if (((bitField0_ & 0x00000001) != 0)) {
            output.writeEnum(6, format_);
        }
        if (((bitField0_ & 0x00000002) != 0)) {
            output.writeBool(7, pretty_);
        }
        getUnknownFields().writeTo(output);
    }

    @Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1) {
            return size;
        }

        size = 0;
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(group_)) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, group_);
        }
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(version_)) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, version_);
        }
        {
            int dataSize = 0;
            for (int i = 0; i < tag_.size(); i++) {
                dataSize += computeStringSizeNoTag(tag_.getRaw(i));
            }
            size += dataSize;
            size += 1 * getTagList().size();
        }
        {
            int dataSize = 0;
            for (int i = 0; i < service_.size(); i++) {
                dataSize += computeStringSizeNoTag(service_.getRaw(i));
            }
            size += dataSize;
            size += 1 * getServiceList().size();
        }
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(openapi_)) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(5, openapi_);
        }
        if (((bitField0_ & 0x00000001) != 0)) {
            size += com.google.protobuf.CodedOutputStream.computeEnumSize(6, format_);
        }
        if (((bitField0_ & 0x00000002) != 0)) {
            size += com.google.protobuf.CodedOutputStream.computeBoolSize(7, pretty_);
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
        if (!(obj instanceof OpenAPIRequest)) {
            return super.equals(obj);
        }
        OpenAPIRequest other = (OpenAPIRequest) obj;

        if (!getGroup().equals(other.getGroup())) {
            return false;
        }
        if (!getVersion().equals(other.getVersion())) {
            return false;
        }
        if (!getTagList().equals(other.getTagList())) {
            return false;
        }
        if (!getServiceList().equals(other.getServiceList())) {
            return false;
        }
        if (!getOpenapi().equals(other.getOpenapi())) {
            return false;
        }
        if (hasFormat() != other.hasFormat()) {
            return false;
        }
        if (hasFormat()) {
            if (format_ != other.format_) {
                return false;
            }
        }
        if (hasPretty() != other.hasPretty()) {
            return false;
        }
        if (hasPretty()) {
            if (getPretty() != other.getPretty()) {
                return false;
            }
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
        hash = (37 * hash) + GROUP_FIELD_NUMBER;
        hash = (53 * hash) + getGroup().hashCode();
        hash = (37 * hash) + VERSION_FIELD_NUMBER;
        hash = (53 * hash) + getVersion().hashCode();
        if (getTagCount() > 0) {
            hash = (37 * hash) + TAG_FIELD_NUMBER;
            hash = (53 * hash) + getTagList().hashCode();
        }
        if (getServiceCount() > 0) {
            hash = (37 * hash) + SERVICE_FIELD_NUMBER;
            hash = (53 * hash) + getServiceList().hashCode();
        }
        hash = (37 * hash) + OPENAPI_FIELD_NUMBER;
        hash = (53 * hash) + getOpenapi().hashCode();
        if (hasFormat()) {
            hash = (37 * hash) + FORMAT_FIELD_NUMBER;
            hash = (53 * hash) + format_;
        }
        if (hasPretty()) {
            hash = (37 * hash) + PRETTY_FIELD_NUMBER;
            hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(getPretty());
        }
        hash = (29 * hash) + getUnknownFields().hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static OpenAPIRequest parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static OpenAPIRequest parseFrom(
            java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static OpenAPIRequest parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static OpenAPIRequest parseFrom(
            com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static OpenAPIRequest parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static OpenAPIRequest parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static OpenAPIRequest parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static OpenAPIRequest parseFrom(
            java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static OpenAPIRequest parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static OpenAPIRequest parseDelimitedFrom(
            java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static OpenAPIRequest parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static OpenAPIRequest parseFrom(
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

    public static Builder newBuilder(OpenAPIRequest prototype) {
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
     * OpenAPI request message.
     * </pre>
     *
     * Protobuf type {@code org.apache.dubbo.metadata.OpenAPIRequest}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:org.apache.dubbo.metadata.OpenAPIRequest)
            OpenAPIRequestOrBuilder {
        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return MetadataServiceV2OuterClass.internal_static_org_apache_dubbo_metadata_OpenAPIRequest_descriptor;
        }

        @Override
        protected FieldAccessorTable internalGetFieldAccessorTable() {
            return MetadataServiceV2OuterClass
                    .internal_static_org_apache_dubbo_metadata_OpenAPIRequest_fieldAccessorTable
                    .ensureFieldAccessorsInitialized(OpenAPIRequest.class, Builder.class);
        }

        // Construct using org.apache.dubbo.metadata.OpenAPIRequest.newBuilder()
        private Builder() {}

        private Builder(BuilderParent parent) {
            super(parent);
        }

        @Override
        public Builder clear() {
            super.clear();
            bitField0_ = 0;
            group_ = "";
            version_ = "";
            tag_ = com.google.protobuf.LazyStringArrayList.emptyList();
            service_ = com.google.protobuf.LazyStringArrayList.emptyList();
            openapi_ = "";
            format_ = 0;
            pretty_ = false;
            return this;
        }

        @Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return MetadataServiceV2OuterClass.internal_static_org_apache_dubbo_metadata_OpenAPIRequest_descriptor;
        }

        @Override
        public OpenAPIRequest getDefaultInstanceForType() {
            return OpenAPIRequest.getDefaultInstance();
        }

        @Override
        public OpenAPIRequest build() {
            OpenAPIRequest result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @Override
        public OpenAPIRequest buildPartial() {
            OpenAPIRequest result = new OpenAPIRequest(this);
            if (bitField0_ != 0) {
                buildPartial0(result);
            }
            onBuilt();
            return result;
        }

        private void buildPartial0(OpenAPIRequest result) {
            int from_bitField0_ = bitField0_;
            if (((from_bitField0_ & 0x00000001) != 0)) {
                result.group_ = group_;
            }
            if (((from_bitField0_ & 0x00000002) != 0)) {
                result.version_ = version_;
            }
            if (((from_bitField0_ & 0x00000004) != 0)) {
                tag_.makeImmutable();
                result.tag_ = tag_;
            }
            if (((from_bitField0_ & 0x00000008) != 0)) {
                service_.makeImmutable();
                result.service_ = service_;
            }
            if (((from_bitField0_ & 0x00000010) != 0)) {
                result.openapi_ = openapi_;
            }
            int to_bitField0_ = 0;
            if (((from_bitField0_ & 0x00000020) != 0)) {
                result.format_ = format_;
                to_bitField0_ |= 0x00000001;
            }
            if (((from_bitField0_ & 0x00000040) != 0)) {
                result.pretty_ = pretty_;
                to_bitField0_ |= 0x00000002;
            }
            result.bitField0_ |= to_bitField0_;
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
            if (other instanceof OpenAPIRequest) {
                return mergeFrom((OpenAPIRequest) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(OpenAPIRequest other) {
            if (other == OpenAPIRequest.getDefaultInstance()) {
                return this;
            }
            if (!other.getGroup().isEmpty()) {
                group_ = other.group_;
                bitField0_ |= 0x00000001;
                onChanged();
            }
            if (!other.getVersion().isEmpty()) {
                version_ = other.version_;
                bitField0_ |= 0x00000002;
                onChanged();
            }
            if (!other.tag_.isEmpty()) {
                if (tag_.isEmpty()) {
                    tag_ = other.tag_;
                    bitField0_ |= 0x00000004;
                } else {
                    ensureTagIsMutable();
                    tag_.addAll(other.tag_);
                }
                onChanged();
            }
            if (!other.service_.isEmpty()) {
                if (service_.isEmpty()) {
                    service_ = other.service_;
                    bitField0_ |= 0x00000008;
                } else {
                    ensureServiceIsMutable();
                    service_.addAll(other.service_);
                }
                onChanged();
            }
            if (!other.getOpenapi().isEmpty()) {
                openapi_ = other.openapi_;
                bitField0_ |= 0x00000010;
                onChanged();
            }
            if (other.hasFormat()) {
                setFormat(other.getFormat());
            }
            if (other.hasPretty()) {
                setPretty(other.getPretty());
            }
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
                            group_ = input.readStringRequireUtf8();
                            bitField0_ |= 0x00000001;
                            break;
                        } // case 10
                        case 18: {
                            version_ = input.readStringRequireUtf8();
                            bitField0_ |= 0x00000002;
                            break;
                        } // case 18
                        case 26: {
                            String s = input.readStringRequireUtf8();
                            ensureTagIsMutable();
                            tag_.add(s);
                            break;
                        } // case 26
                        case 34: {
                            String s = input.readStringRequireUtf8();
                            ensureServiceIsMutable();
                            service_.add(s);
                            break;
                        } // case 34
                        case 42: {
                            openapi_ = input.readStringRequireUtf8();
                            bitField0_ |= 0x00000010;
                            break;
                        } // case 42
                        case 48: {
                            format_ = input.readEnum();
                            bitField0_ |= 0x00000020;
                            break;
                        } // case 48
                        case 56: {
                            pretty_ = input.readBool();
                            bitField0_ |= 0x00000040;
                            break;
                        } // case 56
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

        private Object group_ = "";

        /**
         * <pre>
         * The openAPI group.
         * </pre>
         *
         * <code>string group = 1;</code>
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
         * The openAPI group.
         * </pre>
         *
         * <code>string group = 1;</code>
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
         * The openAPI group.
         * </pre>
         *
         * <code>string group = 1;</code>
         * @param value The group to set.
         * @return This builder for chaining.
         */
        public Builder setGroup(String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            group_ = value;
            bitField0_ |= 0x00000001;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The openAPI group.
         * </pre>
         *
         * <code>string group = 1;</code>
         * @return This builder for chaining.
         */
        public Builder clearGroup() {
            group_ = getDefaultInstance().getGroup();
            bitField0_ = (bitField0_ & ~0x00000001);
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The openAPI group.
         * </pre>
         *
         * <code>string group = 1;</code>
         * @param value The bytes for group to set.
         * @return This builder for chaining.
         */
        public Builder setGroupBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            group_ = value;
            bitField0_ |= 0x00000001;
            onChanged();
            return this;
        }

        private Object version_ = "";

        /**
         * <pre>
         * The openAPI version, using a major.minor.patch versioning scheme
         * e.g. 1.0.1
         * </pre>
         *
         * <code>string version = 2;</code>
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
         * The openAPI version, using a major.minor.patch versioning scheme
         * e.g. 1.0.1
         * </pre>
         *
         * <code>string version = 2;</code>
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
         * The openAPI version, using a major.minor.patch versioning scheme
         * e.g. 1.0.1
         * </pre>
         *
         * <code>string version = 2;</code>
         * @param value The version to set.
         * @return This builder for chaining.
         */
        public Builder setVersion(String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            version_ = value;
            bitField0_ |= 0x00000002;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The openAPI version, using a major.minor.patch versioning scheme
         * e.g. 1.0.1
         * </pre>
         *
         * <code>string version = 2;</code>
         * @return This builder for chaining.
         */
        public Builder clearVersion() {
            version_ = getDefaultInstance().getVersion();
            bitField0_ = (bitField0_ & ~0x00000002);
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The openAPI version, using a major.minor.patch versioning scheme
         * e.g. 1.0.1
         * </pre>
         *
         * <code>string version = 2;</code>
         * @param value The bytes for version to set.
         * @return This builder for chaining.
         */
        public Builder setVersionBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            version_ = value;
            bitField0_ |= 0x00000002;
            onChanged();
            return this;
        }

        private com.google.protobuf.LazyStringArrayList tag_ = com.google.protobuf.LazyStringArrayList.emptyList();

        private void ensureTagIsMutable() {
            if (!tag_.isModifiable()) {
                tag_ = new com.google.protobuf.LazyStringArrayList(tag_);
            }
            bitField0_ |= 0x00000004;
        }

        /**
         * <pre>
         * The openAPI tags. Each tag is an or condition.
         * </pre>
         *
         * <code>repeated string tag = 3;</code>
         * @return A list containing the tag.
         */
        public com.google.protobuf.ProtocolStringList getTagList() {
            tag_.makeImmutable();
            return tag_;
        }

        /**
         * <pre>
         * The openAPI tags. Each tag is an or condition.
         * </pre>
         *
         * <code>repeated string tag = 3;</code>
         * @return The count of tag.
         */
        public int getTagCount() {
            return tag_.size();
        }

        /**
         * <pre>
         * The openAPI tags. Each tag is an or condition.
         * </pre>
         *
         * <code>repeated string tag = 3;</code>
         * @param index The index of the element to return.
         * @return The tag at the given index.
         */
        public String getTag(int index) {
            return tag_.get(index);
        }

        /**
         * <pre>
         * The openAPI tags. Each tag is an or condition.
         * </pre>
         *
         * <code>repeated string tag = 3;</code>
         * @param index The index of the value to return.
         * @return The bytes of the tag at the given index.
         */
        public com.google.protobuf.ByteString getTagBytes(int index) {
            return tag_.getByteString(index);
        }

        /**
         * <pre>
         * The openAPI tags. Each tag is an or condition.
         * </pre>
         *
         * <code>repeated string tag = 3;</code>
         * @param index The index to set the value at.
         * @param value The tag to set.
         * @return This builder for chaining.
         */
        public Builder setTag(int index, String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            ensureTagIsMutable();
            tag_.set(index, value);
            bitField0_ |= 0x00000004;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The openAPI tags. Each tag is an or condition.
         * </pre>
         *
         * <code>repeated string tag = 3;</code>
         * @param value The tag to add.
         * @return This builder for chaining.
         */
        public Builder addTag(String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            ensureTagIsMutable();
            tag_.add(value);
            bitField0_ |= 0x00000004;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The openAPI tags. Each tag is an or condition.
         * </pre>
         *
         * <code>repeated string tag = 3;</code>
         * @param values The tag to add.
         * @return This builder for chaining.
         */
        public Builder addAllTag(Iterable<String> values) {
            ensureTagIsMutable();
            com.google.protobuf.AbstractMessageLite.Builder.addAll(values, tag_);
            bitField0_ |= 0x00000004;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The openAPI tags. Each tag is an or condition.
         * </pre>
         *
         * <code>repeated string tag = 3;</code>
         * @return This builder for chaining.
         */
        public Builder clearTag() {
            tag_ = com.google.protobuf.LazyStringArrayList.emptyList();
            bitField0_ = (bitField0_ & ~0x00000004);
            ;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The openAPI tags. Each tag is an or condition.
         * </pre>
         *
         * <code>repeated string tag = 3;</code>
         * @param value The bytes of the tag to add.
         * @return This builder for chaining.
         */
        public Builder addTagBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            ensureTagIsMutable();
            tag_.add(value);
            bitField0_ |= 0x00000004;
            onChanged();
            return this;
        }

        private com.google.protobuf.LazyStringArrayList service_ = com.google.protobuf.LazyStringArrayList.emptyList();

        private void ensureServiceIsMutable() {
            if (!service_.isModifiable()) {
                service_ = new com.google.protobuf.LazyStringArrayList(service_);
            }
            bitField0_ |= 0x00000008;
        }

        /**
         * <pre>
         * The openAPI services. Each service is an or condition.
         * </pre>
         *
         * <code>repeated string service = 4;</code>
         * @return A list containing the service.
         */
        public com.google.protobuf.ProtocolStringList getServiceList() {
            service_.makeImmutable();
            return service_;
        }

        /**
         * <pre>
         * The openAPI services. Each service is an or condition.
         * </pre>
         *
         * <code>repeated string service = 4;</code>
         * @return The count of service.
         */
        public int getServiceCount() {
            return service_.size();
        }

        /**
         * <pre>
         * The openAPI services. Each service is an or condition.
         * </pre>
         *
         * <code>repeated string service = 4;</code>
         * @param index The index of the element to return.
         * @return The service at the given index.
         */
        public String getService(int index) {
            return service_.get(index);
        }

        /**
         * <pre>
         * The openAPI services. Each service is an or condition.
         * </pre>
         *
         * <code>repeated string service = 4;</code>
         * @param index The index of the value to return.
         * @return The bytes of the service at the given index.
         */
        public com.google.protobuf.ByteString getServiceBytes(int index) {
            return service_.getByteString(index);
        }

        /**
         * <pre>
         * The openAPI services. Each service is an or condition.
         * </pre>
         *
         * <code>repeated string service = 4;</code>
         * @param index The index to set the value at.
         * @param value The service to set.
         * @return This builder for chaining.
         */
        public Builder setService(int index, String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            ensureServiceIsMutable();
            service_.set(index, value);
            bitField0_ |= 0x00000008;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The openAPI services. Each service is an or condition.
         * </pre>
         *
         * <code>repeated string service = 4;</code>
         * @param value The service to add.
         * @return This builder for chaining.
         */
        public Builder addService(String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            ensureServiceIsMutable();
            service_.add(value);
            bitField0_ |= 0x00000008;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The openAPI services. Each service is an or condition.
         * </pre>
         *
         * <code>repeated string service = 4;</code>
         * @param values The service to add.
         * @return This builder for chaining.
         */
        public Builder addAllService(Iterable<String> values) {
            ensureServiceIsMutable();
            com.google.protobuf.AbstractMessageLite.Builder.addAll(values, service_);
            bitField0_ |= 0x00000008;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The openAPI services. Each service is an or condition.
         * </pre>
         *
         * <code>repeated string service = 4;</code>
         * @return This builder for chaining.
         */
        public Builder clearService() {
            service_ = com.google.protobuf.LazyStringArrayList.emptyList();
            bitField0_ = (bitField0_ & ~0x00000008);
            ;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The openAPI services. Each service is an or condition.
         * </pre>
         *
         * <code>repeated string service = 4;</code>
         * @param value The bytes of the service to add.
         * @return This builder for chaining.
         */
        public Builder addServiceBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            ensureServiceIsMutable();
            service_.add(value);
            bitField0_ |= 0x00000008;
            onChanged();
            return this;
        }

        private Object openapi_ = "";

        /**
         * <pre>
         * The openAPI specification version, using a major.minor.patch versioning scheme
         * e.g. 3.0.1, 3.1.0
         * The default value is '3.0.1'.
         * </pre>
         *
         * <code>string openapi = 5;</code>
         * @return The openapi.
         */
        public String getOpenapi() {
            Object ref = openapi_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                String s = bs.toStringUtf8();
                openapi_ = s;
                return s;
            } else {
                return (String) ref;
            }
        }

        /**
         * <pre>
         * The openAPI specification version, using a major.minor.patch versioning scheme
         * e.g. 3.0.1, 3.1.0
         * The default value is '3.0.1'.
         * </pre>
         *
         * <code>string openapi = 5;</code>
         * @return The bytes for openapi.
         */
        public com.google.protobuf.ByteString getOpenapiBytes() {
            Object ref = openapi_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
                openapi_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <pre>
         * The openAPI specification version, using a major.minor.patch versioning scheme
         * e.g. 3.0.1, 3.1.0
         * The default value is '3.0.1'.
         * </pre>
         *
         * <code>string openapi = 5;</code>
         * @param value The openapi to set.
         * @return This builder for chaining.
         */
        public Builder setOpenapi(String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            openapi_ = value;
            bitField0_ |= 0x00000010;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The openAPI specification version, using a major.minor.patch versioning scheme
         * e.g. 3.0.1, 3.1.0
         * The default value is '3.0.1'.
         * </pre>
         *
         * <code>string openapi = 5;</code>
         * @return This builder for chaining.
         */
        public Builder clearOpenapi() {
            openapi_ = getDefaultInstance().getOpenapi();
            bitField0_ = (bitField0_ & ~0x00000010);
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The openAPI specification version, using a major.minor.patch versioning scheme
         * e.g. 3.0.1, 3.1.0
         * The default value is '3.0.1'.
         * </pre>
         *
         * <code>string openapi = 5;</code>
         * @param value The bytes for openapi to set.
         * @return This builder for chaining.
         */
        public Builder setOpenapiBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            openapi_ = value;
            bitField0_ |= 0x00000010;
            onChanged();
            return this;
        }

        private int format_ = 0;

        /**
         * <pre>
         * The format of the response.
         * The default value is 'JSON'.
         * </pre>
         *
         * <code>optional .org.apache.dubbo.metadata.OpenAPIFormat format = 6;</code>
         * @return Whether the format field is set.
         */
        @Override
        public boolean hasFormat() {
            return ((bitField0_ & 0x00000020) != 0);
        }

        /**
         * <pre>
         * The format of the response.
         * The default value is 'JSON'.
         * </pre>
         *
         * <code>optional .org.apache.dubbo.metadata.OpenAPIFormat format = 6;</code>
         * @return The enum numeric value on the wire for format.
         */
        @Override
        public int getFormatValue() {
            return format_;
        }

        /**
         * <pre>
         * The format of the response.
         * The default value is 'JSON'.
         * </pre>
         *
         * <code>optional .org.apache.dubbo.metadata.OpenAPIFormat format = 6;</code>
         * @param value The enum numeric value on the wire for format to set.
         * @return This builder for chaining.
         */
        public Builder setFormatValue(int value) {
            format_ = value;
            bitField0_ |= 0x00000020;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The format of the response.
         * The default value is 'JSON'.
         * </pre>
         *
         * <code>optional .org.apache.dubbo.metadata.OpenAPIFormat format = 6;</code>
         * @return The format.
         */
        @Override
        public OpenAPIFormat getFormat() {
            OpenAPIFormat result = OpenAPIFormat.forNumber(format_);
            return result == null ? OpenAPIFormat.UNRECOGNIZED : result;
        }

        /**
         * <pre>
         * The format of the response.
         * The default value is 'JSON'.
         * </pre>
         *
         * <code>optional .org.apache.dubbo.metadata.OpenAPIFormat format = 6;</code>
         * @param value The format to set.
         * @return This builder for chaining.
         */
        public Builder setFormat(OpenAPIFormat value) {
            if (value == null) {
                throw new NullPointerException();
            }
            bitField0_ |= 0x00000020;
            format_ = value.getNumber();
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The format of the response.
         * The default value is 'JSON'.
         * </pre>
         *
         * <code>optional .org.apache.dubbo.metadata.OpenAPIFormat format = 6;</code>
         * @return This builder for chaining.
         */
        public Builder clearFormat() {
            bitField0_ = (bitField0_ & ~0x00000020);
            format_ = 0;
            onChanged();
            return this;
        }

        private boolean pretty_;

        /**
         * <pre>
         * Whether to pretty print for json.
         * The default value is 'false'.
         * </pre>
         *
         * <code>optional bool pretty = 7;</code>
         * @return Whether the pretty field is set.
         */
        @Override
        public boolean hasPretty() {
            return ((bitField0_ & 0x00000040) != 0);
        }

        /**
         * <pre>
         * Whether to pretty print for json.
         * The default value is 'false'.
         * </pre>
         *
         * <code>optional bool pretty = 7;</code>
         * @return The pretty.
         */
        @Override
        public boolean getPretty() {
            return pretty_;
        }

        /**
         * <pre>
         * Whether to pretty print for json.
         * The default value is 'false'.
         * </pre>
         *
         * <code>optional bool pretty = 7;</code>
         * @param value The pretty to set.
         * @return This builder for chaining.
         */
        public Builder setPretty(boolean value) {

            pretty_ = value;
            bitField0_ |= 0x00000040;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * Whether to pretty print for json.
         * The default value is 'false'.
         * </pre>
         *
         * <code>optional bool pretty = 7;</code>
         * @return This builder for chaining.
         */
        public Builder clearPretty() {
            bitField0_ = (bitField0_ & ~0x00000040);
            pretty_ = false;
            onChanged();
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

        // @@protoc_insertion_point(builder_scope:org.apache.dubbo.metadata.OpenAPIRequest)
    }

    // @@protoc_insertion_point(class_scope:org.apache.dubbo.metadata.OpenAPIRequest)
    private static final OpenAPIRequest DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new OpenAPIRequest();
    }

    public static OpenAPIRequest getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<OpenAPIRequest> PARSER =
            new com.google.protobuf.AbstractParser<OpenAPIRequest>() {
                @Override
                public OpenAPIRequest parsePartialFrom(
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

    public static com.google.protobuf.Parser<OpenAPIRequest> parser() {
        return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<OpenAPIRequest> getParserForType() {
        return PARSER;
    }

    @Override
    public OpenAPIRequest getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }
}
