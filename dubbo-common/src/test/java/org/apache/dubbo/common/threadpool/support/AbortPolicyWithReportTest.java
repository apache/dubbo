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
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

public class AbortPolicyWithReportTest {
    @Test
    public void jStackDumpTest() throws InterruptedException {
        URL url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?dump.directory=/tmp&version=1.0.0&application=morgan&noValue");
        AbortPolicyWithReport abortPolicyWithReport = new AbortPolicyWithReport("Test", url);

        try {
            abortPolicyWithReport.rejectedExecution(() -> System.out.println("hello"), (ThreadPoolExecutor) Executors.newFixedThreadPool(1));
        } catch (RejectedExecutionException rj) {
            // ignore
        }

        Thread.sleep(1000);

    }

    @Test
    public void jStackDumpTest_dumpDirectoryNotExists_cannotBeCreatedTakeUserHome() throws InterruptedException {
        final String dumpDirectory = dumpDirectoryCannotBeCreated();

        URL url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?dump.directory="
                + dumpDirectory
                + "&version=1.0.0&application=morgan&noValue");
        AbortPolicyWithReport abortPolicyWithReport = new AbortPolicyWithReport("Test", url);

        try {
            abortPolicyWithReport.rejectedExecution(new Runnable() {
                @Override
                public void run() {
                    System.out.println("hello");
                }
            }, (ThreadPoolExecutor) Executors.newFixedThreadPool(1));
        } catch (RejectedExecutionException rj) {
            // ignore
        }

        Thread.sleep(1000);
    }

    private String dumpDirectoryCannotBeCreated() {
        final String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // "con" is one of Windows reserved names, https://docs.microsoft.com/en-us/windows/win32/fileio/naming-a-file
            return "con";
        } else {
            return "/dev/full/" + UUID.randomUUID().toString();
        }
    }

    @Test
    public void jStackDumpTest_dumpDirectoryNotExists_canBeCreated() throws InterruptedException {
        final String dumpDirectory = UUID.randomUUID().toString();

        URL url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?dump.directory="
                + dumpDirectory
                + "&version=1.0.0&application=morgan&noValue");
        AbortPolicyWithReport abortPolicyWithReport = new AbortPolicyWithReport("Test", url);

        try {
            abortPolicyWithReport.rejectedExecution(new Runnable() {
                @Override
                public void run() {
                    System.out.println("hello");
                }
            }, (ThreadPoolExecutor) Executors.newFixedThreadPool(1));
        } catch (RejectedExecutionException rj) {
            // ignore
        }

        Thread.sleep(1000);
    }
}