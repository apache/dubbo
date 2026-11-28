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

@com.google.protobuf.Generated
public final class NativeStreamsOuterClass extends com.google.protobuf.GeneratedFile {
    private NativeStreamsOuterClass() {}

    static {
        com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
                com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
                /* major= */ 4,
                /* minor= */ 33,
                /* patch= */ 4,
                /* suffix= */ "",
                "NativeStreamsOuterClass");
    }

    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
        registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
    }

    static final com.google.protobuf.Descriptors.Descriptor
            internal_static_xds_native_testing_NativeStreamMessage_descriptor;
    static final com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internal_static_xds_native_testing_NativeStreamMessage_fieldAccessorTable;

    public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
        return descriptor;
    }

    private static com.google.protobuf.Descriptors.FileDescriptor descriptor;

    static {
        java.lang.String[] descriptorData = {
            "\n\024native_streams.proto\022\022xds.native.testi"
                    + "ng\"$\n\023NativeStreamMessage\022\r\n\005value\030\001 \001(\t"
                    + "2\314\001\n\rNativeStreams\022]\n\007Collect\022\'.xds.nati"
                    + "ve.testing.NativeStreamMessage\032\'.xds.nat"
                    + "ive.testing.NativeStreamMessage(\001\022\\\n\004Cha"
                    + "t\022\'.xds.native.testing.NativeStreamMessa"
                    + "ge\032\'.xds.native.testing.NativeStreamMess"
                    + "age(\0010\001B$\n org.apache.dubbo.xds.test.str"
                    + "eamP\001b\006proto3"
        };
        descriptor = com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(
                descriptorData, new com.google.protobuf.Descriptors.FileDescriptor[] {});
        internal_static_xds_native_testing_NativeStreamMessage_descriptor =
                getDescriptor().getMessageType(0);
        internal_static_xds_native_testing_NativeStreamMessage_fieldAccessorTable =
                new com.google.protobuf.GeneratedMessage.FieldAccessorTable(
                        internal_static_xds_native_testing_NativeStreamMessage_descriptor, new java.lang.String[] {
                            "Value",
                        });
        descriptor.resolveAllFeaturesImmutable();
    }

    // @@protoc_insertion_point(outer_class_scope)
}
