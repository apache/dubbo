package org.apache.dubbo.sample.tri;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.stub.GreeterStub;

public class IGreeterImpl extends GreeterStub.IGreeterImplBase {
    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        responseObserver.onNext(HelloReply.newBuilder().setMessage("Hello!" + request.getName()).build());
        responseObserver.onCompleted();
    }
}
