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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link GenericEventListener} Test
 *
 * @since 2.7.5
 */
public class GenericEventListenerTest {

    private EventDispatcher eventDispatcher;

    private MyGenericEventListener listener;

    @BeforeEach
    public void init() {
        this.listener = new MyGenericEventListener();
        this.eventDispatcher = EventDispatcher.getDefaultExtension();
        this.eventDispatcher.addEventListener(listener);
    }

    @AfterEach
    public void destroy() {
        this.eventDispatcher.removeAllEventListeners();
    }

    @Test
    public void testOnEvent() {
        String value = "Hello,World";
        EchoEvent echoEvent = new EchoEvent(value);
        eventDispatcher.dispatch(echoEvent);
        assertEquals(echoEvent, listener.getEchoEvent());
        assertEquals(value, listener.getEchoEvent().getSource());
    }

    class MyGenericEventListener extends GenericEventListener {

        private EchoEvent echoEvent;

        public void onEvent(EchoEvent echoEvent) {
            this.echoEvent = echoEvent;
        }

        public void event(EchoEvent echoEvent) {
            assertEquals("Hello,World", echoEvent.getSource());
        }

        public void event(EchoEvent echoEvent, Object arg) {
            this.echoEvent = echoEvent;
        }

        public EchoEvent getEchoEvent() {
            return echoEvent;
        }
    }
}
