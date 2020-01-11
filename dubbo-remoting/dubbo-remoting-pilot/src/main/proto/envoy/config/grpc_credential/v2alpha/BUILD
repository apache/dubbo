licenses(["notice"])  # Apache 2

load("@envoy_api//bazel:api_build_system.bzl", "api_go_proto_library", "api_proto_library_internal")

api_proto_library_internal(
    name = "file_based_metadata",
    srcs = ["file_based_metadata.proto"],
    deps = ["//envoy/api/v2/core:base"],
)

api_go_proto_library(
    name = "file_based_metadata",
    proto = ":file_based_metadata",
    deps = [
        "//envoy/api/v2/core:base_go_proto",
    ],
)
