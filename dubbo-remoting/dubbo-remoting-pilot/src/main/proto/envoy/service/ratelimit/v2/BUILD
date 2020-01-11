load("@envoy_api//bazel:api_build_system.bzl", "api_go_grpc_library", "api_proto_library_internal")

licenses(["notice"])  # Apache 2

api_proto_library_internal(
    name = "rls",
    srcs = ["rls.proto"],
    has_services = 1,
    deps = [
        "//envoy/api/v2/core:base",
        "//envoy/api/v2/core:grpc_service",
        "//envoy/api/v2/ratelimit",
    ],
)

api_go_grpc_library(
    name = "rls",
    proto = ":rls",
    deps = [
        "//envoy/api/v2/core:base_go_proto",
        "//envoy/api/v2/core:grpc_service_go_proto",
        "//envoy/api/v2/ratelimit:ratelimit_go_proto",
    ],
)
