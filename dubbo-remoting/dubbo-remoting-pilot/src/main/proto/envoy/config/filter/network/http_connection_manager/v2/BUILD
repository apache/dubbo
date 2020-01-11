load("@envoy_api//bazel:api_build_system.bzl", "api_go_proto_library", "api_proto_library_internal")

licenses(["notice"])  # Apache 2

api_proto_library_internal(
    name = "http_connection_manager",
    srcs = ["http_connection_manager.proto"],
    deps = [
        "//envoy/api/v2:rds",
        "//envoy/api/v2/core:base",
        "//envoy/api/v2/core:config_source",
        "//envoy/api/v2/core:protocol",
        "//envoy/config/filter/accesslog/v2:accesslog",
        "//envoy/type:percent",
    ],
)

api_go_proto_library(
    name = "http_connection_manager",
    proto = ":http_connection_manager",
    deps = [
        "//envoy/api/v2:rds_go_grpc",
        "//envoy/api/v2/core:base_go_proto",
        "//envoy/api/v2/core:config_source_go_proto",
        "//envoy/api/v2/core:protocol_go_proto",
        "//envoy/config/filter/accesslog/v2:accesslog_go_proto",
        "//envoy/type:percent_go_proto",
    ],
)
