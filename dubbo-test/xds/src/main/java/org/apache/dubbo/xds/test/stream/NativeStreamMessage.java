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
package org.apache.dubbo.xds.test.stream;

/**
 * Protobuf type {@code xds.native.testing.NativeStreamMessage}
 */
@com.google.protobuf.Generated
public final class NativeStreamMessage extends com.google.protobuf.GeneratedMessage
        implements
        // @@protoc_insertion_point(message_implements:xds.native.testing.NativeStreamMessage)
        NativeStreamMessageOrBuilder {
    private static final long serialVersionUID = 0L;

    static {
        com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
                com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
                /* major= */ 4,
                /* minor= */ 33,
                /* patch= */ 4,
                /* suffix= */ "",
                "NativeStreamMessage");
    }
    // Use NativeStreamMessage.newBuilder() to construct.
    private NativeStreamMessage(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
        super(builder);
    }

    private NativeStreamMessage() {
        value_ = "";
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return org.apache.dubbo.xds.test.stream.NativeStreamsOuterClass
                .internal_static_xds_native_testing_NativeStreamMessage_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable internalGetFieldAccessorTable() {
        return org.apache.dubbo.xds.test.stream.NativeStreamsOuterClass
                .internal_static_xds_native_testing_NativeStreamMessage_fieldAccessorTable
                .ensureFieldAccessorsInitialized(
                        org.apache.dubbo.xds.test.stream.NativeStreamMessage.class,
                        org.apache.dubbo.xds.test.stream.NativeStreamMessage.Builder.class);
    }

    public static final int VALUE_FIELD_NUMBER = 1;

    @SuppressWarnings("serial")
    private volatile java.lang.Object value_ = "";
    /**
     * <code>string value = 1;</code>
     * @return The value.
     */
    @java.lang.Override
    public java.lang.String getValue() {
        java.lang.Object ref = value_;
        if (ref instanceof java.lang.String) {
            return (java.lang.String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            java.lang.String s = bs.toStringUtf8();
            value_ = s;
            return s;
        }
    }
    /**
     * <code>string value = 1;</code>
     * @return The bytes for value.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString getValueBytes() {
        java.lang.Object ref = value_;
        if (ref instanceof java.lang.String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
            value_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    private byte memoizedIsInitialized = -1;

    @java.lang.Override
    public final boolean isInitialized() {
        byte isInitialized = memoizedIsInitialized;
        if (isInitialized == 1) return true;
        if (isInitialized == 0) return false;

        memoizedIsInitialized = 1;
        return true;
    }

    @java.lang.Override
    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
        if (!com.google.protobuf.GeneratedMessage.isStringEmpty(value_)) {
            com.google.protobuf.GeneratedMessage.writeString(output, 1, value_);
        }
        getUnknownFields().writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1) return size;

        size = 0;
        if (!com.google.protobuf.GeneratedMessage.isStringEmpty(value_)) {
            size += com.google.protobuf.GeneratedMessage.computeStringSize(1, value_);
        }
        size += getUnknownFields().getSerializedSize();
        memoizedSize = size;
        return size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof org.apache.dubbo.xds.test.stream.NativeStreamMessage)) {
            return super.equals(obj);
        }
        org.apache.dubbo.xds.test.stream.NativeStreamMessage other =
                (org.apache.dubbo.xds.test.stream.NativeStreamMessage) obj;

        if (!getValue().equals(other.getValue())) return false;
        if (!getUnknownFields().equals(other.getUnknownFields())) return false;
        return true;
    }

    @java.lang.Override
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

    public static org.apache.dubbo.xds.test.stream.NativeStreamMessage parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static org.apache.dubbo.xds.test.stream.NativeStreamMessage parseFrom(
            java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static org.apache.dubbo.xds.test.stream.NativeStreamMessage parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static org.apache.dubbo.xds.test.stream.NativeStreamMessage parseFrom(
            com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static org.apache.dubbo.xds.test.stream.NativeStreamMessage parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static org.apache.dubbo.xds.test.stream.NativeStreamMessage parseFrom(
            byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static org.apache.dubbo.xds.test.stream.NativeStreamMessage parseFrom(java.io.InputStream input)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessage.parseWithIOException(PARSER, input);
    }

    public static org.apache.dubbo.xds.test.stream.NativeStreamMessage parseFrom(
            java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessage.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static org.apache.dubbo.xds.test.stream.NativeStreamMessage parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessage.parseDelimitedWithIOException(PARSER, input);
    }

    public static org.apache.dubbo.xds.test.stream.NativeStreamMessage parseDelimitedFrom(
            java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessage.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static org.apache.dubbo.xds.test.stream.NativeStreamMessage parseFrom(
            com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessage.parseWithIOException(PARSER, input);
    }

    public static org.apache.dubbo.xds.test.stream.NativeStreamMessage parseFrom(
            com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessage.parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() {
        return newBuilder();
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(org.apache.dubbo.xds.test.stream.NativeStreamMessage prototype) {
        return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    @java.lang.Override
    public Builder toBuilder() {
        return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(com.google.protobuf.GeneratedMessage.BuilderParent parent) {
        Builder builder = new Builder(parent);
        return builder;
    }
    /**
     * Protobuf type {@code xds.native.testing.NativeStreamMessage}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessage.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:xds.native.testing.NativeStreamMessage)
            org.apache.dubbo.xds.test.stream.NativeStreamMessageOrBuilder {
        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return org.apache.dubbo.xds.test.stream.NativeStreamsOuterClass
                    .internal_static_xds_native_testing_NativeStreamMessage_descriptor;
        }

        @java.lang.Override
        protected com.google.protobuf.GeneratedMessage.FieldAccessorTable internalGetFieldAccessorTable() {
            return org.apache.dubbo.xds.test.stream.NativeStreamsOuterClass
                    .internal_static_xds_native_testing_NativeStreamMessage_fieldAccessorTable
                    .ensureFieldAccessorsInitialized(
                            org.apache.dubbo.xds.test.stream.NativeStreamMessage.class,
                            org.apache.dubbo.xds.test.stream.NativeStreamMessage.Builder.class);
        }

        // Construct using org.apache.dubbo.xds.test.stream.NativeStreamMessage.newBuilder()
        private Builder() {}

        private Builder(com.google.protobuf.GeneratedMessage.BuilderParent parent) {
            super(parent);
        }

        @java.lang.Override
        public Builder clear() {
            super.clear();
            bitField0_ = 0;
            value_ = "";
            return this;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return org.apache.dubbo.xds.test.stream.NativeStreamsOuterClass
                    .internal_static_xds_native_testing_NativeStreamMessage_descriptor;
        }

        @java.lang.Override
        public org.apache.dubbo.xds.test.stream.NativeStreamMessage getDefaultInstanceForType() {
            return org.apache.dubbo.xds.test.stream.NativeStreamMessage.getDefaultInstance();
        }

        @java.lang.Override
        public org.apache.dubbo.xds.test.stream.NativeStreamMessage build() {
            org.apache.dubbo.xds.test.stream.NativeStreamMessage result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @java.lang.Override
        public org.apache.dubbo.xds.test.stream.NativeStreamMessage buildPartial() {
            org.apache.dubbo.xds.test.stream.NativeStreamMessage result =
                    new org.apache.dubbo.xds.test.stream.NativeStreamMessage(this);
            if (bitField0_ != 0) {
                buildPartial0(result);
            }
            onBuilt();
            return result;
        }

        private void buildPartial0(org.apache.dubbo.xds.test.stream.NativeStreamMessage result) {
            int from_bitField0_ = bitField0_;
            if (((from_bitField0_ & 0x00000001) != 0)) {
                result.value_ = value_;
            }
        }

        @java.lang.Override
        public Builder mergeFrom(com.google.protobuf.Message other) {
            if (other instanceof org.apache.dubbo.xds.test.stream.NativeStreamMessage) {
                return mergeFrom((org.apache.dubbo.xds.test.stream.NativeStreamMessage) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(org.apache.dubbo.xds.test.stream.NativeStreamMessage other) {
            if (other == org.apache.dubbo.xds.test.stream.NativeStreamMessage.getDefaultInstance()) return this;
            if (!other.getValue().isEmpty()) {
                value_ = other.value_;
                bitField0_ |= 0x00000001;
                onChanged();
            }
            this.mergeUnknownFields(other.getUnknownFields());
            onChanged();
            return this;
        }

        @java.lang.Override
        public final boolean isInitialized() {
            return true;
        }

        @java.lang.Override
        public Builder mergeFrom(
                com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            if (extensionRegistry == null) {
                throw new java.lang.NullPointerException();
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

        private java.lang.Object value_ = "";
        /**
         * <code>string value = 1;</code>
         * @return The value.
         */
        public java.lang.String getValue() {
            java.lang.Object ref = value_;
            if (!(ref instanceof java.lang.String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                value_ = s;
                return s;
            } else {
                return (java.lang.String) ref;
            }
        }
        /**
         * <code>string value = 1;</code>
         * @return The bytes for value.
         */
        public com.google.protobuf.ByteString getValueBytes() {
            java.lang.Object ref = value_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
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
        public Builder setValue(java.lang.String value) {
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

        // @@protoc_insertion_point(builder_scope:xds.native.testing.NativeStreamMessage)
    }

    // @@protoc_insertion_point(class_scope:xds.native.testing.NativeStreamMessage)
    private static final org.apache.dubbo.xds.test.stream.NativeStreamMessage DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new org.apache.dubbo.xds.test.stream.NativeStreamMessage();
    }

    public static org.apache.dubbo.xds.test.stream.NativeStreamMessage getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<NativeStreamMessage> PARSER =
            new com.google.protobuf.AbstractParser<NativeStreamMessage>() {
                @java.lang.Override
                public NativeStreamMessage parsePartialFrom(
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

    public static com.google.protobuf.Parser<NativeStreamMessage> parser() {
        return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<NativeStreamMessage> getParserForType() {
        return PARSER;
    }

    @java.lang.Override
    public org.apache.dubbo.xds.test.stream.NativeStreamMessage getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }
}
