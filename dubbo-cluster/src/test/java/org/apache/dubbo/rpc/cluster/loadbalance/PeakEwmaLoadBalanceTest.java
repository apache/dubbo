/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License")); you may not use this file except in compliance with
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
package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * PeakEwmaLoadBalanceTest
 */
public class PeakEwmaLoadBalanceTest extends LoadBalanceBaseTest {

    private static final int THREAD_NUM = 4;

    private static final int INVOKE_NUM = 2_000;

    private static final long SHAKE_TIME = 50;

    private AtomicInteger sumInvoker1 = new AtomicInteger(0);
    private AtomicInteger sumInvoker2 = new AtomicInteger(0);
    private AtomicInteger sumInvoker5 = new AtomicInteger(0);

    @Test
    public void testWithoutShake() throws InterruptedException {
        //active -> 0
        RpcStatus.endCount(weightInvoker5.getUrl(), weightTestInvocation.getMethodName(), 0L, true);

        List<Callable<Boolean>> tasks = new ArrayList<>();
        IntStream.range(0, THREAD_NUM).forEach(e -> tasks.add(getTask(false)));

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_NUM);
        executorService.invokeAll(tasks);

        Assertions.assertTrue(Math.abs(sumInvoker2.get() - sumInvoker1.get()) <= INVOKE_NUM);
    }

    @Test
    public void testWithShake() throws InterruptedException {
        //active -> 0
        RpcStatus.endCount(weightInvoker5.getUrl(), weightTestInvocation.getMethodName(), 0L, true);

        List<Callable<Boolean>> tasks = new ArrayList<>();
        IntStream.range(0, THREAD_NUM).forEach(e -> tasks.add(getTask(true)));

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_NUM);
        executorService.invokeAll(tasks);

        Assertions.assertTrue(sumInvoker1.get() <= INVOKE_NUM);
    }

    private Callable<Boolean> getTask(boolean needShake) {
        PeakEwmaLoadBalance lb = new PeakEwmaLoadBalance();
        return () -> {
            boolean needShakeTemp = needShake;
            for (int i = 0; i < INVOKE_NUM; i++) {
                Invoker selected = lb.select(weightInvokersSR, null, weightTestInvocation);
                RpcStatus rpcStatus = RpcStatus.getStatus(selected.getUrl(), weightTestInvocation.getMethodName());

                if (i > 100 && needShakeTemp && selected.getUrl().getProtocol().equals("test1")) {
                    //invoker1 shake
                    needShakeTemp = false;
                    rpcStatus.beginCount(selected.getUrl(), weightTestInvocation.getMethodName());
                    TimeUnit.MICROSECONDS.sleep(SHAKE_TIME);
                    rpcStatus.endCount(selected.getUrl(), weightTestInvocation.getMethodName(), SHAKE_TIME, true);
                } else {
                    rpcStatus.beginCount(selected.getUrl(), weightTestInvocation.getMethodName());
                    long time = ThreadLocalRandom.current().nextLong(5, 10);
                    TimeUnit.MICROSECONDS.sleep(time);
                    rpcStatus.endCount(selected.getUrl(), weightTestInvocation.getMethodName(), time, true);
                }

                if (selected.getUrl().getProtocol().equals("test1")) {
                    sumInvoker1.incrementAndGet();
                }

                if (selected.getUrl().getProtocol().equals("test2")) {
                    sumInvoker2.incrementAndGet();
                }

                if (selected.getUrl().getProtocol().equals("test5")) {
                    sumInvoker5.incrementAndGet();
                }
            }
            return true;
        };
    }
}
