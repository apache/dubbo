load("@envoy_api//bazel:api_build_system.bzl", "api_go_proto_library", "api_proto_library_internal")

licenses(["notice"])  # Apache 2

api_proto_library_internal(
    name = "route",
    srcs = ["route.proto"],
    visibility = ["//envoy/api/v2:friends"],
    deps = [
        "//envoy/api/v2/core:base",
        "//envoy/type:percent",
        "//envoy/type:range",
    ],
)

api_go_proto_library(
    name = "route",
    proto = ":route",
    deps = [
        "//envoy/api/v2/core:base_go_proto",
        "//envoy/type:percent_go_proto",
        "//envoy/type:range_go_proto",
    ],
)
