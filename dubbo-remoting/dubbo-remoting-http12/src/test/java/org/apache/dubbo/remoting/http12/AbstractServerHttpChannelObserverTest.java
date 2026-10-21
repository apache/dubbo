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
package org.apache.dubbo.remoting.http12;

import org.apache.dubbo.remoting.http12.h1.Http1ServerChannelObserver;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class AbstractServerHttpChannelObserverTest {

    @Test
    void transportCloseDefersTerminationUntilResponseCompletes() {
        AtomicInteger terminationCount = new AtomicInteger();
        Http1ServerChannelObserver observer = new Http1ServerChannelObserver(mock(HttpChannel.class));
        observer.setTerminationHandler(terminationCount::incrementAndGet);

        observer.close();
        assertEquals(0, terminationCount.get());

        observer.onCompleted();
        observer.onCompleted();
        observer.onError(new RuntimeException("ignored after completion"));
        observer.close();
        assertEquals(1, terminationCount.get());
    }

    @Test
    void terminationHandlesMissingAndFailingHandlers() {
        Http1ServerChannelObserver observerWithoutHandler = new Http1ServerChannelObserver(mock(HttpChannel.class));
        assertDoesNotThrow(() -> observerWithoutHandler.onCompleted());

        Http1ServerChannelObserver observerWithFailingHandler = new Http1ServerChannelObserver(mock(HttpChannel.class));
        observerWithFailingHandler.setTerminationHandler(() -> {
            throw new RuntimeException("cleanup failed");
        });

        assertDoesNotThrow(() -> observerWithFailingHandler.onCompleted());
    }
}
