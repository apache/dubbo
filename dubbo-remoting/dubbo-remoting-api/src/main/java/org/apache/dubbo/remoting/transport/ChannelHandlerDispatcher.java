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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;

/**
 * ChannelListenerDispatcher
 */
public class ChannelHandlerDispatcher implements ChannelHandler {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ChannelHandlerDispatcher.class);

    private final Collection<ChannelHandler> channelHandlers = new CopyOnWriteArraySet<>();

    public ChannelHandlerDispatcher() {
    }

    public ChannelHandlerDispatcher(ChannelHandler... handlers) {
        // if varargs is used, the type of handlers is ChannelHandler[] and it is not null
        // so we should filter the null object
        this(handlers == null ? null : Arrays.asList(handlers));
    }

    public ChannelHandlerDispatcher(Collection<ChannelHandler> handlers) {
        if (CollectionUtils.isNotEmpty(handlers)) {
            // filter null object
            this.channelHandlers.addAll(handlers.stream().filter(Objects::nonNull).collect(Collectors.toSet()));
        }
    }

    public Collection<ChannelHandler> getChannelHandlers() {
        return channelHandlers;
    }

    public ChannelHandlerDispatcher addChannelHandler(ChannelHandler handler) {
        this.channelHandlers.add(handler);
        return this;
    }

    public ChannelHandlerDispatcher removeChannelHandler(ChannelHandler handler) {
        this.channelHandlers.remove(handler);
        return this;
    }

    @Override
    public void connected(Channel channel) {
        for (ChannelHandler listener : channelHandlers) {
            try {
                listener.connected(channel);
            } catch (Throwable t) {
                logger.error(INTERNAL_ERROR, "unknown error in remoting module", "", t.getMessage(), t);
            }
        }
    }

    @Override
    public void disconnected(Channel channel) {
        for (ChannelHandler listener : channelHandlers) {
            try {
                listener.disconnected(channel);
            } catch (Throwable t) {
                logger.error(INTERNAL_ERROR, "unknown error in remoting module", "", t.getMessage(), t);
            }
        }
    }

    @Override
    public void sent(Channel channel, Object message) {
        for (ChannelHandler listener : channelHandlers) {
            try {
                listener.sent(channel, message);
            } catch (Throwable t) {
                logger.error(INTERNAL_ERROR, "unknown error in remoting module", "", t.getMessage(), t);
            }
        }
    }

    @Override
    public void received(Channel channel, Object message) {
        for (ChannelHandler listener : channelHandlers) {
            try {
                listener.received(channel, message);
            } catch (Throwable t) {
                logger.error(INTERNAL_ERROR, "unknown error in remoting module", "", t.getMessage(), t);
            }
        }
    }

    @Override
    public void caught(Channel channel, Throwable exception) {
        for (ChannelHandler listener : channelHandlers) {
            try {
                listener.caught(channel, exception);
            } catch (Throwable t) {
                logger.error(INTERNAL_ERROR, "unknown error in remoting module", "", t.getMessage(), t);
            }
        }
    }

}
