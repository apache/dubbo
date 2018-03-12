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
package com.alibaba.dubbo.rpc.benchmark;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public class RpcBenchmarkClient extends AbstractBenchmarkClient {

    public static void main(String[] args) throws Exception {
        new RpcBenchmarkClient().run(args);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ClientRunnable getClientRunnable(String targetIP, int targetPort, int clientNums, int rpcTimeout,
                                            CyclicBarrier barrier, CountDownLatch latch, long startTime, long endTime) throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
        String runnable = properties.getProperty("classname");
        Class[] parameterTypes = new Class[]{String.class, int.class, int.class, int.class, CyclicBarrier.class,
                CountDownLatch.class, long.class, long.class};
        Object[] parameters = new Object[]{targetIP, targetPort, clientNums, rpcTimeout, barrier, latch, startTime,
                endTime};
        return (ClientRunnable) Class.forName(runnable).getConstructor(parameterTypes).newInstance(parameters);
    }
}
