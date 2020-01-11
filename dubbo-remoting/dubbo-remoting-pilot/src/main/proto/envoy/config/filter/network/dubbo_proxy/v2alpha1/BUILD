load("@envoy_api//bazel:api_build_system.bzl", "api_proto_library")

licenses(["notice"])  # Apache 2

api_proto_library(
    name = "dubbo_proxy",
    srcs = [
        "dubbo_proxy.proto",
        "route.proto",
    ],
    deps = [
        "//envoy/api/v2/core:base",
        "//envoy/api/v2/route",
        "//envoy/type:range",
        "//envoy/type/matcher:string",
    ],
)
