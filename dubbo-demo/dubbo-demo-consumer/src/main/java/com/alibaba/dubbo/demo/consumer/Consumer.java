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
package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.rpc.RpcResult;

import java.util.concurrent.CompletableFuture;

public class Consumer {

    public static void main(String[] args) throws InterruptedException {
       /* //Prevent to get IPV6 address,this way only work in debug mode
        //But you can pass use -Djava.net.preferIPv4Stack=true,then it work well whether in debug mode or not
        System.setProperty("java.net.preferIPv4Stack", "true");
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer.xml"});
        context.start();
        DemoService demoService = (DemoService) context.getBean("demoService"); // get remote service proxy

        while (true) {
            try {
                Thread.sleep(1000);
                CompletableFuture<String> future = demoService.sayHelloAsync("world"); // call remote method
                System.out.println(future.get()); // get result

            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }


        }*/


        RpcResult resultCreated = new RpcResult();
        CompletableFuture<Object> futureCreated = new CompletableFuture();
        futureCreated.whenComplete((Object i, Throwable err) -> {
            if (err != null) {
                resultCreated.setException(err);
            } else {
                resultCreated.setValue(i);
            }
        });

        RpcResult result = new RpcResult();
        CompletableFuture future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            throw new RuntimeException("1");
        }).whenComplete((i, err) -> {
            if (err != null) {
                result.setException(err);
            } else {
                result.setValue(i);
            }
        });


        future.complete(new RuntimeException("aaa"));
        Thread.sleep(500);
        System.out.println(result.getException());
        System.out.println(result.getValue());

        futureCreated.completeExceptionally(new RuntimeException("aaa"));
        Thread.sleep(500);
        System.out.println(resultCreated.getException());
        System.out.println(resultCreated.getValue());
//
//        try {
////            future.completeExceptionally(new RuntimeException("aaa"));
//
//            Object obj =future.get();
//            System.out.println(future.isCompletedExceptionally());
//            System.out.println(obj);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        }

    }
}
