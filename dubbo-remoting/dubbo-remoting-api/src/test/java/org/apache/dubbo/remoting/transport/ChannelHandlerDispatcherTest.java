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
import org.apache.dubbo.remoting.RemotingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

class ChannelHandlerDispatcherTest {

    @Test
    void test() {
        ChannelHandlerDispatcher channelHandlerDispatcher = new ChannelHandlerDispatcher();
        MockChannelHandler channelHandler1 = new MockChannelHandler();
        MockChannelHandler channelHandler2 = new MockChannelHandler();
        channelHandlerDispatcher.addChannelHandler(channelHandler1);
        channelHandlerDispatcher.addChannelHandler(channelHandler2);
        Collection<ChannelHandler> channelHandlers = channelHandlerDispatcher.getChannelHandlers();
        Assertions.assertTrue(channelHandlers.contains(channelHandler1));
        Assertions.assertTrue(channelHandlers.contains(channelHandler2));

        Channel channel = Mockito.mock(Channel.class);
        channelHandlerDispatcher.sent(channel, "test");
        channelHandlerDispatcher.connected(channel);
        channelHandlerDispatcher.disconnected(channel);
        channelHandlerDispatcher.caught(channel, null);
        channelHandlerDispatcher.received(channel, "test");

        Assertions.assertEquals(MockChannelHandler.getSentCount(), 2);
        Assertions.assertEquals(MockChannelHandler.getConnectedCount(), 2);
        Assertions.assertEquals(MockChannelHandler.getDisconnectedCount(), 2);
        Assertions.assertEquals(MockChannelHandler.getCaughtCount(), 2);
        Assertions.assertEquals(MockChannelHandler.getReceivedCount(), 2);

        channelHandlerDispatcher = channelHandlerDispatcher.removeChannelHandler(channelHandler1);
        Assertions.assertFalse(channelHandlerDispatcher.getChannelHandlers().contains(channelHandler1));

    }

    @Test
    void constructorNullObjectTest() {
        ChannelHandlerDispatcher channelHandlerDispatcher = new ChannelHandlerDispatcher(null, null);
        Assertions.assertEquals(0, channelHandlerDispatcher.getChannelHandlers().size());
        ChannelHandlerDispatcher channelHandlerDispatcher1 = new ChannelHandlerDispatcher((MockChannelHandler) null);
        Assertions.assertEquals(0, channelHandlerDispatcher1.getChannelHandlers().size());
        ChannelHandlerDispatcher channelHandlerDispatcher2 = new ChannelHandlerDispatcher(null, new MockChannelHandler());
        Assertions.assertEquals(1, channelHandlerDispatcher2.getChannelHandlers().size());
        ChannelHandlerDispatcher channelHandlerDispatcher3 = new ChannelHandlerDispatcher(Collections.singleton(new MockChannelHandler()));
        Assertions.assertEquals(1, channelHandlerDispatcher3.getChannelHandlers().size());
        Collection<ChannelHandler> mockChannelHandlers = new HashSet<>();
        mockChannelHandlers.add(new MockChannelHandler());
        mockChannelHandlers.add(null);
        ChannelHandlerDispatcher channelHandlerDispatcher4 = new ChannelHandlerDispatcher(mockChannelHandlers);
        Assertions.assertEquals(1, channelHandlerDispatcher4.getChannelHandlers().size());
    }

}

class MockChannelHandler extends ChannelHandlerAdapter {
    private static int sentCount = 0;
    private static int connectedCount = 0;
    private static int disconnectedCount = 0;
    private static int receivedCount = 0;
    private static int caughtCount = 0;

    @Override
    public void connected(Channel channel) throws RemotingException {
        connectedCount++;
        super.connected(channel);
    }

    @Override
    public void disconnected(Channel channel) throws RemotingException {
        disconnectedCount++;
        super.disconnected(channel);
    }

    @Override
    public void sent(Channel channel, Object message) throws RemotingException {
        sentCount++;
        super.sent(channel, message);
    }

    @Override
    public void received(Channel channel, Object message) throws RemotingException {
        receivedCount++;
        super.received(channel, message);
    }

    @Override
    public void caught(Channel channel, Throwable exception) throws RemotingException {
        caughtCount++;
        super.caught(channel, exception);
    }

    public static int getSentCount() {
        return sentCount;
    }

    public static int getConnectedCount() {
        return connectedCount;
    }

    public static int getDisconnectedCount() {
        return disconnectedCount;
    }

    public static int getReceivedCount() {
        return receivedCount;
    }

    public static int getCaughtCount() {
        return caughtCount;
    }
}
