load("@envoy_api//bazel:api_build_system.bzl", "api_go_proto_library", "api_proto_library_internal")

licenses(["notice"])  # Apache 2

api_proto_library_internal(
    name = "endpoint",
    srcs = ["endpoint.proto"],
    visibility = ["//envoy/api/v2:friends"],
    deps = [
        "//envoy/api/v2/auth:cert",
        "//envoy/api/v2/core:address",
        "//envoy/api/v2/core:base",
        "//envoy/api/v2/core:config_source",
        "//envoy/api/v2/core:health_check",
        "//envoy/api/v2/core:protocol",
    ],
)

api_go_proto_library(
    name = "endpoint",
    proto = ":endpoint",
    deps = [
        "//envoy/api/v2/auth:cert_go_proto",
        "//envoy/api/v2/core:address_go_proto",
        "//envoy/api/v2/core:base_go_proto",
        "//envoy/api/v2/core:config_source_go_proto",
        "//envoy/api/v2/core:health_check_go_proto",
        "//envoy/api/v2/core:protocol_go_proto",
    ],
)

api_proto_library_internal(
    name = "load_report",
    srcs = ["load_report.proto"],
    visibility = ["//envoy/api/v2:friends"],
    deps = [
        "//envoy/api/v2/core:address",
        "//envoy/api/v2/core:base",
    ],
)

api_go_proto_library(
    name = "load_report",
    proto = ":load_report",
    deps = [
        "//envoy/api/v2/core:address_go_proto",
        "//envoy/api/v2/core:base_go_proto",
    ],
)
