load("@envoy_api//bazel:api_build_system.bzl", "api_go_proto_library", "api_proto_library_internal")

licenses(["notice"])  # Apache 2

api_proto_library_internal(
    name = "metadata",
    srcs = ["metadata.proto"],
    visibility = ["//visibility:public"],
    deps = [
        ":value",
    ],
)

api_go_proto_library(
    name = "metadata",
    proto = ":metadata",
    deps = [
        ":value_go_proto",
    ],
)

api_proto_library_internal(
    name = "number",
    srcs = ["number.proto"],
    visibility = ["//visibility:public"],
    deps = [
        "//envoy/type:range",
    ],
)

api_go_proto_library(
    name = "number",
    proto = ":number",
    deps = [
        "//envoy/type:range_go_proto",
    ],
)

api_proto_library_internal(
    name = "string",
    srcs = ["string.proto"],
    visibility = ["//visibility:public"],
)

api_go_proto_library(
    name = "string",
    proto = ":string",
)

api_proto_library_internal(
    name = "value",
    srcs = ["value.proto"],
    visibility = ["//visibility:public"],
    deps = [
        ":number",
        ":string",
    ],
)

api_go_proto_library(
    name = "value",
    proto = ":value",
    deps = [
        ":number_go_proto",
        ":string_go_proto",
    ],
)
