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
package org.apache.dubbo.xds.fixture;

@com.google.protobuf.Generated
public final class WireEchoOuterClass extends com.google.protobuf.GeneratedFile {
    private WireEchoOuterClass() {}

    static {
        com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
                com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
                /* major= */ 4,
                /* minor= */ 33,
                /* patch= */ 4,
                /* suffix= */ "",
                "WireEchoOuterClass");
    }

    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
        registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
    }

    static final com.google.protobuf.Descriptors.Descriptor internal_static_xds_testing_WireMessage_descriptor;
    static final com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internal_static_xds_testing_WireMessage_fieldAccessorTable;

    public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
        return descriptor;
    }

    private static com.google.protobuf.Descriptors.FileDescriptor descriptor;

    static {
        java.lang.String[] descriptorData = {
            "\n\017wire_echo.proto\022\013xds.testing\"\034\n\013WireMe"
                    + "ssage\022\r\n\005value\030\001 \001(\t2\213\001\n\010WireEcho\022>\n\010Say"
                    + "Hello\022\030.xds.testing.WireMessage\032\030.xds.te"
                    + "sting.WireMessage\022?\n\007Collect\022\030.xds.testi"
                    + "ng.WireMessage\032\030.xds.testing.WireMessage"
                    + "(\001B \n\034org.apache.dubbo.xds.fixtureP\001b\006pr"
                    + "oto3"
        };
        descriptor = com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(
                descriptorData, new com.google.protobuf.Descriptors.FileDescriptor[] {});
        internal_static_xds_testing_WireMessage_descriptor = getDescriptor().getMessageType(0);
        internal_static_xds_testing_WireMessage_fieldAccessorTable =
                new com.google.protobuf.GeneratedMessage.FieldAccessorTable(
                        internal_static_xds_testing_WireMessage_descriptor, new java.lang.String[] {
                            "Value",
                        });
        descriptor.resolveAllFeaturesImmutable();
    }

    // @@protoc_insertion_point(outer_class_scope)
}
