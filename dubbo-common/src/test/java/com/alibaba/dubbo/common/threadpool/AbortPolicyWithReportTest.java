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
package com.alibaba.dubbo.common.threadpool;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.threadpool.support.AbortPolicyWithReport;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

public class AbortPolicyWithReportTest {
    @Test
    public void jStackDumpTest() throws InterruptedException {
        URL url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?dump.directory=/tmp&version=1.0.0&application=morgan&noValue");
        AbortPolicyWithReport abortPolicyWithReport = new AbortPolicyWithReport("Test", url);

        try {
            abortPolicyWithReport.rejectedExecution(new Runnable() {
                @Override
                public void run() {
                    System.out.println("hello");
                }
            }, (ThreadPoolExecutor) Executors.newFixedThreadPool(1));
        }catch (RejectedExecutionException rj){

        }

        Thread.currentThread().sleep(1000);

    }
}