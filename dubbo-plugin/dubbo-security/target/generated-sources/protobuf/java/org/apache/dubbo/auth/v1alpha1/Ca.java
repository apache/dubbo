// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: ca.proto

package org.apache.dubbo.auth.v1alpha1;

public final class Ca {
  private Ca() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_org_apache_dubbo_auth_v1alpha1_DubboCertificateRequest_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_org_apache_dubbo_auth_v1alpha1_DubboCertificateRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_org_apache_dubbo_auth_v1alpha1_DubboCertificateResponse_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_org_apache_dubbo_auth_v1alpha1_DubboCertificateResponse_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\010ca.proto\022\036org.apache.dubbo.auth.v1alph" +
      "a1\032\034google/protobuf/struct.proto\"_\n\027Dubb" +
      "oCertificateRequest\022\013\n\003csr\030\001 \001(\t\022\014\n\004type" +
      "\030\002 \001(\t\022)\n\010metadata\030\003 \001(\0132\027.google.protob" +
      "uf.Struct\"x\n\030DubboCertificateResponse\022\017\n" +
      "\007success\030\001 \001(\010\022\020\n\010cert_pem\030\002 \001(\t\022\023\n\013trus" +
      "t_certs\030\003 \003(\t\022\023\n\013expire_time\030\004 \001(\003\022\017\n\007me" +
      "ssage\030\005 \001(\t2\244\001\n\027DubboCertificateService\022" +
      "\210\001\n\021CreateCertificate\0227.org.apache.dubbo" +
      ".auth.v1alpha1.DubboCertificateRequest\0328" +
      ".org.apache.dubbo.auth.v1alpha1.DubboCer" +
      "tificateResponse\"\000B-P\001Z)github.com/apach" +
      "e/dubbo-admin/ca/v1alpha1b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          com.google.protobuf.StructProto.getDescriptor(),
        });
    internal_static_org_apache_dubbo_auth_v1alpha1_DubboCertificateRequest_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_org_apache_dubbo_auth_v1alpha1_DubboCertificateRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_org_apache_dubbo_auth_v1alpha1_DubboCertificateRequest_descriptor,
        new java.lang.String[] { "Csr", "ApplicationType", "Metadata", });
    internal_static_org_apache_dubbo_auth_v1alpha1_DubboCertificateResponse_descriptor =
      getDescriptor().getMessageTypes().get(1);
    internal_static_org_apache_dubbo_auth_v1alpha1_DubboCertificateResponse_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_org_apache_dubbo_auth_v1alpha1_DubboCertificateResponse_descriptor,
        new java.lang.String[] { "Success", "CertPem", "TrustCerts", "ExpireTime", "Message", });
    com.google.protobuf.StructProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
