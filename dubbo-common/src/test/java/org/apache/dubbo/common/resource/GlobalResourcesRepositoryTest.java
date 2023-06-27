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
package org.apache.dubbo.common.resource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;

/**
 * {@link GlobalResourcesRepository}
 */
class GlobalResourcesRepositoryTest {

    @Test
    void test() {
        GlobalResourcesRepository repository = GlobalResourcesRepository.getInstance();

        ExecutorService globalExecutorService = GlobalResourcesRepository.getGlobalExecutorService();
        Assertions.assertNotNull(globalExecutorService);

        GlobalDisposable globalDisposable = new GlobalDisposable();
        GlobalResourcesRepository.registerGlobalDisposable(globalDisposable);

        OneOffDisposable oneOffDisposable = new OneOffDisposable();
        repository.registerDisposable(oneOffDisposable);

        repository.destroy();
        Assertions.assertTrue(globalExecutorService.isShutdown());
        Assertions.assertTrue(globalDisposable.isDestroyed());
        Assertions.assertTrue(oneOffDisposable.isDestroyed());
        Assertions.assertTrue(!GlobalResourcesRepository.getGlobalReusedDisposables().isEmpty());
        Assertions.assertTrue(GlobalResourcesRepository.getGlobalReusedDisposables().contains(globalDisposable));
        Assertions.assertTrue(repository.getOneoffDisposables().isEmpty());
    }

    class GlobalDisposable implements Disposable {

        boolean destroyed = false;

        @Override
        public void destroy() {
            destroyed = true;
        }

        public boolean isDestroyed() {
            return destroyed;
        }
    }

    class OneOffDisposable implements Disposable {
        boolean destroyed = false;

        @Override
        public void destroy() {
            destroyed = true;
        }

        public boolean isDestroyed() {
            return destroyed;
        }
    }
}
