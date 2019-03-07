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

import org.apache.dubbo.rpc.service.DemoService;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public class ConsumerDemo {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"spring/dubbo-rsocket-consumer.xml"});
        context.start();
        DemoService demoService = (DemoService) context.getBean("demoService"); // get remote service proxy

        while (true) {
            try {
                Thread.sleep(1000);
                Mono<String> resultMono = demoService.requestMono("world"); // call remote method
                resultMono.doOnNext(new Consumer<String>() {
                    @Override
                    public void accept(String s) {
                        System.out.println(s); // get result
                    }
                }).block();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }


        }

    }

}
