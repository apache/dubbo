package org.apache.dubbo.sample.tri;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.stub.GreeterStub;

public class IGreeterImpl extends GreeterStub.IGreeterImplBase {
    @Override
    public StreamObserver<HelloRequest> sayHello(StreamObserver<HelloReply> responseObserver) {
        return new StreamObserver<HelloRequest>() {
            @Override
            public void onNext(HelloRequest data) {
                responseObserver.onNext(HelloReply.newBuilder().setMessage("Hello!" + data.getName()).build());
            }

            @Override
            public void onError(Throwable throwable) {
                responseObserver.onError(new IllegalStateException("Error"));
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}
