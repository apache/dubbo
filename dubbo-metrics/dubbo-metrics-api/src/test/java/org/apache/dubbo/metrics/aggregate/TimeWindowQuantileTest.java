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
package org.apache.dubbo.metrics.aggregate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class TimeWindowQuantileTest {

    @Test
    void test() {
        TimeWindowQuantile quantile = new TimeWindowQuantile(100, 10, 1);
        for (int i = 1; i <= 100; i++) {
            quantile.add(i);
        }

        Assertions.assertEquals(quantile.quantile(0.01), 2);
        Assertions.assertEquals(quantile.quantile(0.99), 100);
    }

    @Test
    @RepeatedTest(100)
    void testMulti() {

        ExecutorService executorService = Executors.newFixedThreadPool(200);

        TimeWindowQuantile quantile = new TimeWindowQuantile(100, 10, 120);
        int index = 0;
        while (index < 100) {
            for (int i = 0; i < 100; i++) {
                int finalI = i;
                Assertions.assertDoesNotThrow(() -> quantile.add(finalI));
                executorService.execute(() -> quantile.add(finalI));
            }
            index++;
            //            try {
            //                Thread.sleep(1);
            //            } catch (InterruptedException e) {
            //                e.printStackTrace();
            //            }
        }

        executorService.shutdown();
    }
}
