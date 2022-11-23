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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TimeWindowQuantileTest {

    @Test
    void test() throws Exception {
        TimeWindowQuantile quantile = new TimeWindowQuantile(100, 12, 1);
        for (int i = 1; i <= 100; i++) {
            quantile.add(i);
        }

        Assertions.assertEquals(quantile.quantile(0.01), 2);
        Assertions.assertEquals(quantile.quantile(0.99), 100);
        Thread.sleep(1000);
        Assertions.assertEquals(quantile.quantile(0.99), Double.NaN);
    }
}