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
package org.apache.dubbo.registry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NetUtils;

import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_UNDEFINED_ARGUMENT;

/**
 * RegistryPerformanceTest
 */
class PerformanceRegistryTest {

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(PerformanceRegistryTest.class);

    @Test
    void runRegistryTest() {
        String serverProperty = PerformanceUtils.getProperty("server", null);
        if (serverProperty == null) {
            logger.warn(CONFIG_UNDEFINED_ARGUMENT, "", "", "Please set -Dserver=127.0.0.1:9090");
            return;
        }

        final int base = PerformanceUtils.getIntProperty("base", 0);
        final int concurrentThreads = PerformanceUtils.getIntProperty("concurrent", 100);
        int runCount = PerformanceUtils.getIntProperty("runs", 1000);
        boolean isRunsValid = runCount > 0;
        final int runs = isRunsValid ? runCount : Integer.MAX_VALUE;

        final Registry registry = createRegistry(serverProperty);

        startConcurrentThreads(concurrentThreads, runs, registry, base);

        waitForCompletion();
    }

    private Registry createRegistry(String serverProperty) {
        return ExtensionLoader.getExtensionLoader(RegistryFactory.class)
                .getAdaptiveExtension()
                .getRegistry(URL.valueOf("remote://admin:hello1234@" + serverProperty));
    }

    private void startConcurrentThreads(int concurrentThreads, int runs, Registry registry, int base) {
        for (int threadId = 0; threadId < concurrentThreads; threadId++) {
            final int threadIndex = threadId;
            new Thread(() -> registerUrls(runs, registry, base, threadIndex)).start();
        }
    }

    private void registerUrls(int runs, Registry registry, int base, int threadIndex) {
        for (int runIndex = 0; runIndex < runs; runIndex++) {
            String url = "remote://" + NetUtils.getLocalHost() + ":8080/demoService" + threadIndex + "_" + runIndex
                    + "?version=1.0.0&application=demo&dubbo=2.0&interface="
                    + "org.apache.dubbo.demo.DemoService" + (base + threadIndex) + "_" + (base + runIndex);
            registry.register(URL.valueOf(url));
        }
    }

    private void waitForCompletion() {
        synchronized (PerformanceRegistryTest.class) {
            while (true) {
                try {
                    PerformanceRegistryTest.class.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
