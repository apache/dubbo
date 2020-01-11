load("@envoy_api//bazel:api_build_system.bzl", "api_go_proto_library", "api_proto_library_internal")

licenses(["notice"])  # Apache 2

api_proto_library_internal(
    name = "ratelimit",
    srcs = ["ratelimit.proto"],
    visibility = ["//envoy/api/v2:friends"],
)

api_go_proto_library(
    name = "ratelimit",
    proto = ":ratelimit",
)
