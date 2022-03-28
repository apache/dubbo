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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private final List<Disposable> oneoffDisposables = new CopyOnWriteArrayList<>();
    private static final List<Disposable> globalReusedDisposables = new CopyOnWriteArrayList<>();

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

    /**
     * Register a global reused disposable. The disposable will be executed when all dubbo FrameworkModels are destroyed.
     * Note: the global disposable should be registered in static code, it's reusable and will not be removed when dubbo shutdown.
     * @param disposable
     */
    public static void registerGlobalDisposable(Disposable disposable) {
        synchronized (GlobalResourcesRepository.class) {
            if (!globalReusedDisposables.contains(disposable)) {
                globalReusedDisposables.add(disposable);
            }
        }
    }

    public void removeGlobalDisposable(Disposable disposable) {
        synchronized (GlobalResourcesRepository.class) {
            this.globalReusedDisposables.remove(disposable);
        }
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

    public synchronized void destroy() {
        if (logger.isInfoEnabled()) {
            logger.info("Destroying global resources ...");
        }
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }

        // call one-off disposables
        for (Disposable disposable : new ArrayList<>(oneoffDisposables)) {
            try {
                disposable.destroy();
            } catch (Exception e) {
                logger.warn("destroy resources failed: " + e.getMessage(), e);
            }
        }
        // clear one-off disposable
        oneoffDisposables.clear();

        // call global disposable, don't clear globalReusedDisposables for reuse purpose
        for (Disposable disposable : new ArrayList<>(globalReusedDisposables)) {
            try {
                disposable.destroy();
            } catch (Exception e) {
                logger.warn("destroy resources failed: " + e.getMessage(), e);
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info("Dubbo is completely destroyed");
        }
    }

    /**
     * Register a one-off disposable, the disposable is removed automatically on first shutdown.
     * @param disposable
     */
    public synchronized void registerDisposable(Disposable disposable) {
        if (!oneoffDisposables.contains(disposable)) {
            oneoffDisposables.add(disposable);
        }
    }

    public synchronized void removeDisposable(Disposable disposable) {
        this.oneoffDisposables.remove(disposable);
    }


    // for test
    public static List<Disposable> getGlobalReusedDisposables() {
        return globalReusedDisposables;
    }

    // for test
    public List<Disposable> getOneoffDisposables() {
        return oneoffDisposables;
    }
}
