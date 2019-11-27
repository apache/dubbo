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
package org.apache.dubbo.event;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ParallelEventDispatcher} Test
 *
 * @since 2.7.5
 */
public class ParallelEventDispatcherTest {

    private EventDispatcher eventDispatcher;

    private AbstractEventListener listener;

    @BeforeEach
    public void init() {
        eventDispatcher = new ParallelEventDispatcher();
        listener = new EchoEventListener();
        eventDispatcher.addEventListener(listener);
    }

    @Test
    public void testDispatchEvent() throws InterruptedException {
        eventDispatcher.dispatch(new EchoEvent("Hello,World"));
        ForkJoinPool.commonPool().awaitTermination(1, TimeUnit.SECONDS);
        // event has been handled
        assertEquals(1, listener.getEventOccurs());
    }

    @AfterAll
    public static void destroy() {
        ForkJoinPool.commonPool().shutdown();
    }

}
