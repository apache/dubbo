load("@envoy_api//bazel:api_build_system.bzl", "api_go_proto_library", "api_proto_library_internal")

licenses(["notice"])  # Apache 2

api_proto_library_internal(
    name = "health_check",
    srcs = ["health_check.proto"],
    deps = [
        "//envoy/api/v2/route",
        "//envoy/type:percent",
    ],
)

api_go_proto_library(
    name = "health_check",
    proto = ":health_check",
    deps = [
        "//envoy/api/v2/route:route_go_proto",
        "//envoy/type:percent_go_proto",
    ],
)
