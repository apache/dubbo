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

package org.apache.dubbo.common.timer;

import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class HashedWheelTimerTest {

    private class PrintTask implements TimerTask {

        @Override
        public void run(Timeout timeout) {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            System.out.println("task :" + LocalDateTime.now().format(formatter));
        }
    }

    @Test
    public void newTimeout() throws InterruptedException {
        final Timer timer = newTimer();
        for (int i = 0; i < 10; i++) {
            timer.newTimeout(new PrintTask(), 1, TimeUnit.SECONDS);
            Thread.sleep(1000);
        }
        Thread.sleep(5000);
    }

    @Test
    public void stop() throws InterruptedException {
        final Timer timer = newTimer();
        for (int i = 0; i < 10; i++) {
            timer.newTimeout(new PrintTask(), 5, TimeUnit.SECONDS);
            Thread.sleep(100);
        }
        //stop timer
        timer.stop();

        try {
            //this will throw a exception
            timer.newTimeout(new PrintTask(), 5, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Timer newTimer() {
        return new HashedWheelTimer(
                new NamedThreadFactory("dubbo-future-timeout", true),
                100,
                TimeUnit.MILLISECONDS);
    }
}