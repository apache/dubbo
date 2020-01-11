load("@envoy_api//bazel:api_build_system.bzl", "api_go_grpc_library", "api_proto_library_internal")

licenses(["notice"])  # Apache 2

api_proto_library_internal(
    name = "trace_service",
    srcs = ["trace_service.proto"],
    has_services = 1,
    require_py = 0,
    deps = [
        "//envoy/api/v2/core:base",
        "@io_opencensus_trace//:trace_model",
    ],
)

api_go_grpc_library(
    name = "trace_service",
    proto = ":trace_service",
    deps = [
        "//envoy/api/v2/core:base_go_proto",
        "@io_opencensus_trace//:trace_model_go_proto",
    ],
)
