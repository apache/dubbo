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
package org.apache.dubbo.common.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MD5UtilsTest {

    @Test
    public void test() {
        MD5Utils sharedMd5Utils = new MD5Utils();
        final String[] input = {"provider-appgroup-one/org.apache.dubbo.config.spring.api.HelloService:dubboorg.apache.dubbo.config.spring.api.HelloService{REGISTRY_CLUSTER=registry-one, anyhost=true, application=provider-app, background=false, compiler=javassist, deprecated=false, dubbo=2.0.2, dynamic=true, file.cache=false, generic=false, group=group-one, interface=org.apache.dubbo.config.spring.api.HelloService, logger=slf4j, metadata-type=remote, methods=sayHello, organization=test, owner=com.test, release=, service-name-mapping=true, side=provider}",
            "provider-appgroup-two/org.apache.dubbo.config.spring.api.DemoService:dubboorg.apache.dubbo.config.spring.api.DemoService{REGISTRY_CLUSTER=registry-two, anyhost=true, application=provider-app, background=false, compiler=javassist, deprecated=false, dubbo=2.0.2, dynamic=true, file.cache=false, generic=false, group=group-two, interface=org.apache.dubbo.config.spring.api.DemoService, logger=slf4j, metadata-type=remote, methods=sayName,getBox, organization=test, owner=com.test, release=, service-name-mapping=true, side=provider}"};
        final String[] result = {sharedMd5Utils.getMd5(input[0]), new MD5Utils().getMd5(input[1])};

        System.out.println("Expected result: " + Arrays.asList(result));
        int nThreads = 8;
        CountDownLatch latch = new CountDownLatch(nThreads);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        try {

            for (int i = 0; i < nThreads; i++) {
                MD5Utils md5Utils = i < nThreads / 2 ? sharedMd5Utils : new MD5Utils();
                executorService.submit(new Md5Task(input[i % 2], result[i % 2], md5Utils, latch, errors));
            }
            latch.await();
            Assertions.assertEquals(Collections.EMPTY_LIST, errors);
            Assertions.assertEquals(0, latch.getCount());
        } catch (Throwable e) {
            Assertions.fail(StringUtils.toString(e));
        } finally {
            executorService.shutdown();
        }
    }

    static class Md5Task implements Runnable {

        private final String input;
        private final String expected;
        private final MD5Utils md5Utils;
        private final CountDownLatch latch;
        private final List<Throwable> errorCollector;

        public Md5Task(String input, String expected, MD5Utils md5Utils, CountDownLatch latch, List<Throwable> errorCollector) {
            this.input = input;
            this.expected = expected;
            this.md5Utils = md5Utils;
            this.latch = latch;
            this.errorCollector = errorCollector;
        }

        @Override
        public void run() {
            int i = 0;
            long start = System.currentTimeMillis();
            try {
                for (; i < 200; i++) {
                    Assertions.assertEquals(expected, md5Utils.getMd5(input));
                    md5Utils.getMd5("test#" + i);
                }
            } catch (Throwable e) {
                errorCollector.add(e);
                e.printStackTrace();
            } finally {
                long cost = System.currentTimeMillis() - start;
                System.out.println("[" + Thread.currentThread().getName() + "] progress: " + i + ", cost: " + cost);
                latch.countDown();
            }
        }
    }
}
