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
package org.apache.dubbo.common.threadpool.support.loom;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future.State;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VirtualScheduledFutureTest {
    @Test
    void testGetDelay() throws Exception {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");
        ScheduledFuture<?> scheduledFuture = scheduledVirtualExecutorService.schedule(() -> {}, 60, TimeUnit.SECONDS);
        assertTrue(scheduledFuture instanceof VirtualScheduledFuture);
        assertTrue(scheduledFuture.getDelay(TimeUnit.MILLISECONDS) > 0);
    }

    @Test
    void testCancel() throws Exception {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");
        ScheduledFuture<?> scheduledFuture = scheduledVirtualExecutorService.schedule(() -> {}, 60, TimeUnit.SECONDS);
        assertTrue(scheduledFuture instanceof VirtualScheduledFuture);
        assertTrue(scheduledFuture.cancel(true));
        assertTrue(scheduledFuture.isCancelled());
    }

    @Test
    void testIsDone() throws Exception {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");
        ScheduledFuture<?> scheduledFuture =
                scheduledVirtualExecutorService.schedule(() -> {}, 10, TimeUnit.MILLISECONDS);
        assertTrue(scheduledFuture instanceof VirtualScheduledFuture);
        scheduledFuture.get(); // wait for the task to complete
        assertTrue(scheduledFuture.isDone());
    }

    @Test
    void testGet() throws Exception {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");
        ScheduledFuture<String> scheduledFuture =
                scheduledVirtualExecutorService.schedule(() -> "Hello, World!", 1, TimeUnit.MILLISECONDS);
        assertTrue(scheduledFuture instanceof VirtualScheduledFuture);
        assertEquals("Hello, World!", scheduledFuture.get());
    }

    @Test
    void testCompareTo() {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");
        ScheduledFuture<?> scheduledFuture1 = scheduledVirtualExecutorService.schedule(() -> {}, 1, TimeUnit.SECONDS);
        ScheduledFuture<?> scheduledFuture2 = scheduledVirtualExecutorService.schedule(() -> {}, 2, TimeUnit.SECONDS);
        assertTrue(scheduledFuture1 instanceof VirtualScheduledFuture);
        assertTrue(scheduledFuture2 instanceof VirtualScheduledFuture);
        assertThrows(UnsupportedOperationException.class, () -> scheduledFuture1.compareTo(scheduledFuture2));
    }

    @Test
    void testResultNow() throws Exception {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");
        AtomicBoolean executed = new AtomicBoolean();
        ScheduledFuture<String> scheduledFuture = scheduledVirtualExecutorService.schedule(
                () -> {
                    while (!executed.get()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    return "Hello, World!";
                },
                1,
                TimeUnit.MILLISECONDS);
        assertTrue(scheduledFuture instanceof VirtualScheduledFuture);
        assertThrows(IllegalStateException.class, scheduledFuture::resultNow);
        executed.set(true);
        assertEquals("Hello, World!", scheduledFuture.get());
        assertEquals("Hello, World!", scheduledFuture.resultNow());
    }

    @Test
    void testExceptionNow() throws Exception {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");
        RuntimeException runtimeException = new RuntimeException("test");
        AtomicBoolean executed = new AtomicBoolean();
        ScheduledFuture<String> scheduledFuture = scheduledVirtualExecutorService.schedule(
                () -> {
                    while (!executed.get()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    throw runtimeException;
                },
                1,
                TimeUnit.MILLISECONDS);
        assertTrue(scheduledFuture instanceof VirtualScheduledFuture);
        assertThrows(IllegalStateException.class, scheduledFuture::exceptionNow);
        executed.set(true);
        assertThrows(ExecutionException.class, scheduledFuture::get);
        assertEquals(runtimeException, scheduledFuture.exceptionNow());
    }

    @Test
    void testState() throws Exception {
        ScheduledVirtualExecutorService scheduledVirtualExecutorService = new ScheduledVirtualExecutorService("test");
        AtomicBoolean executed = new AtomicBoolean();
        ScheduledFuture<String> scheduledFuture = scheduledVirtualExecutorService.schedule(
                () -> {
                    while (!executed.get()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    return "Hello, World!";
                },
                1,
                TimeUnit.MILLISECONDS);
        assertTrue(scheduledFuture instanceof VirtualScheduledFuture);
        assertEquals(State.RUNNING, scheduledFuture.state());
        executed.set(true);
        assertEquals("Hello, World!", scheduledFuture.get());
        assertEquals(State.SUCCESS, scheduledFuture.state());
    }
}
