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
package org.apache.dubbo.crossthread.interceptor;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.crossthread.toolkit.CallableWrapper;
import org.apache.dubbo.crossthread.toolkit.RunnableWrapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DubboCrossThreadTest {
    @Test
    public void crossThreadCallableTest() throws ExecutionException, InterruptedException, TimeoutException {
        Instrumentation instrumentation = ByteBuddyAgent.install();
        RunnableOrCallableActivation.install(instrumentation);
        String tag = "beta";
        RpcContext.getClientAttachment().setAttachment(CommonConstants.TAG_KEY, tag);
        Callable<String> callable = CallableWrapper.of(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return RpcContext.getClientAttachment().getAttachment(CommonConstants.TAG_KEY);
            }
        });
        ExecutorService threadPool = Executors.newSingleThreadExecutor();
        Future<String> submit = threadPool.submit(callable);
        assertEquals(tag, submit.get(1, TimeUnit.SECONDS));
        threadPool.shutdown();
    }

    private volatile String tagCrossThread = null;

    @Test
    public void crossThreadRunnableTest() throws ExecutionException, InterruptedException {
        Instrumentation instrumentation = ByteBuddyAgent.install();
        RunnableOrCallableActivation.install(instrumentation);
        String tag = "beta";
        RpcContext.getClientAttachment().setAttachment(CommonConstants.TAG_KEY, tag);
        final CountDownLatch latch = new CountDownLatch(1);
        Runnable runnable = RunnableWrapper.of(new Runnable() {
            @Override
            public void run() {
                String tag = RpcContext.getClientAttachment().getAttachment(CommonConstants.TAG_KEY);
                tagCrossThread = tag;
                latch.countDown();
            }
        });
        ExecutorService threadPool = Executors.newSingleThreadExecutor();
        threadPool.submit(runnable);
        latch.await(1, TimeUnit.SECONDS);
        assertEquals(tag, tagCrossThread);
        threadPool.shutdown();
    }

}
