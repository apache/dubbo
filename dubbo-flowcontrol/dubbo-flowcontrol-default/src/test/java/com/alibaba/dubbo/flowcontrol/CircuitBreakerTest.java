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
package com.alibaba.dubbo.flowcontrol;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class CircuitBreakerTest {
    public static void main(String[] args){
        CircuitBreakerConfig circuitBreakerConfig =  CircuitBreakerConfig.newDefault();
        final   CircuitBreaker circuitBreaker = new CircuitBreaker("ttttt",circuitBreakerConfig);
        final CircuitBreaker ss = new CircuitBreaker("ssss",circuitBreakerConfig);
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
        final AtomicInteger count =new AtomicInteger(0);
        for (int i = 0; i < 50; i++) {
            final int index = i;
            cachedThreadPool.execute(new Runnable() {
                public void run() {
                    for(int i=0;i<10000;i++){
                        try{
                            ss.incrTotleCount();
                            circuitBreaker.incrTotleCount();
                          /*  Thread.sleep(10);*/
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
        for (int i = 0; i < 50; i++) {
            final int index = i;
            cachedThreadPool.execute(new Runnable() {
                public void run() {
                    for(int i=0;i<1000;i++){
                        try{
                            circuitBreaker.incrFailCount();
                          /*  Thread.sleep(10);*/
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

       while(true){
            try {
                Thread.sleep(1000l);
                System.out.println("circuitBreaker:"+circuitBreaker.toString());
                System.out.println("error:"+circuitBreaker.closeFailThresholdReached());
                System.out.println("ss===========:"+ss.toString());
                System.out.println("ss===============:"+ss.closeFailThresholdReached());

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
