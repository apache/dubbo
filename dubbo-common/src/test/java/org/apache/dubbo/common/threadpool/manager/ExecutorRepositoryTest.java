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
<<<<<<< HEAD
import org.apache.dubbo.common.extension.ExtensionLoader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_SERVICE_COMPONENT_KEY;

public class ExecutorRepositoryTest {
    private ExecutorRepository executorRepository = ExtensionLoader.getExtensionLoader(ExecutorRepository.class).getDefaultExtension();

    @Test
    public void testGetExecutor() {
        testGet(URL.valueOf("dubbo://127.0.0.1:23456"));
        testGet(URL.valueOf("dubbo://127.0.0.1:23456?side=consumer"));

        Assertions.assertNotNull(executorRepository.getSharedExecutor());
        Assertions.assertNotNull(executorRepository.getServiceExporterExecutor());
=======
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.awaitility.Awaitility.await;

class ExecutorRepositoryTest {
    private ApplicationModel applicationModel;
    private ExecutorRepository executorRepository;

    @BeforeEach
    public void setup() {
        applicationModel = FrameworkModel.defaultModel().newApplication();
        executorRepository = ExecutorRepository.getInstance(applicationModel);
    }

    @AfterEach
    public void teardown() {
        applicationModel.destroy();
    }

    @Test
    void testGetExecutor() {
        testGet(URL.valueOf("dubbo://127.0.0.1:23456/TestService"));
        testGet(URL.valueOf("dubbo://127.0.0.1:23456/TestService?side=consumer"));

        Assertions.assertNotNull(executorRepository.getSharedExecutor());
        Assertions.assertNotNull(executorRepository.getServiceExportExecutor());
        Assertions.assertNotNull(executorRepository.getServiceReferExecutor());
>>>>>>> origin/3.2
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
<<<<<<< HEAD
    public void testGetForProviderSide() throws Exception {
        URL provider1 = URL.valueOf("dubbo://127.0.0.1:6789?side=provider");
        URL provider2 = URL.valueOf("dubbo://127.0.0.1:6790?side=provider");

        Field data = executorRepository.getClass().getDeclaredField("data");
        data.setAccessible(true);

        ExecutorService executor1 = executorRepository.createExecutorIfAbsent(provider1);
        ExecutorService executor2 = executorRepository.createExecutorIfAbsent(provider2);

        Object o = data.get(executorRepository);

        Assertions.assertTrue(o instanceof Map);
        ConcurrentMap<String, ConcurrentMap<Integer, ExecutorService>> executorMap =
                (ConcurrentMap<String, ConcurrentMap<Integer, ExecutorService>>) o;

        ConcurrentMap<Integer, ExecutorService> portExecutorMap = executorMap.get(EXECUTOR_SERVICE_COMPONENT_KEY);

        Assertions.assertNotNull(portExecutorMap.get(provider1.getPort()));
        Assertions.assertNotNull(portExecutorMap.get(provider2.getPort()));

        executor1.shutdown();
        executor2.shutdown();
    }

    @Test
    public void testGetForConsumerSide() throws Exception {
        URL consumer1 = URL.valueOf("dubbo://127.0.0.1:6789?side=consumer");
        URL consumer2 = URL.valueOf("dubbo://127.0.0.1:6790?side=consumer");

        Field data = executorRepository.getClass().getDeclaredField("data");
        data.setAccessible(true);

        ExecutorService executor1 = executorRepository.createExecutorIfAbsent(consumer1);
        ExecutorService executor2 = executorRepository.createExecutorIfAbsent(consumer2);

        Object o = data.get(executorRepository);

        Assertions.assertTrue(o instanceof Map);
        ConcurrentMap<String, ConcurrentMap<Integer, ExecutorService>> executorMap =
                (ConcurrentMap<String, ConcurrentMap<Integer, ExecutorService>>) o;

        ConcurrentMap<Integer, ExecutorService> portExecutorMap = executorMap.get(EXECUTOR_SERVICE_COMPONENT_KEY);

        Assertions.assertNull(portExecutorMap.get(consumer1.getPort()));
        Assertions.assertNull(portExecutorMap.get(consumer2.getPort()));
        Assertions.assertNotNull(portExecutorMap.get(Integer.MAX_VALUE));

        executor1.shutdown();
        executor2.shutdown();
    }

    @Test
    public void testUpdateExecutor() {
        URL url = URL.valueOf("dubbo://127.0.0.1:23456?threads=5");
=======
    void testUpdateExecutor() {
        URL url = URL.valueOf("dubbo://127.0.0.1:23456/TestService?threads=5");
>>>>>>> origin/3.2
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
<<<<<<< HEAD


=======
    }

    @Test
    void testSharedExecutor() throws Exception {
        ExecutorService sharedExecutor = executorRepository.getSharedExecutor();
        CountDownLatch latch = new CountDownLatch(3);
        CountDownLatch latch1 = new CountDownLatch(1);
        sharedExecutor.execute(()->{
            latch.countDown();
            try {
                latch1.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        sharedExecutor.execute(()->{
            latch.countDown();
            try {
                latch1.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        sharedExecutor.submit(()->{
            latch.countDown();
            try {
                latch1.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        await().until(()->latch.getCount() == 0);
        Assertions.assertEquals(3, ((ThreadPoolExecutor)sharedExecutor).getActiveCount());
        latch1.countDown();
        await().until(()->((ThreadPoolExecutor)sharedExecutor).getActiveCount() == 0);
        Assertions.assertEquals(3, ((ThreadPoolExecutor)sharedExecutor).getCompletedTaskCount());
>>>>>>> origin/3.2
    }
}
