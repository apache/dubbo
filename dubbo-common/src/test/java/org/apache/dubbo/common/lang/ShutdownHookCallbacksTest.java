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
package org.apache.dubbo.common.lang;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * {@link ShutdownHookCallbacks}
 *
 * @since 2.7.5
 */
public class ShutdownHookCallbacksTest {

    private static final Logger logger = LoggerFactory.getLogger(ShutdownHookCallbacksTest.class);

    private ShutdownHookCallbacks callbacks;

    @BeforeEach
    public void init() {
        callbacks = new ShutdownHookCallbacks();
    }

    @Test
    public void testSingleton() {
        assertNotNull(callbacks);
    }

    @Test
    public void testCallback() {
        callbacks.callback();
        DefaultShutdownHookCallback callback = (DefaultShutdownHookCallback) callbacks.getCallbacks().iterator().next();
        assertTrue(callback.isExecuted());
    }

    @Test
    public void testCallbackSorted() {
        for (int i = 0; i < 5; i++) {
            final int index = i;
            logger.info("addCallback:" + index);
            callbacks.addCallback(new ShutdownHookCallback() {
                @Override
                public void callback() {
                    logger.info("callBack:" + index);
                }

                @Override
                public int getPriority() {
                    return index;
                }
            });
            final int anotherIndex = i + 5;
            logger.info("addCallback:" + anotherIndex);
            callbacks.addCallback(new ShutdownHookCallback() {
                @Override
                public void callback() {
                    logger.info("callBack:" + anotherIndex);
                }

                @Override
                public int getPriority() {
                    return anotherIndex;
                }
            });
        }
        Collection<ShutdownHookCallback> callbacks = this.callbacks.getCallbacks();
        LinkedList<ShutdownHookCallback> listToSort = new LinkedList<>(callbacks);
        Collections.sort(listToSort);
        assertEquals(callbacks, listToSort);
    }

    @Test
    public void testConcurrentCallback() throws InterruptedException {
        int sizeBefore = callbacks.getCallbacks().size();
        Thread addCallbackThread = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
                final int index = i;
                logger.info("addCallback:" + index);
                callbacks.addCallback(() -> logger.info("callBack:" + index));
            }
        });

        addCallbackThread.start();
        TimeUnit.MILLISECONDS.sleep(500);
        assertDoesNotThrow(callbacks::callback);
        addCallbackThread.join();
        assertEquals(callbacks.getCallbacks().size(), sizeBefore + 10);
    }

    @AfterEach
    public void destroy() {
        callbacks.clear();
        assertTrue(callbacks.getCallbacks().isEmpty());
    }
}
