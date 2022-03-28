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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class FrameworkExecutorRepositoryTest {
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
    public void testGetExecutor() {

        Assertions.assertNotNull(frameworkExecutorRepository.getSharedExecutor());
        frameworkExecutorRepository.nextScheduledExecutor();
    }

    @Test
    public void testSharedExecutor() throws Exception {
        ExecutorService sharedExecutor = frameworkExecutorRepository.getSharedExecutor();
        FrameworkExecutorRepositoryTest.MockTask task1 = new FrameworkExecutorRepositoryTest.MockTask(2000);
        FrameworkExecutorRepositoryTest.MockTask task2 = new FrameworkExecutorRepositoryTest.MockTask(100);
        FrameworkExecutorRepositoryTest.MockTask task3 = new FrameworkExecutorRepositoryTest.MockTask(200);
        sharedExecutor.execute(task1);
        sharedExecutor.execute(task2);
        sharedExecutor.submit(task3);

        Thread.sleep(150);
        Assertions.assertTrue(task1.isRunning());
        Assertions.assertFalse(task1.isDone());
        Assertions.assertTrue(task2.isRunning());
        Assertions.assertTrue(task2.isDone());
        Assertions.assertTrue(task3.isRunning());
        Assertions.assertFalse(task3.isDone());

        Thread.sleep(200);
        Assertions.assertTrue(task3.isDone());
        Assertions.assertFalse(task1.isDone());
    }

    private static class MockTask implements Runnable {
        private long waitTimeMS;
        private AtomicBoolean running = new AtomicBoolean();
        private AtomicBoolean done = new AtomicBoolean();

        public MockTask(long waitTimeMS) {
            this.waitTimeMS = waitTimeMS;
        }

        @Override
        public void run() {
            running.set(true);
            try {
                Thread.sleep(waitTimeMS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            done.set(true);
        }

        public boolean isDone() {
            return done.get();
        }

        public boolean isRunning() {
            return running.get();
        }
    }
}
