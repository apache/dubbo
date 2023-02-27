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
package org.apache.dubbo.rpc.protocol.dubbo.status;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.status.Status;
<<<<<<< HEAD
import org.apache.dubbo.common.status.StatusChecker;
import org.apache.dubbo.common.store.DataStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.ThreadPoolExecutor;


public class ThreadPoolStatusCheckerTest {

    private static ThreadPoolStatusChecker threadPoolStatusChecker;

    @BeforeAll
    public static void setUp() {
        threadPoolStatusChecker =
                (ThreadPoolStatusChecker) ExtensionLoader.getExtensionLoader(StatusChecker.class).getExtension("threadpool");
    }

    @Test
    public void statusUnknownTest() {
        Status status = threadPoolStatusChecker.check();
        Assertions.assertEquals(status.getLevel(), Status.Level.UNKNOWN);
    }

    @Test
    public void statusOkTest() {
        int activeCount = 1;
        int maximumPoolSize = 3;
        String portKey = "8888";
        mockThreadPoolExecutor(activeCount, maximumPoolSize, portKey);

        Status status = threadPoolStatusChecker.check();

        Assertions.assertEquals(status.getLevel(), Status.Level.OK);
        Assertions.assertEquals(status.getMessage(),
                "Pool status:OK, max:" + maximumPoolSize + ", core:0, largest:0, active:" + activeCount + ", task:0, service port: " +
                        portKey);
        destroy(portKey);
    }


    @Test
    public void statusWarnTest() {
        int activeCount = 1;
        int maximumPoolSize = 2;
        String portKey = "8888";
        mockThreadPoolExecutor(activeCount, maximumPoolSize, portKey);

        Status status = threadPoolStatusChecker.check();

        Assertions.assertEquals(status.getLevel(), Status.Level.WARN);
        Assertions.assertEquals(status.getMessage(),
                "Pool status:WARN, max:" + maximumPoolSize + ", core:0, largest:0, active:" + activeCount + ", task:0, service port: 8888");
        destroy(portKey);
    }

    private void mockThreadPoolExecutor(int activeCount, int maximumPoolSize, String portKey) {
        DataStore dataStore = ExtensionLoader.getExtensionLoader(DataStore.class).getDefaultExtension();

        ThreadPoolExecutor executor = Mockito.mock(ThreadPoolExecutor.class);
        Mockito.when(executor.getActiveCount()).thenReturn(activeCount);
        Mockito.when(executor.getMaximumPoolSize()).thenReturn(maximumPoolSize);

        dataStore.put(CommonConstants.EXECUTOR_SERVICE_COMPONENT_KEY, portKey, executor);
    }

    private void destroy(String portKey) {
        DataStore dataStore = ExtensionLoader.getExtensionLoader(DataStore.class).getDefaultExtension();
        dataStore.remove(CommonConstants.EXECUTOR_SERVICE_COMPONENT_KEY, portKey);
=======
import org.apache.dubbo.common.store.DataStore;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * {@link ThreadPoolStatusChecker}
 */
class ThreadPoolStatusCheckerTest {

    @Test
    void test() {
        DataStore dataStore = ExtensionLoader.getExtensionLoader(DataStore.class).getDefaultExtension();
        ExecutorService executorService1 = Executors.newFixedThreadPool(1);
        ExecutorService executorService2 = Executors.newFixedThreadPool(10);
        dataStore.put(CommonConstants.EXECUTOR_SERVICE_COMPONENT_KEY, "8888", executorService1);
        dataStore.put(CommonConstants.EXECUTOR_SERVICE_COMPONENT_KEY, "8889", executorService2);

        ThreadPoolStatusChecker threadPoolStatusChecker = new ThreadPoolStatusChecker(ApplicationModel.defaultModel());
        Status status = threadPoolStatusChecker.check();
        Assertions.assertEquals(status.getLevel(), Status.Level.WARN);
        Assertions.assertEquals(status.getMessage(),
            "Pool status:WARN, max:1, core:1, largest:0, active:0, task:0, service port: 8888;"
                + "Pool status:OK, max:10, core:10, largest:0, active:0, task:0, service port: 8889");

        // reset
        executorService1.shutdown();
        executorService2.shutdown();
        dataStore.remove(CommonConstants.EXECUTOR_SERVICE_COMPONENT_KEY, "8888");
        dataStore.remove(CommonConstants.EXECUTOR_SERVICE_COMPONENT_KEY, "8889");
>>>>>>> origin/3.2
    }
}
