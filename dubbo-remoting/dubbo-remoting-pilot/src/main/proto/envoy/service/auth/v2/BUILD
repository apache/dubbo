load("@envoy_api//bazel:api_build_system.bzl", "api_go_proto_library", "api_proto_library_internal")

licenses(["notice"])  # Apache 2

api_proto_library_internal(
    name = "attribute_context",
    srcs = [
        "attribute_context.proto",
    ],
    deps = [
        "//envoy/api/v2/core:address",
    ],
)

api_proto_library_internal(
    name = "external_auth",
    srcs = [
        "external_auth.proto",
    ],
    has_services = 1,
    visibility = ["//visibility:public"],
    deps = [
        ":attribute_context",
        "//envoy/api/v2/core:base",
        "//envoy/type:http_status",
    ],
)
