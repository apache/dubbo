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

package org.apache.dubbo.rpc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

class TimeoutCountDownTest {

    @Test
    void testTimeoutCountDown() throws InterruptedException {
        TimeoutCountDown timeoutCountDown = TimeoutCountDown.newCountDown(5, TimeUnit.SECONDS);
        Assertions.assertEquals(5 * 1000, timeoutCountDown.getTimeoutInMilli());
        Assertions.assertFalse(timeoutCountDown.isExpired());
        Assertions.assertTrue(timeoutCountDown.timeRemaining(TimeUnit.SECONDS) > 0);
        Assertions.assertTrue(timeoutCountDown.elapsedMillis() < TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS));

        Thread.sleep(6 * 1000);

        Assertions.assertTrue(timeoutCountDown.isExpired());
        Assertions.assertTrue(timeoutCountDown.timeRemaining(TimeUnit.SECONDS) <= 0);
        Assertions.assertTrue(timeoutCountDown.elapsedMillis() > TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS));
    }
}
