package org.apache.dubbo.sample.tri;

import org.apache.dubbo.common.stream.StreamObserver;

public class IGreeterImpl extends DubboIGreeterTriple.IGreeterImplBase {
    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        responseObserver.onNext(HelloReply.newBuilder()
            .setMessage("Hello!" + request.getName())
            .build());
        responseObserver.onCompleted();
    }

    @Override
    public void sayHelloServerStream(HelloRequest request, StreamObserver<HelloReply> replyStream) {
        for (int i = 0; i < 10; i++) {
            replyStream.onNext(HelloReply.newBuilder()
                .setMessage("Hello!" + request.getName() + ":" + i)
                .build());
        }
        replyStream.onCompleted();
    }

    @Override
    public StreamObserver<HelloRequest> sayHelloClientStream(StreamObserver<HelloReply> replyStream) {
        return new StreamObserver<HelloRequest>() {
            @Override
            public void onNext(HelloRequest data) {
            }

            @Override
            public void onError(Throwable throwable) {
                replyStream.onError(throwable);
            }

            @Override
            public void onCompleted() {
                replyStream.onNext(HelloReply.newBuilder()
                    .setMessage("hello!")
                    .build());
                replyStream.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<HelloRequest> sayHelloStream(StreamObserver<HelloReply> replyStream) {
        return new StreamObserver<HelloRequest>() {
            @Override
            public void onNext(HelloRequest data) {
                replyStream.onNext(HelloReply.newBuilder()
                    .setMessage("hello," + data.getName())
                    .build());
            }

            @Override
            public void onError(Throwable throwable) {
                replyStream.onError(throwable);
            }

            @Override
            public void onCompleted() {
                replyStream.onCompleted();
            }
        };
    }
}
