load("@envoy_api//bazel:api_build_system.bzl", "api_go_proto_library", "api_proto_library_internal")

licenses(["notice"])  # Apache 2

package_group(
    name = "friends",
    includes = [
        "//envoy/api/v2:friends",
    ],
    packages = [
        "//envoy/api/v2/cluster",
        "//envoy/api/v2/endpoint",
        "//envoy/api/v2/listener",
        "//envoy/api/v2/route",
    ],
)

api_proto_library_internal(
    name = "cert",
    srcs = ["cert.proto"],
    visibility = [":friends"],
    deps = [
        "//envoy/api/v2/core:base",
        "//envoy/api/v2/core:config_source",
    ],
)

api_go_proto_library(
    name = "cert",
    proto = ":cert",
    deps = [
        "//envoy/api/v2/core:base_go_proto",
        "//envoy/api/v2/core:config_source_go_proto",
    ],
)
