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
package org.apache.dubbo.spring.boot.context.event;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * {@link AwaitingNonWebApplicationListener} Test
 */
class AwaitingNonWebApplicationListenerTest {

    //    @Test
    //    void init() {
    //        AtomicBoolean awaited = AwaitingNonWebApplicationListener.getAwaited();
    //        awaited.set(false);
    //    }
    //
    //    @Test
    //    void testSingleContextNonWebApplication() {
    //        new SpringApplicationBuilder(Object.class)
    //                .web(false)
    //                .run()
    //                .close();
    //
    //        ShutdownHookCallbacks.INSTANCE.addCallback(() -> {
    //            AtomicBoolean awaited = AwaitingNonWebApplicationListener.getAwaited();
    //            assertTrue(awaited.get());
    //            System.out.println("Callback...");
    //        });
    //    }
    //
    @Test
    void testMultipleContextNonWebApplication() {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(Object.class)
                .parent(Object.class)
                .properties("spring.main.web-application-type=none")
                .run();

        AwaitingNonWebApplicationListener listener = new AwaitingNonWebApplicationListener();
        AtomicBoolean awaited = listener.getAwaited();
        assertFalse(awaited.get());
        context.close();
    }
}
