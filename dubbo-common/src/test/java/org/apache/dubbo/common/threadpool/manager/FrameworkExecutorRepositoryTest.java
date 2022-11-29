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

import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.awaitility.Awaitility.await;

class FrameworkExecutorRepositoryTest {
    private FrameworkModel frameworkModel;
    private FrameworkExecutorRepository frameworkExecutorRepository;

    @BeforeEach
    public void setup() {
        frameworkModel = new FrameworkModel();
        frameworkExecutorRepository = frameworkModel.getBeanFactory().getBean(FrameworkExecutorRepository.class);
    }

    @AfterEach
    public void teardown() {
        frameworkModel.destroy();
    }

    @Test
    void testGetExecutor() {

        Assertions.assertNotNull(frameworkExecutorRepository.getSharedExecutor());
        frameworkExecutorRepository.nextScheduledExecutor();
    }

    @Test
    void testSharedExecutor() throws Exception {
        ExecutorService sharedExecutor = frameworkExecutorRepository.getSharedExecutor();
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
    }
}
