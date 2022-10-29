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

import org.apache.dubbo.common.lang.ShutdownHookCallbacks;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link AwaitingNonWebApplicationListener} Test
 */
public class AwaitingNonWebApplicationListenerTest {

    @Before
    public void before() {
        ApplicationModel.reset();
    }

    @After
    public void after() {
        ApplicationModel.reset();
    }

    @Test
    public void init() {
        AtomicBoolean awaited = AwaitingNonWebApplicationListener.getAwaited();
        awaited.set(false);
    }

    @Test
    public void testSingleContextNonWebApplication() {
        new SpringApplicationBuilder(Object.class)
                .web(false)
                .run()
                .close();

        ShutdownHookCallbacks.INSTANCE.addCallback(() -> {
            AtomicBoolean awaited = AwaitingNonWebApplicationListener.getAwaited();
            Assert.assertTrue(awaited.get());
            System.out.println("Callback...");
        });
    }
//
//    @Test
//    public void testMultipleContextNonWebApplication() {
//        new SpringApplicationBuilder(Object.class)
//                .parent(Object.class)
//                .web(false)
//                .run().close();
//        AtomicBoolean awaited = AwaitingNonWebApplicationListener.getAwaited();
//        Assert.assertFalse(awaited.get());
//    }

}
