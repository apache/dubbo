load("@envoy_api//bazel:api_build_system.bzl", "api_go_proto_library", "api_proto_library_internal")

licenses(["notice"])  # Apache 2

api_proto_library_internal(
    name = "http_status",
    srcs = ["http_status.proto"],
    visibility = ["//visibility:public"],
)

api_go_proto_library(
    name = "http_status",
    proto = ":http_status",
)

api_proto_library_internal(
    name = "percent",
    srcs = ["percent.proto"],
    visibility = ["//visibility:public"],
)

api_go_proto_library(
    name = "percent",
    proto = ":percent",
)

api_proto_library_internal(
    name = "range",
    srcs = ["range.proto"],
    visibility = ["//visibility:public"],
)

api_go_proto_library(
    name = "range",
    proto = ":range",
)
