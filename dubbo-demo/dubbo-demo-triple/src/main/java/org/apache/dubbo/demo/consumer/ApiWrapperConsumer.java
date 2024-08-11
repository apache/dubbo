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
package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.demo.GreeterWrapperService;
import org.apache.dubbo.rpc.Constants;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class ApiWrapperConsumer {

    public static void main(String[] args) throws InterruptedException {
        ReferenceConfig<GreeterWrapperService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(GreeterWrapperService.class);
        referenceConfig.setCheck(false);
        referenceConfig.setProtocol(CommonConstants.TRIPLE);
        referenceConfig.setLazy(true);
        referenceConfig.setTimeout(100000);
        if (args.length > 0 && Constants.HTTP3_KEY.equals(args[0])) {
            referenceConfig.setParameters(Collections.singletonMap(Constants.HTTP3_KEY, "true"));
        }

        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        bootstrap
                .application(new ApplicationConfig("dubbo-demo-triple-api-wrapper-consumer"))
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .protocol(new ProtocolConfig(CommonConstants.TRIPLE, -1))
                .reference(referenceConfig)
                .start();

        GreeterWrapperService greeterService = referenceConfig.get();
        System.out.println("dubbo referenceConfig started");
        try {
            System.out.println("Call sayHello");
            String reply = greeterService.sayHello(buildRequest("triple"));
            System.out.println("sayHello reply: " + reply);

            System.out.println("Call sayHelloAsync");
            CompletableFuture<String> sayHelloAsync = greeterService.sayHelloAsync("triple");
            sayHelloAsync.thenAccept(value -> System.out.println("sayHelloAsync reply: " + value));

            StreamObserver<String> responseObserver = new StreamObserver<String>() {
                @Override
                public void onNext(String reply) {
                    System.out.println("sayHelloServerStream onNext: " + reply);
                }

                @Override
                public void onError(Throwable t) {
                    System.out.println("sayHelloServerStream onError: " + t);
                }

                @Override
                public void onCompleted() {
                    System.out.println("sayHelloServerStream onCompleted");
                }
            };
            System.out.println("Call sayHelloServerStream");
            greeterService.sayHelloServerStream(buildRequest("triple"), responseObserver);

            StreamObserver<String> biResponseObserver = new StreamObserver<String>() {
                @Override
                public void onNext(String reply) {
                    System.out.println("biRequestObserver onNext: " + reply);
                }

                @Override
                public void onError(Throwable t) {
                    System.out.println("biResponseObserver onError: " + t);
                }

                @Override
                public void onCompleted() {
                    System.out.println("biResponseObserver onCompleted");
                }
            };
            System.out.println("Call biRequestObserver");
            StreamObserver<String> biRequestObserver = greeterService.sayHelloBiStream(biResponseObserver);
            for (int i = 0; i < 5; i++) {
                biRequestObserver.onNext(buildRequest("triple" + i));
            }
            biRequestObserver.onCompleted();
        } catch (Throwable t) {
            //noinspection CallToPrintStackTrace
            t.printStackTrace();
        }
        Thread.sleep(2000);
    }

    private static String buildRequest(String name) {
        return name;
    }
}
