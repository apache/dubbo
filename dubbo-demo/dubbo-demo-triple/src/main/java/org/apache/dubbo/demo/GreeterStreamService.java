package org.apache.dubbo.demo;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.demo.hello.HelloReply;
import org.apache.dubbo.demo.hello.HelloRequest;

public interface GreeterStreamService {
    StreamObserver<HelloRequest> biStream(StreamObserver<HelloReply> responseObserver);
    void serverStream(HelloRequest request, StreamObserver<HelloReply> responseObserver);
}
