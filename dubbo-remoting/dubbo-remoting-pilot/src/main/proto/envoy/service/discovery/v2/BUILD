load("@envoy_api//bazel:api_build_system.bzl", "api_go_grpc_library", "api_go_proto_library", "api_proto_library_internal")

licenses(["notice"])  # Apache 2

api_proto_library_internal(
    name = "ads",
    srcs = ["ads.proto"],
    has_services = 1,
    deps = [
        "//envoy/api/v2:discovery",
    ],
)

api_go_grpc_library(
    name = "ads",
    proto = ":ads",
    deps = [
        "//envoy/api/v2:discovery_go_proto",
    ],
)

api_proto_library_internal(
    name = "hds",
    srcs = ["hds.proto"],
    has_services = 1,
    deps = [
        "//envoy/api/v2/core:base",
        "//envoy/api/v2/core:health_check",
        "//envoy/api/v2/endpoint",
    ],
)

api_go_grpc_library(
    name = "hds",
    proto = ":hds",
    deps = [
        "//envoy/api/v2/core:base_go_proto",
        "//envoy/api/v2/core:health_check_go_proto",
        "//envoy/api/v2/endpoint:endpoint_go_proto",
    ],
)

api_proto_library_internal(
    name = "sds",
    srcs = ["sds.proto"],
    has_services = 1,
    deps = [
        "//envoy/api/v2:discovery",
    ],
)

api_go_grpc_library(
    name = "sds",
    proto = ":sds",
    deps = [
        "//envoy/api/v2:discovery_go_proto",
    ],
)
