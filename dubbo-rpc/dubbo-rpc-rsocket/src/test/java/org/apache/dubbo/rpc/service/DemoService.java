package org.apache.dubbo.rpc.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

public  interface DemoService {
    String sayHello(String name);

    Set<String> keys(Map<String, String> map);

    String echo(String text);

    Map echo(Map map);

    long timestamp();

    String getThreadName();

    int getSize(String[] strs);

    int getSize(Object[] os);

    Object invoke(String service, String method) throws Exception;

    int stringLength(String str);

    byte getbyte(byte arg);

    long add(int a, long b);

    String errorTest(String name);

    Mono<String> requestMono(String name);

    Mono<String> requestMonoOnError(String name);

    Mono<String> requestMonoBizError(String name);

    Flux<String> requestFlux(String name);
    Flux<String> requestFluxOnError(String name);
    Flux<String> requestFluxBizError(String name);



}
