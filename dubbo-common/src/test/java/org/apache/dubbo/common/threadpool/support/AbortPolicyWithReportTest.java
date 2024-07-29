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
package org.apache.dubbo.common.threadpool.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.threadlocal.NamedInternalThreadFactory;
import org.apache.dubbo.common.threadpool.event.ThreadPoolExhaustedEvent;
import org.apache.dubbo.common.threadpool.event.ThreadPoolExhaustedListener;

import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.OS_NAME_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.OS_WIN_PREFIX;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AbortPolicyWithReportTest {
    @BeforeEach
    public void setUp() {
        AbortPolicyWithReport.lastPrintTime = 0;
    }

    @Test
    void jStackDumpTest() {
        URL url = URL.valueOf(
                "dubbo://admin:hello1234@10.20.130.230:20880/context/path?dump.directory=/tmp&version=1.0.0&application=morgan&noValue=");
        AtomicReference<FileOutputStream> fileOutputStream = new AtomicReference<>();

        AbortPolicyWithReport abortPolicyWithReport = new AbortPolicyWithReport("Test", url) {
            @Override
            protected void jstack(FileOutputStream jStackStream) {
                fileOutputStream.set(jStackStream);
            }
        };
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        AbortPolicyWithReport.lastPrintTime = 0;
        Assertions.assertThrows(RejectedExecutionException.class, () -> {
            abortPolicyWithReport.rejectedExecution(
                    () -> System.out.println("hello"), (ThreadPoolExecutor) executorService);
        });

        await().until(() -> AbortPolicyWithReport.guard.availablePermits() == 1);
        Assertions.assertNotNull(fileOutputStream.get());
    }

    @Test
    void jStack_ConcurrencyDump_Silence_10Min() {
        URL url = URL.valueOf(
                "dubbo://admin:hello1234@10.20.130.230:20880/context/path?dump.directory=/tmp&version=1.0.0&application=morgan&noValue=");
        AtomicInteger jStackCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger finishedCount = new AtomicInteger(0);
        AtomicInteger timeoutCount = new AtomicInteger(0);
        AbortPolicyWithReport abortPolicyWithReport = new AbortPolicyWithReport("Test", url) {
            @Override
            protected void jstack(FileOutputStream jStackStream) {
                jStackCount.incrementAndGet();
                // try to simulate the jstack cost long time, so that AbortPolicyWithReport may jstack repeatedly.
                long startTime = System.currentTimeMillis();
                await().atLeast(200, TimeUnit.MILLISECONDS).until(() -> System.currentTimeMillis() - startTime >= 300);
            }
        };
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                4,
                4,
                0,
                TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(),
                new NamedInternalThreadFactory("jStack_ConcurrencyDump_Silence_10Min", false),
                abortPolicyWithReport);
        int runTimes = 100;
        List<Future<?>> futureList = new LinkedList<>();
        for (int i = 0; i < runTimes; i++) {
            try {
                futureList.add(threadPoolExecutor.submit(() -> {
                    finishedCount.incrementAndGet();
                    long start = System.currentTimeMillis();
                    // try to await 1s to make sure jstack dump thread scheduled
                    await().atLeast(300, TimeUnit.MILLISECONDS).until(() -> System.currentTimeMillis() - start >= 300);
                }));
            } catch (Exception ignored) {
                failureCount.incrementAndGet();
            }
        }
        futureList.forEach(f -> {
            try {
                f.get(500, TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {
                timeoutCount.incrementAndGet();
            }
        });

        System.out.printf(
                "jStackCount: %d, finishedCount: %d, failureCount: %d, timeoutCount: %d %n",
                jStackCount.get(), finishedCount.get(), failureCount.get(), timeoutCount.get());
        Assertions.assertEquals(
                runTimes, finishedCount.get() + failureCount.get(), "all the test thread should be run completely");
        Assertions.assertEquals(1, jStackCount.get(), "'jstack' should be called only once in 10 minutes");
    }

    @Test
    void jStackDumpTest_dumpDirectoryNotExists_cannotBeCreatedTakeUserHome() {
        final String dumpDirectory = dumpDirectoryCannotBeCreated();

        URL url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?dump.directory="
                + dumpDirectory
                + "&version=1.0.0&application=morgan&noValue=true");
        AbortPolicyWithReport abortPolicyWithReport = new AbortPolicyWithReport("Test", url);

        Assertions.assertEquals(System.getProperty("user.home"), abortPolicyWithReport.getDumpPath());
    }

    private String dumpDirectoryCannotBeCreated() {
        final String os = System.getProperty(OS_NAME_KEY).toLowerCase();
        if (os.contains(OS_WIN_PREFIX)) {
            // "con" is one of Windows reserved names,
            // https://docs.microsoft.com/en-us/windows/win32/fileio/naming-a-file
            return "con";
        } else {
            return "/dev/full/" + UUID.randomUUID().toString();
        }
    }

    @Test
    void jStackDumpTest_dumpDirectoryNotExists_canBeCreated() {
        final String dumpDirectory = UUID.randomUUID().toString();

        URL url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?dump.directory="
                + dumpDirectory
                + "&version=1.0.0&application=morgan&noValue=true");
        AbortPolicyWithReport abortPolicyWithReport = new AbortPolicyWithReport("Test", url);

        Assertions.assertNotEquals(System.getProperty("user.home"), abortPolicyWithReport.getDumpPath());
    }

    @Test
    void test_dispatchThreadPoolExhaustedEvent() {
        URL url = URL.valueOf(
                "dubbo://admin:hello1234@10.20.130.230:20880/context/path?dump.directory=/tmp&version=1.0.0&application=morgan&noValue=");
        AbortPolicyWithReport abortPolicyWithReport = new AbortPolicyWithReport("Test", url);
        String msg =
                "Thread pool is EXHAUSTED! Thread Name: DubboServerHandler-127.0.0.1:12345, Pool Size: 1 (active: 0, core: 1, max: 1, largest: 1), Task: 6 (completed: 6), Executor status:(isShutdown:false, isTerminated:false, isTerminating:false), in dubbo://127.0.0.1:12345!, dubbo version: 2.7.3, current host: 127.0.0.1";
        MyListener listener = new MyListener();
        abortPolicyWithReport.addThreadPoolExhaustedEventListener(listener);
        abortPolicyWithReport.dispatchThreadPoolExhaustedEvent(msg);

        assertEquals(listener.getThreadPoolExhaustedEvent().getMsg(), msg);
    }

    static class MyListener implements ThreadPoolExhaustedListener {

        private ThreadPoolExhaustedEvent threadPoolExhaustedEvent;

        @Override
        public void onEvent(ThreadPoolExhaustedEvent event) {
            this.threadPoolExhaustedEvent = event;
        }

        public ThreadPoolExhaustedEvent getThreadPoolExhaustedEvent() {
            return threadPoolExhaustedEvent;
        }
    }
}
