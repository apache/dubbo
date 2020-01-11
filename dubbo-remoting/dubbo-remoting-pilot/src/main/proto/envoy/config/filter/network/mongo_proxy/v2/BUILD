load("@envoy_api//bazel:api_build_system.bzl", "api_proto_library_internal")

licenses(["notice"])  # Apache 2

api_proto_library_internal(
    name = "mongo_proxy",
    srcs = ["mongo_proxy.proto"],
    deps = ["//envoy/config/filter/fault/v2:fault"],
)
