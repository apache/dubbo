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
import org.apache.dubbo.sample.tri.HelloReply;
import org.apache.dubbo.sample.tri.HelloRequest;
import org.apache.dubbo.sample.tri.IGreeter;

import java.io.IOException;


public class ApiConsumer {
    public static void main(String[] args) throws InterruptedException, IOException {
        ReferenceConfig<IGreeter> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setProxy("nativestub");
        referenceConfig.setInterface(IGreeter.class);
        referenceConfig.setCheck(false);
        referenceConfig.setProtocol(CommonConstants.TRIPLE);
        referenceConfig.setLazy(true);
        referenceConfig.setTimeout(100000);

        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        bootstrap.application(new ApplicationConfig("dubbo-demo-triple-api-consumer"))
            .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
            .protocol(new ProtocolConfig(CommonConstants.TRIPLE, -1))
            .reference(referenceConfig)
            .start();

        IGreeter greeterService = referenceConfig.get();
        System.out.println("dubbo referenceConfig started");
        HelloRequest request = HelloRequest.newBuilder().setName("triple").build();
        try {
//            final HelloReply reply = greeterService.sayHello(request);
//            TimeUnit.SECONDS.sleep(1);
//            System.out.println("Reply: " + reply.getMessage());

            StreamObserver<HelloReply> observer = new StreamObserver<HelloReply>() {
                @Override
                public void onNext(HelloReply data) {
                    System.out.println("Received server stream data:" + data);
                }

                @Override
                public void onError(Throwable throwable) {
                    throwable.printStackTrace();
                }

                @Override
                public void onCompleted() {
                    System.out.println("Server stream done ");
                }
            };
            greeterService.sayHelloServerStream(request, observer);
            StreamObserver<HelloRequest> requestObserver = greeterService.sayHelloClientStream(observer);
            for (int i = 0; i < 10; i++) {
                requestObserver.onNext(request);
            }
            requestObserver.onCompleted();

            StreamObserver<HelloRequest> ro2 = greeterService.sayHelloStream(observer);
            for (int i = 0; i < 10; i++) {
                ro2.onNext(request);
            }
            ro2.onCompleted();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        System.in.read();
    }
}
