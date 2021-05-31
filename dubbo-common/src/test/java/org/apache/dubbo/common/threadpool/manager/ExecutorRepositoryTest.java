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
package org.apache.dubbo.common.threadpool.manager;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

public class ExecutorRepositoryTest {
    private ExecutorRepository executorRepository = ExtensionLoader.getExtensionLoader(ExecutorRepository.class).getDefaultExtension();

    @Test
    public void testGetExecutor() {
        testGet(URL.valueOf("dubbo://127.0.0.1:23456"));
        testGet(URL.valueOf("dubbo://127.0.0.1:23456?side=consumer"));

        Assertions.assertNotNull(executorRepository.getSharedExecutor());
        Assertions.assertNotNull(executorRepository.getServiceExporterExecutor());
        executorRepository.nextScheduledExecutor();
    }

    private void testGet(URL url) {
        ExecutorService executorService = executorRepository.createExecutorIfAbsent(url);
        executorService.shutdown();
        executorService = executorRepository.createExecutorIfAbsent(url);
        Assertions.assertFalse(executorService.isShutdown());

        Assertions.assertEquals(executorService, executorRepository.getExecutor(url));
        executorService.shutdown();
        Assertions.assertNotEquals(executorService, executorRepository.getExecutor(url));
    }

    @Test
    public void testUpdateExecutor() {
        URL url = URL.valueOf("dubbo://127.0.0.1:23456?threads=5");
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) executorRepository.createExecutorIfAbsent(url);

        executorService.setCorePoolSize(3);
        executorRepository.updateThreadpool(url, executorService);

        executorService.setCorePoolSize(3);
        executorService.setMaximumPoolSize(3);
        executorRepository.updateThreadpool(url, executorService);

        executorService.setMaximumPoolSize(20);
        executorService.setCorePoolSize(10);
        executorRepository.updateThreadpool(url, executorService);

        executorService.setCorePoolSize(10);
        executorService.setMaximumPoolSize(10);
        executorRepository.updateThreadpool(url, executorService);

        executorService.setCorePoolSize(5);
        executorRepository.updateThreadpool(url, executorService);


    }
}
