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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NamedThreadFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Global resources repository between all framework models.
 * It will be destroyed only after all framework model is destroyed.
 */
public class GlobalResourcesRepository {

    private static final Logger logger = LoggerFactory.getLogger(GlobalResourcesRepository.class);

    private volatile static GlobalResourcesRepository instance;
    private volatile ExecutorService executorService;
    private List<Disposable> disposables = Collections.synchronizedList(new ArrayList<>());

    private GlobalResourcesRepository() {
    }

    public static GlobalResourcesRepository getInstance() {
        if (instance == null) {
            synchronized (GlobalResourcesRepository.class) {
                if (instance == null) {
                    instance = new GlobalResourcesRepository();
                }
            }
        }
        return instance;
    }

    public static ExecutorService getGlobalExecutorService() {
        return getInstance().getExecutorService();
    }

    public ExecutorService getExecutorService() {
        if (executorService == null || executorService.isShutdown()) {
            synchronized (this) {
                if (executorService == null || executorService.isShutdown()) {
                    executorService = Executors.newCachedThreadPool(new NamedThreadFactory("Dubbo-global-shared-handler", true));
                }
            }
        }
        return executorService;
    }

    public void destroy() {
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }

        // notify disposables
        for (Disposable disposable : new ArrayList<>(disposables)) {
            try {
                disposable.destroy();
            } catch (Exception e) {
                logger.warn("destroy resources failed: " + e.getMessage(), e);
            }
        }
        // don't clear disposables for reuse purpose
    }

    public void addDisposable(Disposable disposable) {
        if (!this.disposables.contains(disposable)) {
            this.disposables.add(disposable);
        }
    }

    public void removeDisposable(Disposable disposable) {
        this.disposables.remove(disposable);
    }

}
