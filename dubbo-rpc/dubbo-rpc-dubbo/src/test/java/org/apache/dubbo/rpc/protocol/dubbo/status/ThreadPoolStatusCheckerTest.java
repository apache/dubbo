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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link ThreadPoolStatusChecker}
 */
class ThreadPoolStatusCheckerTest {

    @Test
    void test() {
        DataStore dataStore =
                ExtensionLoader.getExtensionLoader(DataStore.class).getDefaultExtension();
        ExecutorService executorService1 = Executors.newFixedThreadPool(1);
        ExecutorService executorService2 = Executors.newFixedThreadPool(10);
        dataStore.put(CommonConstants.EXECUTOR_SERVICE_COMPONENT_KEY, "8888", executorService1);
        dataStore.put(CommonConstants.EXECUTOR_SERVICE_COMPONENT_KEY, "8889", executorService2);

        try {
            ThreadPoolStatusChecker threadPoolStatusChecker =
                    new ThreadPoolStatusChecker(ApplicationModel.defaultModel());
            Status status = threadPoolStatusChecker.check();
            Assertions.assertEquals(Status.Level.WARN, status.getLevel());

            // DataStore does not guarantee map iteration order, so assert each pool status independently.
            List<String> poolStatuses = Arrays.asList(status.getMessage().split(";"));
            Assertions.assertEquals(2, poolStatuses.size());
            Assertions.assertTrue(poolStatuses.contains(
                    "Pool status:WARN, max:1, core:1, largest:0, active:0, task:0, service port: 8888"));
            Assertions.assertTrue(poolStatuses.contains(
                    "Pool status:OK, max:10, core:10, largest:0, active:0, task:0, service port: 8889"));
        } finally {
            // reset
            executorService1.shutdown();
            executorService2.shutdown();
            dataStore.remove(CommonConstants.EXECUTOR_SERVICE_COMPONENT_KEY, "8888");
            dataStore.remove(CommonConstants.EXECUTOR_SERVICE_COMPONENT_KEY, "8889");
        }
    }
}
