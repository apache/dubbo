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
package org.apache.dubbo.remoting.transport.grizzly;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import java.io.IOException;

/**
 * GrizzlyHandler
 */
public class GrizzlyHandler extends BaseFilter {

    private static final Logger logger = LoggerFactory.getLogger(GrizzlyHandler.class);

    private final URL url;

    private final ChannelHandler handler;

    public GrizzlyHandler(URL url, ChannelHandler handler) {
        this.url = url;
        this.handler = handler;
    }

    @Override
    public NextAction handleConnect(FilterChainContext ctx) throws IOException {
        Connection<?> connection = ctx.getConnection();
        GrizzlyChannel channel = GrizzlyChannel.getOrAddChannel(connection, url, handler);
        try {
            handler.connected(channel);
        } catch (RemotingException e) {
            throw new IOException(StringUtils.toString(e));
        } finally {
            GrizzlyChannel.removeChannelIfDisconnected(connection);
        }
        return ctx.getInvokeAction();
    }

    @Override
    public NextAction handleClose(FilterChainContext ctx) throws IOException {
        Connection<?> connection = ctx.getConnection();
        GrizzlyChannel channel = GrizzlyChannel.getOrAddChannel(connection, url, handler);
        try {
            handler.disconnected(channel);
        } catch (RemotingException e) {
            throw new IOException(StringUtils.toString(e));
        } finally {
            GrizzlyChannel.removeChannelIfDisconnected(connection);
        }
        return ctx.getInvokeAction();
    }

    @Override
    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        Connection<?> connection = ctx.getConnection();
        GrizzlyChannel channel = GrizzlyChannel.getOrAddChannel(connection, url, handler);
        try {
            handler.received(channel, ctx.getMessage());
        } catch (RemotingException e) {
            throw new IOException(StringUtils.toString(e));
        } finally {
            GrizzlyChannel.removeChannelIfDisconnected(connection);
        }
        return ctx.getInvokeAction();
    }

    @Override
    public NextAction handleWrite(FilterChainContext ctx) throws IOException {
        Connection<?> connection = ctx.getConnection();
        GrizzlyChannel channel = GrizzlyChannel.getOrAddChannel(connection, url, handler);
        try {
            handler.sent(channel, ctx.getMessage());
        } catch (RemotingException e) {
            throw new IOException(StringUtils.toString(e));
        } finally {
            GrizzlyChannel.removeChannelIfDisconnected(connection);
        }
        return ctx.getInvokeAction();
    }

    @Override
    public void exceptionOccurred(FilterChainContext ctx, Throwable error) {
        Connection<?> connection = ctx.getConnection();
        GrizzlyChannel channel = GrizzlyChannel.getOrAddChannel(connection, url, handler);
        try {
            handler.caught(channel, error);
        } catch (RemotingException e) {
            logger.error("RemotingException on channel " + channel, e);
        } finally {
            GrizzlyChannel.removeChannelIfDisconnected(connection);
        }
    }

}