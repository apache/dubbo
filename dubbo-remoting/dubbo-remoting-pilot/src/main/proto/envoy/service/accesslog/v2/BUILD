load("@envoy_api//bazel:api_build_system.bzl", "api_go_grpc_library", "api_proto_library_internal")

licenses(["notice"])  # Apache 2

api_proto_library_internal(
    name = "als",
    srcs = ["als.proto"],
    has_services = 1,
    deps = [
        "//envoy/api/v2/core:base",
        "//envoy/api/v2/core:grpc_service",
        "//envoy/data/accesslog/v2:accesslog",
    ],
)

api_go_grpc_library(
    name = "als",
    proto = ":als",
    deps = [
        "//envoy/api/v2/core:base_go_proto",
        "//envoy/data/accesslog/v2:accesslog_go_proto",
    ],
)
