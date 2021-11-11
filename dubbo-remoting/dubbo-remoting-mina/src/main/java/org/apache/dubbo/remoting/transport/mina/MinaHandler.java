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
package org.apache.dubbo.remoting.transport.mina;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.ChannelHandler;

import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;

/**
 * MinaHandler
 */
public class MinaHandler extends IoHandlerAdapter {

    private final URL url;

    private final ChannelHandler handler;

    public MinaHandler(URL url, ChannelHandler handler) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        this.url = url;
        this.handler = handler;
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        MinaChannel channel = MinaChannel.getOrAddChannel(session, url, handler);
        try {
            handler.connected(channel);
        } finally {
            MinaChannel.removeChannelIfDisconnected(session);
        }
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        MinaChannel channel = MinaChannel.getOrAddChannel(session, url, handler);
        try {
            handler.disconnected(channel);
        } finally {
            MinaChannel.removeChannelIfDisconnected(session);
        }
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        MinaChannel channel = MinaChannel.getOrAddChannel(session, url, handler);
        try {
            handler.received(channel, message);
        } finally {
            MinaChannel.removeChannelIfDisconnected(session);
        }
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        MinaChannel channel = MinaChannel.getOrAddChannel(session, url, handler);
        try {
            handler.sent(channel, message);
        } finally {
            MinaChannel.removeChannelIfDisconnected(session);
        }
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        MinaChannel channel = MinaChannel.getOrAddChannel(session, url, handler);
        try {
            handler.caught(channel, cause);
        } finally {
            MinaChannel.removeChannelIfDisconnected(session);
        }
    }

}