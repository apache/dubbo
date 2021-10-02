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

package org.apache.dubbo.common.threadpool;

import org.apache.dubbo.common.threadpool.affinity.AbstractKeyAffinityExecutor;
import org.apache.dubbo.common.threadpool.affinity.KeyAffinityExecutor;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RandomKeyAffinityExecutorTest {

    private static KeyAffinityExecutor<String> keyAffinityExecutor;

    @BeforeAll
    public static void before() {
        keyAffinityExecutor = AbstractKeyAffinityExecutor.newRandomAffinityExecutor();
    }

    @Test
    void test1() {
        for (int i = 0; i < 1000; i++) {
            final String val = i % 5 + "";
            keyAffinityExecutor.execute(val, () -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("thread name=" + Thread.currentThread().getName() + " " + val);
            });
        }
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < 1000; i++) {
            final String val = i % 5 + "";
            keyAffinityExecutor.execute(val, () -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("thread name=" + Thread.currentThread().getName() + " " + val);
            });
        }
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    @AfterAll
    public static void after() {
        keyAffinityExecutor.destroyAll();
    }


}