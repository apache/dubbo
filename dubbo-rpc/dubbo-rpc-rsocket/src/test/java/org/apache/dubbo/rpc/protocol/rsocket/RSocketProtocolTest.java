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
package org.apache.dubbo.rpc.protocol.rsocket;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.service.DemoException;
import org.apache.dubbo.rpc.service.DemoService;
import org.apache.dubbo.rpc.service.DemoServiceImpl;
import org.apache.dubbo.rpc.service.EchoService;
import org.apache.dubbo.rpc.service.RemoteService;
import org.apache.dubbo.rpc.service.RemoteServiceImpl;
import org.junit.AfterClass;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

public class RSocketProtocolTest {

    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    @AfterClass
    public static void after() {
        RSocketProtocol.getRSocketProtocol().destroy();
    }

    @Test
    public void testDemoProtocol() throws Exception {
        DemoService service = new DemoServiceImpl();
        protocol.export(proxy.getInvoker(service, DemoService.class, URL.valueOf("rsocket://127.0.0.1:9020/" + DemoService.class.getName())));
        service = proxy.getProxy(protocol.refer(DemoService.class, URL.valueOf("rsocket://127.0.0.1:9020/" + DemoService.class.getName()).addParameter("timeout", 3000l)));
        assertEquals(service.getSize(new String[]{"", "", ""}), 3);
    }

    @Test
    public void testDubboProtocol() throws Exception {
        DemoService service = new DemoServiceImpl();
        protocol.export(proxy.getInvoker(service, DemoService.class, URL.valueOf("rsocket://127.0.0.1:9010/" + DemoService.class.getName())));
        service = proxy.getProxy(protocol.refer(DemoService.class, URL.valueOf("rsocket://127.0.0.1:9010/" + DemoService.class.getName()).addParameter("timeout", 3000l)));

        assertEquals(service.getSize(null), -1);
        assertEquals(service.getSize(new String[]{"", "", ""}), 3);


        Map<String, String> map = new HashMap<String, String>();
        map.put("aa", "bb");
        Set<String> set = service.keys(map);
        assertEquals(set.size(), 1);
        assertEquals(set.iterator().next(), "aa");
        service.invoke("rsocket://127.0.0.1:9010/" + DemoService.class.getName() + "", "invoke");

        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < 1024 * 32 + 32; i++)
            buf.append('A');
        System.out.println(service.stringLength(buf.toString()));

