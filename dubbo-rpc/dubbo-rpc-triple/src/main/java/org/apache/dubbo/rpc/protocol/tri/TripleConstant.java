package org.apache.dubbo.rpc.protocol.tri;

public interface TripleConstant {
    String STATUS_KEY = "grpc-status";
    String MESSAGE_KEY = "grpc-message";
    String CONTENT_TYPE_KEY = "content-type";
    String CONTENT_PROTO = "application/grpc+proto";
    String APPLICATION_GRPC = "application/grpc";
    String TRICE_ID_KEY = "tri-trace-traceid";
    String RPC_ID_KEY = "tri-trace-rpcid";
    String CONSUMER_APP_NAME_KEY = "tri-consumer-appname";
    String UNIT_INFO_KEY = "tri-unit-info";
    String SERVICE_VERSION = "tri-service-version";
    String SERVICE_GROUP = "tri-service-group";

}
