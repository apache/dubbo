package org.apache.dubbo.rpc.protocol.grpc;

import io.grpc.stub.StreamObserver;

public class HelloServiceImpl extends GreeterGrpc.GreeterImplBase {
    @Override
    public void helloWorld(HelloService.HelloRequest request, StreamObserver<HelloService.HelloResponse> responseObserver) {
        HelloService.HelloResponse response = HelloService.HelloResponse.newBuilder().setResponseData("zhouyang23").build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
