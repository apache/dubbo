load("@envoy_api//bazel:api_build_system.bzl", "api_go_grpc_library", "api_go_proto_library", "api_proto_library_internal")

licenses(["notice"])  # Apache 2

api_proto_library_internal(
    name = "trace",
    srcs = ["trace.proto"],
    visibility = [
        "//envoy/config/bootstrap/v2:__pkg__",
    ],
    deps = [
        "//envoy/api/v2/core:grpc_service",
    ],
)

api_go_proto_library(
    name = "trace",
    proto = ":trace",
    deps = [
        "//envoy/api/v2/core:grpc_service_go_proto",
    ],
)
