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


package org.apache.dubbo.common.threadpool.affinity;

import org.apache.dubbo.common.extension.ExtensionAccessor;
import org.apache.dubbo.common.extension.ExtensionAccessorAware;
import org.apache.dubbo.common.threadlocal.NamedInternalThreadFactory;
import org.apache.dubbo.common.threadpool.manager.DefaultExecutorRepository;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public abstract class AbstractKeyAffinityExecutor<K> implements KeyAffinityExecutor<K>, ExtensionAccessorAware, ScopeModelAware {

    public static final int DEFAULT_QUEUE_SIZE = 1024;

    private final KeyAffinity<K, ExecutorService> keyAffinity;

    public AbstractKeyAffinityExecutor(KeyAffinity<K, ExecutorService> keyAffinity) {
        this.keyAffinity = keyAffinity;
    }

    private ExtensionAccessor extensionAccessor;

    private ApplicationModel applicationModel;

    @Override
    public void setExtensionAccessor(ExtensionAccessor extensionAccessor) {
        this.extensionAccessor = extensionAccessor;
    }

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }


    @Override
    public void execute(K key, Runnable runnable) {
        ExecutorService service = keyAffinity.select(key);
        service.submit(() -> {
            try {
                runnable.run();
            } finally {
                keyAffinity.finishCall(key);
            }
        });
    }

    public ExecutorService getExecutorService(K key) {
        return keyAffinity.select(key);
    }

    @Override
    public <T> Future<T> submit(K key, Callable<T> callable) {
        ExecutorService service = keyAffinity.select(key);
        return service.submit(() -> {
            try {
                return callable.call();
            } finally {
                keyAffinity.finishCall(key);
            }
        });
    }

    @Override
    public void destroyAll() {
        for (ExecutorService service : keyAffinity.getAllVal()) {
            if (service != null) {
                service.shutdown();
            }
        }
    }

    @Override
    public void destroy(K key) {
        ExecutorService service = keyAffinity.select(key, false);
        if (service != null) {
            service.shutdown();
        }
    }

    public static <K> KeyAffinityExecutor<K> newRandomAffinityExecutor() {
        ThreadFactory threadFactory = new NamedInternalThreadFactory("Dubbo-random-key-affinity", true);
        Supplier<ExecutorService> serviceSupplier = AbstractKeyAffinityExecutor.getExecutorService(threadFactory, DEFAULT_QUEUE_SIZE);
        return new RandomKeyAffinityExecutor<>(serviceSupplier, DefaultExecutorRepository.DEFAULT_SCHEDULER_SIZE);
    }

    public static <K> KeyAffinityExecutor<K> newMinActiveAffinityExecutor() {
        ThreadFactory threadFactory = new NamedInternalThreadFactory("Dubbo-min-active-key-affinity", true);
        Supplier<ExecutorService> serviceSupplier = AbstractKeyAffinityExecutor.getExecutorService(threadFactory, DEFAULT_QUEUE_SIZE);
        return new MinActiveKeyAffinityExecutor<>(serviceSupplier, DefaultExecutorRepository.DEFAULT_SCHEDULER_SIZE);
    }


    public static Supplier<ExecutorService> getExecutorService(ThreadFactory threadFactory, int queueBufferSize) {
        return () -> {
            LinkedBlockingQueue<Runnable> queue;
            if (queueBufferSize > 0) {
                queue = new LinkedBlockingQueue<Runnable>(queueBufferSize) {
                    @Override
                    public boolean offer(Runnable e) {
                        try {
                            put(e);
                            return true;
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        return false;
                    }
                };
            } else {
                queue = new LinkedBlockingQueue<>();
            }
            return new ThreadPoolExecutor(1, 1,
                    0, TimeUnit.SECONDS, queue, threadFactory,
                    new ThreadPoolExecutor.AbortPolicy());
        };
    }
}