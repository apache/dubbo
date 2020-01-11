load("@envoy_api//bazel:api_build_system.bzl", "api_proto_library")

licenses(["notice"])  # Apache 2

api_proto_library(
    name = "alts",
    srcs = ["alts.proto"],
    deps = [
        "//envoy/api/v2/core:base",
    ],
)
