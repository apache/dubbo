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
 * Protobuf type {@code org.apache.dubbo.metadata.Revision}
 */
public final class Revision extends com.google.protobuf.GeneratedMessageV3
        implements
        // @@protoc_insertion_point(message_implements:org.apache.dubbo.metadata.Revision)
        RevisionOrBuilder {
    private static final long serialVersionUID = 0L;
    // Use Revision.newBuilder() to construct.
    private Revision(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private Revision() {
        value_ = "";
    }

    @Override
    @SuppressWarnings({"unused"})
    protected Object newInstance(UnusedPrivateParameter unused) {
        return new Revision();
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return MetadataServiceV2OuterClass.internal_static_org_apache_dubbo_metadata_Revision_descriptor;
    }

    @Override
    protected FieldAccessorTable internalGetFieldAccessorTable() {
        return MetadataServiceV2OuterClass.internal_static_org_apache_dubbo_metadata_Revision_fieldAccessorTable
                .ensureFieldAccessorsInitialized(Revision.class, Builder.class);
    }

    public static final int VALUE_FIELD_NUMBER = 1;

    @SuppressWarnings("serial")
    private volatile Object value_ = "";
    /**
     * <code>string value = 1;</code>
     * @return The value.
     */
    @Override
    public String getValue() {
        Object ref = value_;
        if (ref instanceof String) {
            return (String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            String s = bs.toStringUtf8();
            value_ = s;
            return s;
        }
    }
    /**
     * <code>string value = 1;</code>
     * @return The bytes for value.
     */
    @Override
    public com.google.protobuf.ByteString getValueBytes() {
        Object ref = value_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
            value_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    private byte memoizedIsInitialized = -1;

    @Override
    public final boolean isInitialized() {
        byte isInitialized = memoizedIsInitialized;
        if (isInitialized == 1) return true;
        if (isInitialized == 0) return false;

        memoizedIsInitialized = 1;
        return true;
    }

    @Override
    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(value_)) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 1, value_);
        }
        getUnknownFields().writeTo(output);
    }

    @Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1) return size;

        size = 0;
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(value_)) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, value_);
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
        if (!(obj instanceof Revision)) {
            return super.equals(obj);
        }
        Revision other = (Revision) obj;

        if (!getValue().equals(other.getValue())) return false;
        if (!getUnknownFields().equals(other.getUnknownFields())) return false;
        return true;
    }

    @Override
    public int hashCode() {
        if (memoizedHashCode != 0) {
            return memoizedHashCode;
        }
        int hash = 41;
        hash = (19 * hash) + getDescriptor().hashCode();
        hash = (37 * hash) + VALUE_FIELD_NUMBER;
        hash = (53 * hash) + getValue().hashCode();
        hash = (29 * hash) + getUnknownFields().hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static Revision parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static Revision parseFrom(
            java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static Revision parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static Revision parseFrom(
            com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static Revision parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static Revision parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static Revision parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static Revision parseFrom(
            java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static Revision parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static Revision parseDelimitedFrom(
            java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static Revision parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static Revision parseFrom(
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

    public static Builder newBuilder(Revision prototype) {
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
     * Protobuf type {@code org.apache.dubbo.metadata.Revision}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:org.apache.dubbo.metadata.Revision)
            RevisionOrBuilder {
        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return MetadataServiceV2OuterClass.internal_static_org_apache_dubbo_metadata_Revision_descriptor;
        }

        @Override
        protected FieldAccessorTable internalGetFieldAccessorTable() {
            return MetadataServiceV2OuterClass.internal_static_org_apache_dubbo_metadata_Revision_fieldAccessorTable
                    .ensureFieldAccessorsInitialized(Revision.class, Builder.class);
        }

        // Construct using org.apache.dubbo.metadata.Revision.newBuilder()
        private Builder() {}

        private Builder(BuilderParent parent) {
            super(parent);
        }

        @Override
        public Builder clear() {
            super.clear();
            bitField0_ = 0;
            value_ = "";
            return this;
        }

        @Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return MetadataServiceV2OuterClass.internal_static_org_apache_dubbo_metadata_Revision_descriptor;
        }

        @Override
        public Revision getDefaultInstanceForType() {
            return Revision.getDefaultInstance();
        }

        @Override
        public Revision build() {
            Revision result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @Override
        public Revision buildPartial() {
            Revision result = new Revision(this);
            if (bitField0_ != 0) {
                buildPartial0(result);
            }
            onBuilt();
            return result;
        }

        private void buildPartial0(Revision result) {
            int from_bitField0_ = bitField0_;
            if (((from_bitField0_ & 0x00000001) != 0)) {
                result.value_ = value_;
            }
        }

        @Override
        public Builder mergeFrom(com.google.protobuf.Message other) {
            if (other instanceof Revision) {
                return mergeFrom((Revision) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(Revision other) {
            if (other == Revision.getDefaultInstance()) return this;
            if (!other.getValue().isEmpty()) {
                value_ = other.value_;
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
                            value_ = input.readStringRequireUtf8();
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

        private Object value_ = "";
        /**
         * <code>string value = 1;</code>
         * @return The value.
         */
        public String getValue() {
            Object ref = value_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                String s = bs.toStringUtf8();
                value_ = s;
                return s;
            } else {
                return (String) ref;
            }
        }
        /**
         * <code>string value = 1;</code>
         * @return The bytes for value.
         */
        public com.google.protobuf.ByteString getValueBytes() {
            Object ref = value_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
                value_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }
        /**
         * <code>string value = 1;</code>
         * @param value The value to set.
         * @return This builder for chaining.
         */
        public Builder setValue(String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            value_ = value;
            bitField0_ |= 0x00000001;
            onChanged();
            return this;
        }
        /**
         * <code>string value = 1;</code>
         * @return This builder for chaining.
         */
        public Builder clearValue() {
            value_ = getDefaultInstance().getValue();
            bitField0_ = (bitField0_ & ~0x00000001);
            onChanged();
            return this;
        }
        /**
         * <code>string value = 1;</code>
         * @param value The bytes for value to set.
         * @return This builder for chaining.
         */
        public Builder setValueBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            value_ = value;
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

        // @@protoc_insertion_point(builder_scope:org.apache.dubbo.metadata.Revision)
    }

    // @@protoc_insertion_point(class_scope:org.apache.dubbo.metadata.Revision)
    private static final Revision DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new Revision();
    }

    public static Revision getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<Revision> PARSER =
            new com.google.protobuf.AbstractParser<Revision>() {
                @Override
                public Revision parsePartialFrom(
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

    public static com.google.protobuf.Parser<Revision> parser() {
        return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<Revision> getParserForType() {
        return PARSER;
    }

    @Override
    public Revision getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }
}
