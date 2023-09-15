package org.apache.dubbo.rpc.support;

import org.apache.dubbo.common.stream.StreamObserver;

public class DemoService1Impl implements DemoService1{
    @Override
    public StreamObserver<String> sayHello(StreamObserver<String> request) {
        request.onNext("BI_STREAM");
        return request;
    }

    @Override
    public void sayHello(String msg, StreamObserver<String> request) {
        request.onNext(msg);
        request.onNext("SERVER_STREAM");
        request.onCompleted();
    }
}
