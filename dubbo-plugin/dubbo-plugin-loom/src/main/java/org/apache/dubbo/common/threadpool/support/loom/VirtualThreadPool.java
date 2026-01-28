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
package org.apache.dubbo.common.threadpool.support.loom;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.threadpool.ThreadPool;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_THREAD_NAME;
import static org.apache.dubbo.common.constants.CommonConstants.THREADS_VIRTUAL_CORE;
import static org.apache.dubbo.common.constants.CommonConstants.THREAD_NAME_KEY;

/**
 * Creates a thread pool that use virtual thread
 *
 * @see Executors#newVirtualThreadPerTaskExecutor()
 */
public class VirtualThreadPool implements ThreadPool {
    @Override
    public Executor getExecutor(URL url) {
        String name =
                url.getParameter(THREAD_NAME_KEY, (String) url.getAttribute(THREAD_NAME_KEY, DEFAULT_THREAD_NAME));
        int threads = url.getParameter(THREADS_VIRTUAL_CORE, 0);
        if (threads > 0) {
            return new ThreadPoolExecutor(
                    threads,
                    Integer.MAX_VALUE,
                    0L,
                    java.util.concurrent.TimeUnit.MILLISECONDS,
                    new SynchronousQueue<>(),
                    Thread.ofVirtual().name(name, 1).factory());
        } else {
            return Executors.newThreadPerTaskExecutor(
                    Thread.ofVirtual().name(name, 1).factory());
        }
    }
}
