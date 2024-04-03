package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.demo.GreeterStreamService;
import org.apache.dubbo.demo.hello.HelloReply;
import org.apache.dubbo.demo.hello.HelloRequest;

import java.io.IOException;

public class ApiStreamConsumer {
    public static void main(String[] args) throws IOException {
        ReferenceConfig<GreeterStreamService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(GreeterStreamService.class);
        referenceConfig.setCheck(false);
        referenceConfig.setProtocol(CommonConstants.TRIPLE);
        referenceConfig.setLazy(true);
        referenceConfig.setTimeout(1000 * 60 * 30);
        referenceConfig.setRetries(0);

        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        bootstrap
                .application(new ApplicationConfig("dubbo-demo-triple-api-consumer"))
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .protocol(new ProtocolConfig(CommonConstants.TRIPLE, -1))
                .reference(referenceConfig)
                .start();

        GreeterStreamService delegate = referenceConfig.get();
        System.out.println("dubbo referenceConfig started");

        // ============= server stream =============
        delegate.serverStream(HelloRequest.newBuilder()
                .setName("request for serverStream")
                .build(), new StreamObserver<HelloReply>() {
            @Override
            public void onNext(HelloReply data) {
                System.out.println("serverStream responseObserver.onNext: "+data.getMessage());
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {
                System.out.println("serverStream responseObserver.onCompleted");
            }
        });

        // ============= bi-stream =============
        StreamObserver<HelloRequest> requestObserver = delegate.biStream(new StreamObserver<HelloReply>() {
            @Override
            public void onNext(HelloReply data) {
                System.out.println("biStream responseObserver.onNext: "+data.getMessage());
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {
                System.out.println("biStream responseObserver.onCompleted");
            }
        });
        for (int i=0; i<5; ++i) {
            requestObserver.onNext(HelloRequest.newBuilder().setName("request for biStream").build());
        }
        requestObserver.onCompleted();

        System.in.read();
    }
}
