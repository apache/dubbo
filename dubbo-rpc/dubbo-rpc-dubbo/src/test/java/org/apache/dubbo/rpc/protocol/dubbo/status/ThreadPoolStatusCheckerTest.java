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
import org.apache.dubbo.common.store.DataStore;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link ThreadPoolStatusChecker}
 */
class ThreadPoolStatusCheckerTest {

    private DataStore dataStore;
    private String port1;
    private String port2;
    private String testId;

    @BeforeEach
    void setUp() {
        dataStore = ExtensionLoader.getExtensionLoader(DataStore.class).getDefaultExtension();
        // Use unique test identifier to avoid conflicts with parallel tests
        testId = "test-" + System.nanoTime() + "-" + Thread.currentThread().getId();
        // Use dynamic ports to avoid conflicts with parallel tests
        port1 = testId + "-port1";
        port2 = testId + "-port2";
        clearExecutors();
    }

    @AfterEach
    void tearDown() {
        clearExecutors();
    }

    private void clearExecutors() {
        // Clear any existing executors to avoid interference from other tests
        Map<String, Object> executorMap = dataStore.get(CommonConstants.EXECUTOR_SERVICE_COMPONENT_KEY);
        // Shutdown any existing executors before clearing
        for (Object executor : executorMap.values()) {
            if (executor instanceof ExecutorService) {
                ((ExecutorService) executor).shutdown();
            }
        }
        executorMap.clear();
    }

    @Test
    void test() {

        ExecutorService executorService1 = Executors.newFixedThreadPool(1);
        ExecutorService executorService2 = Executors.newFixedThreadPool(10);
        dataStore.put(CommonConstants.EXECUTOR_SERVICE_COMPONENT_KEY, port1, executorService1);
        dataStore.put(CommonConstants.EXECUTOR_SERVICE_COMPONENT_KEY, port2, executorService2);

        ThreadPoolStatusChecker threadPoolStatusChecker = new ThreadPoolStatusChecker(ApplicationModel.defaultModel());
        Status status = threadPoolStatusChecker.check();
        Assertions.assertEquals(status.getLevel(), Status.Level.WARN);

        // Check that the status message contains the expected pool information
        // Since Map iteration order is not guaranteed, we check for both possible orders
        String message = status.getMessage();
        String expectedPool1 = "Pool status:WARN, max:1, core:1, largest:0, active:0, task:0, service port: " + port1;
        String expectedPool2 = "Pool status:OK, max:10, core:10, largest:0, active:0, task:0, service port: " + port2;

        Assertions.assertTrue(
                message.contains(expectedPool1), "Status message should contain pool " + port1 + " info: " + message);
        Assertions.assertTrue(
                message.contains(expectedPool2), "Status message should contain pool " + port2 + " info: " + message);

        // Verify the message contains exactly 2 pools from this test (filter out other parallel tests)
        long testPoolCount = 0;
        if (message.contains(testId)) {
            // Count occurrences of testId in the message
            String[] parts = message.split(testId);
            testPoolCount = parts.length - 1;
        }

        if (testPoolCount == 0) {
            // Fallback: count by semicolons (original logic) but with better error message
            long poolCount = message.chars().filter(ch -> ch == ';').count() + 1;
            Assertions.assertEquals(
                    2, poolCount, "Should have exactly 2 pools, but got: " + message + ". TestId: " + testId);
        } else {
            Assertions.assertEquals(
                    2,
                    testPoolCount,
                    "Should have exactly 2 pools for this test, but got: " + message + ". TestId: " + testId);
        }

        // Shutdown the test executors (tearDown will handle cleanup)
        executorService1.shutdown();
        executorService2.shutdown();
    }
}
