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
 * Metadata request message.
 * </pre>
 *
 * Protobuf type {@code org.apache.dubbo.metadata.MetadataRequest}
 */
public final class MetadataRequest extends com.google.protobuf.GeneratedMessageV3
        implements
        // @@protoc_insertion_point(message_implements:org.apache.dubbo.metadata.MetadataRequest)
        MetadataRequestOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use MetadataRequest.newBuilder() to construct.
    private MetadataRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private MetadataRequest() {
        revision_ = "";
    }

    @Override
    @SuppressWarnings({"unused"})
    protected Object newInstance(UnusedPrivateParameter unused) {
        return new MetadataRequest();
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return MetadataServiceV2OuterClass.internal_static_org_apache_dubbo_metadata_MetadataRequest_descriptor;
    }

    @Override
    protected FieldAccessorTable internalGetFieldAccessorTable() {
        return MetadataServiceV2OuterClass.internal_static_org_apache_dubbo_metadata_MetadataRequest_fieldAccessorTable
                .ensureFieldAccessorsInitialized(MetadataRequest.class, Builder.class);
    }

    public static final int REVISION_FIELD_NUMBER = 1;

    @SuppressWarnings("serial")
    private volatile Object revision_ = "";

    /**
     * <pre>
     * The revision of the metadata.
     * </pre>
     *
     * <code>string revision = 1;</code>
     * @return The revision.
     */
    @Override
    public String getRevision() {
        Object ref = revision_;
        if (ref instanceof String) {
            return (String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            String s = bs.toStringUtf8();
            revision_ = s;
            return s;
        }
    }

    /**
     * <pre>
     * The revision of the metadata.
     * </pre>
     *
     * <code>string revision = 1;</code>
     * @return The bytes for revision.
     */
    @Override
    public com.google.protobuf.ByteString getRevisionBytes() {
        Object ref = revision_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
            revision_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
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
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(revision_)) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 1, revision_);
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
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(revision_)) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, revision_);
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
        if (!(obj instanceof MetadataRequest)) {
            return super.equals(obj);
        }
        MetadataRequest other = (MetadataRequest) obj;

        if (!getRevision().equals(other.getRevision())) {
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
        hash = (37 * hash) + REVISION_FIELD_NUMBER;
        hash = (53 * hash) + getRevision().hashCode();
        hash = (29 * hash) + getUnknownFields().hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static MetadataRequest parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static MetadataRequest parseFrom(
            java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static MetadataRequest parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static MetadataRequest parseFrom(
            com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static MetadataRequest parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static MetadataRequest parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static MetadataRequest parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static MetadataRequest parseFrom(
            java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static MetadataRequest parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static MetadataRequest parseDelimitedFrom(
            java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static MetadataRequest parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static MetadataRequest parseFrom(
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

    public static Builder newBuilder(MetadataRequest prototype) {
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
     * Metadata request message.
     * </pre>
     *
     * Protobuf type {@code org.apache.dubbo.metadata.MetadataRequest}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:org.apache.dubbo.metadata.MetadataRequest)
            MetadataRequestOrBuilder {
        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return MetadataServiceV2OuterClass.internal_static_org_apache_dubbo_metadata_MetadataRequest_descriptor;
        }

        @Override
        protected FieldAccessorTable internalGetFieldAccessorTable() {
            return MetadataServiceV2OuterClass
                    .internal_static_org_apache_dubbo_metadata_MetadataRequest_fieldAccessorTable
                    .ensureFieldAccessorsInitialized(MetadataRequest.class, Builder.class);
        }

        // Construct using org.apache.dubbo.metadata.MetadataRequest.newBuilder()
        private Builder() {}

        private Builder(BuilderParent parent) {
            super(parent);
        }

        @Override
        public Builder clear() {
            super.clear();
            bitField0_ = 0;
            revision_ = "";
            return this;
        }

        @Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return MetadataServiceV2OuterClass.internal_static_org_apache_dubbo_metadata_MetadataRequest_descriptor;
        }

        @Override
        public MetadataRequest getDefaultInstanceForType() {
            return MetadataRequest.getDefaultInstance();
        }

        @Override
        public MetadataRequest build() {
            MetadataRequest result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @Override
        public MetadataRequest buildPartial() {
            MetadataRequest result = new MetadataRequest(this);
            if (bitField0_ != 0) {
                buildPartial0(result);
            }
            onBuilt();
            return result;
        }

        private void buildPartial0(MetadataRequest result) {
            int from_bitField0_ = bitField0_;
            if (((from_bitField0_ & 0x00000001) != 0)) {
                result.revision_ = revision_;
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
            if (other instanceof MetadataRequest) {
                return mergeFrom((MetadataRequest) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(MetadataRequest other) {
            if (other == MetadataRequest.getDefaultInstance()) {
                return this;
            }
            if (!other.getRevision().isEmpty()) {
                revision_ = other.revision_;
                bitField0_ |= 0x00000001;
                onChanged();
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
                            revision_ = input.readStringRequireUtf8();
                            bitField0_ |= 0x00000001;
                            break;
                        } // case 10
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

        private Object revision_ = "";

        /**
         * <pre>
         * The revision of the metadata.
         * </pre>
         *
         * <code>string revision = 1;</code>
         * @return The revision.
         */
        public String getRevision() {
            Object ref = revision_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                String s = bs.toStringUtf8();
                revision_ = s;
                return s;
            } else {
                return (String) ref;
            }
        }

        /**
         * <pre>
         * The revision of the metadata.
         * </pre>
         *
         * <code>string revision = 1;</code>
         * @return The bytes for revision.
         */
        public com.google.protobuf.ByteString getRevisionBytes() {
            Object ref = revision_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
                revision_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <pre>
         * The revision of the metadata.
         * </pre>
         *
         * <code>string revision = 1;</code>
         * @param value The revision to set.
         * @return This builder for chaining.
         */
        public Builder setRevision(String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            revision_ = value;
            bitField0_ |= 0x00000001;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The revision of the metadata.
         * </pre>
         *
         * <code>string revision = 1;</code>
         * @return This builder for chaining.
         */
        public Builder clearRevision() {
            revision_ = getDefaultInstance().getRevision();
            bitField0_ = (bitField0_ & ~0x00000001);
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The revision of the metadata.
         * </pre>
         *
         * <code>string revision = 1;</code>
         * @param value The bytes for revision to set.
         * @return This builder for chaining.
         */
        public Builder setRevisionBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            revision_ = value;
            bitField0_ |= 0x00000001;
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

        // @@protoc_insertion_point(builder_scope:org.apache.dubbo.metadata.MetadataRequest)
    }

    // @@protoc_insertion_point(class_scope:org.apache.dubbo.metadata.MetadataRequest)
    private static final MetadataRequest DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new MetadataRequest();
    }

    public static MetadataRequest getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<MetadataRequest> PARSER =
            new com.google.protobuf.AbstractParser<MetadataRequest>() {
                @Override
                public MetadataRequest parsePartialFrom(
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

    public static com.google.protobuf.Parser<MetadataRequest> parser() {
        return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<MetadataRequest> getParserForType() {
        return PARSER;
    }

    @Override
    public MetadataRequest getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }
}
