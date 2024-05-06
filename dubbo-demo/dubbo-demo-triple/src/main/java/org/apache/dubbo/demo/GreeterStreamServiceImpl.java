package org.apache.dubbo.demo;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.demo.hello.HelloReply;
import org.apache.dubbo.demo.hello.HelloRequest;

public class GreeterStreamServiceImpl implements GreeterStreamService {
    @Override
    public StreamObserver<HelloRequest> biStream(StreamObserver<HelloReply> responseObserver) {
        System.out.println("GreeterStreamServiceImpl.biStream");
        return new StreamObserver<HelloRequest>() {
            @Override
            public void onNext(HelloRequest data) {
                HelloReply reply = HelloReply.newBuilder().setMessage("reply from biStream").build();
                responseObserver.onNext(reply);
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {
                System.out.println("biStream requestObserver.onCompleted");
            }
        };
    }

    @Override
    public void serverStream(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        System.out.println("received request");
        for (int i=0; i<10; ++i) {
            HelloReply reply = HelloReply.newBuilder().setMessage("reply from serverStream."+i).build();
            responseObserver.onNext(reply);
        }
        responseObserver.onCompleted();
    }
}
