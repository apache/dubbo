package com.alibaba.dubbo.demo.consumer;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.stub.StreamObserver;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

/**
 * Created by wuyu on 2017/1/17.
 */
public class GrpcConsumer {
    public static void main(String[] args) throws InterruptedException, IOException {
        ClassPathXmlApplicationContext ctx=new ClassPathXmlApplicationContext("classpath:META-INF/spring/dubbo-demo-consumer.xml");
        GreeterGrpc.Greeter greeter = ctx.getBean("helloWorldService", GreeterGrpc.Greeter.class);
        HelloRequest request = HelloRequest.newBuilder()
                .setName("wuyu")
                .build();
        StreamObserver<HelloReply> observer = new StreamObserver<HelloReply>() {
            @Override
            public void onNext(HelloReply helloReply) {
                System.err.println(helloReply.getMessage());
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {
                System.err.println("完成");
            }
        };
        for (int i = 0; i < 100; i++) {
            greeter.sayHello(request,observer);
        }

        //由于采用异步 必须阻塞
        Thread.sleep(10000);
    }
}
