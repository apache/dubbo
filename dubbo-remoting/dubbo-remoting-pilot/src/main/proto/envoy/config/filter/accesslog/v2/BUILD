load("@envoy_api//bazel:api_build_system.bzl", "api_go_proto_library", "api_proto_library_internal")

licenses(["notice"])  # Apache 2

api_proto_library_internal(
    name = "accesslog",
    srcs = ["accesslog.proto"],
    visibility = [
        "//envoy/config/filter/http/router/v2:__pkg__",
        "//envoy/config/filter/network/http_connection_manager/v2:__pkg__",
        "//envoy/config/filter/network/tcp_proxy/v2:__pkg__",
    ],
    deps = [
        "//envoy/api/v2/core:base",
        "//envoy/api/v2/route",
        "//envoy/type:percent",
    ],
)

api_go_proto_library(
    name = "accesslog",
    proto = ":accesslog",
    deps = [
        "//envoy/api/v2/core:base_go_proto",
        "//envoy/api/v2/route:route_go_proto",
        "//envoy/type:percent_go_proto",
    ],
)
