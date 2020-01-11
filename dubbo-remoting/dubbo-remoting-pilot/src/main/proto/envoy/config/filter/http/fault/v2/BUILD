load("@envoy_api//bazel:api_build_system.bzl", "api_proto_library_internal")

licenses(["notice"])  # Apache 2

api_proto_library_internal(
    name = "fault",
    srcs = ["fault.proto"],
    deps = [
        "//envoy/api/v2/route",
        "//envoy/config/filter/fault/v2:fault",
        "//envoy/type:percent",
    ],
)
