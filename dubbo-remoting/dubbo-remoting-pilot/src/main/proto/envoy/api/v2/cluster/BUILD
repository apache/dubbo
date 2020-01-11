load("@envoy_api//bazel:api_build_system.bzl", "api_go_proto_library", "api_proto_library_internal")

licenses(["notice"])  # Apache 2

api_proto_library_internal(
    name = "circuit_breaker",
    srcs = ["circuit_breaker.proto"],
    visibility = [
        "//envoy/api/v2:__pkg__",
    ],
    deps = [
        "//envoy/api/v2/core:base",
    ],
)

api_go_proto_library(
    name = "circuit_breaker",
    proto = ":circuit_breaker",
    deps = [
        "//envoy/api/v2/core:base_go_proto",
    ],
)

api_proto_library_internal(
    name = "outlier_detection",
    srcs = ["outlier_detection.proto"],
    visibility = [
        "//envoy/api/v2:__pkg__",
    ],
)

api_go_proto_library(
    name = "outlier_detection",
    proto = ":outlier_detection",
)
