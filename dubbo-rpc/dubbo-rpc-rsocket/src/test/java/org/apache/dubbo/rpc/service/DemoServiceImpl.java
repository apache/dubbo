/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.service;

import org.apache.dubbo.rpc.RpcContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
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

    @Override
    public Mono<String> requestMonoWithMonoArg(Mono<String> m1, Mono<String> m2) {
        return m1.zipWith(m2, new BiFunction<String, String, String>() {
            @Override
            public String apply(String s, String s2) {
                return s+" "+s2;
            }
        });
    }

    @Override
    public Flux<String> requestFluxWithFluxArg(Flux<String> f1, Flux<String> f2) {
        return f1.zipWith(f2, new BiFunction<String, String, String>() {
            @Override
            public String apply(String s, String s2) {
                return s+" "+s2;
            }
        });
    }

}

