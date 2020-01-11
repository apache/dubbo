licenses(["notice"])  # Apache 2

load("@envoy_api//bazel:api_build_system.bzl", "api_go_proto_library", "api_proto_library_internal")

api_proto_library_internal(
    name = "rbac",
    srcs = ["rbac.proto"],
    visibility = ["//visibility:public"],
    deps = [
        "//envoy/api/v2/core:address",
        "//envoy/api/v2/route",
        "//envoy/type/matcher:metadata",
        "//envoy/type/matcher:string",
    ],
)

api_go_proto_library(
    name = "rbac",
    proto = ":rbac",
    deps = [
        "//envoy/api/v2/core:address_go_proto",
        "//envoy/api/v2/route:route_go_proto",
        "//envoy/type/matcher:metadata_go_proto",
        "//envoy/type/matcher:string_go_proto",
    ],
)
