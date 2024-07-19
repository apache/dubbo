package org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData;

public enum ConnectionSourceType {
    // Any connection source matches.
    ANY,

    // Match a connection originating from the same host.
    SAME_IP_OR_LOOPBACK,

    // Match a connection originating from a different host.
    EXTERNAL
}
