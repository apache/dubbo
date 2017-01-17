package com.alibaba.dubbo.demo.provider.grpc;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.stub.StreamObserver;

/**
 * Created by wuyu on 2017/1/17.
 */
public class HelloWorldServiceImpl extends GreeterGrpc.AbstractGreeter {
    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        String name = request.getName();
        System.err.println(name);
        responseObserver.onNext(HelloReply.newBuilder().setMessage("Hello " + name).build());
    }
}
