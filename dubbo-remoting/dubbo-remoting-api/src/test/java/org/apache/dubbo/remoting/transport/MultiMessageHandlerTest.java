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
package org.apache.dubbo.remoting.transport;

import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.exchange.support.MultiMessage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * {@link MultiMessageHandler}
 */
class MultiMessageHandlerTest {

    @Test
    void test() throws Exception {
        ChannelHandler handler = Mockito.mock(ChannelHandler.class);
        Channel channel = Mockito.mock(Channel.class);
        MultiMessageHandler multiMessageHandler = new MultiMessageHandler(handler);

        MultiMessage multiMessage = MultiMessage.createFromArray("test1", "test2");
        multiMessageHandler.received(channel, multiMessage);
        // verify
        ArgumentCaptor<Channel> channelArgumentCaptor = ArgumentCaptor.forClass(Channel.class);
        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(handler, Mockito.times(2)).received(channelArgumentCaptor.capture(), objectArgumentCaptor.capture());
        Assertions.assertEquals(objectArgumentCaptor.getAllValues().get(0), "test1");
        Assertions.assertEquals(objectArgumentCaptor.getAllValues().get(1), "test2");
        Assertions.assertEquals(channelArgumentCaptor.getValue(), channel);

        Object obj = new Object();
        multiMessageHandler.received(channel, obj);
        // verify
        Mockito.verify(handler, Mockito.times(3)).received(channelArgumentCaptor.capture(), objectArgumentCaptor.capture());
        Assertions.assertEquals(objectArgumentCaptor.getValue(), obj);
        Assertions.assertEquals(channelArgumentCaptor.getValue(), channel);

        RuntimeException runtimeException = new RuntimeException();
        Mockito.doThrow(runtimeException).when(handler).received(Mockito.any(), Mockito.any());
        multiMessageHandler.received(channel, multiMessage);
        // verify
        ArgumentCaptor<Throwable> throwableArgumentCaptor = ArgumentCaptor.forClass(Throwable.class);
        Mockito.verify(handler, Mockito.times(2)).caught(channelArgumentCaptor.capture(), throwableArgumentCaptor.capture());
        Assertions.assertEquals(throwableArgumentCaptor.getAllValues().get(0), runtimeException);
        Assertions.assertEquals(throwableArgumentCaptor.getAllValues().get(1), runtimeException);
        Assertions.assertEquals(channelArgumentCaptor.getValue(), channel);

    }
}
