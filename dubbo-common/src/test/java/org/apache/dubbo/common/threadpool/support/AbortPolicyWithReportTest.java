/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.common.threadpool.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.threadpool.event.ThreadPoolExhaustedEvent;
import org.apache.dubbo.common.threadpool.event.ThreadPoolExhaustedListener;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.dubbo.common.constants.CommonConstants.OS_NAME_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.OS_WIN_PREFIX;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AbortPolicyWithReportTest {
    @Test
    void jStackDumpTest() throws InterruptedException {
        URL url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?dump.directory=/tmp&version=1.0.0&application=morgan&noValue=");
        AtomicReference<FileOutputStream> fileOutputStream = new AtomicReference<>();

        AbortPolicyWithReport abortPolicyWithReport = new AbortPolicyWithReport("Test", url) {
            @Override
            protected void jstack(FileOutputStream jStackStream) throws Exception {
                fileOutputStream.set(jStackStream);
            }
        };
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        AbortPolicyWithReport.lastPrintTime = 0;
        Assertions.assertThrows(RejectedExecutionException.class, () -> {
            abortPolicyWithReport.rejectedExecution(() -> System.out.println("hello"), (ThreadPoolExecutor) executorService);
        });

        await().until(() -> AbortPolicyWithReport.guard.availablePermits() == 1);
        Assertions.assertNotNull(fileOutputStream.get());
    }

    @Test
    void jStackDumpTest_dumpDirectoryNotExists_cannotBeCreatedTakeUserHome() throws InterruptedException {
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
            // "con" is one of Windows reserved names, https://docs.microsoft.com/en-us/windows/win32/fileio/naming-a-file
            return "con";
        } else {
            return "/dev/full/" + UUID.randomUUID().toString();
        }
    }

    @Test
    void jStackDumpTest_dumpDirectoryNotExists_canBeCreated() throws InterruptedException {
        final String dumpDirectory = UUID.randomUUID().toString();

        URL url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?dump.directory="
            + dumpDirectory
            + "&version=1.0.0&application=morgan&noValue=true");
        AbortPolicyWithReport abortPolicyWithReport = new AbortPolicyWithReport("Test", url);

        Assertions.assertNotEquals(System.getProperty("user.home"), abortPolicyWithReport.getDumpPath());
    }

    @Test
    void test_dispatchThreadPoolExhaustedEvent() {
        URL url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?dump.directory=/tmp&version=1.0.0&application=morgan&noValue=");
        AbortPolicyWithReport abortPolicyWithReport = new AbortPolicyWithReport("Test", url);
        String msg = "Thread pool is EXHAUSTED! Thread Name: DubboServerHandler-127.0.0.1:12345, Pool Size: 1 (active: 0, core: 1, max: 1, largest: 1), Task: 6 (completed: 6), Executor status:(isShutdown:false, isTerminated:false, isTerminating:false), in dubbo://127.0.0.1:12345!, dubbo version: 2.7.3, current host: 127.0.0.1";
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
