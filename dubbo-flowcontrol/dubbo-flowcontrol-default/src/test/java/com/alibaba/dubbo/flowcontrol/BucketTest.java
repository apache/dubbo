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
public class BucketTest {

    public static void main(String[] args) throws InterruptedException {
        final Bucket bucket = new Bucket();
        bucket.setStartTime();
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
        final AtomicInteger count =new AtomicInteger(0);
        for (int i = 0; i < 1000; i++) {
            final int index = i;
            cachedThreadPool.execute(new Runnable() {
                public void run() {
                    for(int i=0;i<100;i++){
                        try{
                            bucket.addLongAdderTotle();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
        Thread.sleep(100);
        System.out.println( bucket.sumLongAdderTotle());
     /* bucket.addLongAdderFail();*/
    /*   Thread.sleep(1000);*/
     /*   System.out.println(bucket.getErrorThresholdPercentage());*/
    }
}
