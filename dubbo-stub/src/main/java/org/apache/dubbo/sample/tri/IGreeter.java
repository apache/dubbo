package org.apache.dubbo.sample.tri;

import org.apache.dubbo.common.stream.StreamObserver;

public interface IGreeter {

        HelloReply sayHello(HelloRequest request);

        void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver);

        void sayHelloServerStream(HelloRequest request, StreamObserver<HelloReply> replyStream);

        StreamObserver<HelloRequest> sayHelloClientStream(StreamObserver<HelloReply> replyStream);

        StreamObserver<HelloRequest> sayHelloStream(StreamObserver<HelloReply> replyStream);

    }
