load("@envoy_api//bazel:api_build_system.bzl", "api_proto_library_internal")

licenses(["notice"])  # Apache 2

api_proto_library_internal(
    name = "router",
    srcs = ["router.proto"],
    deps = ["//envoy/config/filter/accesslog/v2:accesslog"],
)
