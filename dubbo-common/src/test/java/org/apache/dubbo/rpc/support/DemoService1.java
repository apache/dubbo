package org.apache.dubbo.rpc.support;

import org.apache.dubbo.common.stream.StreamObserver;

public interface DemoService1 {
    StreamObserver<String> sayHello(StreamObserver<String> request);

    void sayHello(String msg, StreamObserver<String> request);
}
