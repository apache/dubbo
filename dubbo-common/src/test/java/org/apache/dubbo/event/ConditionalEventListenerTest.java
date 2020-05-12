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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link ConditionalEventListener} test
 *
 * @since 2.7.5
 */
public class ConditionalEventListenerTest {

    private final EventDispatcher eventDispatcher = EventDispatcher.getDefaultExtension();

    @BeforeEach
    public void init() {
        eventDispatcher.removeAllEventListeners();
    }

    @Test
    public void testOnEvent() {

        OnlyHelloWorldEventListener listener = new OnlyHelloWorldEventListener();

        eventDispatcher.addEventListener(listener);

        eventDispatcher.dispatch(new EchoEvent("1"));

        assertNull(listener.getSource());

        eventDispatcher.dispatch(new EchoEvent("Hello,World"));

        assertEquals("Hello,World", listener.getSource());

        // fix EventDispatcherTest.testDefaultMethods may contain OnlyHelloWorldEventListener
        // ( ConditionalEventListenerTest and EventDispatcherTest are running together in one suite case )
        eventDispatcher.removeAllEventListeners();
    }

    static class OnlyHelloWorldEventListener implements ConditionalEventListener<EchoEvent> {

        private String source;

        @Override
        public boolean accept(EchoEvent event) {
            return "Hello,World".equals(event.getSource());
        }

        @Override
        public void onEvent(EchoEvent event) {
            source = (String) event.getSource();
        }

        public String getSource() {
            return source;
        }
    }
}
