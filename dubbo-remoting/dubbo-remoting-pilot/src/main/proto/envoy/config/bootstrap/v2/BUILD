load("@envoy_api//bazel:api_build_system.bzl", "api_go_proto_library", "api_proto_library_internal")

licenses(["notice"])  # Apache 2

api_proto_library_internal(
    name = "bootstrap",
    srcs = ["bootstrap.proto"],
    visibility = ["//visibility:public"],
    deps = [
        "//envoy/api/v2:cds",
        "//envoy/api/v2:lds",
        "//envoy/api/v2/auth:cert",
        "//envoy/api/v2/core:address",
        "//envoy/api/v2/core:base",
        "//envoy/api/v2/core:config_source",
        "//envoy/config/metrics/v2:metrics_service",
        "//envoy/config/metrics/v2:stats",
        "//envoy/config/overload/v2alpha:overload",
        "//envoy/config/ratelimit/v2:rls",
        "//envoy/config/trace/v2:trace",
    ],
)

api_go_proto_library(
    name = "bootstrap",
    proto = ":bootstrap",
    deps = [
        "//envoy/api/v2:cds_go_grpc",
        "//envoy/api/v2:lds_go_grpc",
        "//envoy/api/v2/auth:cert_go_proto",
        "//envoy/api/v2/core:address_go_proto",
        "//envoy/api/v2/core:base_go_proto",
        "//envoy/api/v2/core:config_source_go_proto",
        "//envoy/config/metrics/v2:metrics_service_go_proto",
        "//envoy/config/metrics/v2:stats_go_proto",
        "//envoy/config/overload/v2alpha:overload_go_proto",
        "//envoy/config/ratelimit/v2:rls_go_grpc",
        "//envoy/config/trace/v2:trace_go_proto",
    ],
)
