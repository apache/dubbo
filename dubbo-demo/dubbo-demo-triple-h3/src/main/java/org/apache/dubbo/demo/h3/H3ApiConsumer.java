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
package org.apache.dubbo.demo.h3;

import java.io.IOException;

public class H3ApiConsumer {
    public static void main(String[] args) throws InterruptedException, IOException {
        /* ReferenceConfig<GreeterService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(GreeterService.class);
        referenceConfig.setCheck(false);
        referenceConfig.setProtocol(CommonConstants.TRIPLE);
        referenceConfig.setLazy(true);
        referenceConfig.setTimeout(1000*60*30);
        referenceConfig.setRetries(0);

        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        bootstrap
                .application(new ApplicationConfig("dubbo-demo-triple-api-consumer"))
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .protocol(new ProtocolConfig(CommonConstants.TRIPLE, -1))
                .reference(referenceConfig)
                .start();

        GreeterService greeterService = referenceConfig.get();
        System.out.println("dubbo referenceConfig started");
        try {
            final HelloReply reply = greeterService.sayHello(
                    HelloRequest.newBuilder().setName("triple").build());
            TimeUnit.SECONDS.sleep(1);
            System.out.println("Reply: " + reply.getMessage());

            CompletableFuture<String> sayHelloAsync = greeterService.sayHelloAsync("triple");
            System.out.println("Async Reply: " + sayHelloAsync.get());
        } catch (Throwable t) {
            t.printStackTrace();
        }
        System.in.read();*/
    }
}
