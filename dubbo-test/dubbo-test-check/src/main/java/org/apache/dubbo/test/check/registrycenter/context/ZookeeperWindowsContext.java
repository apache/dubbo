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
package org.apache.dubbo.test.check.registrycenter.context;

import org.apache.dubbo.common.threadlocal.NamedInternalThreadFactory;

import org.apache.commons.exec.ExecuteWatchdog;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The global context for zookeeper on Windows OS.
 */
public class ZookeeperWindowsContext extends ZookeeperContext {

    /**
     * The default executor service to manage the lifecycle of zookeeper.
     */
    private final ExecutorService DEFAULT_EXECUTOR_SERVICE = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS,
        new SynchronousQueue<>(),
        new NamedInternalThreadFactory("mocked-zookeeper", true), new ThreadPoolExecutor.AbortPolicy());

    /**
     * Define the default {@link ExecuteWatchdog} for terminating all registered zookeeper processes.
     */
    private final ExecuteWatchdog WATCHDOG = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);

    /**
     * The map to store the pair of clientPort and PID.
     */
    private Map<Integer, Integer> processIds = new HashMap<>();

    /**
     * Register the process id of zookeeper.
     *
     * @param clientPort the client port of zookeeper.
     * @param pid        the process id of zookeeper instance.
     */
    public void register(int clientPort, int pid) {
        this.processIds.put(clientPort, pid);
    }

    /**
     * Returns the pid of zookeeper instance with the given client port.
     *
     * @param clientPort the client port of zookeeper instance.
     * @return the pid of zookeeper instance.
     */
    public Integer getPid(int clientPort) {
        return this.processIds.get(clientPort);
    }

    /**
     * Remove the registered pid with the given client port.
     * @param clientPort the client port of zookeeper instance.
     */
    public void removePid(int clientPort){
        this.processIds.remove(clientPort);
    }

    /**
     * Returns the default executor service to manage the lifecycle of zookeeper.
     */
    public ExecutorService getExecutorService() {
        return DEFAULT_EXECUTOR_SERVICE;
    }

    /**
     * Returns the {@link ExecuteWatchdog}.
     */
    public ExecuteWatchdog getWatchdog() {
        return WATCHDOG;
    }

    /**
     * Destroy all registered resources.
     */
    public void destroy() {
        this.processIds.clear();
        this.WATCHDOG.destroyProcess();
        try {
            DEFAULT_EXECUTOR_SERVICE.shutdownNow();
        } catch (SecurityException | NullPointerException ex) {
            return;
        }
        try {
            DEFAULT_EXECUTOR_SERVICE.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
