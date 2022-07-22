package org.apache.dubbo.demo.hello;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// auto-generated code
public interface ReactorStreamTest {

    String JAVA_SERVICE_NAME = "org.apache.dubbo.demo.hello.ReactorStreamTest";
    String SERVICE_NAME = "org.apache.dubbo.demo.hello.ReactorStreamTest";

    Mono<HelloReply> testOneToOne(Mono<HelloRequest> requestMono);

    Flux<HelloReply> testOneToMany(Mono<HelloRequest> requestMono);

    Flux<HelloReply> testManyToMany(Flux<HelloRequest> requestFlux);

}
