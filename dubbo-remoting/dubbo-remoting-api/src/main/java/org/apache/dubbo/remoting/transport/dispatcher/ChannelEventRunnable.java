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
package org.apache.dubbo.remoting.transport.dispatcher;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;

public class ChannelEventRunnable implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ChannelEventRunnable.class);

    private final ChannelHandler handler;
    private final Channel channel;
    private final ChannelState state;
    private final Throwable exception;
    private final Object message;

    public ChannelEventRunnable(Channel channel, ChannelHandler handler, ChannelState state) {
        this(channel, handler, state, null);
    }

    public ChannelEventRunnable(Channel channel, ChannelHandler handler, ChannelState state, Object message) {
        this(channel, handler, state, message, null);
    }

    public ChannelEventRunnable(Channel channel, ChannelHandler handler, ChannelState state, Throwable t) {
        this(channel, handler, state, null, t);
    }

    public ChannelEventRunnable(Channel channel, ChannelHandler handler, ChannelState state, Object message, Throwable exception) {
        this.channel = channel;
        this.handler = handler;
        this.state = state;
        this.message = message;
        this.exception = exception;
    }

    @Override
    public void run() {
        this.state.handle(this.channel, this.handler, this.state, this.message, this.exception);
    }

    /**
     * ChannelState
     *
     *
     */
    public enum ChannelState {

        /**
         * CONNECTED
         */
        CONNECTED((channel, handler, state, message, exception) -> {
            try {
                handler.connected(channel);
            } catch (Exception e) {
                logger.warn("ChannelEventRunnable handle " + state + " operation error, channel is " + channel, e);
            }
        }),

        /**
         * DISCONNECTED
         */
        DISCONNECTED((channel, handler, state, message, exception) -> {
            try {
                handler.disconnected(channel);
            } catch (Exception e) {
                logger.warn("ChannelEventRunnable handle " + state + " operation error, channel is " + channel, e);
            }
        }),

        /**
         * SENT
         */
        SENT((channel, handler, state, message, exception) -> {
            try {
                handler.sent(channel, message);
            } catch (Exception e) {
                logger.warn("ChannelEventRunnable handle " + state + " operation error, channel is " + channel
                        + ", message is " + message, e);
            }
        }),

        /**
         * RECEIVED
         */
        RECEIVED((channel, handler, state, message, exception) -> {
            try {
                handler.received(channel, message);
            } catch (Exception e) {
                logger.warn("ChannelEventRunnable handle " + state + " operation error, channel is " + channel
                        + ", message is " + message, e);
            }
        }),

        /**
         * CAUGHT
         */
        CAUGHT((channel, handler, state, message, exception) -> {
            try {
                handler.caught(channel, exception);
            } catch (Exception e) {
                logger.warn("ChannelEventRunnable handle " + state + " operation error, channel is " + channel
                        + ", message is: " + message + ", exception is " + exception, e);
            }
        });

        private final HandlerFunction function;

        ChannelState(HandlerFunction handleFunction) {
            this.function = handleFunction;
        }

        private void handle(Channel channel, ChannelHandler handler, ChannelState state, Object message, Throwable exception) {
            function.handle(channel, handler, state, message, exception);
        }
    }

    private interface HandlerFunction {
        void handle(Channel channel, ChannelHandler handler, ChannelState state, Object message, Throwable exception);
    }

}
