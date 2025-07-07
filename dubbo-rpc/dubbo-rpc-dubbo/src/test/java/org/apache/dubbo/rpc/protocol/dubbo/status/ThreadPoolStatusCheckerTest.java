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

    @BeforeEach
    void setUp() {
        dataStore = ExtensionLoader.getExtensionLoader(DataStore.class).getDefaultExtension();
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
        dataStore.put(CommonConstants.EXECUTOR_SERVICE_COMPONENT_KEY, "8888", executorService1);
        dataStore.put(CommonConstants.EXECUTOR_SERVICE_COMPONENT_KEY, "8889", executorService2);

        ThreadPoolStatusChecker threadPoolStatusChecker = new ThreadPoolStatusChecker(ApplicationModel.defaultModel());
        Status status = threadPoolStatusChecker.check();
        Assertions.assertEquals(status.getLevel(), Status.Level.WARN);

        // Check that the status message contains the expected pool information
        // Since Map iteration order is not guaranteed, we check for both possible orders
        String message = status.getMessage();
        String expectedPool8888 = "Pool status:WARN, max:1, core:1, largest:0, active:0, task:0, service port: 8888";
        String expectedPool8889 = "Pool status:OK, max:10, core:10, largest:0, active:0, task:0, service port: 8889";

        Assertions.assertTrue(
                message.contains(expectedPool8888), "Status message should contain pool 8888 info: " + message);
        Assertions.assertTrue(
                message.contains(expectedPool8889), "Status message should contain pool 8889 info: " + message);

        // Verify the message contains exactly 2 pools (no interference from other tests)
        long poolCount = message.chars().filter(ch -> ch == ';').count() + 1;
        Assertions.assertEquals(2, poolCount, "Should have exactly 2 pools, but got: " + message);

        // Shutdown the test executors (tearDown will handle cleanup)
        executorService1.shutdown();
        executorService2.shutdown();
    }
}
