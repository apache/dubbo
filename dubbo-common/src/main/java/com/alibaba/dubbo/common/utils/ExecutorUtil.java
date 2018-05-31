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
package com.alibaba.dubbo.common.utils;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExecutorUtil {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorUtil.class);

    private static final ThreadPoolExecutor shutdownExecutor = new ThreadPoolExecutor(0, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(100),
            new NamedThreadFactory("Close-ExecutorService-Timer", true));

    public static boolean isShutdown(Executor executor) {
        if (executor instanceof ExecutorService) {
            return ((ExecutorService) executor).isShutdown();
        }
        return false;
    }

    public static void gracefulShutdown(Executor executor, int timeout) {
        // 忽略，若不是 ExecutorService ，或者已经关闭
        if (!(executor instanceof ExecutorService) || isShutdown(executor)) {
            return;
        }
        // 关闭，禁止新的任务提交，将原有任务执行完
        final ExecutorService es = (ExecutorService) executor;
        try {
            es.shutdown(); // Disable new tasks from being submitted
        } catch (SecurityException ex2) {
            return;
        } catch (NullPointerException ex2) {
            return;
        }
        // 等待原有任务执行完。若等待超时，强制结束所有任务
        try {
            if (!es.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                es.shutdownNow();
            }
        } catch (InterruptedException ex) {
            // 发生 InterruptedException 异常，也强制结束所有任务
            es.shutdownNow();
            Thread.currentThread().interrupt();
        }
        // 若未关闭成功，新开线程去关闭
        if (!isShutdown(es)) {
            newThreadToCloseExecutor(es);
        }
    }

    public static void shutdownNow(Executor executor, final int timeout) {
        // 忽略，若不是 ExecutorService ，或者已经关闭
        if (!(executor instanceof ExecutorService) || isShutdown(executor)) {
            return;
        }
        // 立即关闭，包括原有任务也打断
        final ExecutorService es = (ExecutorService) executor;
        try {
            es.shutdownNow();
        } catch (SecurityException ex2) {
            return;
        } catch (NullPointerException ex2) {
            return;
        }
        // 等待原有任务被打断完成
        try {
            es.awaitTermination(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        // 若未关闭成功，新开线程去关闭
        if (!isShutdown(es)) {
            newThreadToCloseExecutor(es);
        }
    }

    private static void newThreadToCloseExecutor(final ExecutorService es) {
        if (!isShutdown(es)) {
            shutdownExecutor.execute(new Runnable() {
                public void run() {
                    try {
                        // 循环 1000 次，不断强制结束线程池
                        for (int i = 0; i < 1000; i++) {
                            // 立即关闭，包括原有任务也打断
                            es.shutdownNow();
                            // 等待原有任务被打断完成
                            if (es.awaitTermination(10, TimeUnit.MILLISECONDS)) {
                                break;
                            }
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    } catch (Throwable e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            });
        }
    }

    /**
     * append thread name with url address
     *
     * @return new url with updated thread name
     */
    public static URL setThreadName(URL url, String defaultName) {
        String name = url.getParameter(Constants.THREAD_NAME_KEY, defaultName);
        name = new StringBuilder(32).append(name).append("-").append(url.getAddress()).toString();
        url = url.addParameter(Constants.THREAD_NAME_KEY, name);
        return url;
    }

}