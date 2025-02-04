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

public final class MetadataServiceV2OuterClass {
    private MetadataServiceV2OuterClass() {}

    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
        registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
    }

    static final com.google.protobuf.Descriptors.Descriptor
            internal_static_org_apache_dubbo_metadata_MetadataRequest_descriptor;
    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            internal_static_org_apache_dubbo_metadata_MetadataRequest_fieldAccessorTable;
    static final com.google.protobuf.Descriptors.Descriptor
            internal_static_org_apache_dubbo_metadata_MetadataInfoV2_descriptor;
    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            internal_static_org_apache_dubbo_metadata_MetadataInfoV2_fieldAccessorTable;
    static final com.google.protobuf.Descriptors.Descriptor
            internal_static_org_apache_dubbo_metadata_MetadataInfoV2_ServicesEntry_descriptor;
    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            internal_static_org_apache_dubbo_metadata_MetadataInfoV2_ServicesEntry_fieldAccessorTable;
    static final com.google.protobuf.Descriptors.Descriptor
            internal_static_org_apache_dubbo_metadata_ServiceInfoV2_descriptor;
    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            internal_static_org_apache_dubbo_metadata_ServiceInfoV2_fieldAccessorTable;
    static final com.google.protobuf.Descriptors.Descriptor
            internal_static_org_apache_dubbo_metadata_ServiceInfoV2_ParamsEntry_descriptor;
    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            internal_static_org_apache_dubbo_metadata_ServiceInfoV2_ParamsEntry_fieldAccessorTable;
    static final com.google.protobuf.Descriptors.Descriptor
            internal_static_org_apache_dubbo_metadata_OpenAPIRequest_descriptor;
    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            internal_static_org_apache_dubbo_metadata_OpenAPIRequest_fieldAccessorTable;
    static final com.google.protobuf.Descriptors.Descriptor
            internal_static_org_apache_dubbo_metadata_OpenAPIInfo_descriptor;
    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            internal_static_org_apache_dubbo_metadata_OpenAPIInfo_fieldAccessorTable;

    public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
        return descriptor;
    }

    private static com.google.protobuf.Descriptors.FileDescriptor descriptor;

    static {
        String[] descriptorData = {
            "\n\031metadata_service_v2.proto\022\031org.apache."
                    + "dubbo.metadata\"#\n\017MetadataRequest\022\020\n\010rev"
                    + "ision\030\001 \001(\t\"\324\001\n\016MetadataInfoV2\022\013\n\003app\030\001 "
                    + "\001(\t\022\017\n\007version\030\002 \001(\t\022I\n\010services\030\003 \003(\01327"
                    + ".org.apache.dubbo.metadata.MetadataInfoV"
                    + "2.ServicesEntry\032Y\n\rServicesEntry\022\013\n\003key\030"
                    + "\001 \001(\t\0227\n\005value\030\002 \001(\0132(.org.apache.dubbo."
                    + "metadata.ServiceInfoV2:\0028\001\"\340\001\n\rServiceIn"
                    + "foV2\022\014\n\004name\030\001 \001(\t\022\r\n\005group\030\002 \001(\t\022\017\n\007ver"
                    + "sion\030\003 \001(\t\022\020\n\010protocol\030\004 \001(\t\022\014\n\004port\030\005 \001"
                    + "(\005\022\014\n\004path\030\006 \001(\t\022D\n\006params\030\007 \003(\01324.org.a"
                    + "pache.dubbo.metadata.ServiceInfoV2.Param"
                    + "sEntry\032-\n\013ParamsEntry\022\013\n\003key\030\001 \001(\t\022\r\n\005va"
                    + "lue\030\002 \001(\t:\0028\001\"\311\001\n\016OpenAPIRequest\022\r\n\005grou"
                    + "p\030\001 \001(\t\022\017\n\007version\030\002 \001(\t\022\013\n\003tag\030\003 \003(\t\022\017\n"
                    + "\007service\030\004 \003(\t\022\017\n\007openapi\030\005 \001(\t\022=\n\006forma"
                    + "t\030\006 \001(\0162(.org.apache.dubbo.metadata.Open"
                    + "APIFormatH\000\210\001\001\022\023\n\006pretty\030\007 \001(\010H\001\210\001\001B\t\n\007_"
                    + "formatB\t\n\007_pretty\"!\n\013OpenAPIInfo\022\022\n\ndefi"
                    + "nition\030\001 \001(\t*.\n\rOpenAPIFormat\022\010\n\004JSON\020\000\022"
                    + "\010\n\004YAML\020\001\022\t\n\005PROTO\020\0022\342\001\n\021MetadataService"
                    + "V2\022h\n\017GetMetadataInfo\022*.org.apache.dubbo"
                    + ".metadata.MetadataRequest\032).org.apache.d"
                    + "ubbo.metadata.MetadataInfoV2\022c\n\016GetOpenA"
                    + "PIInfo\022).org.apache.dubbo.metadata.OpenA"
                    + "PIRequest\032&.org.apache.dubbo.metadata.Op"
                    + "enAPIInfoBZ\n\031org.apache.dubbo.metadataP\001"
                    + "Z;dubbo.apache.org/dubbo-go/v3/metadata/"
                    + "triple_api;triple_apib\006proto3"
        };
        descriptor = com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(
                descriptorData, new com.google.protobuf.Descriptors.FileDescriptor[] {});
        internal_static_org_apache_dubbo_metadata_MetadataRequest_descriptor =
                getDescriptor().getMessageTypes().get(0);
        internal_static_org_apache_dubbo_metadata_MetadataRequest_fieldAccessorTable =
                new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                        internal_static_org_apache_dubbo_metadata_MetadataRequest_descriptor, new String[] {
                            "Revision",
                        });
        internal_static_org_apache_dubbo_metadata_MetadataInfoV2_descriptor =
                getDescriptor().getMessageTypes().get(1);
        internal_static_org_apache_dubbo_metadata_MetadataInfoV2_fieldAccessorTable =
                new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                        internal_static_org_apache_dubbo_metadata_MetadataInfoV2_descriptor, new String[] {
                            "App", "Version", "Services",
                        });
        internal_static_org_apache_dubbo_metadata_MetadataInfoV2_ServicesEntry_descriptor =
                internal_static_org_apache_dubbo_metadata_MetadataInfoV2_descriptor
                        .getNestedTypes()
                        .get(0);
        internal_static_org_apache_dubbo_metadata_MetadataInfoV2_ServicesEntry_fieldAccessorTable =
                new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                        internal_static_org_apache_dubbo_metadata_MetadataInfoV2_ServicesEntry_descriptor,
                        new String[] {
                            "Key", "Value",
                        });
        internal_static_org_apache_dubbo_metadata_ServiceInfoV2_descriptor =
                getDescriptor().getMessageTypes().get(2);
        internal_static_org_apache_dubbo_metadata_ServiceInfoV2_fieldAccessorTable =
                new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                        internal_static_org_apache_dubbo_metadata_ServiceInfoV2_descriptor, new String[] {
                            "Name", "Group", "Version", "Protocol", "Port", "Path", "Params",
                        });
        internal_static_org_apache_dubbo_metadata_ServiceInfoV2_ParamsEntry_descriptor =
                internal_static_org_apache_dubbo_metadata_ServiceInfoV2_descriptor
                        .getNestedTypes()
                        .get(0);
        internal_static_org_apache_dubbo_metadata_ServiceInfoV2_ParamsEntry_fieldAccessorTable =
                new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                        internal_static_org_apache_dubbo_metadata_ServiceInfoV2_ParamsEntry_descriptor, new String[] {
                            "Key", "Value",
                        });
        internal_static_org_apache_dubbo_metadata_OpenAPIRequest_descriptor =
                getDescriptor().getMessageTypes().get(3);
        internal_static_org_apache_dubbo_metadata_OpenAPIRequest_fieldAccessorTable =
                new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                        internal_static_org_apache_dubbo_metadata_OpenAPIRequest_descriptor, new String[] {
                            "Group", "Version", "Tag", "Service", "Openapi", "Format", "Pretty", "Format", "Pretty",
                        });
        internal_static_org_apache_dubbo_metadata_OpenAPIInfo_descriptor =
                getDescriptor().getMessageTypes().get(4);
        internal_static_org_apache_dubbo_metadata_OpenAPIInfo_fieldAccessorTable =
                new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                        internal_static_org_apache_dubbo_metadata_OpenAPIInfo_descriptor, new String[] {
                            "Definition",
                        });
    }

    // @@protoc_insertion_point(outer_class_scope)
}
