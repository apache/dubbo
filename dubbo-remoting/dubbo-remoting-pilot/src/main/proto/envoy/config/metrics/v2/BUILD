load("@envoy_api//bazel:api_build_system.bzl", "api_go_grpc_library", "api_go_proto_library", "api_proto_library_internal")

licenses(["notice"])  # Apache 2

api_proto_library_internal(
    name = "metrics_service",
    srcs = ["metrics_service.proto"],
    visibility = [
        "//envoy/config/bootstrap/v2:__pkg__",
    ],
    deps = [
        "//envoy/api/v2/core:grpc_service",
    ],
)

api_go_proto_library(
    name = "metrics_service",
    proto = ":metrics_service",
    deps = [
        "//envoy/api/v2/core:grpc_service_go_proto",
    ],
)

api_proto_library_internal(
    name = "stats",
    srcs = ["stats.proto"],
    visibility = [
        "//envoy/config/bootstrap/v2:__pkg__",
    ],
    deps = [
        "//envoy/api/v2/core:address",
        "//envoy/type/matcher:string",
    ],
)

api_go_proto_library(
    name = "stats",
    proto = ":stats",
    deps = [
        "//envoy/api/v2/core:address_go_proto",
        "//envoy/type/matcher:string_go_proto",
    ],
)
