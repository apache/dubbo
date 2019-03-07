package org.apache.dubbo.rpc.service;

import org.apache.dubbo.rpc.RpcContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class DemoServiceImpl implements DemoService {
    public DemoServiceImpl() {
        super();
    }

    public String sayHello(String name) {
        return "hello " + name;
    }

    public String echo(String text) {
        return text;
    }

    public Map echo(Map map) {
        return map;
    }

    public long timestamp() {
        return System.currentTimeMillis();
    }

    public String getThreadName() {
        return Thread.currentThread().getName();
    }

    public int getSize(String[] strs) {
        if (strs == null)
            return -1;
        return strs.length;
    }

    public int getSize(Object[] os) {
        if (os == null)
            return -1;
        return os.length;
    }

    public Object invoke(String service, String method) throws Exception {
        System.out.println("RpcContext.getContext().getRemoteHost()=" + RpcContext.getContext().getRemoteHost());
        return service + ":" + method;
    }

    public int stringLength(String str) {
        return str.length();
    }


    public byte getbyte(byte arg) {
        return arg;
    }


    public Set<String> keys(Map<String, String> map) {
        return map == null ? null : map.keySet();
    }


    public long add(int a, long b) {
        return a + b;
    }

    @Override
    public String errorTest(String name) {
        int a = 1 / 0;
        return null;
    }

    public Mono<String> requestMono(String name) {
        return Mono.just("hello " + name);
    }

    public Mono<String> requestMonoOnError(String name) {
        return Mono.error(new DemoException(name));
    }

    public Mono<String> requestMonoBizError(String name) {
        int a = 1 / 0;
        return Mono.just("hello " + name);
    }

    @Override
    public Flux<String> requestFlux(String name) {

        return Flux.create(new Consumer<FluxSink<String>>() {
            @Override
            public void accept(FluxSink<String> fluxSink) {
                for (int i = 0; i < 5; i++) {
                    fluxSink.next(name + " " + i);
                }
                fluxSink.complete();
            }
        });

    }

    @Override
    public Flux<String> requestFluxOnError(String name) {

        return Flux.create(new Consumer<FluxSink<String>>() {
            @Override
            public void accept(FluxSink<String> fluxSink) {
                for (int i = 0; i < 5; i++) {
                    fluxSink.next(name + " " + i);
                }
                fluxSink.error(new DemoException());
            }
        });

    }

    @Override
    public Flux<String> requestFluxBizError(String name) {
        int a = 1 / 0;
        return Flux.create(new Consumer<FluxSink<String>>() {
            @Override
            public void accept(FluxSink<String> fluxSink) {
                for (int i = 0; i < 5; i++) {
                    fluxSink.next(name + " " + i);
                }
                fluxSink.error(new DemoException());
            }
        });
    }

}

