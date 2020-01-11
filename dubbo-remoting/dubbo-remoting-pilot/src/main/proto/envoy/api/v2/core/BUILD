load("@envoy_api//bazel:api_build_system.bzl", "api_go_grpc_library", "api_go_proto_library", "api_proto_library_internal")

licenses(["notice"])  # Apache 2

package_group(
    name = "friends",
    includes = [
        "//envoy/api/v2:friends",
    ],
    packages = [
        "//envoy/api/v2/auth",
        "//envoy/api/v2/cluster",
        "//envoy/api/v2/endpoint",
        "//envoy/api/v2/listener",
        "//envoy/api/v2/route",
    ],
)

api_proto_library_internal(
    name = "address",
    srcs = ["address.proto"],
    visibility = [
        ":friends",
    ],
    deps = [":base"],
)

api_go_proto_library(
    name = "address",
    proto = ":address",
    deps = [":base_go_proto"],
)

api_proto_library_internal(
    name = "base",
    srcs = ["base.proto"],
    visibility = [
        ":friends",
    ],
    deps = [
        "//envoy/type:percent",
    ],
)

api_go_proto_library(
    name = "base",
    proto = ":base",
    deps = ["//envoy/type:percent_go_proto"],
)

api_proto_library_internal(
    name = "health_check",
    srcs = ["health_check.proto"],
    visibility = [
        ":friends",
    ],
    deps = [
        ":base",
        "//envoy/type:range",
    ],
)

api_go_proto_library(
    name = "health_check",
    proto = ":health_check",
    deps = [
        ":base_go_proto",
        "//envoy/type:range_go_proto",
    ],
)

api_proto_library_internal(
    name = "config_source",
    srcs = ["config_source.proto"],
    visibility = [
        ":friends",
    ],
    deps = [
        ":base",
        ":grpc_service",
    ],
)

api_go_proto_library(
    name = "config_source",
    proto = ":config_source",
    deps = [
        ":base_go_proto",
        ":grpc_service_go_proto",
    ],
)

api_go_proto_library(
    name = "http_uri",
    proto = ":http_uri",
)

api_proto_library_internal(
    name = "http_uri",
    srcs = ["http_uri.proto"],
    visibility = [
        ":friends",
    ],
)

api_proto_library_internal(
    name = "grpc_service",
    srcs = ["grpc_service.proto"],
    visibility = [
        ":friends",
    ],
    deps = [":base"],
)

api_go_proto_library(
    name = "grpc_service",
    proto = ":grpc_service",
    deps = [":base_go_proto"],
)

api_proto_library_internal(
    name = "protocol",
    srcs = ["protocol.proto"],
    visibility = [
        ":friends",
    ],
)

api_go_proto_library(
    name = "protocol",
    proto = ":protocol",
)
