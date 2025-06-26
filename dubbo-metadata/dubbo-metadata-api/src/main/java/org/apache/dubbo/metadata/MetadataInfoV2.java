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
 * Metadata information message.
 * </pre>
 *
 * Protobuf type {@code org.apache.dubbo.metadata.MetadataInfoV2}
 */
public final class MetadataInfoV2 extends com.google.protobuf.GeneratedMessageV3
        implements
        // @@protoc_insertion_point(message_implements:org.apache.dubbo.metadata.MetadataInfoV2)
        MetadataInfoV2OrBuilder {
    private static final long serialVersionUID = 0L;

    // Use MetadataInfoV2.newBuilder() to construct.
    private MetadataInfoV2(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private MetadataInfoV2() {
        app_ = "";
        version_ = "";
    }

    @Override
    @SuppressWarnings({"unused"})
    protected Object newInstance(UnusedPrivateParameter unused) {
        return new MetadataInfoV2();
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return MetadataServiceV2OuterClass.internal_static_org_apache_dubbo_metadata_MetadataInfoV2_descriptor;
    }

    @SuppressWarnings({"rawtypes"})
    @Override
    protected com.google.protobuf.MapField internalGetMapField(int number) {
        switch (number) {
            case 3:
                return internalGetServices();
            default:
                throw new RuntimeException("Invalid map field number: " + number);
        }
    }

    @Override
    protected FieldAccessorTable internalGetFieldAccessorTable() {
        return MetadataServiceV2OuterClass.internal_static_org_apache_dubbo_metadata_MetadataInfoV2_fieldAccessorTable
                .ensureFieldAccessorsInitialized(MetadataInfoV2.class, Builder.class);
    }

    public static final int APP_FIELD_NUMBER = 1;

    @SuppressWarnings("serial")
    private volatile Object app_ = "";

    /**
     * <pre>
     * The application name.
     * </pre>
     *
     * <code>string app = 1;</code>
     * @return The app.
     */
    @Override
    public String getApp() {
        Object ref = app_;
        if (ref instanceof String) {
            return (String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            String s = bs.toStringUtf8();
            app_ = s;
            return s;
        }
    }

    /**
     * <pre>
     * The application name.
     * </pre>
     *
     * <code>string app = 1;</code>
     * @return The bytes for app.
     */
    @Override
    public com.google.protobuf.ByteString getAppBytes() {
        Object ref = app_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
            app_ = b;
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
     * The application version.
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
     * The application version.
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

    public static final int SERVICES_FIELD_NUMBER = 3;

    private static final class ServicesDefaultEntryHolder {
        static final com.google.protobuf.MapEntry<String, ServiceInfoV2> defaultEntry =
                com.google.protobuf.MapEntry.<String, ServiceInfoV2>newDefaultInstance(
                        MetadataServiceV2OuterClass
                                .internal_static_org_apache_dubbo_metadata_MetadataInfoV2_ServicesEntry_descriptor,
                        com.google.protobuf.WireFormat.FieldType.STRING,
                        "",
                        com.google.protobuf.WireFormat.FieldType.MESSAGE,
                        ServiceInfoV2.getDefaultInstance());
    }

    @SuppressWarnings("serial")
    private com.google.protobuf.MapField<String, ServiceInfoV2> services_;

    private com.google.protobuf.MapField<String, ServiceInfoV2> internalGetServices() {
        if (services_ == null) {
            return com.google.protobuf.MapField.emptyMapField(ServicesDefaultEntryHolder.defaultEntry);
        }
        return services_;
    }

    public int getServicesCount() {
        return internalGetServices().getMap().size();
    }

    /**
     * <pre>
     * A map of service information.
     * </pre>
     *
     * <code>map&lt;string, .org.apache.dubbo.metadata.ServiceInfoV2&gt; services = 3;</code>
     */
    @Override
    public boolean containsServices(String key) {
        if (key == null) {
            throw new NullPointerException("map key");
        }
        return internalGetServices().getMap().containsKey(key);
    }

    /**
     * Use {@link #getServicesMap()} instead.
     */
    @Override
    @Deprecated
    public java.util.Map<String, ServiceInfoV2> getServices() {
        return getServicesMap();
    }

    /**
     * <pre>
     * A map of service information.
     * </pre>
     *
     * <code>map&lt;string, .org.apache.dubbo.metadata.ServiceInfoV2&gt; services = 3;</code>
     */
    @Override
    public java.util.Map<String, ServiceInfoV2> getServicesMap() {
        return internalGetServices().getMap();
    }

    /**
     * <pre>
     * A map of service information.
     * </pre>
     *
     * <code>map&lt;string, .org.apache.dubbo.metadata.ServiceInfoV2&gt; services = 3;</code>
     */
    @Override
    public /* nullable */ ServiceInfoV2 getServicesOrDefault(
            String key,
            /* nullable */
            ServiceInfoV2 defaultValue) {
        if (key == null) {
            throw new NullPointerException("map key");
        }
        java.util.Map<String, ServiceInfoV2> map = internalGetServices().getMap();
        return map.containsKey(key) ? map.get(key) : defaultValue;
    }

    /**
     * <pre>
     * A map of service information.
     * </pre>
     *
     * <code>map&lt;string, .org.apache.dubbo.metadata.ServiceInfoV2&gt; services = 3;</code>
     */
    @Override
    public ServiceInfoV2 getServicesOrThrow(String key) {
        if (key == null) {
            throw new NullPointerException("map key");
        }
        java.util.Map<String, ServiceInfoV2> map = internalGetServices().getMap();
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
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(app_)) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 1, app_);
        }
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(version_)) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 2, version_);
        }
        com.google.protobuf.GeneratedMessageV3.serializeStringMapTo(
                output, internalGetServices(), ServicesDefaultEntryHolder.defaultEntry, 3);
        getUnknownFields().writeTo(output);
    }

    @Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1) {
            return size;
        }

        size = 0;
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(app_)) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, app_);
        }
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(version_)) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, version_);
        }
        for (java.util.Map.Entry<String, ServiceInfoV2> entry :
                internalGetServices().getMap().entrySet()) {
            com.google.protobuf.MapEntry<String, ServiceInfoV2> services__ = ServicesDefaultEntryHolder.defaultEntry
                    .newBuilderForType()
                    .setKey(entry.getKey())
                    .setValue(entry.getValue())
                    .build();
            size += com.google.protobuf.CodedOutputStream.computeMessageSize(3, services__);
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
        if (!(obj instanceof MetadataInfoV2)) {
            return super.equals(obj);
        }
        MetadataInfoV2 other = (MetadataInfoV2) obj;

        if (!getApp().equals(other.getApp())) {
            return false;
        }
        if (!getVersion().equals(other.getVersion())) {
            return false;
        }
        if (!internalGetServices().equals(other.internalGetServices())) {
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
        hash = (37 * hash) + APP_FIELD_NUMBER;
        hash = (53 * hash) + getApp().hashCode();
        hash = (37 * hash) + VERSION_FIELD_NUMBER;
        hash = (53 * hash) + getVersion().hashCode();
        if (!internalGetServices().getMap().isEmpty()) {
            hash = (37 * hash) + SERVICES_FIELD_NUMBER;
            hash = (53 * hash) + internalGetServices().hashCode();
        }
        hash = (29 * hash) + getUnknownFields().hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static MetadataInfoV2 parseFrom(java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static MetadataInfoV2 parseFrom(
            java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static MetadataInfoV2 parseFrom(com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static MetadataInfoV2 parseFrom(
            com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static MetadataInfoV2 parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static MetadataInfoV2 parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static MetadataInfoV2 parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static MetadataInfoV2 parseFrom(
            java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static MetadataInfoV2 parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static MetadataInfoV2 parseDelimitedFrom(
            java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static MetadataInfoV2 parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static MetadataInfoV2 parseFrom(
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

    public static Builder newBuilder(MetadataInfoV2 prototype) {
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
     * Metadata information message.
     * </pre>
     *
     * Protobuf type {@code org.apache.dubbo.metadata.MetadataInfoV2}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
            implements
            // @@protoc_insertion_point(builder_implements:org.apache.dubbo.metadata.MetadataInfoV2)
            MetadataInfoV2OrBuilder {
        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return MetadataServiceV2OuterClass.internal_static_org_apache_dubbo_metadata_MetadataInfoV2_descriptor;
        }

        @SuppressWarnings({"rawtypes"})
        protected com.google.protobuf.MapField internalGetMapField(int number) {
            switch (number) {
                case 3:
                    return internalGetServices();
                default:
                    throw new RuntimeException("Invalid map field number: " + number);
            }
        }

        @SuppressWarnings({"rawtypes"})
        protected com.google.protobuf.MapField internalGetMutableMapField(int number) {
            switch (number) {
                case 3:
                    return internalGetMutableServices();
                default:
                    throw new RuntimeException("Invalid map field number: " + number);
            }
        }

        @Override
        protected FieldAccessorTable internalGetFieldAccessorTable() {
            return MetadataServiceV2OuterClass
                    .internal_static_org_apache_dubbo_metadata_MetadataInfoV2_fieldAccessorTable
                    .ensureFieldAccessorsInitialized(MetadataInfoV2.class, Builder.class);
        }

        // Construct using org.apache.dubbo.metadata.MetadataInfoV2.newBuilder()
        private Builder() {}

        private Builder(BuilderParent parent) {
            super(parent);
        }

        @Override
        public Builder clear() {
            super.clear();
            bitField0_ = 0;
            app_ = "";
            version_ = "";
            internalGetMutableServices().clear();
            return this;
        }

        @Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return MetadataServiceV2OuterClass.internal_static_org_apache_dubbo_metadata_MetadataInfoV2_descriptor;
        }

        @Override
        public MetadataInfoV2 getDefaultInstanceForType() {
            return MetadataInfoV2.getDefaultInstance();
        }

        @Override
        public MetadataInfoV2 build() {
            MetadataInfoV2 result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @Override
        public MetadataInfoV2 buildPartial() {
            MetadataInfoV2 result = new MetadataInfoV2(this);
            if (bitField0_ != 0) {
                buildPartial0(result);
            }
            onBuilt();
            return result;
        }

        private void buildPartial0(MetadataInfoV2 result) {
            int from_bitField0_ = bitField0_;
            if (((from_bitField0_ & 0x00000001) != 0)) {
                result.app_ = app_;
            }
            if (((from_bitField0_ & 0x00000002) != 0)) {
                result.version_ = version_;
            }
            if (((from_bitField0_ & 0x00000004) != 0)) {
                result.services_ = internalGetServices();
                result.services_.makeImmutable();
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
            if (other instanceof MetadataInfoV2) {
                return mergeFrom((MetadataInfoV2) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(MetadataInfoV2 other) {
            if (other == MetadataInfoV2.getDefaultInstance()) {
                return this;
            }
            if (!other.getApp().isEmpty()) {
                app_ = other.app_;
                bitField0_ |= 0x00000001;
                onChanged();
            }
            if (!other.getVersion().isEmpty()) {
                version_ = other.version_;
                bitField0_ |= 0x00000002;
                onChanged();
            }
            internalGetMutableServices().mergeFrom(other.internalGetServices());
            bitField0_ |= 0x00000004;
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
                            app_ = input.readStringRequireUtf8();
                            bitField0_ |= 0x00000001;
                            break;
                        } // case 10
                        case 18: {
                            version_ = input.readStringRequireUtf8();
                            bitField0_ |= 0x00000002;
                            break;
                        } // case 18
                        case 26: {
                            com.google.protobuf.MapEntry<String, ServiceInfoV2> services__ = input.readMessage(
                                    ServicesDefaultEntryHolder.defaultEntry.getParserForType(), extensionRegistry);
                            internalGetMutableServices()
                                    .getMutableMap()
                                    .put(services__.getKey(), services__.getValue());
                            bitField0_ |= 0x00000004;
                            break;
                        } // case 26
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

        private Object app_ = "";

        /**
         * <pre>
         * The application name.
         * </pre>
         *
         * <code>string app = 1;</code>
         * @return The app.
         */
        public String getApp() {
            Object ref = app_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                String s = bs.toStringUtf8();
                app_ = s;
                return s;
            } else {
                return (String) ref;
            }
        }

        /**
         * <pre>
         * The application name.
         * </pre>
         *
         * <code>string app = 1;</code>
         * @return The bytes for app.
         */
        public com.google.protobuf.ByteString getAppBytes() {
            Object ref = app_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
                app_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <pre>
         * The application name.
         * </pre>
         *
         * <code>string app = 1;</code>
         * @param value The app to set.
         * @return This builder for chaining.
         */
        public Builder setApp(String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            app_ = value;
            bitField0_ |= 0x00000001;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The application name.
         * </pre>
         *
         * <code>string app = 1;</code>
         * @return This builder for chaining.
         */
        public Builder clearApp() {
            app_ = getDefaultInstance().getApp();
            bitField0_ = (bitField0_ & ~0x00000001);
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The application name.
         * </pre>
         *
         * <code>string app = 1;</code>
         * @param value The bytes for app to set.
         * @return This builder for chaining.
         */
        public Builder setAppBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            app_ = value;
            bitField0_ |= 0x00000001;
            onChanged();
            return this;
        }

        private Object version_ = "";

        /**
         * <pre>
         * The application version.
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
         * The application version.
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
         * The application version.
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
         * The application version.
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
         * The application version.
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

        private com.google.protobuf.MapField<String, ServiceInfoV2> services_;

        private com.google.protobuf.MapField<String, ServiceInfoV2> internalGetServices() {
            if (services_ == null) {
                return com.google.protobuf.MapField.emptyMapField(ServicesDefaultEntryHolder.defaultEntry);
            }
            return services_;
        }

        private com.google.protobuf.MapField<String, ServiceInfoV2> internalGetMutableServices() {
            if (services_ == null) {
                services_ = com.google.protobuf.MapField.newMapField(ServicesDefaultEntryHolder.defaultEntry);
            }
            if (!services_.isMutable()) {
                services_ = services_.copy();
            }
            bitField0_ |= 0x00000004;
            onChanged();
            return services_;
        }

        public int getServicesCount() {
            return internalGetServices().getMap().size();
        }

        /**
         * <pre>
         * A map of service information.
         * </pre>
         *
         * <code>map&lt;string, .org.apache.dubbo.metadata.ServiceInfoV2&gt; services = 3;</code>
         */
        @Override
        public boolean containsServices(String key) {
            if (key == null) {
                throw new NullPointerException("map key");
            }
            return internalGetServices().getMap().containsKey(key);
        }

        /**
         * Use {@link #getServicesMap()} instead.
         */
        @Override
        @Deprecated
        public java.util.Map<String, ServiceInfoV2> getServices() {
            return getServicesMap();
        }

        /**
         * <pre>
         * A map of service information.
         * </pre>
         *
         * <code>map&lt;string, .org.apache.dubbo.metadata.ServiceInfoV2&gt; services = 3;</code>
         */
        @Override
        public java.util.Map<String, ServiceInfoV2> getServicesMap() {
            return internalGetServices().getMap();
        }

        /**
         * <pre>
         * A map of service information.
         * </pre>
         *
         * <code>map&lt;string, .org.apache.dubbo.metadata.ServiceInfoV2&gt; services = 3;</code>
         */
        @Override
        public /* nullable */ ServiceInfoV2 getServicesOrDefault(
                String key,
                /* nullable */
                ServiceInfoV2 defaultValue) {
            if (key == null) {
                throw new NullPointerException("map key");
            }
            java.util.Map<String, ServiceInfoV2> map = internalGetServices().getMap();
            return map.containsKey(key) ? map.get(key) : defaultValue;
        }

        /**
         * <pre>
         * A map of service information.
         * </pre>
         *
         * <code>map&lt;string, .org.apache.dubbo.metadata.ServiceInfoV2&gt; services = 3;</code>
         */
        @Override
        public ServiceInfoV2 getServicesOrThrow(String key) {
            if (key == null) {
                throw new NullPointerException("map key");
            }
            java.util.Map<String, ServiceInfoV2> map = internalGetServices().getMap();
            if (!map.containsKey(key)) {
                throw new IllegalArgumentException();
            }
            return map.get(key);
        }

        public Builder clearServices() {
            bitField0_ = (bitField0_ & ~0x00000004);
            internalGetMutableServices().getMutableMap().clear();
            return this;
        }

        /**
         * <pre>
         * A map of service information.
         * </pre>
         *
         * <code>map&lt;string, .org.apache.dubbo.metadata.ServiceInfoV2&gt; services = 3;</code>
         */
        public Builder removeServices(String key) {
            if (key == null) {
                throw new NullPointerException("map key");
            }
            internalGetMutableServices().getMutableMap().remove(key);
            return this;
        }

        /**
         * Use alternate mutation accessors instead.
         */
        @Deprecated
        public java.util.Map<String, ServiceInfoV2> getMutableServices() {
            bitField0_ |= 0x00000004;
            return internalGetMutableServices().getMutableMap();
        }

        /**
         * <pre>
         * A map of service information.
         * </pre>
         *
         * <code>map&lt;string, .org.apache.dubbo.metadata.ServiceInfoV2&gt; services = 3;</code>
         */
        public Builder putServices(String key, ServiceInfoV2 value) {
            if (key == null) {
                throw new NullPointerException("map key");
            }
            if (value == null) {
                throw new NullPointerException("map value");
            }
            internalGetMutableServices().getMutableMap().put(key, value);
            bitField0_ |= 0x00000004;
            return this;
        }

        /**
         * <pre>
         * A map of service information.
         * </pre>
         *
         * <code>map&lt;string, .org.apache.dubbo.metadata.ServiceInfoV2&gt; services = 3;</code>
         */
        public Builder putAllServices(java.util.Map<String, ServiceInfoV2> values) {
            internalGetMutableServices().getMutableMap().putAll(values);
            bitField0_ |= 0x00000004;
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

        // @@protoc_insertion_point(builder_scope:org.apache.dubbo.metadata.MetadataInfoV2)
    }

    // @@protoc_insertion_point(class_scope:org.apache.dubbo.metadata.MetadataInfoV2)
    private static final MetadataInfoV2 DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new MetadataInfoV2();
    }

    public static MetadataInfoV2 getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<MetadataInfoV2> PARSER =
            new com.google.protobuf.AbstractParser<MetadataInfoV2>() {
                @Override
                public MetadataInfoV2 parsePartialFrom(
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

    public static com.google.protobuf.Parser<MetadataInfoV2> parser() {
        return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<MetadataInfoV2> getParserForType() {
        return PARSER;
    }

    @Override
    public MetadataInfoV2 getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }
}