        // cast to EchoService
        EchoService echo = proxy.getProxy(protocol.refer(EchoService.class, URL.valueOf("rsocket://127.0.0.1:9010/" + DemoService.class.getName()).addParameter("timeout", 3000l)));
        assertEquals(echo.$echo(buf.toString()), buf.toString());
        assertEquals(echo.$echo("test"), "test");
        assertEquals(echo.$echo("abcdefg"), "abcdefg");
        assertEquals(echo.$echo(1234), 1234);
    }


    @Test
    public void testDubboProtocolThrowable() throws Exception {
        DemoService service = new DemoServiceImpl();
        protocol.export(proxy.getInvoker(service, DemoService.class, URL.valueOf("rsocket://127.0.0.1:9010/" + DemoService.class.getName())));
        service = proxy.getProxy(protocol.refer(DemoService.class, URL.valueOf("rsocket://127.0.0.1:9010/" + DemoService.class.getName()).addParameter("timeout", 3000l)));
        try {
            service.errorTest("mike");
        } catch (Throwable t) {
            assertEquals(t.getClass(), ArithmeticException.class);
        }
    }

    @Test
    public void testDubboProtocolMultiService() throws Exception {
        DemoService service = new DemoServiceImpl();
        protocol.export(proxy.getInvoker(service, DemoService.class, URL.valueOf("rsocket://127.0.0.1:9010/" + DemoService.class.getName())));
        service = proxy.getProxy(protocol.refer(DemoService.class, URL.valueOf("rsocket://127.0.0.1:9010/" + DemoService.class.getName()).addParameter("timeout", 3000l)));

        RemoteService remote = new RemoteServiceImpl();
        protocol.export(proxy.getInvoker(remote, RemoteService.class, URL.valueOf("rsocket://127.0.0.1:9010/" + RemoteService.class.getName())));
        remote = proxy.getProxy(protocol.refer(RemoteService.class, URL.valueOf("rsocket://127.0.0.1:9010/" + RemoteService.class.getName()).addParameter("timeout", 3000l)));

        service.sayHello("world");

        // test netty client
        assertEquals("world", service.echo("world"));
        assertEquals("hello world", remote.sayHello("world"));

        EchoService serviceEcho = (EchoService) service;
        assertEquals(serviceEcho.$echo("test"), "test");

        EchoService remoteEecho = (EchoService) remote;
        assertEquals(remoteEecho.$echo("ok"), "ok");
    }


    @Test
    public void testRequestMono() throws Exception {
        DemoService service = new DemoServiceImpl();
        protocol.export(proxy.getInvoker(service, DemoService.class, URL.valueOf("rsocket://127.0.0.1:9020/" + DemoService.class.getName())));
        service = proxy.getProxy(protocol.refer(DemoService.class, URL.valueOf("rsocket://127.0.0.1:9020/" + DemoService.class.getName()).addParameter("timeout", 3000l)));
        Mono<String> result = service.requestMono("mike");

        result.doOnNext(new Consumer<String>() {
            @Override
            public void accept(String s) {
                assertEquals(s, "hello mike");
                System.out.println(s);
            }
        }).block();

        Mono<String> result2 = service.requestMonoOnError("mike");
        result2.onErrorResume(DemoException.class, new Function<DemoException, Mono<String>>() {
            @Override
            public Mono<String> apply(DemoException e) {
                return Mono.just(e.getClass().getName());
            }
        }).doOnNext(new Consumer<String>() {
            @Override
            public void accept(String s) {
                assertEquals(DemoException.class.getName(), s);
            }
        }).block();

        Mono<String> result3 = service.requestMonoBizError("mike");
        result3.onErrorResume(ArithmeticException.class, new Function<ArithmeticException, Mono<String>>() {
            @Override
            public Mono<String> apply(ArithmeticException e) {
                return Mono.just(e.getClass().getName());
            }
        }).doOnNext(new Consumer<String>() {
            @Override
            public void accept(String s) {
                assertEquals(ArithmeticException.class.getName(), s);
            }
        }).block();

    }

    @Test
    public void testRequestFlux() throws Exception {
        DemoService service = new DemoServiceImpl();
        protocol.export(proxy.getInvoker(service, DemoService.class, URL.valueOf("rsocket://127.0.0.1:9020/" + DemoService.class.getName())));
        service = proxy.getProxy(protocol.refer(DemoService.class, URL.valueOf("rsocket://127.0.0.1:9020/" + DemoService.class.getName()).addParameter("timeout", 3000l)));

        {
            Flux<String> result = service.requestFlux("mike");
            result.doOnNext(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    System.out.println(s);
                }
            }).blockLast();
        }


        {
            Flux<String> result2 = service.requestFluxOnError("mike");
            result2.onErrorResume(new Function<Throwable, Publisher<? extends String>>() {
                @Override
                public Publisher<? extends String> apply(Throwable throwable) {
                    return Flux.just(throwable.getClass().getName());
                }
            }).takeLast(1).doOnNext(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    assertEquals(DemoException.class.getName(), s);
                }
            }).blockLast();
        }

        {
            Flux<String> result3 = service.requestFluxBizError("mike");
            result3.onErrorResume(new Function<Throwable, Publisher<? extends String>>() {
                @Override
                public Publisher<? extends String> apply(Throwable throwable) {
                    return Flux.just(throwable.getClass().getName());
                }
            }).takeLast(1).doOnNext(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    assertEquals(ArithmeticException.class.getName(), s);
                }
            }).blockLast();
        }
    }

}
