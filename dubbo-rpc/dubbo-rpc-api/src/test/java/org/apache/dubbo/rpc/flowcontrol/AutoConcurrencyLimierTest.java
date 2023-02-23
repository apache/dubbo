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
package org.apache.dubbo.rpc.flowcontrol;

import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class AutoConcurrencyLimierTest extends FlowControlBaseTest{
    Long elapsed[];
    @BeforeEach
    public void setForHSTest(){
        elapsed = new Long[5];
        elapsed[0] = 1000L;
        elapsed[1] = 2000L;
        elapsed[2] = 3000L;
        elapsed[3] = 4000L;
        elapsed[4] = 5000L;

    }


    @Test
    public void testAutoConcurrencyLimier(){
        AutoConcurrencyLimier flowControl = new AutoConcurrencyLimier();
        flowControl.setApplicationModel(ApplicationModel.defaultModel());
        int[] max = new int[5];
        for(int i = 1; i <= 5; i++)
            flowControl.Begin();
        for(int i = 0; i < 5; i++){
            flowControl.End(elapsed[i]);
            max[i] = flowControl.getMaxConcurrency();
            System.out.println(max[i]);
        }

        for (int i = 0; i < 4; i++){
            Assertions.assertTrue(max[i] >= max[i+1]);
        }
    }

    @Test
    public void testAutoConcurrencyLimierInMultiThread() throws Exception {
        AutoConcurrencyLimier flowControl = new AutoConcurrencyLimier();
        flowControl.setApplicationModel(ApplicationModel.defaultModel());

        int threadNum = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadNum);
        List<Thread> threadList = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger();
        for (int i = 1; i <= 5*threadNum; i++)
            flowControl.Begin();

        for (int i = 0; i < threadNum; i++){
            Thread thread = new Thread(() -> {
                try{
                    startLatch.await();
                    for(int j=0;j<5;j++){
                        flowControl.End(100L);
                        successCount.incrementAndGet();
                        System.out.println("epoch is " + j +" the max concurrency is " + flowControl.getMaxConcurrency());
                    }
                    endLatch.countDown();
                }catch (Exception e){
                    e.printStackTrace();
                }
            });
            threadList.add(thread);
        }

        threadList.forEach(Thread::start);
        startLatch.countDown();
        endLatch.await();
        Assertions.assertEquals(successCount.get(),5 * threadNum);
    }

}
